# 현재 상태 (2026-11-24)

- Repository: SoonCheol/WebDungji (main); Android project: app-project.
- Previous successful Actions build: https://github.com/SoonCheol/WebDungji/actions/runs/44375818412
- User confirmed existing 0.1.0 install/update on 2026-11-19.
- New source version: 0.2.2 / versionCode 4; existing app ID and signing secrets retained.
- Settings: current/latest version and GitHub Releases APK download when newer.
- Portable JSON backup: editable output filename; SAF save/open (Google Drive document provider if available); merge or explicit replace.
- Backups include active webapp registrations, custom icons and webapp-specific settings. Exclude sessions, cookies, passwords and WebView storage.
- Google OAuth, automated Drive synchronization, conflict reconciliation and automatic background scheduling NOT implemented.
- The new version is not considered built, published or device-verified until CI result and release asset are confirmed.

- v0.2.1: HTTPS selection precedes the address field. Pasted HTTP/HTTPS prefixes are stripped and the checkbox determines the saved scheme. Invalid URLs are rejected for both save and metadata fetch. Existing HTTP entries initialize as unchecked. User confirmed v0.2.1 in-app update works on a physical device; URL editor behavior not separately confirmed.

- v0.2.2: Full-width address field with HTTPS checkbox and dynamic HTTP/HTTPS prefix inside the outline. URL normalization, validation, metadata fetch and stored data unchanged. CI, release and device verification pending.
