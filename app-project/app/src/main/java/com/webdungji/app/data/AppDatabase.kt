package com.webdungji.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "web_apps")
data class WebAppEntity(
 @PrimaryKey val id: String,
 val name: String,
 val startUrl: String,
 val iconRef: String? = null,
 val profileId: String = "default",
 val displayOrder: Long,
 val javaScript: Boolean = true,
 val images: Boolean = true,
 val textZoom: Int = 100,
 val createdAt: Long,
 val updatedAt: Long,
 val deletedAt: Long? = null,
 val revision: Long = 1,
 val dirty: Boolean = true,
 val schemaVersion: Int = 1
)

@Entity(tableName = "local_changes", indices = [Index("appId")])
data class LocalChange(
 @PrimaryKey val changeId: String,
 val appId: String,
 val revision: Long,
 val changedAt: Long,
 val operation: String
)

@Dao
interface WebAppDao {
 @Query("SELECT * FROM web_apps WHERE deletedAt IS NULL ORDER BY displayOrder, createdAt")
 fun observeActive(): Flow<List<WebAppEntity>>
 @Query("SELECT * FROM web_apps WHERE id = :id") suspend fun find(id: String): WebAppEntity?
 @Query("SELECT * FROM web_apps ORDER BY displayOrder") suspend fun allIncludingDeleted(): List<WebAppEntity>
 @Query("SELECT * FROM web_apps WHERE dirty = 1 ORDER BY updatedAt") suspend fun pending(): List<WebAppEntity>
 @Query("SELECT * FROM local_changes ORDER BY changedAt, changeId") suspend fun changes(): List<LocalChange>
 @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM web_apps") suspend fun nextOrder(): Long
 @Upsert suspend fun upsert(app: WebAppEntity)
 @Insert suspend fun record(change: LocalChange)
 @Query("UPDATE web_apps SET dirty = 0 WHERE id = :id AND revision = :revision") suspend fun acknowledge(id: String, revision: Long)
 @Query("DELETE FROM local_changes WHERE appId = :id AND revision <= :revision") suspend fun acknowledgeChanges(id: String, revision: Long)
}

@Database(entities = [WebAppEntity::class, LocalChange::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun webApps(): WebAppDao }
