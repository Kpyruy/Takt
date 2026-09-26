# Signing a Takt release

Android updates require the same signing key for every release. Keep the keystore **outside this repository**, back it up securely, and never commit the file or its passwords. Losing it means existing installations cannot receive updates signed with a replacement key.

Create the key once. `keytool` comes with a JDK or Android Studio's bundled JDK:

```sh
mkdir -p "$HOME/.local/share/takt"
chmod 700 "$HOME/.local/share/takt"
keytool -genkeypair -v \
  -keystore "$HOME/.local/share/takt/release-key.jks" \
  -alias takt -keyalg RSA -keysize 4096 -validity 10000
chmod 600 "$HOME/.local/share/takt/release-key.jks"
```

Export the path, alias, and passwords in the shell for the build. Avoid passing passwords on the Gradle command line or saving them in `gradle.properties`:

```zsh
export TAKT_RELEASE_STORE_FILE="$HOME/.local/share/takt/release-key.jks"
export TAKT_RELEASE_KEY_ALIAS=takt
read -rs 'TAKT_RELEASE_STORE_PASSWORD?Keystore password: '
export TAKT_RELEASE_STORE_PASSWORD
read -rs 'TAKT_RELEASE_KEY_PASSWORD?Key password: '
export TAKT_RELEASE_KEY_PASSWORD
JAVA_HOME=/path/to/jdk17 ./gradlew :app:assembleRelease
```

The output is `app/build/outputs/apk/release/app-release.apk`. Verify its signing certificate and compute the checksum before uploading it to GitHub Releases:

```sh
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

Before using this key for the first public release, save an encrypted backup of the keystore and the alias/passwords in a separate place. Existing debug APK installations use a different key, so a release APK cannot update them in place. Export or sync data to `Documents/Takt`, uninstall the debug app, install the signed release, then reconnect the folder to restore the data.
