# Serene Launcher

A minimalist, fully offline Android home screen. Pinned apps on a quiet home page, a searchable A–Z app drawer, and nothing else: no accounts, no ads, no network access.

## Install

Download the latest APK from the [Releases](../../releases) page and open it on your phone. Android will ask you to allow installs from your browser or file manager the first time.

Then make it your home screen: **Settings → Apps → Default apps → Home app → Serene Launcher**.

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
3. The **Release** workflow builds the APK, signs it, checks it has no internet permission, and attaches `serene-launcher-0.2.0.apk` and its `.sha256` checksum to the release (creating the release if you only pushed a tag). A pushed tag with a dash, such as `v0.2.0-test`, is published as a pre-release.

If a secret is missing, the workflow stops without publishing anything.
