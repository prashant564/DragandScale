package com.example.dragandscale

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max
import kotlin.math.min


const val IMAGE_SCALE_CODE = 5
const val IMAGE_DRAG_CODE = 6
var selectedPhotoUri: Uri? = null
private var mScaleGestureDetector: ScaleGestureDetector? = null
private var mScaleFactor = 1.0f
private lateinit var mImageView: ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_upload_drag.setOnClickListener {
            selectImageFromGallery(IMAGE_DRAG_CODE)
        }
        btn_upload_scale.setOnClickListener {
            selectImageFromGallery(IMAGE_SCALE_CODE)
        }
        btn_drag.setOnClickListener {
            iv_drag.setOnTouchListener(listener)
        }
        mImageView = findViewById(R.id.iv_scale)
        btn_scale.setOnClickListener {
            mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    var listener = View.OnTouchListener(function = { view, motionEvent ->
        if (motionEvent.action == MotionEvent.ACTION_MOVE) {
            view.y = motionEvent.rawY - view.height / 2
            view.x = motionEvent.rawX - view.width / 2
        }
        true
    })

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_DRAG_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            iv_drag.setImageURI(selectedPhotoUri)
        }
        if (requestCode == IMAGE_SCALE_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            iv_scale.setImageURI(selectedPhotoUri)
        }
    }

    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        mScaleGestureDetector!!.onTouchEvent(motionEvent)
        return true
    }

    private fun selectImageFromGallery(code: Int){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, code)
    }
}

private class ScaleListener : SimpleOnScaleGestureListener() {
    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        mScaleFactor *= scaleGestureDetector.scaleFactor
        mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))
        mImageView.scaleX = mScaleFactor
        mImageView.scaleY = mScaleFactor
        return true
    }
}
