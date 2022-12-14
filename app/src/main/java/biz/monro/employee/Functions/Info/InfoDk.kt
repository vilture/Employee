package biz.monro.employee.Functions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import biz.monro.employee.BarcodeScanner.BarcodeVision
import biz.monro.employee.BarcodeScanner.excActivityFragment
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityInfodkBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.redmadrobot.inputmask.MaskedTextChangedListener

class InfoDk : AppCompatActivity(), excActivityFragment {
    private lateinit var binding: ActivityInfodkBinding
    private val requestCodeCameraPermission = 10001
    private val manager = supportFragmentManager

    // отсканированная ДК
    private var scannedDK = ""

    // данные по дк
    private var mapDK = LinkedHashMap<String, String>()

    // форматированные строки таблицы
    private val tf = Typeface.create("@font/geometria_medium", Typeface.NORMAL)
    private val tfb = Typeface.create("@font/geometria_medium", Typeface.BOLD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfodkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState?.getString("DK") != null)
            scannedDK = savedInstanceState.getString("DK").toString()

        if (scannedDK.isEmpty())
            getDK()

        // кнопка запуска сканера
        binding.scanDK.setOnClickListener {
            getDK()
        }

        binding.btnDkphone.setOnClickListener {
            val tel: String = binding.editDkphone.text.replace("""[^0-9\\+]""".toRegex(), "")

            if (tel.length != 12)
                return@setOnClickListener

            searchPhone()
            closeKey()
            binding.frameGetdk.visibility = View.GONE
            binding.shadow.visibility = View.GONE
            binding.scanDK.visibility = View.VISIBLE
        }

