package biz.monro.employee.Functions.InfoKiosk

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityInfokioskBinding
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

class SimilarProdItemFragment(matnr: String) : Fragment() {


    private var inMatnr = matnr

    // список фотографий
    private var urlImg: ArrayList<String> = ArrayList()
    private var imgSlider: SliderLayout? = null

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
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val itemBinding =
            ActivityInfokioskBinding.inflate(LayoutInflater.from(container?.context), container, false)

        itemBinding.btnSimilar.visibility = View.GONE
        itemBinding.scanMatnrKiosk.visibility = View.GONE
        itemBinding.txInfowarn.visibility = View.GONE


        itemBinding.btnLabst.gravity = Gravity.CENTER
        itemBinding.btnLabst.setOnClickListener {
            if (inMatnr.isNotEmpty()) {
                itemBinding.frameLabstsz.visibility = View.VISIBLE
                itemBinding.shadow.visibility = View.VISIBLE


            }
            itemBinding.btnSzclose.setOnClickListener {
                itemBinding.frameLabstsz.visibility = View.GONE
                itemBinding.shadow.visibility = View.GONE
            }
        }

        // настраиваем слайдер
        imgSlider = itemBinding.imgSlider
        itemBinding.imgIndicator.gravity = Gravity.BOTTOM and Gravity.CENTER
        itemBinding.imgSlider.setCustomIndicator(itemBinding.imgIndicator)
        itemBinding.imgSlider.setPresetTransformer(SliderLayout.Transformer.Tablet)
        itemBinding.imgSlider.setBackgroundResource(R.color.monroWhite)
        itemBinding.imgSlider.setDuration(5000)

        // собираем и отображаем старницу с товаром
        val result = getTovar(itemBinding)
        if (result.isNotEmpty()) {
            AlertDialog.Builder(activity as SimilarProd, R.style.AlertDialogCustomR)
                .setTitle("Ошибка")
                .setMessage(result)
                .setPositiveButton("Прочитал") { _, _ ->
                    (activity as SimilarProd).onBackPressed()
                }
                .create().show()
        }

        return view
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getTovar(view: ActivityInfokioskBinding): String {
        var result = ""

        mapMatnr.clear()
        mapLabst.clear()
        werksLabst.clear()
        headrowLabst.clear()
        cityLabst.clear()
        listPhotos.clear()

        // запрашиваем информацию по материалу
        val apiUniqMat = HashMap<String, String>()
        apiUniqMat["function"] = "get_mat_data"
        apiUniqMat["date"] = CommonFun.curDate()
        apiUniqMat["werks"] = CommonFun.readPrefValue(activity as SimilarProd, CommonFun.PREF_WERKS)
        apiUniqMat["matnr"] = inMatnr

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
            result = "Получение товара в магазине\n" + ex.message.toString()
            return result
        }

        // запрашиваем информацию по остаткам
        val apiUniqLbs = HashMap<String, String>()
        apiUniqLbs["function"] = "get_labst"
        apiUniqLbs["date"] = CommonFun.curDate()
        apiUniqLbs["werks"] = CommonFun.readPrefValue(activity as SimilarProd, CommonFun.PREF_WERKS)
        apiUniqLbs["pmata"] = inMatnr

        val tsl = APICallTask(
            api_params = apiUniqLbs,
            hash_params = "date,werks,function,pmata",
            CallBack = null,
            callsrc = "",
            api_url = CommonFun.getIpMagServ(activity as SimilarProd)
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
            Toast.makeText(activity, "Товара нет в вашем магазине", Toast.LENGTH_LONG).show()
        }

