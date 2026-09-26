# Shiksha Rojgar release signing and future updates

## Permanent identity
- applicationId: `com.shiksharojgar.app`
- Current release version: `1.5.0`
- Current versionCode: `6`
- Release alias: `shiksha-rojgar-release`

Every future Play Store/direct APK release must keep the same `applicationId` and the same release signing key. Only increase `versionCode` (7, 8, 9...) and update `versionName` (1.5.1, 1.6.0, 2.0.0, etc.).

## GitHub Actions secrets
Create these repository secrets. Never commit the keystore or passwords to GitHub source code.

- `SHIKSHA_KEYSTORE_B64` = base64 contents of the private JKS keystore
- `SHIKSHA_KEYSTORE_PASSWORD` = keystore password
- `SHIKSHA_KEY_ALIAS` = `shiksha-rojgar-release`
- `SHIKSHA_KEY_PASSWORD` = key password

The release workflow reconstructs the keystore only inside the GitHub runner, signs the APK, verifies its certificate, and then uploads the signed APK artifact.

## Important
The old 1.4.0 APK found in the project history was signed with Android Debug certificate. That old certificate is different from this new permanent release key. Therefore, this new 1.5.0 release is intended as the start of the permanent signed update line; existing debug-signed 1.4.0 installs may need a one-time uninstall/reinstall.

Do not lose the private keystore. If it is lost, future APKs cannot be signed with this same identity and cannot update installations signed by it.

## Admin panel security
The Admin Activity is not exposed as a launcher screen and is not shown in normal UI. It can be reached only through the hidden admin entry, then Firebase Authentication and an `admin=true` custom claim are required. The password is not stored in the APK.

## Channel sharing / deep link
- The app now accepts `shiksharojgar://channel` and opens the Shiksha Rojgar Channel.
- The shared message also includes `https://www.shiksharojgar.com/app/channel` as a web/fallback link.
- For automatic Android App Links opening from HTTPS, publish an `assetlinks.json` file at `https://www.shiksharojgar.com/.well-known/assetlinks.json` containing this app's release SHA-256 certificate fingerprint. The custom `shiksharojgar://channel` link works without domain verification.
