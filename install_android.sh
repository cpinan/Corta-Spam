#!/bin/bash
# install_android.sh — Build and install a KMP Compose Android app to any connected device.
# Usage:
#   ./install_android.sh                    # Build + install + launch (default)
#   ./install_android.sh --no-launch        # Build + install only
#   ./install_android.sh --release          # Build release variant
#   ./install_android.sh --device <serial>  # Target specific device
#
# Works with any Gradle-based KMP/Android project. No hardcoded paths.

set -euo pipefail

LAUNCH=true
VARIANT="Debug"
DEVICE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-launch)
            LAUNCH=false
            shift
            ;;
        --release)
            VARIANT="Release"
            shift
            ;;
        --device)
            DEVICE="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [--no-launch] [--release] [--device <serial>]"
            echo ""
            echo "  Build and install a KMP Compose Android app to a connected device."
            echo ""
            echo "  --no-launch    Install only, do not launch"
            echo "  --release      Build the release variant"
            echo "  --device SERIAL Target a specific device by adb serial"
            echo ""
            echo "  Requires: adb (Android Debug Bridge), Gradle wrapper (./gradlew)"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# — Colors —
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# — Check prerequisites —
if ! command -v adb &>/dev/null; then
    echo -e "${RED}Error: adb not found in PATH. Install Android SDK platform-tools.${NC}"
    exit 1
fi

if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: ./gradlew not found. Run from the project root.${NC}"
    exit 1
fi

# — Device detection —
if [ -n "$DEVICE" ]; then
    if ! adb -s "$DEVICE" get-state &>/dev/null; then
        echo -e "${RED}Error: device '$DEVICE' not available.${NC}"
        echo "Connected devices:"
        adb devices -l | grep -v "List of devices" | grep -v "^$" || echo "  (none)"
        exit 1
    fi
    echo -e "${YELLOW}Target device: $DEVICE${NC}"
else
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
    DEVICE_COUNT=$(echo "$DEVICES" | grep -c . || true)

    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}Error: no devices connected. Plug in a device or start an emulator.${NC}"
        exit 1
    fi

    if [ "$DEVICE_COUNT" -gt 1 ]; then
        echo -e "${YELLOW}Multiple devices found:${NC}"
        echo "$DEVICES" | nl
        echo ""
        echo "Use --device <serial> to pick one, or disconnect extras."
        exit 1
    fi

    DEVICE=$(echo "$DEVICES" | head -1)
    echo -e "${YELLOW}Device: $DEVICE${NC}"
fi

VARIANT_LOWER=$(echo "$VARIANT" | tr '[:upper:]' '[:lower:]')

# — Build —
# Always invoke Gradle rather than hand-rolling a staleness check: Gradle's own
# incremental build already no-ops (near-instant, near-silent with -q) when
# nothing changed, and correctly rebuilds when any source/resource/build file
# did — a mtime comparison against ./gradlew (which itself never changes)
# cannot detect that and would silently install stale code.
echo -e "${YELLOW}Building $VARIANT APK...${NC}"
./gradlew :androidApp:assemble${VARIANT} -q
# Matched by directory, not by filename. `archivesName` in androidApp/build.gradle.kts names
# outputs corta-spam-<versionName>-<versionCode>-<variant>.apk so a file sitting in Downloads
# says which release it is -- this script still looked for androidApp-debug.apk and had been
# failing with "APK not found" ever since. Globbing the variant directory survives the next
# version bump too, which a hardcoded name would not.
APK=$(find "androidApp/build/outputs/apk/${VARIANT_LOWER}" -name '*.apk' 2>/dev/null | head -1)
if [ -z "$APK" ]; then
    echo -e "${RED}Error: APK not found after build.${NC}"
    exit 1
fi
echo -e "${YELLOW}APK: $(basename "$APK")${NC}"

# — Install —
echo -e "${YELLOW}Installing...${NC}"
adb -s "$DEVICE" install -r "$APK" 2>&1

# — Launch —
if [ "$LAUNCH" = true ]; then
    echo -e "${YELLOW}Launching...${NC}"
    # Read both from the built APK rather than hardcoding: applicationId (org.carlospinan.cortaspam)
    # and the activity's own package (org.carlospinan.bloqueador.app, the module namespace) are
    # deliberately different, so "<applicationId>/.MainActivity" does not resolve.
    adb -s "$DEVICE" shell am start -n org.carlospinan.cortaspam/org.carlospinan.bloqueador.app.MainActivity 2>&1
fi

echo -e "${GREEN}Done.${NC}"
