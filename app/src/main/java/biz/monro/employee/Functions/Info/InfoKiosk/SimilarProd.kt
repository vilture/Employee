@file:Suppress("DEPRECATION")

package biz.monro.employee.Functions.InfoKiosk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.Adapter.ItemDecoration
import biz.monro.employee.Adapter.RecyclerItemClickListener
import biz.monro.employee.Adapter.SimilarProdAdapter
import biz.monro.employee.Adapter.similarProd
import biz.monro.employee.R
import biz.monro.employee.databinding.ActivitySimilarprodBinding
import org.json.JSONObject
import java.net.URL


class SimilarProd : AppCompatActivity() {

    private lateinit var binding: ActivitySimilarprodBinding

    // материал основа
    private var kioskMatnr = ""
    private var kioskMaktx = ""

    // адаптер списка
    private lateinit var similarAdapter: SimilarProdAdapter

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySimilarprodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        kioskMatnr = intent.getStringExtra("MATNR")!!
        kioskMaktx = intent.getStringExtra("MAKTX")!!

        // если материал не передан, то выходим из функции
        if (kioskMatnr.isEmpty())
            return

        // задаем свой адаптер нашему списку
        initRecyclerView()
        // добавляем данные в список
        addDataSet()

        // навешиваем слушателя на нажатие карточки товара
        val fm = supportFragmentManager
        binding.similarList.addOnItemTouchListener(
            RecyclerItemClickListener(binding.similarList,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, matnr: String) {
                        fm.beginTransaction()
                            .replace(R.id.similar_container, SimilarProdItemFragment(matnr))
                            .addToBackStack(null)
                            .commit()
                        binding.similarContainer.visibility = View.VISIBLE
                    }
                })
        )
    }

    /**
     * добавляем данные в список материалов
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun addDataSet() {
        val list = ArrayList<similarProd>()

        // запросим данные по похожим товарам из АПИ
        val apiUniq = HashMap<String, String>()
        apiUniq["function"] = "get_goods_like"
        apiUniq["date"] = CommonFun.curDate()
        apiUniq["werks"] = CommonFun.readPrefValue(this, CommonFun.PREF_WERKS)
        apiUniq["matnr"] = kioskMatnr

        val tsz = APICallTask(
            api_params = apiUniq,
            hash_params = "date,werks,function,matnr",
            CallBack = null,
            callsrc = "",
            api_url = URL(CommonFun.API_URLWEB_Release)
        ).execute()
        val json = tsz.get()
        val error: String

        try {
            val jsonGoods = json.optJSONArray("goods_like")
            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")

            // ошибки при получении JSON
            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            // заполняем массив товаров
            for (i in 0 until jsonGoods.length()) {
                val jsonObject = jsonGoods.getJSONObject(i)
                var racode = ""
                if (jsonObject.optString("ra_code").isNotEmpty()){
                    racode = "-" + jsonObject.optString("ra_code").substring(2) + "%"
                }
                list.add(
                    similarProd(
                        jsonObject.optString("photoURL"),
                        jsonObject.optString("matnr"),
                        jsonObject.optString("maktx"),
                        jsonObject.optString("price"),
                        racode
                    )
                )
            }

            // переносим все в нашу таблицу похожих товаров
            similarAdapter.submitList(list)

        } catch (ex: Exception) {
            ex.printStackTrace()

            AlertDialog.Builder(this, R.style.AlertDialogCustomR)
                .setTitle("Ошибка")
                .setMessage("Не удалось получить похожие товары\n" + ex.message.toString())
                .setPositiveButton("Прочитал") { dialog, _ -> onBackPressed() }
                .create().show()
        }
    }


    /**
     * инициализируем адаптер списка
     */
    @SuppressLint("SetTextI18n")
    private fun initRecyclerView() {
        binding.similarText.text = "Похожие модели на\n$kioskMaktx\nв наличии:"
        binding.similarList.apply {
            layoutManager = LinearLayoutManager(this@SimilarProd)
            val topSpacingDecorator = ItemDecoration(30)
            addItemDecoration(topSpacingDecorator)
            similarAdapter = SimilarProdAdapter()
            adapter = similarAdapter
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        binding.similarContainer.visibility = View.GONE
    }


    /**
     * на всякий случай уберем долгое нажатие на кнопку назад
     * на некоторый телефонах это вызывает режим 2-layer screen
     * что в свою очередь разрушает активность и перестраивает формат экрана
     */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            true
        } else super.onKeyLongPress(keyCode, event)
    }
}