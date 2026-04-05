package com.yaycheckmate.app.data

import android.content.Context
import android.graphics.BitmapFactory
import com.yaycheckmate.app.ml.TfliteImageEmbedder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Single entry for persistence + embedding cache refresh when images/metadata change.
 */
class LostObjectRepository(
    private val appContext: Context,
    private val dao: LostObjectDao,
    private val embedder: TfliteImageEmbedder,
) {

    fun observeAll(): Flow<List<LostObjectEntity>> = dao.observeAll()

    fun observeById(id: Long): Flow<LostObjectEntity?> = dao.observeById(id)

    suspend fun getById(id: Long): LostObjectEntity? = dao.getById(id)

    suspend fun delete(entity: LostObjectEntity) = withContext(Dispatchers.IO) {
        resolveImageFile(entity.imageRelativePath)?.delete()
        dao.delete(entity)
    }

    suspend fun saveNewObject(
        name: String,
        description: String,
        tags: String,
        tempImageFile: File,
        latitude: Double?,
        longitude: Double?,
    ): Long = withContext(Dispatchers.IO) {
        val relative = "objects/${UUID.randomUUID()}.jpg"
        val dest = File(appContext.filesDir, relative)
        dest.parentFile?.mkdirs()
        tempImageFile.copyTo(dest, overwrite = true)
        tempImageFile.delete()

        val bitmap = BitmapFactory.decodeFile(dest.absolutePath)
            ?: throw IllegalStateException("Could not decode saved image")
        val embedding = embedder.embed(bitmap)
        bitmap.recycle()

        val entity = LostObjectEntity(
            name = name.trim(),
            description = description.trim(),
            tags = tags.trim(),
            imageRelativePath = relative,
            createdAtMillis = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            cachedEmbedding = embedding,
        )
        dao.insert(entity)
    }

    suspend fun updateObject(entity: LostObjectEntity, newImageFile: File?) = withContext(Dispatchers.IO) {
        var updated = entity
        if (newImageFile != null) {
            val dest = File(appContext.filesDir, entity.imageRelativePath)
            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { out ->
                newImageFile.inputStream().use { it.copyTo(out) }
            }
            newImageFile.delete()
            val bitmap = BitmapFactory.decodeFile(dest.absolutePath)
                ?: throw IllegalStateException("Could not decode image")
            val embedding = embedder.embed(bitmap)
            bitmap.recycle()
            updated = updated.copy(cachedEmbedding = embedding)
        }
        dao.update(updated)
    }

    fun resolveImageFile(relativePath: String): File? {
        val f = File(appContext.filesDir, relativePath)
        return if (f.exists()) f else null
    }

    suspend fun recomputeEmbeddingIfMissing(entity: LostObjectEntity): LostObjectEntity =
        withContext(Dispatchers.IO) {
            if (entity.cachedEmbedding != null) return@withContext entity
            val file = resolveImageFile(entity.imageRelativePath) ?: return@withContext entity
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext entity
            val emb = embedder.embed(bitmap)
            bitmap.recycle()
            val fixed = entity.copy(cachedEmbedding = emb)
            dao.update(fixed)
            fixed
        }
}
