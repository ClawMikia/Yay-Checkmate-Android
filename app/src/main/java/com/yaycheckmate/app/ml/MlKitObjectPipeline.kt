package com.yaycheckmate.app.ml

import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around ML Kit’s default object detector for bounding boxes on the preview stream.
 * Runs synchronously on the caller thread after converting [ImageProxy] → [InputImage]
 * (keeps frame pacing under control from the throttled analyzer).
 */
class MlKitObjectPipeline {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build(),
    )

    fun close() {
        detector.close()
    }

    /**
     * Detects objects; caller must still [ImageProxy.close] after this returns.
     */
    fun detectBlocking(image: ImageProxy, rotationDegrees: Int): List<DetectedObject> {
        val mediaImage = image.image ?: return emptyList()
        val input = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        return try {
            Tasks.await(detector.process(input), 500, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
