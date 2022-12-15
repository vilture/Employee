package biz.monro.employee.Functions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import biz.monro.employee.Adapter.RecyclerViewAdapter
import biz.monro.employee.BuildConfig
import biz.monro.employee.databinding.ActivityFotorepBinding
import biz.monro.employee.databinding.ItemFotoBinding
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors


class RepFoto : AppCompatActivity() {
    private val images = ArrayList<String>()
    private lateinit var binding: ActivityFotorepBinding
    private lateinit var bindingFoto: ItemFotoBinding

    lateinit var currentPhotoPath: String
    private var CAMERA_REQUEST = 100

    private var imagePaths: ArrayList<String>? = null
    private var imagesRV: RecyclerView? = null
    private var imageRVAdapter: RecyclerViewAdapter? = null

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFotorepBinding.inflate(layoutInflater)
        bindingFoto = ItemFotoBinding.inflate(layoutInflater)

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

        //подгрузим галерею
        loadImageAsync()

        binding.btnTakefoto.setOnClickListener {
            resultTakefoto.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
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
                    }
                }
            })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    val resultTakefoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                imagePaths!!.add(currentPhotoPath)
            }

            imageRVAdapter!!.notifyDataSetChanged()
        }


    @OptIn(DelicateCoroutinesApi::class)
    private fun loadImageAsync() = GlobalScope.async {
        openviewer()
    }

    @SuppressLint("Recycle", "NotifyDataSetChanged")
    fun openviewer() {
        imagePaths = ArrayList()
        imagesRV = binding.rvFoto

        imageRVAdapter = RecyclerViewAdapter(this@RepFoto, imagePaths!!)

        var fotoList: List<File> = emptyList()
        val manager = GridLayoutManager(this@RepFoto, 4)

        imagesRV!!.layoutManager = manager
        imagesRV!!.adapter = imageRVAdapter

        val path = createFotoFolder().toURI()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fotoList = Files.walk(Paths.get(path))
                .filter { path: Path? ->
                    Files.isRegularFile(
                        path
                    )
                }
                .map { obj: Path -> obj.toFile() }
                .collect(Collectors.toList())
        } else {
            fotoList = File(path).listFiles()?.toList() as List<File>
        }

        fotoList.forEach { imagePaths!!.add(it.absoluteFile.toString()) }

        imageRVAdapter!!.notifyDataSetChanged()
    }

    private fun createFotoFolder(): File {
        val mdir = File(Environment.getExternalStorageDirectory(), "MonroAPP")
        if (!mdir.exists()) {
            mdir.mkdir()
        }
        val fdir = File(mdir.absolutePath, "photorep")
        if (!fdir.exists()) {
            fdir.mkdir()
        }

        return fdir
    }

    /**
     * путь файла для фото
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val path = createFotoFolder()

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
            path
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
            loadImageAsync()
        }
    }
}
