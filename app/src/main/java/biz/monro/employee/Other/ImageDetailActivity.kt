package biz.monro.employee.Other

import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityImageDetailBinding
import com.squareup.picasso.Picasso
import java.io.File
import kotlin.math.max
import kotlin.math.min


class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding

    private var imgPath: String? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imgPath = intent.getStringExtra("imgPath")

        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        if (!imgPath.isNullOrBlank()) {
            val imgFile = File(imgPath!!)

            if (imgFile.exists()) {
                Picasso.get().load(imgFile).placeholder(R.drawable.launch_screen)
                    .into(binding.maxImage)
            }
        }

    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        scaleGestureDetector?.onTouchEvent(motionEvent)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))

            binding.maxImage.scaleX = mScaleFactor
            binding.maxImage.scaleY = mScaleFactor
            return true
        }
    }
}
