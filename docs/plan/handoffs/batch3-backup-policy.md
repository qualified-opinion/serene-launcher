# B3 Backup policy (handoff pack)

Repo: `qualified-opinion/serene-launcher`. Branch: `fix/backup-policy` from main `e70d64f`. Engine: Codex (default). Reviewer: Kiln with Claude Code.
Fixes: N-04 (cloud backup on with unedited sample rules). Issue #23 is shrunk to this policy only; the in-app backup file stays later.

## Goal
Serene's settings never leave the phone through Google cloud backup. That matches the offline promise (no `INTERNET` permission). Moving to a new phone by cable or direct transfer still carries the settings over.

## Decision (Jan owes it, ROADMAP decision 2)
Default is cloud backup **off**. Jan's "go" on this pack confirms that default. If Jan wants it on, this pack is void and Kiln rewrites it.

## Scope
1. `AndroidManifest.xml`: set `android:allowBackup="false"`. Remove `android:fullBackupContent` and delete `res/xml/backup_rules.xml`. They only matter when backup is on, before Android 12.
2. Rewrite `res/xml/data_extraction_rules.xml` (Android 12 and later) with no sample comments:
   - `<cloud-backup>` excludes every domain (`root`, `file`, `database`, `sharedpref`, `external`). This is a second guard, because `allowBackup="false"` already turns cloud backup off.
   - `<device-transfer>` includes only `domain="file" path="datastore/"`, where the DataStore file `serene_launcher_preferences.preferences_pb` lives.
   - Keep `android:dataExtractionRules` in the manifest.
3. A short `docs/plan/` note, one paragraph in ROADMAP under decision 2: "Resolved: off. Device-to-device transfer kept."
4. In the ROADMAP status table, mark the B2 row "Merged #32, `e70d64f`" and set the B3 row to "In review" with a link to this pack.

## Out of scope
The in-app export and import file (#23), any new permission, any change to the settings code, and B4 to B6.

## Acceptance criteria
- On the built debug APK, `aapt2 dump xmltree --file AndroidManifest.xml` (or `apkanalyzer manifest print`) shows `allowBackup=false`, shows `dataExtractionRules`, and doesn't show `fullBackupContent`.
- `backup_rules.xml` is gone, and no resource still refers to it.
- `data_extraction_rules.xml` matches the scope above and passes lint with no new warnings. The build check is green with test totals no lower than 62.
- No change in `app/src/main/java`.
- The diff stays under about 40 lines plus the docs.

## Risks
- On Android 12 and later, transfer to a new phone carries work-profile pins (`@serial`). Those serials can differ on the new phone, so those pins may not match any app. B2 prunes only when an app is removed, so a stale pin would stay saved but hidden. That's acceptable for v1; it goes on the open list as N-16.
- Users who already have a cloud backup keep it on Google's side until it expires. Nothing in the app can delete it. Note this in the PR description.

## Phone steps for Jan (debug build, installed over B2)
1. Install over B2. Pins, pin order and settings are unchanged.
2. Restart the phone. Everything is still the same.
3. Optional, if a second phone is handy: a direct phone-to-phone transfer brings the settings across.
