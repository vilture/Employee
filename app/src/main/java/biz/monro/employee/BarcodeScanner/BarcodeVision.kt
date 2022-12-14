@file:Suppress(
    "DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)

package biz.monro.employee.BarcodeScanner

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import biz.monro.employee.databinding.BarcodevisionBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.ByteArrayOutputStream


/**
 * Фрагмент для использования камеры
 */
class BarcodeVision(barcMode: Int) : Fragment() {

    private var myDetector: ViewFinder? = null
    private var format = barcMode
    private lateinit var camera: CameraSource
    private var prefCamera: Camera? = null
    private var flashmode = false
    private var exchange: excActivityFragment? = null

    private var itemBinding: BarcodevisionBinding? = null
    private val binding get() = itemBinding!!

    // отрисовывем экран
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        itemBinding = BarcodevisionBinding.inflate(inflater, container, false)

        // вспышка
        binding.btnFlash.setOnClickListener {
            flashOnButton()
        }

        // инициализируем детектор ШК + подключаем камеру
        try {
            val detector = BarcodeDetector.Builder(activity)
                .setBarcodeFormats(format)
                .build()
            // видоискатель
            myDetector = ViewFinder(detector, 800, 600)

            camera = CameraSource.Builder(activity, myDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
                .build()

            binding.surfaceCamera.requestFocus()
            binding.surfaceCamera.holder.addCallback(surfaceCallback)

            // подвязываем свой процеесор обработки изображений
            myDetector!!.setProcessor(processor)

        } catch (e: Exception) {
            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        itemBinding = null
    }

    /*
    функция вспышки камеры
     */
    private fun flashOnButton() {
        prefCamera = getCamera(camera)
        if (prefCamera != null) {
            try {
                val param = prefCamera!!.parameters
                param.flashMode =
                    if (!flashmode) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                prefCamera!!.parameters = param
                flashmode = !flashmode
                if (flashmode) {
                    Toast.makeText(activity, "Вспышка включена", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Вспышка выключена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.message?.let { Log.i("BarcodeParam", it) }
                e.printStackTrace()
            }
        }
    }

    fun flash(camera: CameraSource, pflashmode: Boolean) {
        prefCamera = getCamera(camera)
        if (prefCamera != null) {
            try {
                val param = prefCamera!!.parameters
                param.flashMode =
                    if (pflashmode) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                prefCamera!!.parameters = param
            } catch (e: Exception) {
                e.message?.let { Log.i("BarcodeFlash", it) }
                e.printStackTrace()
            }
        }
    }

    /*
    получим активную камеру и ее поток
     */
    private fun getCamera(cameraSource: CameraSource): Camera? {
        val declaredFields = CameraSource::class.java.declaredFields
        for (field in declaredFields) {
            if (field.type === Camera::class.java) {
                field.isAccessible = true
                try {
                    return field.get(cameraSource) as Camera
                } catch (e: Exception) {
                    e.message?.let { Log.i("BarcodeCamera", it) }
                    e.printStackTrace()
                }
                break
            }
        }
        return null
    }

    /*
    переопределяем процесс обработки  изображений
     */
    private val processor = object : Detector.Processor<Barcode> {
        override fun release() {}

        @SuppressLint("MissingPermission")
        override fun receiveDetections(detect: Detector.Detections<Barcode>) {
            try {
                val bcode = detect.detectedItems.valueAt(0)

                when (bcode.format) {
                    Barcode.DATA_MATRIX -> {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            camera.stop()
                            Log.i("BarcodeCallback", "camera stopped")

                            // убираем все непечатные символы в начале ШК
                            if (bcode.displayValue.startsWith("\u001D"))
                                bcode.displayValue = bcode.displayValue.substring(1)

                            camera = exchange!!.getBarcode(bcode.displayValue, camera, flashmode)
                        }
                    }
                    Barcode.ITF -> {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            camera.stop()
                            Log.i("BarcodeCallback", "camera stopped")

                            camera = exchange!!.getBarcode(bcode.displayValue, camera, flashmode)
                        }
                    }
                    Barcode.UPC_A -> {
                        val handler = Handler(Looper.getMainLooper())
                        handler.post {
                            camera.stop()
                            Log.i("BarcodeCallback", "camera stopped")

                            camera = exchange!!.getBarcode(bcode.displayValue, camera, flashmode)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    /*
    запускаем слушателя камеры
     */
    private val surfaceCallback = object : SurfaceHolder.Callback {
        @SuppressLint("MissingPermission")
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        activity!!,
                        CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                camera.start(holder)
            } catch (exception: Exception) {
                Toast.makeText(activity, "Что-то пошло не так", Toast.LENGTH_LONG).show()
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            camera.stop()
        }
    }

    /*
    подвязываем обмен полученными данными с родительской активностью
     */
    @Deprecated("Deprecated in Java")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        exchange = activity as excActivityFragment
    }

}

// TODO: 17.12.20 пока не разобрался как его отрисовать на экране видимо
/*
видоискатель
 */
class ViewFinder(delegate: Detector<Barcode>, bWidth: Int, bHeight: Int) :
    Detector<Barcode>() {
    private var detector: Detector<Barcode> = delegate
    private var boxWidth = bWidth
    private var boxHeight = bHeight

    override fun detect(frame: Frame): SparseArray<Barcode> {
        val width = frame.metadata.width
        val height = frame.metadata.height
        val right = width / 2 + boxHeight / 2
        val left = width / 2 - boxHeight / 2
        val bottom = height / 2 + boxWidth / 2
        val top = height / 2 - boxWidth / 2

        val yuvImage =
            YuvImage(frame.grayscaleImageData.array(), ImageFormat.NV21, width, height, null)
        val byteArrayOutputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(left, top, right, bottom), 100, byteArrayOutputStream)
        val jpegArray = byteArrayOutputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)

        val croppedFrame = Frame.Builder()
            .setBitmap(bitmap)
            .setRotation(frame.metadata.rotation)
            .build()

        return detector.detect(croppedFrame)
    }

    override fun isOperational(): Boolean {
        return detector.isOperational
    }
}