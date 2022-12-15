@file:Suppress("DEPRECATION")

package biz.monro.employee.BaseActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.BuildConfig
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivityAuthBinding
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.system.exitProcess


class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    private var loginAttempts = 3
    private var code: String? = null
    private var cntEnginerMode = 0

    private var cityList = HashMap<String, String>()
    private var werksList = ArrayList<Map<String, List<String>>>()
    private var werksTel = HashMap<String, ArrayList<String>>()
    private var werksId = HashMap<String, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppThemeNoTitle)

        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.DEBUG)
            Runtime.getRuntime().exec("am set-debug-app biz.monro.employee")


        // загружаем авторизацию
        if (loadPass()) {
            return
        }

        binding.attempts.setBackgroundColor(Color.RED)

        // атрибуты запроса списка магазинов
        val apiWerksPar = HashMap<String, String>()
        apiWerksPar["function"] = "get_werks_list"
        apiWerksPar["date"] = CommonFun.curDate()

        val ts = APICallTask(api_params = apiWerksPar, hash_params = "function,date")
        ts.execute()


        val error = parseWerks(ts.get())

        // проверим на ошибки получение магазина
        if (error != "") {
            binding.attempts.visibility = View.VISIBLE
            binding.attempts.text = error
        } else {
            binding.attempts.visibility = View.INVISIBLE
        }

        // спиннер для городов
        val spinCity = ArrayList(cityList.values)
        spinCity.sort()

        val adapCity = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item, spinCity
        )
        binding.authCitySpin.adapter = adapCity


        // выбор города и подтягивание магазинов
        binding.authCitySpin.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    itemSelected: View, selectedItemPosition: Int, selectedId: Long
                ) {
                    binding.authCodeEdt.isEnabled = false
                    binding.buttonLogin.isEnabled = false
                    val selCity = binding.authCitySpin.selectedItem.toString()
                    // спиннер для магазинов
                    werksTel = spinWerks(selCity)
                    if (werksTel.isEmpty())
                        binding.authTelView.text = ""
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        // выбор магазина и подтягивание телефона
        binding.authMagSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                itemSelected: View, selectedItemPosition: Int, selectedId: Long
            ) {
                binding.authCodeEdt.isEnabled = false
                binding.buttonLogin.isEnabled = false
                val selWerks = binding.authMagSpin.selectedItem.toString()
                // спиннер для магазинов
                getWrkTel(selWerks)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // кнопка отправить код
        binding.authSendcodeBtn.setOnClickListener {
            // код отправляем с ожиданием в 1.5минуты
            object : CountDownTimer(90000, 1000) {
                //Здесь обновляем текст счетчика обратного отсчета с каждой секундой
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    binding.authTimer.isEnabled = true
                    binding.authTimer.text =
                        "Отправить повторно код через: " + millisUntilFinished / 1000 + " сек."
                    binding.authSendcodeBtn.isEnabled = false
                }

                override fun onFinish() {
                    binding.authTimer.text = ""
                    binding.authTimer.isEnabled = false
                    binding.authSendcodeBtn.isEnabled = true
                }
            }.start()


            sendSMS()
        }

        // кнопка войти
        binding.buttonLogin.setOnClickListener {
            login()
        }
    }

    // набор магазинов в зависимости от города
    private fun spinWerks(city: String): HashMap<String, ArrayList<String>> {
        var cKey: String? = null
        val werksTel = HashMap<String, ArrayList<String>>()

        for (c in cityList.entries) {
            if (c.value == city) {
                cKey = c.key
                break
            }
        }

        // заполняем спиннер для магазинов
        val spinWerks = ArrayList<String>()

        for (i in 0 until werksList.size) {
            for (w in werksList[i].entries) {
                for (c in w.value) {
                    if (c == cKey) {
                        werksTel[w.key] = arrayListOf(w.value[0], w.value[3])
                        //werksTel[w.value[0]] = w.value[3]
                        spinWerks.add(w.value[0])
                    }
                }
            }
        }

        spinWerks.sort()
        val adapWerks = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, spinWerks
        )
        binding.authMagSpin.adapter = adapWerks

        return werksTel
    }

    // проставление телефона
    private fun getWrkTel(name: String) {

        for (c in werksTel.entries) {
            if (c.value[0] == name) {
                werksId.clear()
                binding.authTelView.text = c.value[1]
                binding.authSendcodeBtn.isEnabled = binding.authTelView.text.isNotEmpty()

                if (binding.authTelView.text.isNotEmpty())
                    werksId[c.key] = c.value[0]
                break
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun parseWerks(json: JSONObject): String {
        var error = ""

        try {
            val jsonCity = json.optJSONArray("city_list")
            val jsonWerks = json.optJSONArray("werks_list")

            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")


            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            for (i in 0 until jsonCity.length()) {
                val jsonObject = jsonCity.getJSONObject(i)
                cityList[jsonObject.optString("code").toString()] =
                    jsonObject.optString("name").toString()
            }

            for (i in 0 until jsonWerks.length()) {
                val jsonObject = jsonWerks.getJSONObject(i)

                val map = HashMap<String, List<String>>()
                val list = ArrayList<String>()

                list.add(jsonObject.optString("name").toString())
                list.add(jsonObject.optString("rpnum").toString())
                list.add(jsonObject.optString("city").toString())
                list.add(jsonObject.optString("phone_num").toString())
                map[jsonObject.optString("werks").toString()] = list

                werksList.add(map)
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
            error = "Ошибка JSON магазинов"
        }

        return error
    }

    private fun parseSMS(json: JSONObject): String? {
        var error = ""

        try {
            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            val jCode = json.getString("code")

            if (jCode.isNotEmpty()) {
                binding.authCodeEdt.isEnabled = true
                binding.buttonLogin.isEnabled = true
                code = jCode
            } else {
                binding.authCodeEdt.isEnabled = true
                binding.buttonLogin.isEnabled = true
                binding.attempts.visibility = View.VISIBLE
                binding.attempts.text = "Ошибка отправки СМС"
                code = ""
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
            error = "Ошибка JSON СМС"
        }

        if (error != "") {
            binding.attempts.visibility = View.VISIBLE
            binding.attempts.text = error
        } else {
            binding.attempts.visibility = View.INVISIBLE
        }

        return code
    }

    private fun login() {
        val codeEdt = binding.authCodeEdt.text.toString()
        if (codeEdt == code) {
            // сохраняем настройку
            savePass()

            val intent = Intent(this@AuthActivity, MainActivity::class.java)
            startActivity(intent)

        } else {
            loginAttempts--
            // Делаем видимыми текстовые поля, указывающие на количество оставшихся попыток:
            binding.attempts.visibility = View.VISIBLE
            binding.attempts.text = "Не верно введен код из СМС"

            // Когда выполнено 3 безуспешных попытки залогиниться, выставляем
            // кнопке настройку невозможности нажатия
            if (loginAttempts == 0) {
                code = ""
                binding.buttonLogin.isEnabled = false
                binding.attempts.text = "Введено больше 3 попыток. Отправьте код заново"
            }
        }
    }


    // отправка СМС
    private fun sendSMS() {

        // атрибуты запроса отправки смс
        val apiSMSPar = HashMap<String, String>()
        apiSMSPar["function"] = "werks_auth_sms"
        apiSMSPar["date"] = CommonFun.curDate()
        apiSMSPar["werks"] = werksId.entries.iterator().next().key

        val ts = APICallTask(api_params = apiSMSPar, hash_params = "function,date,werks")
        ts.execute()
        code = parseSMS(ts.get())
        if (CommonFun.getIDevice(this) == "c5bcef83bdd0684f1014260e5ece167c")
            Toast.makeText(this, code, Toast.LENGTH_LONG).show()
    }


    // загржаем авторизацию
    private fun loadPass(): Boolean {
        val werks = CommonFun.readPrefValue(applicationContext, CommonFun.PREF_WERKS)
        val wname = CommonFun.readPrefValue(applicationContext, CommonFun.PREF_WNAME)

        return if (werks != "") {
            werksId[werks] = wname
            intent =
                Intent(this@AuthActivity, MainActivity::class.java).putExtra("werksId", werksId)
            startActivity(intent)
            true
        } else
            false
    }

    // сохраняем авторизацию
    private fun savePass() {
        code = ""
        binding.authCodeEdt.text.clear()

        val it = werksId.entries.iterator().next()

        CommonFun.savePrefValue(applicationContext, CommonFun.PREF_WERKS, it.key)
        CommonFun.savePrefValue(applicationContext, CommonFun.PREF_WNAME, it.value)
    }


    override fun onDestroy() {
        super.onDestroy()
        onBackPressed()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }


    // для активации режима разработчика отслеживаем нажатия качельки
    // и сопоставляем уникальный ключ с введенным
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            binding.Login.setOnLongClickListener {
                cntEnginerMode += 1
                true
            }

            if (cntEnginerMode == 4) {
                cntEnginerMode = 0

                val android_id = ("D" + Build.BOARD.length + Build.BRAND.length +
                        Build.DISPLAY.length + Build.MANUFACTURER.length + Build.USER.length)
                    .substring(0, 5)

                val ad = AlertDialog.Builder(this, R.style.AlertDialogCustomW)
                    .setTitle("Ваш ID")
                    .setMessage("Введите ID")
                    .setCancelable(false)

                val id = EditText(this)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                id.hint = "ID"
                id.inputType = InputType.TYPE_CLASS_TEXT
                id.layoutParams = lp

                ad.setView(id)

                ad.setPositiveButton("Сохранить") { dialog, _ ->
                    if (id.text.toString().isNotEmpty() && (id.text.toString() == android_id))
                        savePass()
                    else
                        dialog.dismiss()
                }
                ad.setNegativeButton("Выход") { dialog, _ ->
                    dialog.cancel()
                }
                ad.show()

                Toast.makeText(this, "Режим разработчика", Toast.LENGTH_SHORT).show()
            }

        }
        return super.onKeyDown(keyCode, event)
    }
}