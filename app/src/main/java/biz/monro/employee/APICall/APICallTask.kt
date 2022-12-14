@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package biz.monro.employee.APICall

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Base64
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


/** Специальный класс асинхронной задачи для выполнения запросов к серверу API
 *  Обработка результата задачи выполняется посредством вызова методов переданного в задачу
 *  интерфейса APICallCallback.     */

class APICallTask(
    val api_params: HashMap<String, String>,
    val hash_params: String,
    val callsrc: String = "",
    private var CallBack: APICallCallback? = null,
    val api_jspar: JSONObject? = null,
    val api_url: URL? = null
) : AsyncTask<Void, Int, JSONObject>() {
    // статические константы и определения
    companion object {
        const val API_user: String = "userAPI"
        const val API_Pwd: String = "We5gtyul"

        const val API_Connecting: Int = 1
        const val API_RunQuery: Int = 2
        const val API_Processing: Int = 3

        /** создаем JSON объект статуса и запоминаем код */
        fun putStatus(jo: JSONObject, code: String, msg: String) {
            val status = JSONObject()

            status.put("code", code)
            status.put("usermsg", msg)

            jo.put("status", status)
        }
    }

    private var resCode: String = ""


    /** очистка интерфейса обратного вызова */
    fun clearCallBack() {
        this.CallBack = null
    }

    /** собственно метод выполняемый в фоне */
    override fun doInBackground(vararg params: Void?): JSONObject {
        // переносим параметры в Json и добавляем хеш
        val jsonParam = paramsToJson()

        // выполняем web запрос
        val jsonResult = doAPIWebRequest(jsonParam)

        // анализируем результат запроса, если он не содержит статуса и кода возврата - добавялем их
        val status: JSONObject = jsonResult.optJSONObject("status") ?: JSONObject()

        resCode = status.optString("code", "")

        if (resCode.isEmpty())
            jsonResult.put(
                "status",
                createStatus("550", "Внутренняя ошибка - неверный результат вызова API")
            )

        // вызываем метод, который должен выполняться при успешном вызове API
        if (resCode == "0")
            if (CallBack != null) {
                this.CallBack?.onAPISucessAsync(callsrc, jsonResult)
            }


        return jsonResult
    }

    /** обновление прогресса */
    override fun onProgressUpdate(vararg values: Int?) {
        val ccode = values[0] ?: 0
        val cperc = values[1] ?: 0

        if (ccode != 0)
            this.CallBack?.onAPIProgressUpdate(ccode, cperc)
    }

    /** вызов метода обработки результата */
    override fun onPostExecute(result: JSONObject) {

        // анализируем обработки результат запроса, если он не содержит статуса и кода возврата - добавялем их
        val status: JSONObject = result.optJSONObject("status") ?: JSONObject()
        resCode = status.optString("code", "")

        if (resCode.isEmpty())
            result.put(
                "status",
                createStatus("555", "Внутренняя ошибка - неверный результат обработки")
            )


        if (resCode == "0")
            this.CallBack?.onAPISucess(callsrc, result)
        else
            this.CallBack?.onAPIError(callsrc, result)
    }

    /** выполнение web запроса к серверу API */
    private fun doAPIWebRequest(param: JSONObject): JSONObject {
        var jsonResult = JSONObject()

        // готовим соединение
        val url: URL = api_url ?: if (BuildConfig.DEBUG) URL(CommonFun.API_URL_Debug) else URL(
            CommonFun.API_URL_Release
        )

        trustEveryone()

        val conn = url.openConnection() as HttpsURLConnection


        try {
            val authUsr = ("$API_user:$API_Pwd").toByteArray(charset("UTF-8"))
            val authLine = Base64.encodeToString(authUsr, Base64.NO_WRAP)

            // настройки соединения
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Basic $authLine")
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Connection", "close")
            conn.connectTimeout = 8000
            conn.useCaches = false
            conn.doOutput = true
            conn.doInput = true

            // соединяемся
            publishProgress(API_Connecting, 0)
            conn.connect()

            // передаем запрос
            publishProgress(API_RunQuery, 0)
            val outputS = DataOutputStream(conn.outputStream)
            val writer = BufferedWriter(OutputStreamWriter(outputS, "UTF-8"))
            writer.write(param.toString())
            writer.flush()
            writer.close()
            outputS.flush()
            outputS.close()

            // анализируем ответ сервера
            if (conn.responseCode != 200) {
                jsonResult.put(
                    "status",
                    createStatus(
                        "551",
                        "Ошибка обработки запроса сервера: ${conn.responseMessage}"
                    )
                )
            } else {
                try {
                    val response = BufferedReader(
                        InputStreamReader(
                            conn.inputStream,
                            "UTF-8"
                        ) as Reader?
                    ).readText()
                    jsonResult = JSONObject(response)

                    // анализируем ответ сервера
                    val status: JSONObject = jsonResult.optJSONObject("status")
                    val code = status.optString("code", "") ?: ""
                    if (code.isEmpty())
                        jsonResult.put(
                            "status",
                            createStatus(
                                "553",
                                "Ошибка неверный ответ сервера (нет кода результата)"
                            )
                        )


                } catch (e: JSONException) {
                    jsonResult.put(
                        "status",
                        createStatus("552", "Ошибка неправильного ответа сервера (ошибка JSON)")
                    )
                }
            }

        } catch (e: Exception) {
            val text = if (e.message.toString().contains("Invalid host", true))
                "не указан сервер подключения"
            else
                e.message.toString()

            jsonResult.put(
                "status",
                createStatus("554", "Ошибка подключения к серверу: $text")
            )
        } finally {
            try {
                conn.inputStream?.close()
                conn.disconnect()
            } catch (e: IOException) {
            }
        }

        publishProgress(API_Processing, 0)
        return jsonResult
    }

    // разрешение подключения без сертификатов SSL
    @SuppressLint("TrustAllX509TrustManager")
    private fun trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<X509TrustManager>(object : X509TrustManager {

                override fun checkClientTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate?> {
                    return arrayOfNulls(0)
                }
            }), SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(
                context.socketFactory
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /** вычисление хеша для текущего запроса */
    private fun getHashString(): String {
        val hashItem = hash_params.split(",")
        var hash = ""

        for (item in hashItem) {
            if (api_params.containsKey(item))
                hash += api_params[item]
        }

        hash += if (api_url == null)
            "dghklkls554-ld"
        else
            "DfW23!gg"

        val bytes = hash.toByteArray(charset("UTF-8"))
        val md = MessageDigest.getInstance("SHA-256")
        val ba = md.digest(bytes)
        hash = String.format("%064x", BigInteger(1, ba))

        return hash
    }


    /** переносим параметры из входящей матрицы в Json объект и добавляем хеш */
    private fun paramsToJson(): JSONObject {
        val jsonParam = JSONObject()
        val hash = getHashString()


        api_params.forEach { (key, value) ->
            jsonParam.put(key, value)
        }

        /** добавляем параметры, переданные в дополнительном  JSON объекте **/
        api_jspar?.keys()?.forEach { It -> jsonParam.put(It, api_jspar.opt(It)) }

        jsonParam.put("hash", hash)
        return jsonParam
    }

    /** создаем JSON объект статуса и запоминаем код */
    private fun createStatus(code: String, msg: String): JSONObject {
        val status = JSONObject()

        resCode = code
        status.put("code", resCode)
        status.put("usermsg", msg)

        return status
    }
}