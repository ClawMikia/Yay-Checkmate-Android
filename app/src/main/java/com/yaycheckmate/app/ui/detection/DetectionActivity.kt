package com.yaycheckmate.app.ui.detection

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yaycheckmate.app.R
import com.yaycheckmate.app.YayCheckmateApp
import com.yaycheckmate.app.data.LostObjectEntity
import com.yaycheckmate.app.databinding.ActivityDetectionBinding
import com.yaycheckmate.app.ml.ColorSignature
import com.yaycheckmate.app.ml.MlKitObjectPipeline
import com.yaycheckmate.app.ml.SimilarityEngine
import com.yaycheckmate.app.ml.TfliteImageEmbedder
import com.yaycheckmate.app.util.BitmapUtil
import com.yaycheckmate.app.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time “metal detector” experience: throttled frames, ML Kit boxes, TFLite embeddings,
 * cosine similarity vs registered objects, purple pulse + progress UI.
 */
class DetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetectionBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var embedder: TfliteImageEmbedder
    private lateinit var mlKit: MlKitObjectPipeline
    private lateinit var repository: com.yaycheckmate.app.data.LostObjectRepository

    private var allObjects: List<LostObjectEntity> = emptyList()
    private var preselectedId: Long? = null
    private var lastAnalyzeMs = 0L
    private var pulseScaleX: ObjectAnimator? = null
    private var pulseScaleY: ObjectAnimator? = null
    private var lastStrongVibration = 0L
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var appliedPreselectedTarget = false

    /**
     * Spinner index mirrored for the camera thread: never read [android.widget.Spinner] off the main thread.
     */
    private val selectedSpinnerIndex = AtomicInteger(0)

    /** Reference-photo color signature for the single selected object (histogram blend). */
    @Volatile
    private var cachedRefObjectId: Long = -1L

    @Volatile
    private var cachedRefHistogram: FloatArray? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
        }
        refreshUserLocationIfPossible()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as YayCheckmateApp
        embedder = app.embedder
        repository = app.repository
        mlKit = MlKitObjectPipeline()

        preselectedId = intent.getLongExtra(EXTRA_OBJECT_ID, -1L).takeIf { it > 0L }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.confidenceText.text = getString(R.string.confidence_format, 0)

        binding.targetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSpinnerIndex.set(position)
                maybeRecomputeEmbeddingForSelection(position)
                refreshReferenceAuxiliaryData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeAll().collect { list ->
                    val restoreId = objectIdAtSpinnerIndex(selectedSpinnerIndex.get())
                    allObjects = list
                    setupSpinner(list, restoreId)
                }
            }
        }

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
        )
    }

    private fun objectIdAtSpinnerIndex(index: Int): Long? {
        if (index <= 0) return null
        return allObjects.getOrNull(index - 1)?.id
    }

    private fun setupSpinner(list: List<LostObjectEntity>, restoreObjectId: Long?) {
        val labels = mutableListOf(getString(R.string.all_objects))
        labels += list.map { it.name }
        val adapter = ArrayAdapter(this, R.layout.item_spinner_target, android.R.id.text1, labels)
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.targetSpinner.adapter = adapter

        var newIndex = if (restoreObjectId != null) {
            val idx = list.indexOfFirst { it.id == restoreObjectId }
            if (idx >= 0) idx + 1 else 0
        } else {
            0
        }
        newIndex = newIndex.coerceIn(0, maxOf(0, labels.size - 1))
        binding.targetSpinner.setSelection(newIndex, false)
        selectedSpinnerIndex.set(newIndex)

        if (!appliedPreselectedTarget) {
            val id = preselectedId
            if (id == null) {
                appliedPreselectedTarget = true
            } else if (list.isNotEmpty()) {
                val idx = list.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    val pos = idx + 1
                    binding.targetSpinner.setSelection(pos, false)
                    selectedSpinnerIndex.set(pos)
                }
                appliedPreselectedTarget = true
            }
        }

        maybeRecomputeEmbeddingForSelection(selectedSpinnerIndex.get())
        refreshReferenceAuxiliaryData()
    }

    /**
     * Loads the registration photo for the current spinner selection and builds a color histogram
     * used to blend with TFLite similarity (single-object mode only).
     */
    private fun refreshReferenceAuxiliaryData() {
        lifecycleScope.launch(Dispatchers.Default) {
            val idx = selectedSpinnerIndex.get()
            if (idx <= 0) {
                cachedRefObjectId = -1L
                cachedRefHistogram = null
                return@launch
            }
            val entity = allObjects.getOrNull(idx - 1)
            if (entity == null) {
                cachedRefObjectId = -1L
                cachedRefHistogram = null
                return@launch
            }
            val file = repository.resolveImageFile(entity.imageRelativePath)
            if (file == null) {
                cachedRefObjectId = -1L
                cachedRefHistogram = null
                return@launch
            }
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp == null) {
                cachedRefObjectId = -1L
                cachedRefHistogram = null
                return@launch
            }
            val hist = ColorSignature.fromBitmap(bmp)
            bmp.recycle()
            cachedRefObjectId = entity.id
            cachedRefHistogram = hist
        }
    }

    private fun maybeRecomputeEmbeddingForSelection(position: Int) {
        if (position <= 0) return
        val entity = allObjects.getOrNull(position - 1) ?: return
        if (entity.cachedEmbedding != null) return
        lifecycleScope.launch(Dispatchers.IO) {
            repository.recomputeEmbeddingIfMissing(entity)
        }
    }

    private fun refreshUserLocationIfPossible() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        lifecycleScope.launch {
            val loc = LocationHelper.fetchLastLocation(this@DetectionActivity)
            userLatitude = loc?.latitude
            userLongitude = loc?.longitude
        }
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(cameraExecutor) { image ->
                processFrame(image)
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val minInterval = 1000L / TARGET_FPS
        if (now - lastAnalyzeMs < minInterval) {
            image.close()
            return
        }
        lastAnalyzeMs = now

        val rotation = image.imageInfo.rotationDegrees
        val detections = try {
            mlKit.detectBlocking(image, rotation)
        } catch (_: Exception) {
            emptyList()
        }

        val bitmap = try {
            image.toBitmap()
        } catch (_: Exception) {
            image.close()
            return
        }
        image.close()

        val bw = bitmap.width
        val bh = bitmap.height

        val targets = activeTargets()
        if (targets.isEmpty()) {
            bitmap.recycle()
            runOnUiThread { showIdleUi() }
            return
        }

        lifecycleScope.launch(Dispatchers.Default) {
            var bestScore = 0f
            var bestName = ""
            var bestBox: Rect? = null
            var bestEntity: LostObjectEntity? = null

            fun considerEmbedding(emb: FloatArray, box: Rect?, cropForHist: Bitmap?) {
                val refHist = cachedRefHistogram
                val refId = cachedRefObjectId
                for (obj in targets) {
                    val ref = obj.cachedEmbedding ?: continue
                    var s = SimilarityEngine.cosineSimilarity(emb, ref)
                    if (
                        targets.size == 1 &&
                        cropForHist != null &&
                        refHist != null &&
                        refId == obj.id
                    ) {
                        val liveHist = ColorSignature.fromBitmap(cropForHist)
                        val h = SimilarityEngine.cosineSimilarity(liveHist, refHist)
                        s = 0.52f * s + 0.48f * h
                    }
                    if (s > bestScore) {
                        bestScore = s
                        bestName = obj.name
                        bestBox = box
                        bestEntity = obj
                    }
                }
            }

            try {
                if (detections.isNotEmpty()) {
                    for (det in detections) {
                        val rect = det.boundingBox
                        if (rect.width() <= 1 || rect.height() <= 1) continue
                        val crop = BitmapUtil.cropSafe(bitmap, rect)
                        val emb = embedder.embed(crop)
                        considerEmbedding(emb, rect, crop)
                        if (crop !== bitmap) crop.recycle()
                    }
                }

                // Several center crops so the object can line up with registration framing.
                val fractions = floatArrayOf(1f, 0.82f, 0.64f, 0.45f)
                for (f in fractions) {
                    val region = BitmapUtil.centerCropFraction(bitmap, f)
                    val emb = embedder.embed(region)
                    considerEmbedding(emb, null, region)
                    if (region !== bitmap) region.recycle()
                }
            } finally {
                bitmap.recycle()
            }

            val ui = buildUiState(bestScore, bestName, bestBox, bestEntity, bw, bh)
            withContext(Dispatchers.Main) {
                applyUi(ui)
            }
        }
    }

    private fun activeTargets(): List<LostObjectEntity> {
        val list = allObjects
        val idx = selectedSpinnerIndex.get()
        if (idx <= 0) return list
        val entity = list.getOrNull(idx - 1) ?: return emptyList()
        if (entity.cachedEmbedding == null) return emptyList()
        return listOf(entity)
    }

    private data class UiFrame(
        val similarity: Float,
        val message: String,
        val percent: Int,
        val progress: Int,
        val box: Rect?,
        val bitmapW: Int,
        val bitmapH: Int,
        val direction: String?,
        val distanceText: String?,
    )

    private fun buildUiState(
        bestScore: Float,
        bestName: String,
        bestBox: Rect?,
        bestEntity: LostObjectEntity?,
        bitmapW: Int,
        bitmapH: Int,
    ): UiFrame {
        val message = when {
            bestScore >= SimilarityEngine.THRESHOLD_HIGH ->
                getString(R.string.match_strong) + if (bestName.isNotEmpty()) " ($bestName)" else ""
            bestScore >= SimilarityEngine.THRESHOLD_MEDIUM ->
                getString(R.string.match_weak) + if (bestName.isNotEmpty()) " ($bestName)" else ""
            else -> getString(R.string.scanning)
        }

        // Always reflect raw score on the meter; thresholds only change status text / pulse.
        val progress = SimilarityEngine.toDisplayPercent(bestScore)

        val direction = if (bestBox != null && bitmapW > 0) {
            val cx = (bestBox.left + bestBox.right) / 2f / bitmapW
            when {
                cx < 0.42f -> getString(R.string.direction_left)
                cx > 0.58f -> getString(R.string.direction_right)
                else -> getString(R.string.direction_center)
            }
        } else null

        val distanceText = if (
            bestEntity != null &&
            bestEntity.latitude != null &&
            bestEntity.longitude != null &&
            userLatitude != null &&
            userLongitude != null
        ) {
            val meters = LocationHelper.distanceMeters(
                userLatitude!!,
                userLongitude!!,
                bestEntity.latitude!!,
                bestEntity.longitude!!,
            )
            getString(R.string.distance_hint, formatDistance(meters))
        } else null

        return UiFrame(
            similarity = bestScore,
            message = message,
            percent = SimilarityEngine.toDisplayPercent(bestScore),
            progress = progress,
            box = bestBox,
            bitmapW = bitmapW,
            bitmapH = bitmapH,
            direction = if (bestScore >= SimilarityEngine.THRESHOLD_MEDIUM) direction else null,
            distanceText = if (bestScore >= SimilarityEngine.THRESHOLD_MEDIUM) distanceText else null,
        )
    }

    private fun applyUi(frame: UiFrame) {
        binding.statusMessage.text = frame.message
        binding.confidenceText.text = getString(R.string.confidence_format, frame.percent)
        binding.signalBar.progress = frame.progress

        if (frame.distanceText != null) {
            binding.distanceHint.text = frame.distanceText
            binding.distanceHint.visibility = android.view.View.VISIBLE
        } else {
            binding.distanceHint.visibility = android.view.View.GONE
        }

        if (frame.direction != null) {
            binding.directionHint.text = frame.direction
            binding.directionHint.visibility = android.view.View.VISIBLE
        } else {
            binding.directionHint.visibility = android.view.View.GONE
        }

        val box = frame.box
        if (box != null && frame.bitmapW > 0 && frame.bitmapH > 0 &&
            frame.similarity >= SimilarityEngine.THRESHOLD_MEDIUM
        ) {
            binding.boxOverlay.setNormalizedBox(
                box.left / frame.bitmapW.toFloat(),
                box.top / frame.bitmapH.toFloat(),
                box.right / frame.bitmapW.toFloat(),
                box.bottom / frame.bitmapH.toFloat(),
            )
        } else {
            binding.boxOverlay.clearBox()
        }

        updatePulse(frame.similarity)
        maybeVibrateStrong(frame.similarity)
    }

    private fun showIdleUi() {
        binding.statusMessage.text = getString(R.string.scanning)
        binding.confidenceText.text = getString(R.string.confidence_format, 0)
        binding.signalBar.progress = 0
        binding.boxOverlay.clearBox()
        updatePulse(0f)
    }

    private fun updatePulse(similarity: Float) {
        binding.pulseRing.alpha = if (similarity >= SimilarityEngine.THRESHOLD_LOW) {
            (0.15f + (similarity.coerceIn(0f, 1f) * 0.55f)).coerceIn(0.1f, 0.85f)
        } else {
            0.05f
        }

        val duration = when {
            similarity >= SimilarityEngine.THRESHOLD_HIGH -> 350L
            similarity >= SimilarityEngine.THRESHOLD_MEDIUM -> 700L
            similarity >= SimilarityEngine.THRESHOLD_LOW -> 1400L
            else -> 2200L
        }

        if (pulseScaleX == null) {
            pulseScaleX = ObjectAnimator.ofFloat(binding.pulseRing, View.SCALE_X, 0.85f, 1.22f).apply {
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                this.duration = duration
                start()
            }
            pulseScaleY = ObjectAnimator.ofFloat(binding.pulseRing, View.SCALE_Y, 0.85f, 1.22f).apply {
                interpolator = LinearInterpolator()
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                this.duration = duration
                start()
            }
        } else {
            pulseScaleX?.duration = duration
            pulseScaleY?.duration = duration
        }
    }

    private fun maybeVibrateStrong(similarity: Float) {
        if (similarity < SimilarityEngine.THRESHOLD_HIGH) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastStrongVibration < 1200L) return
        lastStrongVibration = now
        val vib = ContextCompat.getSystemService(this, Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(45)
        }
    }

    private fun formatDistance(meters: Float): String {
        return if (meters >= 1000) "%.1f km".format(meters / 1000f)
        else if (meters >= 1) "%.0f m".format(meters)
        else "%.0f cm".format(meters * 100f)
    }

    override fun onDestroy() {
        pulseScaleX?.cancel()
        pulseScaleY?.cancel()
        mlKit.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_OBJECT_ID = "detection_object_id"
        private const val TARGET_FPS = 8

        fun createIntent(context: Context, objectId: Long?): Intent =
            Intent(context, DetectionActivity::class.java).apply {
                objectId?.let { putExtra(EXTRA_OBJECT_ID, it) }
            }
    }
}