        binding.btnDkscan.setOnClickListener {
            // проверим разрешение на доступ к камере
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            )
                CommonFun.camPermission(this@InfoDk)
            else {
                closeKey()
                openCamera()

                binding.frameGetdk.visibility = View.GONE
                binding.shadow.visibility = View.GONE
            }
        }
    }

    private fun searchPhone() {
        val isError = getDataDK("")
        if (isError.isNotEmpty()) {
            CommonFun.beeper(this, 'E')
            AlertDialog.Builder(this, R.style.AlertDialogCustomR)
                .setTitle("Ошибка")
                .setMessage(isError)
                .setPositiveButton("Прочитал") { dialog, _ ->
                    dialog.cancel()
                }
                .setOnCancelListener {
                }
                .create().show()
        } else {
            // если все успешно,то чистим экран
            binding.tableDk.removeAllViews()
        }

        // заполняем таблицы
        collectTable()
    }

    private fun getDK() {
        binding.frameGetdk.visibility = View.VISIBLE
        binding.shadow.visibility = View.VISIBLE
        binding.scanDK.visibility = View.GONE

        binding.editDkphone.text.clear()

        val listener =
            MaskedTextChangedListener("+7 ([000]) [000] [00] [00]", binding.editDkphone)

        binding.editDkphone.addTextChangedListener(listener)
        binding.editDkphone.onFocusChangeListener = listener
        binding.editDkphone.hint = listener.placeholder()

    }


    /**
     *  Получем ШК ДК для анализа
     */
    @SuppressLint("MissingPermission")
    override fun getBarcode(
        barc: String,
        cameraSource: CameraSource,
        flashmode: Boolean
    ): CameraSource {
        val surfaceCamera = findViewById<SurfaceView>(R.id.surfaceCamera)

        if (barc.length != 12) {
            cameraSource.start(surfaceCamera.holder)
            return cameraSource
        }

        val error = getDataDK(barc)

        if (error.isNotEmpty()) {
            CommonFun.beeper(this, 'E')
            AlertDialog.Builder(this, R.style.AlertDialogCustomR)
                .setTitle("Ошибка")
                .setMessage(error)
                .setPositiveButton("Прочитал") { dialog, _ ->
                    dialog.cancel()
                    cameraSource.start(surfaceCamera.holder)
                }
                .setOnCancelListener {
                    cameraSource.start(surfaceCamera.holder)
                }
                .create().show()
            return cameraSource
        } else {
            // если все успешно,то чистим экран
            binding.tableDk.removeAllViews()
        }

        binding.scanDK.visibility = View.VISIBLE

        // заполняем таблицы
        collectTable()

        manager.popBackStack()
        return cameraSource
    }


    /**
     * заполняем таблицу данных ДК
     */
    @SuppressLint("RtlHardcoded", "ResourceType")
    private fun collectTable() {
        /*
         заполним таблицу информации о дк
         */
        for (infodk in mapDK.entries) {
            val tbrow = TableRow(this)

            val t1v = TextView(this)
            t1v.text = infodk.key
            t1v.typeface = tfb
            t1v.textSize = 16f
            t1v.setTextColor(Color.BLACK)
            t1v.gravity = Gravity.LEFT
            tbrow.addView(t1v)

            val t2v = TextView(this)
            t2v.text = infodk.value
            t2v.typeface = tf
            t1v.textSize = 16f
            t2v.setTextColor(Color.BLACK)
            t2v.gravity = Gravity.RIGHT
            tbrow.addView(t2v)

            binding.tableDk.addView(tbrow)
        }

    }


    /**
     * Получаем информация по ДК из АПИ
     */
    private fun getDataDK(barc: String): String {
        var result = ""

        mapDK.clear()

        scannedDK = barc
        scannedDK = "000079541695"

        mapDK["Номер ДК"] = "79541695"
        mapDK["Статус активности"] = "активный"
        mapDK["Кол-во покупок"] = "73"
        mapDK["Кол-во возвратов"] = "0"
        mapDK["Самый покупаемый товар"] = "женская обувь"
        mapDK["Любимый магазин"] = "магазин № 10 Новосибирск"
        mapDK["Место выдачи ДК"] = "СКЛАД СВТ"
        mapDK["Дата выдачи ДК"] = "24.04.2015"
        mapDK["Дата последней покупки"] = "27.05.2021"
        mapDK["Лояльность к РА"] = "безразличен к акциям"

        //TODO реализация апи
        // запрашиваем информацию по ДК
//        val apiUniqDK = HashMap<String, String>()
//        apiUniqDK["function"] = ""
//        apiUniqDK["date"] = CommonFun.curDate()
//        apiUniqDK["barcode"] = scannedDK
//
//        val tsm = APICallTask(
//            api_params = apiUniqDK,
//            hash_params = "date,function,barcode",
//            CallBack = null,
//            callsrc = "",
//            api_url = URL(CommonFun.API_URLWEB_Release)
//        ).execute()
//        val json = tsm.get()
//        val error: String
//
//        try {
//            val jsonStatus = json.getString("status")
//            val jObjectS = JSONObject(jsonStatus)
//            val jsonCode = jObjectS.getString("code")
//
//            // ошибки при получении JSON
//            if (jsonCode != "0") {
//                val jsonMessage = jObjectS.getString("usermsg")
//                error = "$jsonCode $jsonMessage"
//
//                throw Exception(error)
//            }
//
//            mapDK["Номер ДК"] = json.getString("")
//            mapDK["Статус активности"] = json.getString("")
//            mapDK["Кол-во покупок"] = json.getString("")
//            mapDK["Кол-во возвратов"] = json.getString("")
//            mapDK["Самый покупаемый товар"] = json.getString("")
//            mapDK["Любимый магазин"] = json.getString("")
//            mapDK["Место выдачи ДК"] = json.getString("")
//            mapDK["Дата выдачи ДК"] = json.getString("")
//            mapDK["Дата последней покупки"] = json.getString("")
//            mapDK["Лояльность к РА"] = json.getString("")
//
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//            scannedDK = ""
//            result = "Получение ДК\n" + ex.message.toString()
//            return result
//        }


        Toast.makeText(this, scannedDK + result, Toast.LENGTH_SHORT).show()
        return result
    }


    /**
    открываем фрагмент с считывателем
     */
    private fun openCamera() {
        val transaction = manager.beginTransaction()
        binding.scanDK.visibility = View.GONE

        transaction.replace(R.id.infodk_container, BarcodeVision(Barcode.UPC_A))
            .addToBackStack(null)
            .commit()
    }

    /**
     * закрытие клавиатуры
     */
    private fun closeKey() {
        val inputManager: InputMethodManager =
            this@InfoDk.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    /**
    разрешения
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openCamera()
            else {
                Toast.makeText(this, "Разрешения для камеры не получены", Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("DK", scannedDK)
        outState.putSerializable("tabdk", mapDK)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        scannedDK = savedInstanceState.getString("DK").toString()
        mapDK = savedInstanceState.getSerializable("tabdk") as LinkedHashMap<String, String>

        collectTable()
    }

    override fun onBackPressed() {
        if (scannedDK.isEmpty()) {
            getDK()
            binding.scanDK.visibility = View.GONE
        } else
            binding.scanDK.visibility = View.VISIBLE

        super.onBackPressed()
    }
}