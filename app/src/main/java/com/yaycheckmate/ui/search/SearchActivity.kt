package com.yaycheckmate.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.databinding.ActivitySearchBinding
import com.yaycheckmate.utils.toFormattedDuration
import com.yaycheckmate.utils.toast
import com.yaycheckmate.viewmodel.SearchViewModel
import com.yaycheckmate.viewmodel.SearchViewModelFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OBJECT_ID = "extra_object_id"
    }

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(application as YayCheckmateApp)
    }

    private lateinit var cameraExecutor: ExecutorService
    private var objectDetector: ObjectDetector? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else toast("Camera permission required for scanning")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val objectId = intent.getLongExtra(EXTRA_OBJECT_ID, -1L)
        if (objectId == -1L) { finish(); return }

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupObjectDetector()

        viewModel.loadObject(objectId)
        setupObservers()
        setupButtons()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewModel.startSearch()
    }

    private fun setupObjectDetector() {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .enableMultipleObjects()
            .build()
        objectDetector = ObjectDetection.getClient(options)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("SearchActivity", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        objectDetector?.process(image)
            ?.addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isNotEmpty()) {
                    val best = detectedObjects.maxByOrNull {
                        it.labels.firstOrNull()?.confidence ?: 0f
                    }
                    val label = best?.labels?.firstOrNull()?.text ?: "Object"
                    val conf = ((best?.labels?.firstOrNull()?.confidence ?: 0f) * 100).toInt()
                    runOnUiThread { viewModel.updateDetection(label, conf) }
                }
                imageProxy.close()
            }
            ?.addOnFailureListener { imageProxy.close() }
    }

    private fun setupObservers() {
        viewModel.currentObject.observe(this) { obj ->
            obj ?: return@observe
            binding.tvObjectName.text = obj.name
            binding.tvDifficulty.text = obj.difficulty
        }

        viewModel.elapsedSeconds.observe(this) { seconds ->
            binding.tvTimer.text = seconds.toFormattedDuration()
        }

        viewModel.detectionConfidence.observe(this) { conf ->
            binding.progressConfidence.progress = conf
            binding.tvConfidence.text = "$conf%"
            val color = when {
                conf >= 80 -> ContextCompat.getColor(this, com.yaycheckmate.R.color.colorSuccess)
                conf >= 50 -> ContextCompat.getColor(this, com.yaycheckmate.R.color.colorWarning)
                else -> ContextCompat.getColor(this, com.yaycheckmate.R.color.colorAccent)
            }
            binding.tvConfidence.setTextColor(color)
        }

        viewModel.detectionLabel.observe(this) { label ->
            binding.tvDetectionLabel.text = label
        }

        viewModel.mascotMessage.observe(this) { msg ->
            binding.tvMascotMessage.text = msg
        }

        viewModel.heatmapData.observe(this) { heatmap ->
            if (heatmap.isNotEmpty()) {
                val sb = StringBuilder("Search here first:\n")
                heatmap.entries.take(3).forEach { (loc, pct) ->
                    sb.append("📍 $loc: $pct%\n")
                }
                binding.tvHeatmapHint.text = sb.toString().trim()
            } else {
                binding.tvHeatmapHint.text = "No location history yet.\nSearch everywhere!"
            }
        }

        viewModel.searchCompleted.observe(this) { session ->
            session ?: return@observe
            vibrate()
            showFoundDialog(session.foundLocation, session.durationSeconds, session.xpEarned)
        }
    }

    private fun setupButtons() {
        binding.btnFound.setOnClickListener { showLocationPicker() }
        binding.btnCancel.setOnClickListener {
            viewModel.cancelSearch()
            finish()
        }
    }

    private fun showLocationPicker() {
        val locations = arrayOf(
            "Bedroom", "Living Room", "Kitchen", "Bathroom",
            "Office", "Car", "Bag/Backpack", "Coat Pocket",
            "Drawer", "Table", "Sofa", "Shelf", "Other"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Where did you find it?")
            .setItems(locations) { _, which ->
                viewModel.markFound(locations[which])
            }
            .show()
    }

    private fun showFoundDialog(location: String, duration: Long, xp: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 Checkmate! Found it!")
            .setMessage("Location: $location\nTime: ${duration.toFormattedDuration()}\n\n+$xp XP earned!")
            .setPositiveButton("Awesome!") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun vibrate() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        } catch (e: Exception) { /* ignore */ }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        objectDetector?.close()
    }
}
