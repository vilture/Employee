@file:Suppress("DEPRECATION")

package biz.monro.employee.Functions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import biz.monro.employee.BuildConfig
import biz.monro.employee.databinding.ActivityFotorepBinding
import java.io.File
import java.io.IOException


class RepFoto : AppCompatActivity() {
    private val images = ArrayList<String>()
    private lateinit var binding: ActivityFotorepBinding

    lateinit var currentPhotoPath: String
    private var CAMERA_REQUEST = 100


    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFotorepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!CommonFun.checkMServIP(this)) {
            finish()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            CommonFun.camPermission(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            CommonFun.filesPermission(this)


        binding.btnTakefoto.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: Exception) {
                        Toast.makeText(this@RepFoto, ex.message, Toast.LENGTH_SHORT).show()
                        null
                    }

                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            applicationContext,
                            BuildConfig.APPLICATION_ID + ".provider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST)
                    }
                }
            }
        }
    }


    /**
     * путь временного файла для фото
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val mdir = File(Environment.getExternalStorageDirectory(), "MonroAPP")
        if (!mdir.exists()) {
            mdir.mkdir()
        }
        val fdir = File(mdir.absolutePath, "photorep")
        if (!fdir.exists()) {
            fdir.mkdir()
        }

        return File.createTempFile(
            "${
                CommonFun.readPrefValue(
                    this,
                    CommonFun.PREF_WERKS
                )
            }_${
                CommonFun.curDate()
            }_",
            ".jpg",
            fdir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


    /**
     * запоминаем результат фотографирования
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            try {
                Log.i("debug", data?.extras?.get("data").toString())
            } catch (e: Exception) {
                e.message?.let { Log.i("debug", it) }
            }

            //val imageBitmap = data?.extras!!.get("data") as Bitmap


        }
    }
}
