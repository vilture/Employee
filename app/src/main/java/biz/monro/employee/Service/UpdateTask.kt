package biz.monro.employee.Service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.BuildConfig
import biz.monro.employee.Functions.CommonFun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*


class UpdateTask : Service() {

    private val timer = Timer()
    private val TAG = "updLogs"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Сервис обновлений запущен")

        val apiPar = HashMap<String, String>()
        apiPar["function"] = "sw_check_update"
        apiPar["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiPar["date"] = CommonFun.curDate()
        apiPar["prodcode"] = CommonFun.PRODCODE
        apiPar["platform"] = CommonFun.PLATFORM
        apiPar["cvers"] = BuildConfig.VERSION_NAME

        // каждый час инициируем проверку обновлений
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                GlobalScope.launch(Dispatchers.Main) { checkUpdate(apiPar) }
            }
        }, 0, 3600000)

        return START_STICKY
    }

    fun checkUpdate(apiPar: HashMap<String, String>) {
        Log.i("updLogs", "Проверка обновлений")

        val ts = APICallTask(
            api_params = apiPar,
            hash_params = "werks,function,date,prodcode,cvers"
        ).execute()

        val jsAvailUpd = ts.get()

        try {
            val jsonStatus = jsAvailUpd.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")


            // ошибки при получении короба JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("message")
                Log.i("updLogs", "$jsonCode $jsonMessage")

                val intent = Intent("Update")
                intent.putExtra("updatetask", true)
                intent.putExtra("newvers", "")
                when (jsonCode) {
                    "7" -> intent.putExtra(
                        "upderror",
                        "Проверьте и установите правильную дату на устройстве"
                    )
                    else -> intent.putExtra("upderror", jsonMessage)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                throw Exception()
            }

            val jsonUpdSt = jsAvailUpd.getString("update_status")
            val jsonVers = jsAvailUpd.getString("new_vers")
            if (jsonUpdSt == "S") {
                Log.i("updLogs", "Найдены обновления")

                val intent = Intent("Update")
                intent.putExtra("updatetask", true)
                intent.putExtra("newvers", jsonVers)
                intent.putExtra("upderror", "")
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            } else {
                Log.i("updLogs", "У вас последняя версия")
            }


        } catch (e: Exception) {
            Log.i("updLogs", "Ошибка обновления ${e.message}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "Сервис обновления остановлен")
    }

}
