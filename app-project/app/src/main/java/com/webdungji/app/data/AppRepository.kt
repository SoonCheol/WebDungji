package com.webdungji.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import com.webdungji.app.domain.UrlPolicy
import com.webdungji.app.domain.readLimited
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class AppRepository(val db: AppDatabase, private val context: Context) {
 val apps = db.webApps().observeActive()
 suspend fun save(draft: WebAppEntity): WebAppEntity = db.withTransaction {
  val normalized = requireNotNull(UrlPolicy.registration(draft.startUrl)) { "Invalid HTTP(S) address" }
  require(draft.name.isNotBlank() && draft.name.length <= 120)
  require(draft.textZoom in 50..200)
  require(draft.profileId == "default")
  val old = db.webApps().find(draft.id)
  require(old?.deletedAt == null) { "Deleted entry" }
  val now = System.currentTimeMillis()
  val entity = draft.copy(name = draft.name.trim(), startUrl = normalized, createdAt = old?.createdAt ?: now,
   displayOrder = old?.displayOrder ?: db.webApps().nextOrder(), updatedAt = now,
   revision = (old?.revision ?: 0) + 1, dirty = true, deletedAt = null)
  db.webApps().upsert(entity)
  db.webApps().record(LocalChange(UUID.randomUUID().toString(), entity.id, entity.revision, now, "upsert"))
  entity
 }
 suspend fun delete(id: String) = db.withTransaction {
  val old = db.webApps().find(id) ?: return@withTransaction
  if (old.deletedAt != null) return@withTransaction
  val now = System.currentTimeMillis()
  val dead = old.copy(updatedAt = now, deletedAt = now, revision = old.revision + 1, dirty = true)
  db.webApps().upsert(dead)
  db.webApps().record(LocalChange(UUID.randomUUID().toString(), id, dead.revision, now, "delete"))
 }
 fun iconFile(ref: String?): File? = ref?.takeIf { it.matches(Regex("[a-f0-9]{64}\\.png")) }?.let { File(context.filesDir, "icons/$it") }
 suspend fun importImage(uri: Uri): String = withContext(Dispatchers.IO) {
  val bytes = context.contentResolver.openInputStream(uri)?.use { it.readLimited(2_000_000) } ?: error("Image unavailable")
  require(bytes.size <= 2_000_000) { "Image too large" }
  storeIcon(bytes) ?: error("Unsupported image")
 }
 suspend fun storeIcon(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
  if(bytes.size > 2_000_000) return@withContext null
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  if(bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) return@withContext null
  val options = BitmapFactory.Options().apply { inSampleSize = (maxOf(bounds.outWidth, bounds.outHeight) / 256).coerceAtLeast(1) }
  val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null
  val scale = minOf(1f, 256f / maxOf(decoded.width, decoded.height))
  val bitmap = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
  val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
  if (bitmap !== decoded) bitmap.recycle()
  decoded.recycle()
  val name = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) } + ".png"
  val directory = File(context.filesDir, "icons").apply { mkdirs() }
  val target = File(directory, name)
  if(!target.exists()) { val temp = File.createTempFile("icon", ".tmp", directory); temp.writeBytes(png); if(!temp.renameTo(target)) { temp.delete(); check(target.exists()) } }
  name
 }
}
