# Privacy

Serene Launcher collects nothing and sends nothing.

- **No internet access.** The app doesn't ask for Android's internet permission, so it can't connect to anything. Every build checks this automatically and fails if the permission ever appears.
- **No accounts, ads, analytics or crash reporting.** There are no tracking or advertising libraries in the app.
- **Your app list stays on your phone.** Serene reads the list of apps installed on your phone, including work-profile apps, only to show and open them. It doesn't store or share that list.
- **Optional accessibility service.** If you turn on double-tap to lock, Serene asks you to switch on its accessibility service in Android's Accessibility settings. The service only presses the lock button, or opens notifications, when you use a gesture on Home. It can't read your screen or what you type, and it stays off unless you turn it on.
- **What it saves, and where.** Your pins and their order, hidden apps, the names you give apps, and your Settings choices are saved in the app's private storage on your phone. Nothing else is saved.
- **No cloud backup.** Android's Google cloud backup is turned off for Serene. If you move to a new phone with a cable or a direct phone-to-phone transfer, your Serene settings can come along. They never pass through anyone's servers.
- **Settings file, only if you ask.** Settings → Export settings saves your pins, names, hidden apps and options to a file you choose, using Android's file picker. Serene never makes this file by itself, and Import settings only reads the one file you pick.
- **Removing it.** Uninstalling Serene, or clearing its storage in Android's settings, deletes everything it saved.

Serene can open other apps, Android's alarm and calendar screens, and an app's settings page when you ask it to. What those apps do is up to them.

The source code is public in this repository, so anyone can check these claims.
