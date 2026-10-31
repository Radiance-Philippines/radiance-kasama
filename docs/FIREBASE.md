# Alerts on the owner's phone: the Firebase project (radiance-pos#172)

Push alerts (a new notice, a chat message, the day closed, a short cash count) need a free Firebase project, in
hello@radianceph.com's Google account. About 10 minutes of clicks; done once.

1. Open <https://console.firebase.google.com> and sign in as **hello@radianceph.com**.
2. **Create a project**, name it **Radiance POS**. Google Analytics: **off**. Create.
3. In the project, **Add app → Android**:
   - Android package name: **com.radianceph.pos**, nickname *Radiance POS*. Register app, then Next, Next,
     Continue to console (nothing else is needed from those pages).
   - **Add app → Android** again: **com.radianceph.pos.debug**, nickname *Radiance POS (test)*.
4. **Project settings** (the gear) → General → Your apps → download **google-services.json** (one file, both apps).
5. **Project settings → Service accounts → Firebase Admin SDK → Generate new private key**. A `.json` file downloads.
   **This one is secret**: don't send it in a chat or an email.
6. Tell Claude it's done:
   - google-services.json: Claude opens a file box on the laptop; it goes to ribs02 for the app's build
     (`/srv/build/android-pos/keys/`, never in this repository).
   - The private key: you upload it yourself on **admin.radianceph.com → POS v3 → Push**, where it's kept encrypted.
     Claude never sees it; the admin sends the alerts with it.
