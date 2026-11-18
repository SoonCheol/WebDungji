package com.webdungji.app.sync

import androidx.room.withTransaction
import com.webdungji.app.data.*
import android.util.Base64

/** Metadata-only wire contract. No WebView storage, cookies, passwords or tokens. */
data class SyncWebApp(
 val id: String, val name: String, val startUrl: String, val iconPngBase64: String?,
 val displayOrder: Long, val profileId: String, val iconStyle: String?,
 val javaScript: Boolean, val images: Boolean, val textZoom: Int,
 val createdAt: Long, val updatedAt: Long, val deletedAt: Long?, val revision: Long,
 val bookmarks: List<SyncBookmark> = emptyList(), val tags: List<String> = emptyList()
)
data class SyncBookmark(val id: String, val title: String, val url: String, val order: Long)
data class SyncEnvelope(val schemaVersion: Int = 1, val apps: List<SyncWebApp>)
data class RemoteSnapshot(val cursor: String?, val data: SyncEnvelope)
interface SyncRemoteStore {
 suspend fun download(afterCursor: String?): RemoteSnapshot
 suspend fun upload(changes: SyncEnvelope): RemoteSnapshot
}
/** Remote implementation and conflict merge are deliberately deferred. */
class LocalSyncSource(private val repository: AppRepository) {
 suspend fun pending(): SyncEnvelope = repository.db.withTransaction {
  SyncEnvelope(apps = repository.db.webApps().pending().map { app ->
   val icon = repository.iconFile(app.iconRef)?.takeIf { it.exists() }?.readBytes()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
   SyncWebApp(app.id, app.name, app.startUrl, icon, app.displayOrder, app.profileId, app.iconRef?.takeIf { it.startsWith("preset:") }, app.javaScript, app.images,
    app.textZoom, app.createdAt, app.updatedAt, app.deletedAt, app.revision)
  })
 }
 suspend fun acknowledge(sent: SyncEnvelope) = repository.db.withTransaction {
  require(sent.schemaVersion == 1)
  sent.apps.forEach { repository.db.webApps().acknowledge(it.id, it.revision); repository.db.webApps().acknowledgeChanges(it.id, it.revision) }
 }
}
