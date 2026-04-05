package com.yaycheckmate.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One registered lost/found item with optional GPS and a cached visual embedding
 * so we do not re-run TFLite on every library scroll.
 */
@Entity(tableName = "lost_objects")
data class LostObjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val tags: String,
    /** Path under [Context.filesDir] (no leading slash). */
    val imageRelativePath: String,
    val createdAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    /** L2-normalized MobileNet output used as an embedding proxy; see [com.yaycheckmate.app.ml.TfliteImageEmbedder]. */
    val cachedEmbedding: FloatArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LostObjectEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (tags != other.tags) return false
        if (imageRelativePath != other.imageRelativePath) return false
        if (createdAtMillis != other.createdAtMillis) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (cachedEmbedding != null) {
            if (other.cachedEmbedding == null) return false
            if (!cachedEmbedding.contentEquals(other.cachedEmbedding)) return false
        } else if (other.cachedEmbedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + imageRelativePath.hashCode()
        result = 31 * result + createdAtMillis.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (cachedEmbedding?.contentHashCode() ?: 0)
        return result
    }
}
