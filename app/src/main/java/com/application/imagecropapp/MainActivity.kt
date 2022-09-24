package com.application.imagecropapp

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.application.imagecropapp.databinding.ActivityMainBinding
import com.application.imagecropapp.databinding.CameraDailogBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    val APP_TAG = "IMAGE_CROP"
    var photoFileName = "photo.jpg"
    lateinit var photoFile: File

    val PERMISSION_ID = 44

    private var bitmap: Bitmap? = null
    lateinit var binding: ActivityMainBinding

    var filePath: String? = null
    var fileUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onResume() {
        super.onResume()
        if (binding.cropImageView.drawable == null) {
            showImageDailog()
        }
    }

    private fun showImageDailog() {

        val alt = android.app.AlertDialog.Builder(this)
        val dv = CameraDailogBinding.inflate(layoutInflater)

        alt.setView(dv.root)
        val dialog = alt.create()
        dialog.show()
        dv.dailogCamera.setOnClickListener {
            enableRuntimePermissionToAccessCamera()
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            photoFile = getPhotoFileUri(photoFileName)


            val fileProvider: Uri =
                FileProvider.getUriForFile(
                    this,
                    "com.application.imagecropapp.fileprovider",
                    photoFile
                )

            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            cameraIntent.launch(intent)

            dialog.dismiss()
        }

        dv.dailogGallery.setOnClickListener {
            galleryIntent.launch(
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
            )
            dialog.dismiss()
        }
    }

    @Throws(IOException::class)
    private fun getPhotoFileUri(photoFileName: String): File {

        val mediaStorageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(APP_TAG, "failed to create directory")
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + photoFileName)

    }


    /** Run Time Permission for Camera App */

    fun enableRuntimePermissionToAccessCamera() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {

            // Printing toast message after enabling runtime permission.
            Toast.makeText(
                applicationContext,
                "CAMERA permission allows us to Access CAMERA app",
                Toast.LENGTH_LONG
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_ID
            )
        }
    }

    // Activity Result for Cropping
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == -1 && requestCode == 101) {
            val result = data!!.getStringExtra("RESULT")
            if (result != null) {
                binding.cropImageView.setImageURI(Uri.parse(result))
            }
        }
    }

    // Activity Result for Camera
    val cameraIntent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                Log.d("Path", photoFile.absolutePath)
                filePath = photoFile.absolutePath
                bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.cropImageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Activity Result for Gallery
    val galleryIntent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {

            val data = result.data
            try {
                if (data != null) {
                    val contentURI = data.data
                    filePath = getRealPathFromURI(contentURI.toString())
                    fileUri = data.data
                    Log.d("Path", filePath.toString())
                    bitmap =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            contentURI?.let {
                                ImageDecoder.createSource(
                                    this.contentResolver,
                                    it
                                )
                            }?.let { ImageDecoder.decodeBitmap(it) }
                        } else {
                            MediaStore.Images.Media.getBitmap(
                                this.contentResolver,
                                contentURI
                            )
                        }
                    binding.cropImageView.setImageBitmap(bitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.img_action, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_img -> {
                showImageDailog()
            }
            R.id.crop_img -> {
                val intent = Intent(this@MainActivity, CropperActivity::class.java)
                intent.putExtra("DATA", fileUri.toString())
                startActivityForResult(intent, 101)
            }
            R.id.flip_img -> {
                flipImageHorizontal()
            }
            R.id.rotate_img -> {
                flipImageVertical()
            }
            R.id.save_img -> {
                saveImage()
            }
            R.id.info_img -> {
                imageDetails()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    fun flipImageHorizontal() {
        val matrix = Matrix()
        matrix.setScale(-1f, 1f)

        try {
            bitmap =
                Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            binding.cropImageView.setImageBitmap(bitmap)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }

    fun flipImageVertical() {
        val matrix = Matrix()
        matrix.setRotate(180f)
        matrix.postScale(-1f, 1f)

        try {
            bitmap =
                Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
            binding.cropImageView.setImageBitmap(bitmap)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }

    fun imageDetails() {
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath!!)
            val builder = StringBuilder()

            builder.append(
                "Date & Time: " +
                        exif.getAttribute(
                            ExifInterface.TAG_DATETIME
                        )
                        + "\n\n"
            )
            builder.append("Flash: " + exif.getAttribute(ExifInterface.TAG_FLASH) + "\n")
            builder.append(
                "Focal Length: " + exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) + "\n\n"
            )
            builder.append(
                "GPS Datestamp: " + exif.getAttribute(
                    ExifInterface.TAG_FLASH
                ) + "\n"
            )
            builder.append(
                "GPS Latitude: " + exif.getAttribute(
                    ExifInterface.TAG_GPS_LATITUDE
                ) + "\n"
            )
            builder.append(
                "GPS Longitude: " + exif.getAttribute(
                    ExifInterface.TAG_GPS_LONGITUDE
                ) + "\n"
            )
            builder.append(
                "GPS Processing Method: " + exif.getAttribute(
                    ExifInterface.TAG_GPS_PROCESSING_METHOD
                ) + "\n"
            )
            builder.append(
                "GPS Timestamp: " + exif.getAttribute(
                    ExifInterface.TAG_GPS_TIMESTAMP
                ) + "\n\n"
            )
            builder.append(
                "Image Length: " + exif.getAttribute(
                    ExifInterface.TAG_IMAGE_LENGTH
                ) + "\n"
            )
            builder.append(
                "Image Width: " + exif.getAttribute(
                    ExifInterface.TAG_IMAGE_WIDTH
                ) + "\n\n"
            )
            builder.append(
                "Camera Make: " + exif.getAttribute(
                    ExifInterface.TAG_MAKE
                ) + "\n"
            )
            builder.append(
                "Camera Model: " + exif.getAttribute(
                    ExifInterface.TAG_MODEL
                ) + "\n"
            )
            builder.append(
                "Camera Orientation: " + exif.getAttribute(
                    ExifInterface.TAG_ORIENTATION
                ) + "\n"
            )
            builder.append(
                "Camera White Balance: " + exif.getAttribute(
                    ExifInterface.TAG_WHITE_BALANCE
                ) + "\n"
            )
            AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(builder.toString())
                .setNegativeButton(
                    "OK"
                ) { _: DialogInterface, _: Int ->

                }
                .show()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    private fun getRealPathFromURI(contentURI: String): String? {
        val contentUri = Uri.parse(contentURI)
        val cursor: Cursor? = contentResolver.query(contentUri, null, null, null, null)
        return if (cursor == null) {
            contentUri.path
        } else {
            cursor.moveToFirst()
            val index: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(index)
        }
    }

    private fun saveImage() {
        val path = Environment.DIRECTORY_PICTURES.toString()
        var fOut: OutputStream?
        val file = File(
            path,
            "crop_Image_${UUID.randomUUID()}.jpg"
        ) // the File to save , append increasing numeric counter to prevent files from getting overwritten.

        fOut = FileOutputStream(file)

        val pictureBitmap: Bitmap = binding.cropImageView.drawable.toBitmap()

        pictureBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            85,
            fOut
        ) // saving the Bitmap to a file compressed as a JPEG with 85% compression rate

        fOut.flush()

        fOut.close()


        MediaStore.Images.Media.insertImage(
            contentResolver,
            file.absolutePath,
            file.name,
            file.name
        )
    }
}