# Shiksha Rojgar Channel App Link

The app now shares this normal HTTPS link:
`https://shiksha-rojgar.web.app/channel`

For direct Android routing, Firebase Hosting must be deployed from the `firebase/` folder. The project already contains:
- `firebase/public/.well-known/assetlinks.json`
- `firebase/public/channel/index.html`
- Hosting config in `firebase/firebase.json`

Deploy once with:
```bash
cd firebase
firebase login
firebase use shiksha-rojgar
firebase deploy --only hosting
```

The app release SHA-256 in assetlinks is the permanent release certificate. If testing a debug APK, Android App Links may need the debug certificate as well; the release APK should be used for final testing.

The custom `shiksharojgar://channel` scheme remains supported as a secondary direct link.
