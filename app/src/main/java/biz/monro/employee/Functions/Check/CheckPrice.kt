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
import org.json.JSONObject

/**
 * Проверка цен товаров
 */
class CheckPrice : AppCompatActivity(), excActivityFragment {
    private lateinit var binding: ActivityScancontBinding

    private val requestCodeCameraPermission = 10001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScancontBinding.inflate(layoutInflater)
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
        else
            openFragment()
    }

    /**
    открываем фрагмент с считывателем
     */
    private fun openFragment() {
        supportFragmentManager.beginTransaction().apply {
            replace(binding.fragmentHolder.id, BarcodeVision(Barcode.ITF))
            addToBackStack(null)
            commit()
        }
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

        val result = checkResult(barc)

        val ad = if (result.substring(0, 1) == "S") {
            AlertDialog.Builder(
                this,
                resources.getIdentifier("AlertDialogCustomW", "style", packageName)
            )
        } else {
            AlertDialog.Builder(
                this,
                resources.getIdentifier("AlertDialogCustomR", "style", packageName)
            )
        }
        ad.setTitle("Проверка цены")
        ad.setMessage(result.substring(1))
        ad.setPositiveButton("Прочитал") { dialog, _ ->
            dialog.cancel()
            cameraSource.start(surfaceCamera.holder)
            BarcodeVision(Barcode.ITF).flash(cameraSource, flashmode)
            Log.i("callback", "camera started")
        }
        ad.setOnCancelListener {
            cameraSource.start(surfaceCamera.holder)
            BarcodeVision(Barcode.ITF).flash(cameraSource, flashmode)
            Log.i("callback", "camera started")
        }

        when (result.substring(0, 1)) {
            "S" -> {        // успешно
                CommonFun.beeper(this, 'S')
                ad.create().show()
            }
            "E" -> {            // ошибка
                ad.setCancelable(false)
                CommonFun.beeper(this, 'E')
                ad.create().show()
            }
            "C" -> {        // пропуск
                cameraSource.start(surfaceCamera.holder)
                BarcodeVision(Barcode.ITF).flash(cameraSource, flashmode)
                Log.i("callback", "camera started")
            }
        }

        Log.i("callback", "camera started")

        return cameraSource
    }

    /**
    обращаемся к АПИ и проверяем ШК на ошибки
     */

/*
Формат ШК ценника
16 символов, interleaved 2 of 5

9 - модифицированный номер материала SAP (КодТов)
4 - цена товара (целочисленное значение без копеек, максимально допустимая цена 9999)
2 - код типа ценника (00-99)
1 - контрольная сумма
*/
    private fun checkResult(barc: String): String {
        // первый символ в результате индикатор ошибки
        // S - Успешно
        // E - Ошибочно
        // C - пропуск

        var result: String

        // 00- отдельный материал
        // 9 - фикса
//        if (barc.substring(0, 1) == "9" || barc.substring(0, 2) == "00") {
//            Toast.makeText(this, "Не верный ШК(фикса)$barc", Toast.LENGTH_LONG).show()
//            return "C"
//        }
//        var matnr = (barc.substring(0, 7).toInt() - 1000000 + 1000000000).toString()
//        matnr = ("000000000000000000$matnr").substring(matnr.length)

        val matnr = CommonFun.getMatnrFromBC(barc)
        val matprice = barc.substring(9, 13).replaceFirst("^0+(?!$)".toRegex(), "")

        // запрашиваем информацию по материалу )
        val apiUniqPar = HashMap<String, String>()
        apiUniqPar["function"] = "get_price"
        apiUniqPar["date"] = CommonFun.curDate()
        apiUniqPar["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiUniqPar["pmata"] = matnr
        apiUniqPar["pdate"] = CommonFun.curDate()

        val ts = APICallTask(
            api_params = apiUniqPar,
            hash_params = "date,werks,function,pmata",
            CallBack = null,
            callsrc = "",
            api_url = CommonFun.getIpMagServ(this)
        ).execute()
        val json = ts.get()

        val error: String

        try {
            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            // ошибки при получении JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            result = if (matprice != json.getString("price")) {
                "EНесоответствие цены " + matprice + " != " + json.getString("price") + "\n\n"
            } else {
                "SЦена корректна \n\n"
            }

            result += "мат.:" + matnr.replaceFirst("^0+(?!$)".toRegex(), "") + "\n" +
                    "наим." + json.getString("sertname") + "\n" +
                    "артикул: " + json.getString("maktx") + "\n" +
                    "верх: " + json.getString("mat_up") + "\n" +
                    "подклад: " + json.getString("mat_down") + "\n" +
                    "подошва: " + json.getString("mat_sole") + "\n" +
                    "цена: " + json.getString("price")

        } catch (ex: Exception) {
            ex.printStackTrace()

            result = "E" + ex.message.toString()
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