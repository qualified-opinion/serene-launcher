# Serene Launcher

A minimalist, fully offline Android home screen. Pinned apps on a quiet home page, a searchable A–Z app drawer, and nothing else: no accounts, no ads, no network access.

## Install

Download the latest APK from the [Releases](../../releases) page and open it on your phone. Android will ask you to allow installs from your browser or file manager the first time.

Then make it your home screen: **Settings → Apps → Default apps → Home app → Serene Launcher**.

## Build it yourself

You need JDK 17 and the Android SDK (Android Studio installs both).

```sh
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest  # unit tests
```

### Signed release builds

Release builds are signed only when a keystore is configured; otherwise `assembleRelease` produces an unsigned APK. Never commit the keystore or its passwords.

Either create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/serene-release.jks
storePassword=...
keyAlias=serene
keyPassword=...
```

or set the environment variables `SERENE_KEYSTORE_PATH`, `SERENE_KEYSTORE_PASSWORD`, `SERENE_KEY_ALIAS` and `SERENE_KEY_PASSWORD`.

Keep a backup of the keystore: Android only installs updates signed with the same key.
