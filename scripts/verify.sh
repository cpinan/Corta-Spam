#!/usr/bin/env bash
# verify.sh — the checks this project expects to be green before a commit.
#
# There is one copy of this sequence so the skills, CI and a human all run the same thing.
# Historically each lived in a different place and drifted: verifySqlDelightMigration was in no
# one's habit for months, and Android Lint was in nobody's at all because it could not run.
#
# Usage:
#   ./scripts/verify.sh              # everything (default)
#   ./scripts/verify.sh --fast       # skip iOS and release; for a tight edit loop
#   ./scripts/verify.sh --release    # everything plus the R8/minified build
set -euo pipefail
cd "$(dirname "$0")/.."

FAST=false
RELEASE=false
for arg in "$@"; do
  case "$arg" in
    --fast)    FAST=true ;;
    --release) RELEASE=true ;;
    --help|-h) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "unknown flag: $arg" >&2; exit 2 ;;
  esac
done

TASKS=(
  :shared:compileDebugKotlinAndroid
  :androidApp:compileDebugKotlin
  ktlintCheck
  :shared:testDebugUnitTest
  :androidApp:testDebugUnitTest
  # Not in the build/check graph, so nothing else triggers it. A schema/query mismatch is
  # invisible until this runs, and it is wired into CI for the same reason.
  :shared:verifySqlDelightMigration
  # Only usable since AGP 8.13; on 8.7.3 every lint task died on Kotlin metadata it could not
  # read. Catches what ktlint never looks at: permission guards, dead SDK_INT branches,
  # locale-specific plural forms.
  :androidApp:lintDebug
  :shared:lintDebug
)

if [ "$FAST" = false ]; then
  # Non-optional for anything under shared/src/commonMain. commonMain code that only ever ran on
  # Android has compiled fine here and broken on iOS more than once.
  TASKS+=(:shared:compileKotlinIosSimulatorArm64)
  # commonTest as well as commonMain, because CI runs :shared:iosSimulatorArm64Test and this
  # script did not compile a single line of it. Kotlin/Native rejects characters the JVM accepts
  # in a backticked test name -- a comma is enough -- so three tests that had passed on the JVM
  # for months failed the iOS job the first time it reached them, with nothing local to catch it.
  TASKS+=(:shared:compileTestKotlinIosSimulatorArm64)
fi

if [ "$RELEASE" = true ]; then
  # R8 is shrink-sensitive around serializers and the Telecom entry points named in the manifest.
  TASKS+=(:androidApp:assembleRelease)
fi

echo "==> ./gradlew ${TASKS[*]}"
./gradlew "${TASKS[@]}" --console=plain

echo
echo "All green."
[ "$FAST" = true ] && echo "NOTE: --fast skipped iOS. Run without it before committing a commonMain change."
exit 0
