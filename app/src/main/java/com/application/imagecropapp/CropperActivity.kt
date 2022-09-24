package com.application.imagecropapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

class CropperActivity : AppCompatActivity() {
    lateinit var result: String
    lateinit var fileUri: Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropper)

        readIntent()

        val destUri = StringBuilder(UUID.randomUUID().toString()).append(".jpg").toString()

        val option = UCrop.Options()

        UCrop.of(fileUri, Uri.fromFile(File(cacheDir, destUri)))
            .withOptions(option)
            .withAspectRatio(0f, 0f)
            .useSourceImageAspectRatio()
            .withMaxResultSize(2000, 2000)
            .start(this@CropperActivity);
    }

    private fun readIntent() {
        val intent: Intent = intent
        if (intent.hasExtra("DATA")) {
            result = intent.getStringExtra("DATA")!!
            fileUri = Uri.parse(result)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            setResult(-1, Intent().putExtra("RESULT", resultUri.toString()))
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {

            val cropError = UCrop.getError(data!!)
        }
    }
}