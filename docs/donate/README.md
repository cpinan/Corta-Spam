# Donation QR codes

Two images, linked from [`DONATE.md`](../../DONATE.md) and [`DONATE_ES.md`](../../DONATE_ES.md):

| File | Source | Shows |
|---|---|---|
| `yape-qr.png` | Yape app → **Mi QR** → share | QR + "Paga aquí con Yape" + account holder's name |
| `plin-qr.png` | Bank app → **Plin** → *Mi QR* → share | Plin logo + name + QR + payment instructions |

Both are 760 px wide, palette-quantised to 16 colours, ~35 KB each. The originals were phone
screenshots at 1131×1599 and 1080×1381; the Yape one had its share/download button bar clipped at
the bottom edge and a large empty margin below the name, so it was trimmed to 1131×1320 first.

## What was checked before publishing

These files are public and permanent the moment they are pushed, and a QR is just an encoded
string that anyone can decode back to plain text. So both were decoded and inspected rather than
trusted:

- **Payload contents.** Both are EMVCo merchant-presented QR payloads. Yape's carries an opaque
  base64 account token plus `YAPERO` / `Lima`; Plin's carries `Plin Network P2P`, a hex account
  GUID, `P2P Transfer` / `Lima`. Neither encodes a phone number — checked explicitly against the
  Peruvian mobile pattern (`9########`, with and without the `51` country code) and against the
  account holder's name.
- **Nothing sensitive rendered in the image.** The account holder's full name is visible on both,
  which is unavoidable and is what a payer expects to see. No phone number is printed on either.
- **Still scannable after processing.** Trim → downscale → quantise each re-decoded to a byte
  count identical to the original, so the payload survived every step.

## Re-verifying, or replacing a QR

Decoding needs no installed tooling — macOS Vision does it. Save as `qrdecode.swift`:

```swift
import Foundation
import Vision
import AppKit

for path in CommandLine.arguments.dropFirst() {
    guard let img = NSImage(contentsOfFile: path),
          let cg = img.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
        print("\(path)\tERROR: cannot load"); continue
    }
    let req = VNDetectBarcodesRequest()
    req.symbologies = [.qr]
    try? VNImageRequestHandler(cgImage: cg, options: [:]).perform([req])
    for r in (req.results ?? []) { print("\(path)\t\(r.payloadStringValue ?? "<binary>")") }
}
```

```sh
swift qrdecode.swift docs/donate/yape-qr.png docs/donate/plin-qr.png
```

If you replace either image: **look at it, then decode it** before committing. Some Yape and Plin
share flows do print the phone number — crop it out, or regenerate from a screen that omits it. A
number published here is the same kind of number this app exists to keep out of a stranger's hands.
