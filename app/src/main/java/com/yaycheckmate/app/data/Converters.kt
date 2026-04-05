package com.yaycheckmate.app.data

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Persists cached embedding vectors as raw bytes for Room.
 */
class Converters {

    @TypeConverter
    fun floatArrayToBytes(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val buffer = ByteBuffer.allocate(value.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in value) buffer.putFloat(f)
        return buffer.array()
    }

    @TypeConverter
    fun bytesToFloatArray(value: ByteArray?): FloatArray? {
        if (value == null || value.isEmpty()) return null
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(value.size / 4)
        for (i in out.indices) out[i] = buffer.float
        return out
    }
}
