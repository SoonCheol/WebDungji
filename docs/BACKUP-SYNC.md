# BACKUP-SYNC

Manual backup uses Android Storage Access Framework. Google Drive may appear as a location in the Android file picker when installed and authenticated; the app itself does not implement a Google Drive OAuth or sync API.
Portable JSON stores registered active webapps, IDs, names, URLs, icons, item order, JavaScript/image/zoom settings. It does not transfer cookies, credentials, WebView site storage or login sessions.
The user can edit the NEW backup file name before creating it. Existing backup file renaming is not implemented.
Restore validates schema, IDs, fields and image payloads before a database transaction. Merge preserves existing IDs and adds missing records; replace explicitly clears registrations and reimports. No background restore occurs.
Future automatic sync needs Google Drive authentication, stable remote file IDs, conditional writes, conflict resolution, deletion tombstones, retries and device tests. Do not call this feature complete.
