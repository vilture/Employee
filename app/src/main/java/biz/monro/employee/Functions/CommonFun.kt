@file:Suppress("DEPRECATION")

package biz.monro.employee.Functions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.BuildConfig
import biz.monro.employee.R
import com.google.android.material.snackbar.Snackbar
import com.redmadrobot.inputmask.MaskedTextChangedListener
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*


/**
реализация общих методов, используемых в разных модулях программы
для использования их как static-like методы объявлены в общем объекте класса
 */

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class CommonFun {
    companion object {
        const val PRODCODE: String = "Employee"
        const val PLATFORM: String = "A007"
        const val VERSION: String = "VERSION"


        const val PREF_WERKS: String = "WERKS"
        const val PREF_WNAME: String = "WERKS_NAME"
        const val PREF_WIP: String = "MSERV_IP"

        // апи мобильного приложения
        const val API_URL_DebugC: String = "https://www.monro.biz:44008/mobile_api/v1.0"

        //        const val API_URL_DebugC: String = "https://www.monro.biz:44018/SWP/mobile_api/v1.0"
//        var API_URL_Debug: String = "https://www.monro.biz:44018/SWP/mobile_api/v1.0"
        var API_URL_Debug: String = "https://www.monro.biz:44008/mobile_api/v1.0"
        const val API_URL_Release: String = "https://www.monro.biz:44008/mobile_api/v1.0"

        // апи для обновления
        const val API_URLUPD_DebugC: String = "https://www.monro.biz:44018/SWP/ext_api/v1.0"
        var API_URLUPD_Debug: String = "https://www.monro.biz:44018/SWP/ext_api/v1.0"
        const val API_URLUPD_Release: String = "https://www.monro.biz:44008/ext_api/v1.0"

        // апи для вебданных
        const val API_URLWEB_Release: String = "https://www.monro.biz:44088/web_api/v1.0"

        const val permission = 10001

        /**
         * чтение значения из SharedPrefs
         */
        fun readPrefValue(context: Context, key: String): String {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val res = sharedPreferences?.getString(key, "")
            return res ?: ""
        }

        /**
         * чтение значения из SharedPrefs как целого числа
         */
        fun readPrefIntValue(context: Context, key: String, defVal: Int = 0): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val str = sharedPreferences?.getString(key, "")
            var res = defVal

            try {
                res = str?.toInt() ?: defVal
            } catch (e: NumberFormatException) {
            }

            return res
        }

        /**
         *  запись значения в SharedPrefs
         */
        fun savePrefValue(context: Context, key: String, value: String) {
            val sPref = PreferenceManager.getDefaultSharedPreferences(context)
            val ed = sPref!!.edit()

            ed.putString(key, value)
            ed.apply()
        }

        /**
         * удалить значения в SharedPrefs
         */
        fun deletePref(context: Context) {
            val sPref = PreferenceManager.getDefaultSharedPreferences(context)
            sPref.edit().clear().apply()
        }

        /**
         * текущая дата
         */
        fun curDate(): String {
            return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        }

        /**
         * приводим дату и время в читабельный вид
         */
        fun convert_datetime(date: String, time: String): MutableList<String> {
            val converted: MutableList<String> = mutableListOf()

            var dat = ""
            var tim = ""

            if (date.isNotEmpty()) {
                dat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                    SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.getDefault()
                    ).parse(date)
                )
            }

            converted.add(dat)

            if (time.isNotEmpty()) {
                tim = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                    SimpleDateFormat(
                        "HHmmss",
                        Locale.getDefault()
                    ).parse(time)
                )
            }

            converted.add(tim)

            return converted
        }

        /**
         * высчитываем разницу дней
         */
        @SuppressLint("SimpleDateFormat")
        fun getDaysDiff(date: String): Long {
            val cDate = SimpleDateFormat("dd.MM.yyyy").parse(date)
            val lDate = Calendar.getInstance().time

            val milliseconds = lDate.time - cDate.time
            return milliseconds / (24 * 60 * 60 * 1000)
        }

        /**
         * опредлим свой snackbar
         */
        fun snackbar(layout: View, str: String) {
            val snackBar: Snackbar = Snackbar.make(layout, str, Snackbar.LENGTH_LONG)
            val view = snackBar.view
            val params = view.layoutParams as CoordinatorLayout.LayoutParams
            params.gravity = Gravity.TOP
            view.layoutParams = params
            snackBar.show()
        }


        /**
         * бипер для сканера
         */
        fun beeper(activity: Activity, mode: Char) {
            val beep: MediaPlayer?
            val path: Int

            try {
                path = if (mode == 'S') {
                    activity.resources.getIdentifier("successbeep", "raw", activity.packageName)
                } else {
                    activity.resources.getIdentifier("failbeep", "raw", activity.packageName)
                }

                beep = MediaPlayer.create(activity.applicationContext, path)
                beep.start()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * проверка обновлений
         */
        fun checkUpdate(context: Context) {
            val apiPar = HashMap<String, String>()
            apiPar["function"] = "sw_check_update"
            apiPar["werks"] = readPrefValue(context, "LOGIN")
            apiPar["date"] = curDate()
            apiPar["prodcode"] = PRODCODE
            apiPar["platform"] = PLATFORM
            apiPar["cvers"] = BuildConfig.VERSION_NAME

            val ts = APICallTask(
                api_params = apiPar,
                hash_params = "werks,function,date,prodcode,cvers"
            ).execute()

            val jsAvailUpd = ts.get()
            var jsonCode = ""
            try {
                val jsonStatus = jsAvailUpd.getString("status")
                val jObjectS = JSONObject(jsonStatus)
                jsonCode = jObjectS.getString("code")


                // ошибки при получении короба JSON
                if (jsonCode != "0") {
                    val jsonMessage = jObjectS.getString("message")
                    throw Exception(jsonMessage)
                }

                val jsonUpdSt = jsAvailUpd.getString("update_status")
                val jsonVers = jsAvailUpd.getString("new_vers")
                if (jsonUpdSt == "S") {

                    val ad = AlertDialog.Builder(context, R.style.AlertDialogCustomR)
                    ad.setTitle("Доступно обновление")

                    ad.setMessage("Вы хотите обновить приложение сейчас?")
                    ad.setPositiveButton("ОБНОВИТЬ") { dialog, _ ->
                        update(context, jsonVers)
                    }

                    ad.setNegativeButton("ПОЗЖЕ")
                    { dialog, _ -> dialog.dismiss() }

                    val alert = ad.create()
                    alert.show()

                } else {
                    throw Exception("Обновления отсутствуют")
                }

            } catch (e: Exception) {
                when (jsonCode) {
                    "7" -> Toast.makeText(
                        context,
                        "Проверьте и установите правильную дату на устройстве",
                        Toast.LENGTH_LONG
                    ).show()

                    else -> Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        /**
         * обновление приложения
         */
        fun update(context: Context, newvers: String): Boolean {
            var destination: String =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
            val fileName = "Employee.apk"
            destination += fileName
            val uri: Uri = Uri.parse("file://$destination")

            //Удалить файл обновления, если существует
            val file = File(destination)
            if (file.exists())
                file.delete()

            // параметры запроса url строки
            val apiPar = HashMap<String, String>()
            apiPar["function"] = "sw_get_update"
            apiPar["werks"] = readPrefValue(context, "LOGIN")
            apiPar["date"] = curDate()
            apiPar["prodcode"] = PRODCODE
            apiPar["platform"] = PLATFORM
            apiPar["cvers"] = newvers

            val ts = APICallTask(
                api_params = apiPar,
                hash_params = "werks,function,date,prodcode,cvers"
            )

            ts.execute()
            val jsHashUpd = ts.get()
            try {
                val jsonStatus = jsHashUpd.getString("status")
                val jObjectS = JSONObject(jsonStatus)
                val jsonCode = jObjectS.getString("code")

                if (jsonCode != "0") {
                    val jsonMessage = jObjectS.getString("message")
                    throw Exception("$jsonCode $jsonMessage")
                }

                val jsonLinkHash = jsHashUpd.getString("linkHash")
                if (jsonLinkHash.isNotEmpty()) {
                    // формируем url строку для загрузки
                    val host =
                        if (BuildConfig.DEBUG) API_URLUPD_Debug else API_URLUPD_Release
                    val url =
                        "$host/update?prodcode=$PRODCODE&platform=$PLATFORM&vers=$newvers&hash=$jsonLinkHash"
                    //установим downloadmanager
                    val manager =
                        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    val request =
                        DownloadManager.Request(Uri.parse(url))

                    //установить пункт назначения
                    request.setDescription("Установка обновлений...")
                    request.setTitle(context.getString(R.string.app_name))
                    request.setMimeType("application/vnd.android.package-archive")
                    request.setDestinationUri(uri)

                    // устанавливаем apk
                    showInstallOption(context, destination, uri)
                    manager.enqueue(request)
                    Toast.makeText(context, "Загрузка...", Toast.LENGTH_LONG)
                        .show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                return false
            }

            return true
        }

        /**
         * установка обновления
         */
        private fun showInstallOption(context: Context, destination: String, uri: Uri) {
            // установить BroadcastReceiver для установки приложения при загрузке .apk
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + ".provider",
                            File(destination)
                        )
                        val install = Intent(Intent.ACTION_VIEW)
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        install.data = contentUri
                        context.startActivity(install)
                        context.unregisterReceiver(this)
                        contentUri.path?.let { Log.i("updLogs", it) }
                        // finish()

                    } else {
                        val install = Intent(Intent.ACTION_VIEW)
                        install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        install.setDataAndType(
                            uri,
                            "\"application/vnd.android.package-archive\""
                        )
                        context.startActivity(install)
                        Log.i("updLogs", uri.toString())
                        context.unregisterReceiver(this)
                        // finish()

                    }
                }


            }
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        /**
        получим разрешение на камеру
         */
        fun camPermission(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                permission
            )
        }

        fun filesPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    val uri: Uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    permission
                )
            } else {
                return
            }
        }

        /**
         * проверим заполненость адреса сервера магазина
         */
        @SuppressLint("MissingPermission")
        fun checkMServIP(ctx: Context): Boolean {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (wifiInfo == null || !wifiInfo.isConnected) {
                Toast.makeText(ctx, "Вы не подключены к сети магазина", Toast.LENGTH_LONG).show()
                return false
            }

            if (readPrefValue(ctx, PREF_WIP).isEmpty()) {
                savePrefWIP(ctx)
            }

            return true
        }

        /**
         * проверить разрешение экрана
         */
        fun checkResDev(ctx: Context): Boolean {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ctx.display
            } else {
                ctx.getSystemService<DisplayManager>()?.getDisplay(Display.DEFAULT_DISPLAY)
            }

            if (display == null) {
                Toast.makeText(ctx, "Ваш дисплей телефона не поддерживается", Toast.LENGTH_LONG)
                    .show()
                return false
            }

            val point = Point()
            display.getSize(point)
            if (point.y < 1300) {
                Toast.makeText(ctx, "Ваш дисплей телефона не поддерживается", Toast.LENGTH_LONG)
                    .show()
                return false
            }
            return true
        }

        /**
         * получить строку с адресов сервера
         */
        fun getIpMagServ(context: Context): URL {
            return if (BuildConfig.DEBUG &&
                API_URL_Debug == "https://www.monro.biz:44018/SWP/mobile_api/v1.0"
            ) {
                URL(
                    "https://" + readPrefValue(
                        context,
                        PREF_WIP
                    ) + ":44018/StServer3/mobile_api/v1.0"
                )
            } else {
                URL(
                    "https://" + readPrefValue(
                        context,
                        PREF_WIP
                    ) + ":8443/StServer3/mobile_api/v1.0"
                )
            }
        }

        /**
         * диалог сохранения адреса сервера магазина
         */
        @SuppressLint("ResourceType")
        fun savePrefWIP(ctx: Context): Boolean {
            val save = false
            val ad = AlertDialog.Builder(ctx, R.style.AlertDialogCustomW)
                .setTitle("Сервер магазина")
                .setMessage("Введите адрес сервера магазина")
                .setCancelable(false)
                .setIcon(R.raw.mservip)

            val ip = EditText(ctx)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )


            ip.inputType = InputType.TYPE_CLASS_NUMBER
            ip.keyListener = DigitsKeyListener.getInstance("0123456789 -.")
            ip.layoutParams = lp
            ip.setText(readPrefValue(ctx, PREF_WIP))
            ip.setSelection(ip.text.length)


            val listener: MaskedTextChangedListener = if (BuildConfig.DEBUG &&
                API_URL_Debug == "https://www.monro.biz:44018/SWP/mobile_api/v1.0"
            )
                MaskedTextChangedListener("80.89.132.14", ip)
            else
                MaskedTextChangedListener("{192.168.}[000].[000]", ip)

            ip.addTextChangedListener(listener)
            ip.onFocusChangeListener = listener

            ip.hint = listener.placeholder()
            ad.setView(ip)

            ad.setPositiveButton("Сохранить") { dialog, _ ->
                if (ip.text.toString().isNotEmpty()) {
                    savePrefValue(ctx, PREF_WIP, ip.text.toString())
                    save
                } else {
                    !save
                    dialog.dismiss()
                }
            }
            ad.setNegativeButton("Выход") { dialog, _ ->
                dialog.cancel()
            }
            ad.show()

            return save
        }

        /**
         * получение номера материала по ШК
         */
        fun getMatnrFromBC(barc: String): String {
            var matnr = ""

            try {
                matnr = barc.substring(0, 10)

                matnr = if (matnr.substring(0, 1) == "1") {
                    // вариант родового товара, ценник или этикетка
                    "000000001000" + matnr.substring(1, 7)
                } else {
                    if (matnr.substring(0, 3) == "001" || matnr.substring(0, 3) == "009")
                    // отдельный материал, ценник
                        "000000000" + matnr.substring(0, 9)
                    else
                    // отдельный материал - этикетка
                        "00000000$matnr"
                }
            } catch (Ex: Exception) {
                matnr = "000000000000000000"
            }

            return matnr
        }

        /*
        получим ID устройства
        */
        fun getIDevice(context: Context): String {
            val pseudoID = "24" +
                    Build.BOARD + Build.BRAND + Build.DEVICE +
                    Build.DISPLAY + Build.HOST +
                    Build.ID + Build.MANUFACTURER +
                    Build.MODEL.length + Build.PRODUCT +
                    Build.TAGS + Build.TYPE +
                    Build.USER

            val idHashed = String.format(
                "%032x",
                BigInteger(
                    1,
                    MessageDigest.getInstance("MD5").digest(pseudoID.toByteArray(Charsets.UTF_8))
                )
            )
            return idHashed
        }

        /**
         * текст из что нового после обновления
         */
        fun whatIsNew(): String {
            val text =
                "\u2022" + "В проверку КМ перед продажей добавлено сравнение допустимых поставщиков\n\n"


            return text
        }
    }
}