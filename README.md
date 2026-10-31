# Serene Launcher

A minimalist, fully offline Android home screen. Pinned apps on a quiet home page, a searchable A–Z app drawer, and nothing else: no accounts, no ads, no network access.

## Install

Serene Launcher is only published on this project's [Releases](../../releases) page. It isn't on Google Play.

1. On your phone, open the [latest release](../../releases/latest) and download `serene-launcher-X.Y.Z.apk`.
2. Open the downloaded file. The first time, Android asks you to allow installs from your browser or file manager: tap **Settings**, turn on **Allow from this source**, then go back and tap **Install**.
3. Press the Home button. If Android asks which app to use, pick **Serene Launcher** and tap **Always**. If it doesn't ask, go to **Settings → Apps → Default apps → Home app** and choose **Serene Launcher**. (The menu names differ a little between phone makers.)

To check the download, compare its SHA-256 checksum with the `.sha256` file attached to the same release.

### Updating

Download the newer APK from Releases and install it over the old one. Your pins and settings stay. Serene Launcher never checks for updates itself, because it has no internet access; watch the repository's releases on GitHub if you want to hear about new versions.

If you installed a test build made before the first signed release, Android will refuse the update because it was signed with a different key. Uninstall the old one first; this resets your pins and settings once.

### Going back to your old launcher

Go to **Settings → Apps → Default apps → Home app** and choose your previous launcher. To remove Serene Launcher completely, uninstall it like any other app; that also deletes everything it stored.

### Privacy and licence

Serene Launcher has no internet permission and collects nothing. See [PRIVACY.md](PRIVACY.md) for what it stores on your phone. Changes in each version are in [CHANGELOG.md](CHANGELOG.md). The code is open source under the [MIT licence](LICENSE).

## Build it yourself

You need JDK 21 and the Android SDK (Android Studio installs both). JDK 21 is required by the Robolectric unit tests, which run against Android 16 (API 36).

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

### Version numbers

Release builds take their version from the tag they're built from: tag `v1.2.3` gives version name `1.2.3` and version code `10203`. A pre-release tag such as `v1.2.3-beta1` gives the name `1.2.3-beta1` and the same code as `1.2.3`, so the final release can be installed over it. Minor and patch numbers must stay below 100. Local builds without a tag are named `0.0.0-dev`.

To build a specific version locally: `./gradlew assembleRelease -PsereneVersion=1.2.3`.

## Publishing a release (owner only)

Releases are built and signed by the **Release** workflow on GitHub when you push a version tag. You set up the signing key once; after that every release is one tag.

### One-time setup: create the signing key

Do this yourself on your own computer. Nobody else, and no AI agent, should create, see or upload this key. If the key is lost or changed, people who installed Serene Launcher can't update it without uninstalling first and losing their settings.

1. Create the key (you need a JDK installed, which gives you `keytool`):

   ```sh
   keytool -genkeypair -v -keystore serene-release.jks -alias serene \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   It asks for a password and some name details. Use a long password and write it down.

2. Store `serene-release.jks` and the password somewhere safe and offline, for example an encrypted USB stick plus a password manager. Keep two copies. Never put the key in this repository.

3. Turn the key into text so GitHub can store it:

   ```sh
   base64 -w 0 serene-release.jks > serene-release.jks.base64   # on macOS: base64 -i serene-release.jks -o serene-release.jks.base64
   ```

4. On GitHub, open the repository's **Settings → Secrets and variables → Actions → New repository secret** and add four secrets:

   | Name | Value |
   |---|---|
   | `SERENE_KEYSTORE_BASE64` | the contents of `serene-release.jks.base64` |
   | `SERENE_KEYSTORE_PASSWORD` | the keystore password |
   | `SERENE_KEY_ALIAS` | `serene` (the alias you used above) |
   | `SERENE_KEY_PASSWORD` | the key password (the same as the keystore password unless you chose a different one) |

5. Delete `serene-release.jks.base64` from your computer once the secrets are saved.

### Each release

1. Make sure `main` has everything you want and CI is green.
2. Create a tag, either with `git tag v0.2.0 && git push origin v0.2.0`, or on GitHub under **Releases → Draft a new release**: type the new tag (for example `v0.2.0`) in **Choose a tag**, then press **Publish release**.
3. Before tagging, rename **Unreleased** in [CHANGELOG.md](CHANGELOG.md) to the version, for example `## [0.2.0] - 2026-11-01`. The workflow uses that section as the release notes (if you draft the release on GitHub, leave its description empty to get them) (for a tag like `v0.2.0-beta1` it looks for `0.2.0-beta1`, then `0.2.0`). Without one, GitHub writes notes from the merged PRs.
4. The **Release** workflow builds the APK, signs it, checks it has no internet permission, and attaches `serene-launcher-0.2.0.apk` and its `.sha256` checksum to the release (creating the release if you only pushed a tag). A pushed tag with a dash, such as `v0.2.0-test`, is published as a pre-release.

If a secret is missing, the workflow stops without publishing anything.
