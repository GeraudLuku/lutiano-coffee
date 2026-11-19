# Seller app: release checklist

What is done in the code, and what only people or accounts can do.

## Done in the code
- **Android signing:** `apps/seller-app/android/app/build.gradle.kts` signs release builds with the upload key named in `android/key.properties`. Without that file a release build is signed with the debug key and cannot be uploaded. `key.properties` and `*.jks` are git-ignored.
- **Build:** `cd apps/seller-app && flutter build appbundle --release --dart-define=LUTIANO_API=https://<production api>/api/v1` gives `build/app/outputs/bundle/release/app-release.aab` (about 68 MB before Play's per-device splitting).
- **Crash reports:** Firebase Crashlytics, Android release builds only, no personal details. Reports show in the Firebase console under Crashlytics (enable it there once).
- **Session:** the token is kept in secure storage; expired tokens start at sign-in.

## Needs you
1. **Back up the upload key now.** `apps/seller-app/android/upload-keystore.jks` and `android/key.properties` exist only on this Mac. Copy both to a password manager or a safe place. When you create the app in the Play Console, use Play App Signing and upload the first `.aab`; if the key is lost, Google can reset an upload key, but only if Play App Signing is on. The key was generated here with a random password; you can replace it with your own (`keytool -genkeypair ...`) before the first upload.
2. **Play Console:** create the app `com.lutiano.lutiano_seller`, fill the store listing and the data-safety form (name, phone, email, photo, sales and cash records, crash logs, push token), add the Privacy Policy URL, then upload the `.aab` to the **Internal testing** track and add testers.
3. **Legal review** of the Terms of Use and Privacy Policy (draft French and English in `lib/l10n/app_*.arb`, keys `termsBody` and `privacyBody`); publish the privacy policy at a public URL for the store.
4. **Production API URL and HTTPS** for the `--dart-define`; the API must be reachable from phones.
5. **Pilot:** 2 or 3 sellers at one location for a week, with a fix list. One-page training sheet per seller role.
6. **Real push test** on a phone on a network that does not intercept TLS (the corporate proxy here blocks Google services). Check: sign in, a notification arrives while the app is closed, tapping it opens the right screen.
7. **Low-end Android check:** a cheap phone (2 GB RAM) on a release build: cold start, scrolling the product grid and sales list, taking a photo.
8. **iOS:** needs an Apple Developer account; then add `GoogleService-Info.plist`, push capability and an APNs key.
9. **Cash board:** the design pack has no board for Cash (batch 04), so the Cash screens cannot be compared with a design; provide one or accept the current layout.

---

# Customer app: release checklist

App id `com.lutiano.lutiano_customer`, name "LUTIANO Coffee", in `apps/customer-app`.

## Done in the code
- **Android signing:** same as the Seller app: release builds use the upload key named in `apps/customer-app/android/key.properties` (never committed), else the debug key (cannot be uploaded). A release APK builds (`flutter build apk --release`, about 74 MB); for Play use `flutter build appbundle --release --dart-define=LUTIANO_API=https://<production api>/api/v1`.
- **Firebase is optional until you add the file:** the push and Crashlytics plugins apply only when `android/app/google-services.json` exists, so the app builds and runs without it (no push, no crash reports).
- **Backend:** customer tokens are separate from staff tokens (audience and role), customer endpoints are refused to staff and the reverse (tested). Orders, rewards and support have idempotency or atomic state changes and tests.
- **Reviews (Phase 6):** backend and Flutter review of the new code: unused import and two inline package names removed; no layering leaks, `print`, or unmanaged lifecycle found in the customer app; analyze and format clean.

## Needs you
1. **Upload key:** the customer app signs with the same upload key as the Seller app (a copy of `upload-keystore.jks` and `key.properties` in `apps/customer-app/android`). Back it up outside this Mac before the first Play upload.
2. **Firebase:** register `com.lutiano.lutiano_customer`, add `google-services.json`, enable Crashlytics. Then test real push (the proxy here blocks it) on a phone: place an order, accept it as a seller, check the notification arrives with the app closed and opens the order.
3. **Google sign-in:** OAuth clients in Google Cloud (Android with the debug and upload SHA-1, iOS, web client id), then set `lutiano.social.google.client-ids` on the API and `--dart-define` the client id in the app. **Apple sign-in:** needs the Apple Developer account and "Sign in with Apple" on the bundle id; set `lutiano.social.apple.audiences`. Real tokens have not been tested.
4. **Play Console:** the listing and data-safety form (name, phone, email, photo, orders, loyalty, support messages, push token, crash logs), Privacy Policy URL.
5. **Legal review** of the customer Terms and Privacy text (keys `termsBody`, `privacyBody`) and public URLs.
6. **SMS provider:** the production OTP sender and sender id, and its cost per sign-up (customers can request codes by themselves).
7. **Mobile money** (MTN MoMo, Orange Money): needs the provider contracts; the checkout is ready for another method.
8. **Opening hours:** set them on each sales point in the admin portal; a sales point with no hours is treated as always open, so customers could order while it is shut.
9. **Pilot:** one sales point for a week with a seller trained on accepting, preparing and handing over orders and on scanning reward QR codes.
10. **iOS:** Apple Developer account, `GoogleService-Info.plist`, push capability and APNs key.
11. **Camera scan** on a real phone (the simulator has no camera) and a low-end Android check.
