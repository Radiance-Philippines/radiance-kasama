# Radiance Kasama

The phone helper for Radiance POS V3 (Radiance-Philippines/radiance-pos#172; the design: radiance-pos
`docs/android/DESIGN.md`). It is not a POS: the shop sells on the PC, and Kasama works next to it. Each
person's own phone, linked once by the shop's owner (Office > Phones):

- **Scan for my PC:** each barcode goes into the sale (or receiving) open on the PC where they're signed in.
- **Price check** and **Count stock** (Update stock's own change, the person's own permissions).
- **Time clock** (#173): scan the QR on the branch's time-clock PC, at the shop (GPS), from a phone whose key
  proved it lives in its hardware.
- **Alerts** (#175): a voided sale, a short or over cash count, the day closed, a message or notice for the person,
  a time-clock try refused, even with the app closed (Firebase Cloud Messaging; the shop never holds the key).

Every request is signed by a key made in the phone's hardware (Android Keystore, with key attestation the shop
checks), never a password.

## Build (on ribs02, never the laptop)

`/srv/build/android-pos`: the project in `src/`, its own Gradle home, the SDK and JDK 25 of `/srv/build/android`
(read only). Two flavors, the same package and signer: `github` (the download page; updates itself from this
repository's Releases, #1) and `play` (kept for a later Google Play listing, which forbids an app updating itself). `:app:assembleGithubDebug`,
`:app:testGithubDebugUnitTest` (Robolectric draws the screens into `/srv/build/android-pos/screens`, or
`SCREENS_DIR`), `:app:assembleGithubRelease` / `:app:bundlePlayRelease` with the release key in
`/srv/build/android-pos/keys/signing.properties`.

Not in the repository: the release key, and `app/google-services.json` (the Firebase project of
hello@radianceph.com, copied in from the keys folder at build time). Your own build needs your own Firebase project's
`google-services.json` for `com.radianceph.pos` and `com.radianceph.pos.debug` (docs/FIREBASE.md).
