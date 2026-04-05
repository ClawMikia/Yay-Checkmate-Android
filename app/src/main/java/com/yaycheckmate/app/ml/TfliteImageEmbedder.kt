package com.yaycheckmate.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs the bundled MobileNet V2 1.0 224 **quantized** ImageNet classifier and
 * treats the output tensor (after dequantization to float) as a visual embedding.
 *
 * This is a practical on-device proxy for “same object / similar appearance” when
 * a dedicated feature-vector model is not bundled; swap [MODEL_ASSET] for a TF Hub
 * feature extractor without changing callers.
 */
class TfliteImageEmbedder(context: Context) : Closeable {

    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val isQuantizedInput: Boolean
    val outputSize: Int

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, MODEL_ASSET)
        val options = Interpreter.Options().apply {
            numThreads = 4
            useNNAPI = false
        }
        interpreter = Interpreter(modelBuffer, options)
        val inTensor = interpreter.getInputTensor(0)
        val inShape = inTensor.shape()
        inputHeight = inShape[1]
        inputWidth = inShape[2]
        isQuantizedInput = inTensor.dataType() == DataType.UINT8
        val outShape = interpreter.getOutputTensor(0).shape()
        outputSize = outShape.last()
    }

    /**
     * Produces an L2-normalized embedding suitable for cosine similarity.
     */
    fun embed(bitmap: Bitmap): FloatArray {
        // getPixels requires ARGB_8888 (hardware-backed bitmaps must be copied).
        val argb = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val scaled = if (argb.width == inputWidth && argb.height == inputHeight) {
            argb
        } else {
            Bitmap.createScaledBitmap(argb, inputWidth, inputHeight, true)
        }
        if (argb !== bitmap && argb !== scaled) argb.recycle()

        val input = bitmapToModelInput(scaled)
        if (scaled !== bitmap) scaled.recycle()

        val outTensor = interpreter.getOutputTensor(0)
        val raw = runInference(input, outTensor)
        return SimilarityEngine.l2Normalize(raw)
    }

    /**
     * Quantized models expose UINT8/INT8 outputs; passing [FloatArray] to [Interpreter.run] throws at runtime.
     */
    private fun runInference(input: Any, outTensor: Tensor): FloatArray {
        return when (outTensor.dataType()) {
            DataType.FLOAT32 -> {
                val output = Array(1) { FloatArray(outputSize) }
                interpreter.run(input, output)
                output[0]
            }
            DataType.UINT8, DataType.INT8 -> {
                val buf = ByteBuffer.allocateDirect(outTensor.numBytes())
                buf.order(ByteOrder.nativeOrder())
                interpreter.run(input, buf)
                buf.rewind()
                dequantizeToFloat(buf, outTensor)
            }
            else -> error("Unsupported output tensor type: ${outTensor.dataType()}")
        }
    }

    private fun dequantizeToFloat(buffer: ByteBuffer, tensor: Tensor): FloatArray {
        val qp = tensor.quantizationParams()
        val scale = qp.scale.takeIf { it > 0f && !it.isNaN() } ?: 1f
        val zeroPoint = qp.zeroPoint
        val floats = FloatArray(outputSize)
        when (tensor.dataType()) {
            DataType.UINT8 -> {
                for (i in 0 until outputSize) {
                    val q = buffer.get().toInt() and 0xFF
                    floats[i] = (q - zeroPoint) * scale
                }
            }
            DataType.INT8 -> {
                for (i in 0 until outputSize) {
                    val q = buffer.get().toInt()
                    floats[i] = (q - zeroPoint) * scale
                }
            }
            else -> Unit
        }
        return floats
    }

    private fun bitmapToModelInput(bitmap: Bitmap): Any {
        if (!isQuantizedInput) {
            val buffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * 4)
            buffer.order(ByteOrder.nativeOrder())
            fillFloatRgb(buffer, bitmap)
            buffer.rewind()
            return buffer
        }
        val buffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3)
        buffer.order(ByteOrder.nativeOrder())
        fillUint8Rgb(buffer, bitmap)
        buffer.rewind()
        return buffer
    }

    private fun fillUint8Rgb(buffer: ByteBuffer, bitmap: Bitmap) {
        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte())
            buffer.put(((pixel shr 8) and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
    }

    /** Float model fallback: values in [0,1] if needed; most quant models use uint8 path. */
    private fun fillFloatRgb(buffer: ByteBuffer, bitmap: Bitmap) {
        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private const val MODEL_ASSET = "mobilenet_v2_1.0_224_quant.tflite"
    }
}