        // запрашиваем информацию по остаткам в других магазинах
        val apiUniqLsz = HashMap<String, String>()
        apiUniqLsz["function"] = "get_labst_by_size"
        apiUniqLsz["date"] = CommonFun.curDate()
        apiUniqLsz["werks"] = CommonFun.readPrefValue(activity as SimilarProd, CommonFun.PREF_WERKS)
        apiUniqLsz["matnr"] = inMatnr

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
            result = "Получение остатков в других магазинах\n" + ex.message.toString()
            return result
        }

        //чистим экран
        view.tableMatnr.removeAllViews()
        view.tableLabst.removeAllViews()
        view.tableLabstsz.removeAllViews()
        urlImg.removeAll(urlImg)
        view.imgSlider.removeAllSliders()

        // заполняем слайдер
        getPhotosAndParse(view)

        // заполняем таблицы
        collectTable(view)

        return result
    }

    /**
     * получаем фотки
     */
    private fun getPhotosAndParse(view: ActivityInfokioskBinding) {
        if (listPhotos.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.Default) {
                listPhotos.forEach { f -> urlImg.add(f) }

                launch(Dispatchers.Main) {
                    view.imgSlider.removeAllSliders()
                    for (photo in urlImg) {
                        val sliderView = TextSliderView(activity)
                        sliderView
                            .setScaleType(BaseSliderView.ScaleType.CenterInside)
                            .image(photo)

                        view.imgSlider.addSlider(sliderView)
                    }
                }
            }
        } else {
            // парсим старничку товара и вытаскиваем фотки
            GlobalScope.launch(Dispatchers.Default) {
                val url = "https://www.monro.biz/nsk/product/?id=$inMatnr"
                val doc = Jsoup.connect(url).get()
                val photos = doc.select("img[src~=WEBIMAGE_[0-9]+.JPG]")

                if (photos.isNotEmpty()) {
                    photos.forEach { f -> urlImg.add("https://www.monro.biz" + f.attr("src")) }
                }

                launch(Dispatchers.Main) {
                    view.imgSlider.removeAllSliders()
                    for (photo in urlImg) {
                        val sliderView = TextSliderView(activity)
                        sliderView
                            .setScaleType(BaseSliderView.ScaleType.CenterInside)
                            .image(photo)

                        view.imgSlider.addSlider(sliderView)
                    }
                }
            }
        }
    }

    /**
     * заполняем таблицы данных материала
     */
    @SuppressLint("RtlHardcoded", "ResourceType")
    private fun collectTable(view: ActivityInfokioskBinding) {
        /*
         заполним таблицу информации о материале
         */
        for (infom in mapMatnr.entries) {
            val tbrow = TableRow(activity)

            val t1v = TextView(activity)
            t1v.text = infom.key
            t1v.typeface = tfb
            t1v.textSize = 16f
            t1v.setTextColor(Color.BLACK)
            t1v.gravity = Gravity.LEFT
            tbrow.addView(t1v)

            val t2v = TextView(activity)
            t2v.text = infom.value
            t2v.typeface = tf
            t1v.textSize = 16f
            t2v.setTextColor(Color.BLACK)
            t2v.gravity = Gravity.RIGHT
            tbrow.addView(t2v)

            view.tableMatnr.addView(tbrow)
        }

        /*
         заполним таблицу остатков материала
         */
        val tbrow0 = TableRow(activity)
        // заголовок
        val tv0 = TextView(activity)
        tv0.typeface = tfb
        tv0.textSize = 16f
        tv0.text = " Размер "
        tv0.setTextColor(Color.BLACK)
        tv0.gravity = Gravity.CENTER
        tbrow0.addView(tv0)

        val tv1 = TextView(activity)
        tv1.typeface = tfb
        tv1.textSize = 16f
        tv1.text = " Остаток "
        tv1.setTextColor(Color.BLACK)
        tv1.gravity = Gravity.CENTER
        tbrow0.addView(tv1)

        view.tableLabst.addView(tbrow0)
        // позиции
        for (infol in mapLabst.entries) {
            val tbrow = TableRow(activity)
            val t1v = TextView(activity)
            t1v.text = infol.key
            t1v.typeface = tf
            t1v.textSize = 16f
            t1v.setTextColor(Color.BLACK)
            t1v.gravity = Gravity.LEFT and Gravity.CENTER
            tbrow.addView(t1v)

            val t2v = TextView(activity)
            t2v.text = infol.value
            t2v.textSize = 16f
            t2v.typeface = tf
            t2v.setTextColor(Color.BLACK)
            t2v.gravity = Gravity.LEFT and Gravity.CENTER
            tbrow.addView(t2v)

            view.tableLabst.addView(tbrow)
        }

        /*
        заполним таблицу остатков на других магазинах
         */
        val tbrowsz = TableRow(activity)

        // заголовок
        for (head in headrowLabst) {
            val tv = TextView(activity)
            tv.typeface = tfb
            tv.textSize = 16f
            tv.text = head
            tv.setBackgroundResource(R.drawable.shape_row)
            tv.setTextColor(Color.BLACK)
            tv.gravity = Gravity.CENTER
            tbrowsz.addView(tv)
        }
        view.tableLabstsz.addView(tbrowsz)

        // позиции
        for (city in cityLabst.entries) {
            val tbrow = TableRow(activity)
            val tv = TextView(activity)
            tv.text = city.value

            tv.setBackgroundResource(R.drawable.shape_row)
            tv.typeface = tfb
            tv.textSize = 16f

            tv.setTextColor(Color.BLACK)
            tv.gravity = Gravity.CENTER
            tbrow.addView(tv)

            view.tableLabstsz.addView(tbrow)

            for (werks in werksLabst.entries) {
                if (werks.key.substring(0, 1).toInt() == city.key) {
                    val tbrow = TableRow(activity)

                    val t1v = TextView(activity)
                    t1v.text = werks.key.substring(1)
                    t1v.setBackgroundResource(R.drawable.shape_row)
                    t1v.typeface = tf
                    t1v.textSize = 16f

                    t1v.setTextColor(Color.BLACK)
                    t1v.gravity = Gravity.LEFT
                    tbrow.addView(t1v)

                    for (labst in werks.value) {
                        val image = ImageView(activity)
                        image.setBackgroundResource(R.drawable.shape_row)
                        if (labst.value == 'X')
                            image.setImageResource(R.raw.radiox)
                        else
                            image.setImageResource(R.raw.radio)

                        tbrow.gravity = Gravity.CENTER_VERTICAL
                        tbrow.addView(image)
                    }


                    view.tableLabstsz.addView(tbrow)
                }
            }
        }
    }

    // остановим слайдер чтобы не было утечек
    override fun onDestroy() {
        imgSlider?.stopAutoCycle()
        super.onDestroy()
    }
}