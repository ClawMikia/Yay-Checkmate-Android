package com.yaycheckmate.app.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.isVisible
import com.yaycheckmate.app.databinding.ActivityAddObjectBinding
import com.yaycheckmate.app.util.LocationHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/**
 * Registers a new object: live rear-camera preview, capture, optional GPS, Room persistence + embedding cache.
 */
class AddObjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddObjectBinding
    private val viewModel: AddObjectViewModel by viewModels()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var pendingCaptureFile: File? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) {
            startCamera()
        } else {
            Toast.makeText(this, com.yaycheckmate.app.R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
        }
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            refreshLocationLabel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddObjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnSave.setOnClickListener { saveObject() }
        binding.inputName.doOnTextChanged { _, _, _, _ ->
            binding.btnSave.isEnabled =
                pendingCaptureFile != null && binding.inputName.text?.isNotBlank() == true
        }

        viewModel.saveState.observe(this) { state ->
            when (state) {
                is AddObjectViewModel.SaveState.Saving -> binding.btnSave.isEnabled = false
                is AddObjectViewModel.SaveState.Done -> {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AddObjectViewModel.SaveState.Error -> {
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }

        ensurePermissionsAndCamera()
    }

    private fun ensurePermissionsAndCamera() {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (need.isNotEmpty()) {
            permissionLauncher.launch(need.toTypedArray())
        } else {
            startCamera()
            refreshLocationLabel()
        }
    }

    private fun refreshLocationLabel() {
        lifecycleScope.launch {
            val loc = LocationHelper.fetchLastLocation(this@AddObjectActivity)
            if (loc != null) {
                lastLatitude = loc.latitude
                lastLongitude = loc.longitude
                binding.locationStatus.text = "GPS: ${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}"
            } else {
                binding.locationStatus.text = "Location not available (optional)"
            }
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
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val file = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            opts,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        pendingCaptureFile = file
                        binding.capturedPreview.isVisible = true
                        binding.capturedPreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                        binding.btnSave.isEnabled = binding.inputName.text?.isNotBlank() == true
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@AddObjectActivity, "Capture failed", Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }

    private fun saveObject() {
        val file = pendingCaptureFile ?: run {
            Toast.makeText(this, "Capture a photo first", Toast.LENGTH_SHORT).show()
            return
        }
        val name = binding.inputName.text?.toString().orEmpty()
        if (name.isBlank()) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.save(
            tempImageFile = file,
            name = name,
            description = binding.inputDescription.text?.toString().orEmpty(),
            tags = binding.inputTags.text?.toString().orEmpty(),
            latitude = lastLatitude,
            longitude = lastLongitude,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
