# Radiance POS V3 for Android

The phone side of Radiance POS V3 (Radiance-Philippines/radiance-pos#172; the design: radiance-pos
`docs/android/DESIGN.md`). Each person's own phone, linked once by the shop's owner (Office > Phones):

- **Scan for my PC:** each barcode goes into the sale (or receiving) open on the PC where they're signed in.
- **Price check** and **Count stock** (Update stock's own change, the person's own permissions).
- **Time clock** (#173): scan the QR on the branch's time-clock PC, at the shop (GPS), from a phone whose key
  proved it lives in its hardware.

Every request is signed by a key made in the phone's hardware (Android Keystore, with key attestation the shop
checks), never a password.

## Build (on ribs02, never the laptop)

`/srv/build/android-pos`: the project in `src/`, its own Gradle home, the SDK and JDK 25 of `/srv/build/android`
(read only). `:app:assembleDebug`, `:app:testDebugUnitTest` (Robolectric draws the screens into
`/srv/build/android-pos/screens`), `:app:assembleRelease` with the release key in
`/srv/build/android-pos/keys/signing.properties` (outside the repository; not made yet).
