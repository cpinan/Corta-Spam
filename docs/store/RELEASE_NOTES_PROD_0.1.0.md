# Release notes — 0.1.0 (versionCode 3), **production**

Paste each block into Play Console → Production → release notes. Play caps each language at
**500 characters**. Counted, not estimated: **es-419 458**, **en-US 415**.

> These are **not** the internal-testing notes in [`RELEASE_NOTES_0.1.0.md`](RELEASE_NOTES_0.1.0.md).
> Those ask the reader to *check* that the phone rings, which is right for a tester and wrong for a
> store listing — it tells the public the app is unverified. Do not paste one where the other goes.

<es-419>
Primera versión pública.

Corta Spam es tu app de teléfono: filtra cada llamada entrante con las reglas que escribes tú, antes de que el teléfono suene.

Puedes bloquear números concretos, patrones con comodines, códigos de país, franjas horarias completas y a quien insiste demasiado. Tus contactos y tu lista de permitidos siempre pasan.

El registro te dice qué regla decidió cada llamada.

Para que funcione, ponla como tu app de teléfono predeterminada.
</es-419>
<en-US>
First public release.

Corta Spam is your phone app: it screens every incoming call against rules you write yourself, before your phone rings.

Block specific numbers, wildcard patterns, country codes, whole time windows, and callers who try too often. Your contacts and your allowlist always come through.

The call log tells you which rule decided each call.

It needs to be set as your default phone app to work.
</en-US>

---

## Why these say what they say

**The last line is the most important one.** Corta Spam does nothing until it holds the dialer
role. Someone who installs it, grants nothing, sees no blocking and leaves a one-star review is the
most likely bad outcome for this app, and one sentence prevents it.

**No accuracy promise.** Nothing claims the app "stops spam". It blocks by *your* rules plus a small
bundled list; it is not a crowd-sourced spam database, and a claim the app cannot keep becomes a
refund request.

**No "no ads, no tracking".** That language is barred from the *short description* only, and these
are release notes — but this listing was already flagged once for exactly that phrasing. The
positioning lives in the full description, where it is allowed, and there is nothing to gain by
testing the boundary with the reviewer who flagged it.

**The auto-responder is not mentioned.** It is experimental, ships off, and on modern Android the
caller may not hear it. Advertising the one feature that cannot be relied on invites complaints
about it.
