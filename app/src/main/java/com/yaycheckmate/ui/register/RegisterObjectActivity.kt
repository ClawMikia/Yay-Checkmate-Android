package com.yaycheckmate.ui.register

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.yaycheckmate.R
import com.yaycheckmate.YayCheckmateApp
import com.yaycheckmate.databinding.ActivityRegisterObjectBinding
import com.yaycheckmate.utils.GameConstants
import com.yaycheckmate.utils.toast
import com.yaycheckmate.viewmodel.RegisterViewModel
import com.yaycheckmate.viewmodel.RegisterViewModelFactory

class RegisterObjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterObjectBinding

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(application as YayCheckmateApp)
    }

    private var currentPhotoAngle = "front"

    private val categories = listOf(
        "Keys & Accessories", "Electronics", "Documents", "Clothing",
        "Bags & Wallets", "Tools", "Personal Care", "Other"
    )

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCameraForAngle(currentPhotoAngle)
        else toast("Camera permission needed for photos")
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            updatePhotoUI(currentPhotoAngle)
            toast("${currentPhotoAngle.replaceFirstChar { it.uppercase() }} photo saved!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterObjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCategoryDropdown()
        setupDifficultyPreview()
        setupPhotoButtons()
        setupSaveButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, R.layout.item_dropdown, categories)
        (binding.tilCategory.editText as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupDifficultyPreview() {
        binding.etObjectName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateDifficultyPreview()
        }
    }

    private fun updateDifficultyPreview() {
        val name = binding.etObjectName.text.toString()
        if (name.isNotEmpty()) {
            val (difficulty, score) = GameConstants.getDifficultyForName(name)
            binding.tvDifficultyPreview.text = "Detection Difficulty: $difficulty"
            binding.tvDifficultyPreview.setTextColor(GameConstants.getDifficultyColor(difficulty))
        }
    }

    private fun setupPhotoButtons() {
        binding.btnPhotoFront.setOnClickListener { requestCameraAndCapture("front") }
        binding.btnPhotoBack.setOnClickListener { requestCameraAndCapture("back") }
        binding.btnPhotoLeft.setOnClickListener { requestCameraAndCapture("left") }
        binding.btnPhotoRight.setOnClickListener { requestCameraAndCapture("right") }
        binding.btnPhotoTop.setOnClickListener { requestCameraAndCapture("top") }
    }

    private fun requestCameraAndCapture(angle: String) {
        currentPhotoAngle = angle
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCameraForAngle(angle)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCameraForAngle(angle: String) {
        val file = com.yaycheckmate.utils.ImageUtils.createImageFile(this, "OBJ_${angle.uppercase()}")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.provider", file
        )
        when (angle) {
            "front" -> viewModel.photoFrontPath = file.absolutePath
            "back" -> viewModel.photoBackPath = file.absolutePath
            "left" -> viewModel.photoLeftPath = file.absolutePath
            "right" -> viewModel.photoRightPath = file.absolutePath
            "top" -> viewModel.photoTopPath = file.absolutePath
        }
        cameraLauncher.launch(uri)
    }

    private fun updatePhotoUI(angle: String) {
        val btn = when (angle) {
            "front" -> binding.btnPhotoFront
            "back" -> binding.btnPhotoBack
            "left" -> binding.btnPhotoLeft
            "right" -> binding.btnPhotoRight
            "top" -> binding.btnPhotoTop
            else -> null
        }
        btn?.text = "✓ ${angle.replaceFirstChar { it.uppercase() }}"
        btn?.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSuccess))
        updatePhotoCount()
    }

    private fun updatePhotoCount() {
        var count = 0
        if (viewModel.photoFrontPath.isNotEmpty()) count++
        if (viewModel.photoBackPath.isNotEmpty()) count++
        if (viewModel.photoLeftPath.isNotEmpty()) count++
        if (viewModel.photoRightPath.isNotEmpty()) count++
        if (viewModel.photoTopPath.isNotEmpty()) count++
        binding.tvPhotoCount.text = "$count/5 photos taken"
        binding.progressPhotos.progress = (count * 20)
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val name = binding.etObjectName.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (name.isEmpty()) {
                binding.tilObjectName.error = "Object name is required"
                return@setOnClickListener
            }
            if (category.isEmpty()) {
                binding.tilCategory.error = "Please select a category"
                return@setOnClickListener
            }
            binding.tilObjectName.error = null
            binding.tilCategory.error = null
            viewModel.registerObject(name, category, description)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.btnSave.isEnabled = !loading
            binding.btnSave.text = if (loading) "Saving..." else "Register Object"
        }

        viewModel.registrationResult.observe(this) { result ->
            result.onSuccess {
                toast("Object registered successfully! +25 XP")
                finish()
            }
            result.onFailure {
                toast("Failed to register: ${it.message}")
            }
        }
    }
}
