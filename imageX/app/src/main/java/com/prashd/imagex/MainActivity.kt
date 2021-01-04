package com.prashd.imagex

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.prashd.imagex.databinding.ActivityMainBinding
import com.prashd.imagex.ml.FinalModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel


class MainActivity : AppCompatActivity() {

    val PERMISSION_CODE: Int = 1000
    val IMAGE_CAPTURE_CODE: Int = 1001
    var image_uri: Uri? = null
    var selectedPhotoUri: Uri? = null
    var clicked: Boolean = false
    val IMAGE_PICK_CODE = 5
    private val TAG = "MainActivity"
    private lateinit var bitmap: Bitmap
    private lateinit var binding: ActivityMainBinding

    val labelsList = arrayListOf("Safe", "Unsafe")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CameraActivity()
        binding.btnGallery.setOnClickListener {
            clicked = true
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,IMAGE_PICK_CODE)

        }
    }

    private fun CameraActivity() {
        binding.btnCamera.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkCallingOrSelfPermission(android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
                    //permission denied
                    val permission = arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    openCamera()
                }
            } else {
                openCamera()
            }
        }
    }

    private fun openCamera() {
        this@MainActivity.runOnUiThread {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From the camera")
            image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
            startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(baseContext, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this@MainActivity.runOnUiThread {
            if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, image_uri)
                binding.ivCapturedImg.setImageBitmap(bitmap)
                runImageClassifier(bitmap)
//                val result: String = ImageClassifier.predict(bitmap)
//                binding.tvResult.text = result
            }

            if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
                //proceed and check the selected image
                selectedPhotoUri = data.data
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
                binding.ivCapturedImg.setImageURI(selectedPhotoUri)
                runImageClassifier(bitmap)
            }
        }
    }

    fun runImageClassifier(bitmap: Bitmap){
        try {

            val probabilityProcessor =
                TensorProcessor.Builder().add(NormalizeOp(0f, 255f)).build()

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            var tImage = TensorImage(DataType.FLOAT32)

            tImage.load(bitmap)
            tImage = imageProcessor.process(tImage)
            val model = FinalModel.newInstance(this@MainActivity)
            val outputs =
                model.process(probabilityProcessor.process(tImage.tensorBuffer))
            val outputBuffer = outputs.outputFeature0AsTensorBuffer

            val tensorLabel = TensorLabel(labelsList, outputBuffer)

            val safeProbability = tensorLabel.mapWithFloatValue.get("Safe")
            Log.d(TAG,"$safeProbability")
            if (safeProbability?.compareTo(0.5)!! < 0){
                binding.tvResult.text = "Not safe"
            } else {
                binding.tvResult.text = "Safe"
            }

        } catch (e: Exception) {
            Log.d("TAG", "Exception is " + e.localizedMessage)
        }
    }

    override fun onDestroy() {
        FinalModel.newInstance(this@MainActivity).close()
        super.onDestroy()
    }
}