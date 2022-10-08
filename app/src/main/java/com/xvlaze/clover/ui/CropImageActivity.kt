package com.xvlaze.clover.ui

import android.os.Bundle
import com.canhub.cropper.CropImageActivity
import com.xvlaze.clover.databinding.ActivityCropImageBinding

class CropImageActivity : CropImageActivity() {
    private lateinit var binding: ActivityCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCropImageView(binding.cropImageView)
    }

    // FIXME: No va!
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}