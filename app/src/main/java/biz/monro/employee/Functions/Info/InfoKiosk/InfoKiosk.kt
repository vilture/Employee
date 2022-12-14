package biz.monro.employee.Functions.InfoKiosk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.BarcodeScanner.BarcodeVision
import biz.monro.employee.BarcodeScanner.excActivityFragment
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityInfokioskBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.potyvideo.slider.library.SliderLayout
import com.potyvideo.slider.library.SliderTypes.BaseSliderView
import com.potyvideo.slider.library.SliderTypes.TextSliderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap


class InfoKiosk : AppCompatActivity(),
    excActivityFragment,
    SearchArtikulFragment.DialogListener {
    private lateinit var binding: ActivityInfokioskBinding
    private val requestCodeCameraPermission = 10001
    private val manager = supportFragmentManager

    // отсканированный материал
    private var scannedMatnr = ""

    // список фотографий
    private var urlImg: ArrayList<String> = ArrayList()

    // данные по товару
    private var mapMatnr = LinkedHashMap<String, String>()

    // фото к товару
    private var listPhotos = ArrayList<String>()

    // данные по остаткам в магазине
    private var mapLabst = HashMap<String, String>()

    // данные по остаткам по другим магазинам
    private var werksLabst = LinkedHashMap<String, HashMap<String, Char>>()
    private var headrowLabst = ArrayList<String>()
    private var cityLabst = LinkedHashMap<Int, String>()

    // форматированные строки таблицы
    private val tf = Typeface.create("@font/geometria_medium", Typeface.NORMAL)
    private val tfb = Typeface.create("@font/geometria_medium", Typeface.BOLD)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfokioskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState?.getString("matnr") != null)
            scannedMatnr = savedInstanceState.getString("matnr").toString()

        // проверим разрешение устройства
        if (!CommonFun.checkResDev(this)) {
            finish()
        }

        // проверим ввод сервера магазина
        if (!CommonFun.checkMServIP(this)) {
            finish()
        }

        // проверим разрешение на доступ к камере
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            CommonFun.camPermission(this)
        else {
            if (scannedMatnr == "") {
                openCamera()
            } else {
                binding.txInfowarn.visibility = View.GONE
            }
        }

        // кнопка запуска сканера
        binding.scanMatnrKiosk.setOnClickListener {
            openCamera()
        }

        // поиск по артикулу
        binding.scanMatnrKiosk.setOnLongClickListener {
            searchArtikul()

            true
        }

        // остатки в других магазинах
        binding.btnLabst.setOnClickListener {
            if (scannedMatnr.isNotEmpty()) {
                binding.frameLabstsz.visibility = View.VISIBLE
                binding.shadow.visibility = View.VISIBLE
                binding.scanMatnrKiosk.visibility = View.GONE

            }
            binding.btnSzclose.setOnClickListener {
                binding.frameLabstsz.visibility = View.GONE
                binding.shadow.visibility = View.GONE
                binding.scanMatnrKiosk.visibility = View.VISIBLE
            }
        }

        // похожие товары
        binding.btnSimilar.setOnClickListener {
            if (scannedMatnr.isNotEmpty()) {
                val intent = Intent(this, SimilarProd::class.java)
                intent.putExtra("MATNR", scannedMatnr)
                intent.putExtra("MAKTX", mapMatnr.getValue("Артикул"))
                startActivity(intent)
            }
        }

        // настраиваем слайдер
        binding.imgIndicator.gravity = Gravity.BOTTOM and Gravity.CENTER
        binding.imgSlider.setCustomIndicator(binding.imgIndicator)
        binding.imgSlider.setPresetTransformer(SliderLayout.Transformer.Tablet)
        binding.imgSlider.setBackgroundResource(R.color.monroWhite)
        binding.imgSlider.setDuration(5000)


    }


    /**
    открываем фрагмент с считывателем
     */
    private fun openCamera() {
        val transaction = manager.beginTransaction()
        binding.scanMatnrKiosk.visibility = View.GONE

        transaction.replace(R.id.info_container, BarcodeVision(Barcode.ITF))
            .addToBackStack(null)
            .commit()
    }

    /**
    получаем ШК и обрабатываем результат
     */
    @SuppressLint("MissingPermission")
    override fun getBarcode(
        barc: String,
        cameraSource: CameraSource,
        flashmode: Boolean
    ): CameraSource {
        val surfaceCamera = findViewById<SurfaceView>(R.id.surfaceCamera)

        if (barc.length != 16) {
            cameraSource.start(surfaceCamera.holder)
            return cameraSource

        }

        val error = getDataTov(barc, "")

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
            binding.tableMatnr.removeAllViews()
            binding.tableLabst.removeAllViews()
            binding.tableLabstsz.removeAllViews()
            urlImg.removeAll(urlImg)
            binding.imgSlider.removeAllSliders()
        }

        binding.scanMatnrKiosk.visibility = View.VISIBLE

        // заполняем слайдер
        getPhotosAndParse()

        // заполняем таблицы
        collectTable()

        manager.popBackStack()
        return cameraSource
    }

    /**
     * получить товар по вводу артикула
     */
    private fun searchArtikul() {
        val searchArtikul = SearchArtikulFragment()
        searchArtikul.show(supportFragmentManager, "search")
    }

    /**
     * заполняем полученный материал из поиска по артикулу
     */
    override fun selectedMatnr(matnr: String) {
        val search = supportFragmentManager.findFragmentByTag("search")
        if (search != null) {
            val df = search as DialogFragment
            df.dismiss()
        }

        val error = getDataTov("", matnr)
        binding.tableMatnr.removeAllViews()
        binding.tableLabst.removeAllViews()
        binding.tableLabstsz.removeAllViews()
        urlImg.removeAll(urlImg)
        binding.imgSlider.removeAllSliders()

        if (error.isNotEmpty()) {
            CommonFun.beeper(this, 'E')
            AlertDialog.Builder(this, R.style.AlertDialogCustomR)
                .setTitle("Ошибка")
                .setMessage(error)
                .setPositiveButton("Прочитал") { dialog, _ ->
                    dialog.cancel()
                }
                .create().show()
        } else {
            // если все успешно,то заполняем экран товара
            binding.scanMatnrKiosk.visibility = View.VISIBLE

            // заполняем слайдер
            getPhotosAndParse()

            // заполняем таблицы
            collectTable()
        }
    }

    /**
     * заполняем таблицы данных материала
     */
    @SuppressLint("RtlHardcoded", "ResourceType")
    private fun collectTable() {
        /*
         заполним таблицу информации о материале
         */
        for (infom in mapMatnr.entries) {
            val tbrow = TableRow(this)

            val t1v = TextView(this)
            t1v.text = infom.key
            t1v.typeface = tfb
            t1v.textSize = 16f
            t1v.setTextColor(Color.BLACK)
            t1v.gravity = Gravity.LEFT
            tbrow.addView(t1v)

            val t2v = TextView(this)
            t2v.text = infom.value
            t2v.typeface = tf
            t1v.textSize = 16f
            t2v.setTextColor(Color.BLACK)
            t2v.gravity = Gravity.RIGHT
            tbrow.addView(t2v)

            binding.tableMatnr.addView(tbrow)
        }

        /*
         заполним таблицу остатков материала
         */
        val tbrow0 = TableRow(this)
        // заголовок
        val tv0 = TextView(this)
        tv0.typeface = tfb
        tv0.textSize = 16f
        tv0.text = " Размер "
        tv0.setTextColor(Color.BLACK)
        tv0.gravity = Gravity.CENTER
        tbrow0.addView(tv0)

        val tv1 = TextView(this)
        tv1.typeface = tfb
        tv1.textSize = 16f
        tv1.text = " Остаток "
        tv1.setTextColor(Color.BLACK)
        tv1.gravity = Gravity.CENTER
        tbrow0.addView(tv1)

        binding.tableLabst.addView(tbrow0)
        // позиции
        for (infol in mapLabst.entries) {
            val tbrow = TableRow(this)
            val t1v = TextView(this)
            t1v.text = infol.key
            t1v.typeface = tf
            t1v.textSize = 16f
            t1v.setTextColor(Color.BLACK)
            t1v.gravity = Gravity.LEFT and Gravity.CENTER
            tbrow.addView(t1v)

            val t2v = TextView(this)
            t2v.text = infol.value
            t2v.textSize = 16f
            t2v.typeface = tf
            t2v.setTextColor(Color.BLACK)
            t2v.gravity = Gravity.LEFT and Gravity.CENTER
            tbrow.addView(t2v)

            binding.tableLabst.addView(tbrow)
        }

        /*
        заполним таблицу остатков на других магазинах
         */
        val tbrowsz = TableRow(this)

        // заголовок
        for (head in headrowLabst) {
            val tv = TextView(this)
            tv.typeface = tfb
            tv.textSize = 16f
            tv.text = head
            tv.setBackgroundResource(R.drawable.shape_row)
            tv.setTextColor(Color.BLACK)
            tv.gravity = Gravity.CENTER
            tbrowsz.addView(tv)
        }
        binding.tableLabstsz.addView(tbrowsz)

        // позиции
        for (city in cityLabst.entries) {
            val tbrow = TableRow(this)
            val tv = TextView(this)
            tv.text = city.value

            tv.setBackgroundResource(R.drawable.shape_row)
            tv.typeface = tfb
            tv.textSize = 16f

            tv.setTextColor(Color.BLACK)
            tv.gravity = Gravity.CENTER
            tbrow.addView(tv)

            binding.tableLabstsz.addView(tbrow)

            for (werks in werksLabst.entries) {
                if (werks.key.substring(0, 1).toInt() == city.key) {
                    val tbrow = TableRow(this)

                    val t1v = TextView(this)
                    t1v.text = werks.key.substring(1)
                    t1v.setBackgroundResource(R.drawable.shape_row)
                    t1v.typeface = tf
                    t1v.textSize = 16f

                    t1v.setTextColor(Color.BLACK)
                    t1v.gravity = Gravity.LEFT
                    tbrow.addView(t1v)

                    for (labst in werks.value) {
                        val image = ImageView(this)
                        image.setBackgroundResource(R.drawable.shape_row)
                        if (labst.value == 'X')
                            image.setImageResource(R.raw.radiox)
                        else
                            image.setImageResource(R.raw.radio)

                        tbrow.gravity = Gravity.CENTER_VERTICAL
                        tbrow.addView(image)
                    }


                    binding.tableLabstsz.addView(tbrow)
                }
            }
        }
    }

    /**
     * получаем фотки
     */
    private fun getPhotosAndParse() {
        if (listPhotos.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.Default) {
                listPhotos.forEach { f -> urlImg.add(f) }

                launch(Dispatchers.Main) {
                    binding.imgSlider.removeAllSliders()
                    for (photo in urlImg) {
                        val sliderView = TextSliderView(applicationContext)
                        sliderView
                            .setScaleType(BaseSliderView.ScaleType.CenterInside)
                            .image(photo)

                        binding.imgSlider.addSlider(sliderView)
                    }
                }
            }
        } else {
            // парсим старничку товара и вытаскиваем фотки
            GlobalScope.launch(Dispatchers.Default) {
                val url = "https://www.monro.biz/nsk/product/?id=$scannedMatnr"
                val doc = Jsoup.connect(url).get()
                val photos = doc.select("img[src~=WEBIMAGE_[0-9]+.JPG]")

                if (photos.isNotEmpty()) {
                    photos.forEach { f -> urlImg.add("https://www.monro.biz" + f.attr("src")) }
                }

                launch(Dispatchers.Main) {
                    binding.imgSlider.removeAllSliders()
                    for (photo in urlImg) {
                        val sliderView = TextSliderView(applicationContext)
                        sliderView
                            .setScaleType(BaseSliderView.ScaleType.CenterInside)
                            .image(photo)

                        binding.imgSlider.addSlider(sliderView)
                    }
                }
            }
        }
    }

    /*
    Формат ШК ценника
    16 символов, interleaved 2 of 5

    9 - модифицированный номер материала SAP (КодТов)
    4 - цена товара (целочисленное значение без копеек, максимально допустимая цена 9999)
    2 - код типа ценника (00-99)
    1 - контрольная сумма
    */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getDataTov(barc: String, matnr: String): String {
        var result = ""

        mapMatnr.clear()
        mapLabst.clear()
        werksLabst.clear()
        headrowLabst.clear()
        cityLabst.clear()
        listPhotos.clear()
        binding.txInfowarn.visibility = View.GONE

        scannedMatnr = if (barc.isNotEmpty())
            CommonFun.getMatnrFromBC(barc)
        else
            matnr

        // запрашиваем информацию по материалу
        val apiUniqMat = HashMap<String, String>()
        apiUniqMat["function"] = "get_mat_data"
        apiUniqMat["date"] = CommonFun.curDate()
        apiUniqMat["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiUniqMat["matnr"] = scannedMatnr

        val tsm = APICallTask(
            api_params = apiUniqMat,
            hash_params = "date,werks,function,matnr",
            CallBack = null,
            callsrc = "",
            api_url = URL(CommonFun.API_URLWEB_Release)
        ).execute()
        val jsonm = tsm.get()
        val errorm: String

        try {
            val jsonPhotos = jsonm.optJSONArray("photos")

            val jsonStatus = jsonm.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            // ошибки при получении JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                errorm = "$jsonCode $jsonMessage"

                throw Exception(errorm)
            }

            mapMatnr["Артикул"] = jsonm.getString("maktx")
            mapMatnr["Торг.марка"] = jsonm.getString("trademark")
            if (jsonm.getString("color").isNotEmpty())
                mapMatnr["Цвет"] = jsonm.getString("color")
            mapMatnr["Наим. по серт."] = jsonm.getString("sertname")
            mapMatnr["Производитель"] = jsonm.getString("prodland")

            if (jsonm.getString("mat_up").isNotEmpty() &&
                jsonm.getString("mat_up") != "<>"
            )
                mapMatnr["Мат.верха"] = jsonm.getString("mat_up")

            if (jsonm.getString("mat_down").isNotEmpty() &&
                jsonm.getString("mat_down") != "<>"
            )
                mapMatnr["Мат.подклада"] = jsonm.getString("mat_down")

            if (jsonm.getString("mat_sole").isNotEmpty() &&
                jsonm.getString("mat_sole") != "<>"
            )
                mapMatnr["Мат.подошвы"] = jsonm.getString("mat_sole")

            mapMatnr["Цена/Макс"] = jsonm.getString("price") + " / " + jsonm.getString("maxprice")

            if (jsonm.getString("ra_code").isNotEmpty()) {
                mapMatnr["Скидка"] = jsonm.getString("ra_code").substring(2) + "%"
            }

            // вытаскиваем качественные фотки
            for (i in 0 until jsonPhotos.length()) {
                val jsonObject = jsonPhotos.getJSONObject(i)
                if (jsonObject.optString("type") == "W") {
                    listPhotos.add(jsonObject.optString("URL"))
                }
            }

            // если качественных нет, то берем качество черновик
            if (listPhotos.isEmpty()) {
                for (i in 0 until jsonPhotos.length()) {
                    val jsonObject = jsonPhotos.getJSONObject(i)
                    if (jsonObject.optString("type") == "D") {
                        listPhotos.add(jsonObject.optString("URL"))
                    }
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            scannedMatnr = ""
            binding.txInfowarn.visibility = View.VISIBLE
            result = "Получение товара в магазине\n" + ex.message.toString()
            return result
        }

        // запрашиваем информацию по остаткам
        val apiUniqLbs = HashMap<String, String>()
        apiUniqLbs["function"] = "get_labst"
        apiUniqLbs["date"] = CommonFun.curDate()
        apiUniqLbs["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiUniqLbs["pmata"] = scannedMatnr

        val tsl = APICallTask(
            api_params = apiUniqLbs,
            hash_params = "date,werks,function,pmata",
            CallBack = null,
            callsrc = "",
            api_url = CommonFun.getIpMagServ(this)
        ).execute()
        val jsonl = tsl.get()
        val errorl: String

        try {
            val jsonVariants = jsonl.optJSONArray("variants")

            val jsonStatus = jsonl.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            // ошибки при получении JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                errorl = "$jsonCode $jsonMessage"

                throw Exception(errorl)
            }

            for (i in 0 until jsonVariants.length()) {
                val jsonObject = jsonVariants.getJSONObject(i)
                mapLabst[jsonObject.optString("size")] = jsonObject.optString("labst")
            }

            if (jsonVariants.length() == 0) {
                mapLabst["1"] = jsonl.getInt("labst").toString()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
            scannedMatnr = ""
            result = "Получение остатка в магазине\n" + ex.message.toString()
            return result
        }

        // запрашиваем информацию по остаткам в других магазинах
        val apiUniqLsz = HashMap<String, String>()
        apiUniqLsz["function"] = "get_labst_by_size"
        apiUniqLsz["date"] = CommonFun.curDate()
        apiUniqLsz["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiUniqLsz["matnr"] = scannedMatnr

        val tsz = APICallTask(
            api_params = apiUniqLsz,
            hash_params = "date,werks,function,matnr",
            CallBack = null,
            callsrc = "",
            api_url = URL(CommonFun.API_URLWEB_Release)
        ).execute()
        val jsonlsz = tsz.get()
        val errorlsz: String

        val variants = LinkedHashMap<String, String>()

        try {
            val jsonVariants = jsonlsz.optJSONArray("variants")
            val jsonRestsH = jsonlsz.optJSONArray("rests")
            val jsonMatnr = jsonlsz.getString("matnr")

            val jsonStatus = jsonlsz.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            // ошибки при получении JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                errorlsz = "$jsonCode $jsonMessage"

                throw Exception(errorlsz)
            }

            headrowLabst.add("Адрес магазина")
            if (jsonlsz.getString("attyp") == "1") {
                for (i in 0 until jsonVariants.length()) {
                    val jsonObject = jsonVariants.getJSONObject(i)
                    variants[jsonObject.optString("matnr")] = jsonObject.optString("size")
                    headrowLabst.add(jsonObject.optString("size"))
                }
            } else {
                variants[jsonMatnr] = "1"
                headrowLabst.add("+")
            }

            // проходимся по остаткам магазина
            var cntCity = 0
            for (h in 0 until jsonRestsH.length()) {
                val jsonObjectH = jsonRestsH.getJSONObject(h)
                val jsonRestsP = jsonObjectH.optJSONArray("rests")
                val rests = LinkedHashMap<String, String>()
                val razm = LinkedHashMap<String, Char>()

                // собираем таблицу cityLabst Порядковое число:Город
                var cntInCity: Int
                if (jsonObjectH.optString("city") !in cityLabst.values) {
                    cntCity += 1
                    cntInCity = cntCity
                    cityLabst[cntCity] = jsonObjectH.optString("city")
                } else {
                    // если город уже есть, то запоминаем индекс (ключ)
                    val key = cityLabst.filterValues { it == jsonObjectH.optString("city") }.keys
                    cntInCity = key.first()
                }

                // В адресе(ключ) проставляем первым символом наше число
                // оно будет соответствовать городу, в дальнейшем
                // по этому числу будет анализировать порядок и группировку
                // вывода остатков по городам
                var address = cntInCity.toString() +
                        jsonObjectH.optString("street") + " " +
                        jsonObjectH.optString("addr")
                if (jsonObjectH.optString("tc_name").isNotEmpty())
                    address += "," + jsonObjectH.optString("tc_name")


                // заполняем таблицу остатков к магазину
                for (p in 0 until jsonRestsP.length()) {
                    val jsonObjectP = jsonRestsP.getJSONObject(p)

                    rests[jsonObjectP.optString("matnr")] = jsonObjectP.optString("labst")
                }

                // теперь ищем размеры в наличии на данном магазине
                for (size in variants) {
                    if (size.key in rests.keys)
                        razm[size.value] = 'X'
                    else
                        razm[size.value] = ' '
                }

                werksLabst[address] = razm
            }

            // сортируем список магазин-остатков по порядку
            val sorted = TreeMap(werksLabst)
            werksLabst.clear()
            werksLabst.putAll(sorted)

        } catch (ex: Exception) {
            ex.printStackTrace()
            scannedMatnr = ""
            result = "Получение остатков в других магазинах\n" + ex.message.toString()
            return result
        }


        return result
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
        outState.putString("matnr", scannedMatnr)
        outState.putSerializable("tabmatnr", mapMatnr)
        outState.putSerializable("tablabst", mapLabst)
        outState.putSerializable("photos", listPhotos)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        scannedMatnr = savedInstanceState.getString("matnr").toString()
        mapMatnr = savedInstanceState.getSerializable("tabmatnr") as LinkedHashMap<String, String>
        mapLabst = savedInstanceState.getSerializable("tablabst") as HashMap<String, String>
        listPhotos = savedInstanceState.getSerializable("photos") as ArrayList<String>

        getPhotosAndParse()
        collectTable()
    }


    override fun onStop() {
        binding.imgSlider.stopAutoCycle()
        super.onStop()
    }

    override fun onBackPressed() {
        if (binding.frameLabstsz.visibility == View.VISIBLE) {
            binding.btnSzclose.performClick()
            return
        }

        binding.scanMatnrKiosk.visibility = View.VISIBLE
        super.onBackPressed()
    }

}