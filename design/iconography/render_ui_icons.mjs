#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);

let sharp;
try {
  sharp = require("sharp");
} catch {
  console.error(
    "Missing dependency: sharp. Install it with `npm install --no-save sharp` " +
      "or expose an existing installation through NODE_PATH.",
  );
  process.exit(1);
}

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDirectory, "../..");
const sourceDirectory = path.join(projectRoot, "design/iconography/ui");
const outputDirectory = path.join(
  projectRoot,
  "shared/src/commonMain/composeResources/drawable",
);

const iconNames = [
  "ic_home",
  "ic_keypad",
  "ic_call_log",
  "ic_block_lists",
  "ic_settings",
  "ic_blocked_number",
  "ic_allowlist",
  "ic_patterns",
  "ic_countries",
  "ic_quiet_hours",
  "ic_autoresponder",
  "ic_spam_provider",
  "ic_backup",
  "ic_stats",
  "ic_blocking",
  "ic_contacts",
  "ic_default_action",
  "ic_privacy",
  "ic_brand_app",
  "ic_unknown_call",
  "ic_delete",
  "ic_action_rules",
];

const monochromeRootAttributes = [
  'viewBox="0 0 24 24"',
  'fill="none"',
  'stroke="#000"',
  'stroke-width="1.8"',
  'stroke-linecap="round"',
  'stroke-linejoin="round"',
];

async function renderIcon(iconName) {
  const sourcePath = path.join(sourceDirectory, `${iconName}.svg`);
  const outputPath = path.join(outputDirectory, `${iconName}.png`);
  const source = await fs.readFile(sourcePath);
  const sourceText = source.toString("utf8");

  if (iconName !== "ic_brand_app") {
    const missingAttributes = monochromeRootAttributes.filter(
      (attribute) => !sourceText.includes(attribute),
    );
    if (missingAttributes.length > 0) {
      throw new Error(
        `${iconName}.svg is missing required root attributes: ${missingAttributes.join(", ")}`,
      );
    }
  }

  // A 24-unit SVG rendered at 384 DPI produces 96 physical pixels directly.
  // No background is supplied, so untouched pixels remain transparent.
  await sharp(source, { density: 384 })
    .resize(96, 96, { fit: "fill" })
    .ensureAlpha()
    .png({ compressionLevel: 9, adaptiveFiltering: true })
    .toFile(outputPath);
}

async function validateIcon(iconName) {
  const outputPath = path.join(outputDirectory, `${iconName}.png`);
  const image = sharp(outputPath);
  const metadata = await image.metadata();

  if (metadata.width !== 96 || metadata.height !== 96) {
    throw new Error(
      `${iconName}.png is ${metadata.width}x${metadata.height}; expected 96x96`,
    );
  }
  if (metadata.channels !== 4 || metadata.hasAlpha !== true) {
    throw new Error(
      `${iconName}.png is not RGBA with alpha (channels=${metadata.channels}, hasAlpha=${metadata.hasAlpha})`,
    );
  }

  const { data, info } = await image.ensureAlpha().raw().toBuffer({
    resolveWithObject: true,
  });
  let transparentPixels = 0;
  let visiblePixels = 0;
  let opaquePixels = 0;
  let nonBlackVisiblePixels = 0;
  let navyPixels = 0;
  let coralPixels = 0;

  for (let offset = 0; offset < data.length; offset += info.channels) {
    const red = data[offset];
    const green = data[offset + 1];
    const blue = data[offset + 2];
    const alpha = data[offset + 3];

    if (alpha === 0) transparentPixels += 1;
    if (alpha > 0) {
      visiblePixels += 1;
      if (alpha >= 250) opaquePixels += 1;
      if (red !== 0 || green !== 0 || blue !== 0) {
        nonBlackVisiblePixels += 1;
      }
      if (red === 23 && green === 35 && blue === 60) navyPixels += 1;
      if (red === 239 && green === 106 && blue === 91) coralPixels += 1;
    }
  }

  if (transparentPixels === 0) {
    throw new Error(`${iconName}.png has no transparent background pixels`);
  }
  if (visiblePixels === 0 || opaquePixels < 32) {
    throw new Error(`${iconName}.png has insufficient visible solid artwork`);
  }
  if (iconName === "ic_brand_app") {
    if (navyPixels === 0 || coralPixels === 0) {
      throw new Error(
        "ic_brand_app.png must retain both exact brand colors #17233C and #EF6A5B",
      );
    }
  } else if (nonBlackVisiblePixels !== 0) {
    throw new Error(
      `${iconName}.png contains ${nonBlackVisiblePixels} visible non-black pixels`,
    );
  }

  return {
    name: `${iconName}.png`,
    size: `${metadata.width}x${metadata.height}`,
    format: "RGBA",
    transparentPixels,
    visiblePixels,
    opaquePixels,
  };
}

await fs.mkdir(outputDirectory, { recursive: true });
await Promise.all(iconNames.map(renderIcon));
const results = await Promise.all(iconNames.map(validateIcon));

console.table(results);
console.log(`Rendered and validated ${results.length} interface icons.`);
