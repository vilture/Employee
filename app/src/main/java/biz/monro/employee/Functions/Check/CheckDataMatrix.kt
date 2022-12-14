package biz.monro.employee.Functions

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.BarcodeScanner.BarcodeVision
import biz.monro.employee.BarcodeScanner.excActivityFragment
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityScancontBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * функция проверки КМ перед продажей (DataMatrix)
 */
class CheckDataMatrix : AppCompatActivity(), excActivityFragment {

    private lateinit var binding: ActivityScancontBinding

    private val requestCodeCameraPermission = 10001
    private val manager = supportFragmentManager
    private val transaction = manager.beginTransaction()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScancontBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            CommonFun.camPermission(this)
        else
            openFragment()
    }


    /*
    открываем фрагмент с считывателем
     */
    private fun openFragment() {

        supportFragmentManager.beginTransaction().apply {
            replace(binding.fragmentHolder.id, BarcodeVision(Barcode.DATA_MATRIX))
            addToBackStack(null)
            commit()
        }
    }

    /*
    получаем ШК и обрабатываем результат
     */
    @SuppressLint("MissingPermission")
    override fun getBarcode(
        barc: String,
        cameraSource: CameraSource,
        flashmode: Boolean
    ): CameraSource {
        val result = checkResult(barc)
        val surfaceCamera = findViewById<SurfaceView>(R.id.surfaceCamera)

        if (result != "Статус: ОК") {
            CommonFun.beeper(this, 'E')
            val ad = AlertDialog.Builder(
                this,
                resources.getIdentifier("AlertDialogCustomR", "style", packageName)
            )
            ad.setTitle("Ошибки по ШК")
            ad.setMessage(result)
            ad.setCancelable(false)
            ad.setPositiveButton("Прочитал") { dialog, _ ->
                dialog.cancel()
                cameraSource.start(surfaceCamera.holder)
                BarcodeVision(Barcode.DATA_MATRIX).flash(cameraSource, flashmode)
                Log.i("callback", "camera started")
            }
            ad.create().show()
        } else {
            CommonFun.beeper(this, 'S')
            cameraSource.start(surfaceCamera.holder)
            BarcodeVision(Barcode.DATA_MATRIX).flash(cameraSource, flashmode)
            Log.i("callback", "camera started")
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }

        return cameraSource
    }


    /*
    обращаемся к АПИ и проверяем ШК на ошибки
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun checkResult(barc: String): String {
        var result = ""
        var error = ""

        val ja = JSONArray()
        val jpar = JSONObject()

        val gtin = barc.substring(2, 16)
        val serial = barc.substring(18, 31)

        val apiUniqPar = HashMap<String, String>()
        apiUniqPar["function"] = "get_mark_info"
        apiUniqPar["date"] = CommonFun.curDate()
        apiUniqPar["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)


        val jo = JSONObject()
        ja.put(jo.put("gtin", gtin))
        ja.put(jo.put("serial", serial))
        jpar.put("serials", ja)

        val ts = APICallTask(
            api_params = apiUniqPar,
            hash_params = "werks,date,function",
            CallBack = null,
            api_jspar = jpar,
            callsrc = ""
        ).execute()

        try {
            val json = ts.get()

            val jsonMark = json.optJSONArray("mark_info")
            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            if (jsonMark.length() == 0) {
                CommonFun.snackbar(binding.scancontainer, "$gtin $serial")
                throw Exception("Данные по маркировке не найдены, к продаже не допущен!")
            }

            for (i in 0 until jsonMark.length()) {
                val jsonObject = jsonMark.getJSONObject(i)
                val jStatus = jsonObject.optString("status").toString()
                val jWerks = jsonObject.optString("werks").toString()
                val jMatnr = jsonObject.optString("matnr").toString()
                val jBarc = jsonObject.optString("barcode").toString()
                val jMarkcode = jsonObject.optString("markcode").toString()
                val jsonObuLst = jsonObject.optString("obukrs_list")
                val jsonObukrs = jsonObject.optString("obukrs")

                // проверяем полученные данные
                if (jWerks != CommonFun.readPrefValue(this, CommonFun.PREF_WERKS))
                    result += "Маркировка для магазина $jWerks\n"

                if (jsonObject.optString("zbukrs").toString() !=
                    jsonObject.optString("wzbukrs").toString()
                )
                    result += "ЮрЛицо не совпадает\n"

                result += "Статус: "
                when (jStatus) {
                    "N" -> result += "новый"
                    "P" -> result += "передан в ИС МП"
                    "D" -> result += "удален"
                    "S" -> result += "получен из ИС МП"
                    "E" -> result += "ошибка обработки в ИС МП"
                    "L" -> result += "код маркировки не получен из ИС МП"
                    "B" -> result += "этикетка напечатана"
                    "T" -> result += "отгружен в Россию"
                    "C" -> result += "ГТД есть, ожидается ввод в оборот"
                    "W" -> result += "ОК" //введен в оборот, но еще не оприходован на складе"
                    "O" -> result += "ОК" //введен в оборот
                    "J" -> result += "введен в оборот, подготовлен к печати"
                    "I" -> result += "введен в оборот, но еще не напечатан"
                    "R" -> result += "выведен из оборота"
                    "Q" -> result += "необходимо вывести из оборота"
                    "M" -> result += "передан оптовому покупателю для дальнейшей реализации"
                    "Z" -> result += "продан без вывода из оборота"
                    "A" -> result += "продан без вывода из оборота, проблемный"
                    else -> result += "не известный статус"
                }

                // сравниваем допустимых поставщиков
                val obukrsLst = jsonObuLst.split(",").map { it -> it.trim() }
                if (jsonObukrs.isNotEmpty()) {
                    if (!obukrsLst.contains(jsonObukrs))
                        result += "\nПоставщик КМ не совпадает"
                }

                if (jMarkcode != "9999999999" && jMatnr != "-")
                    CommonFun.snackbar(binding.scancontainer, jMatnr)
                else {
                    CommonFun.snackbar(binding.scancontainer, "Спецмагазин")
                    result = "Статус: ОК"
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            result = ex.message.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
            result = error
        }


        return result
    }

    /*
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
                openFragment()
            else {
                Toast.makeText(this, "Разрешения для камеры не получены", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

}
