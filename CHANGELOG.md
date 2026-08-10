## v1.3.0 - 2026-08-10
* feat(salary): calculate net salary with Social Security and IRS withholding
## v1.2.21 - 2026-08-06
* fix(auth): request ID token in Google Sign-In flow
## v1.2.20 - 2026-08-05
* ci: sync mobile-release.yml (bot-commit exclusion in change-diff)
## v1.2.19 - 2026-08-05
* ci: sync mobile-release.yml (.github diff exclusion) and add version marker
## v1.2.18 - 2026-08-05
* ci: sync mobile-release.yml (idempotency + change-detection fixes)
## v1.2.17 - 2026-08-05
* fix(ci): mark gradlew as executable
## v1.2.16 - 2026-08-05
* ci: sync mobile-release.yml fix for workflow_run trigger
## v1.2.15 - 2026-08-05
* ci: adopt centralized versioning + mobile-release workflows
* ci: automate signed APK build and GitHub release on tag push
## v1.2.13 - 2026-08-05
* fix(register): reserve space for keyboard on Registar Turnos form
* fix(auth): show Snackbar for uiMessage on LoginScreen
* fix(profile): persist default hourly rate edit to Firestore
## v1.2.10 - 2026-08-05
* docs: rewrite README with real Firebase setup steps
* fix(test): initialize FirebaseApp in Robolectric tests
* fix(auth): roll back orphaned Auth account if profile write fails
* docs: mark 2.2 (.gitignore) as already resolved
* fix(sync): key cloud salary records by uid instead of username
* fix(dashboard): stop showing generic "Utilizador" while profile loads
* fix(auth): clear auth error/success state when entering Login/SignUp
* fix(auth): remove username-based login, email only
* fix(security): lock down Firestore rules, was open to anonymous read
* docs: add incremental bugfix plan
* feat(auth): migrate to Firebase Auth/Firestore, fix salary record save
## v1.1.0 - 2026-05-25
* feat: lauching the apk version
## v1.0.1 - 2026-05-25
* fix: fixing an little issue
## v1.0.0 - 2026-05-25
* breaking change : first version of the application
