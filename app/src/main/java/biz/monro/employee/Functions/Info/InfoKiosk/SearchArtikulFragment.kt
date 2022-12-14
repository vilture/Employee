package biz.monro.employee.Functions.InfoKiosk

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.APICall.APICallTask
import biz.monro.employee.R
import biz.monro.employee.databinding.MessageDialogBinding
import org.json.JSONException
import org.json.JSONObject
import java.util.*


class SearchArtikulFragment : AppCompatDialogFragment() {
    private lateinit var mdView: MessageDialogBinding

    private var listener: DialogListener? = null
    private var artEdit = ""
    private var artList = HashMap<String, String>()

    @SuppressLint("ResourceAsColor")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mdView =
            MessageDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = AlertDialog.Builder(requireActivity(), R.style.AlertDialogCustomW)
        dialog.setView(mdView.root)


        dialog.setCancelable(true)

        mdView.mdText.text = "Поиск по артикулу"
        mdView.mdText.setTextColor(Color.BLACK)
        mdView.mdEdit.hint = "Артикул"
        mdView.mdEdit.isFocusable = true
        mdView.mdBtn1.text = "Найти"
        mdView.mdBtn2.visibility = View.GONE
        mdView.mdSpin.visibility = View.INVISIBLE

        mdView.mdEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    artList.clear()
                    mdView.mdSpin.adapter = null
                    if (s.length > 2) {
                        getListMatnr(mdView, s)
                        mdView.mdSpin.visibility = View.VISIBLE
                    } else
                        mdView.mdSpin.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        mdView.mdBtn1.setOnClickListener {
            var selMatnr = ""
            if (mdView.mdSpin.selectedItem != null) {
                val selFind = mdView.mdSpin.selectedItem.toString()
                for (c in artList.entries) {
                    if (c.value == selFind) {
                        selMatnr = c.key
                        break
                    }
                }
                if (selMatnr != "")
                    listener!!.selectedMatnr(selMatnr)
            }
        }

        mdView.mdEdit.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                artEdit = mdView.mdEdit.text.toString()
            }
        }


        return dialog.create()
    }

    private fun getListMatnr(mdView: MessageDialogBinding, s: CharSequence) {
        // атрибуты запроса списка поиска по артикулу
        val apiFind = HashMap<String, String>()
        apiFind["function"] = "find_material"
        apiFind["date"] = CommonFun.curDate()
        apiFind["werks"] = CommonFun.readPrefValue(requireActivity(), CommonFun.PREF_WERKS)
        apiFind["instr"] = s.toString()
        apiFind["exlbst"] = "X"

        val ts = APICallTask(
            api_params = apiFind,
            hash_params = "date,werks,function,instr",
            CallBack = null,
            callsrc = "",
            api_url = CommonFun.getIpMagServ(requireActivity())
        ).execute()


        val okData = parseListFind(ts.get())

        if (okData != "") {
            Toast.makeText(requireActivity(), okData, Toast.LENGTH_SHORT)
                .show()
        } else {
            val spin = ArrayList(artList.values)
            spin.sort()

            val adapter = ArrayAdapter(
                requireActivity(),
                R.layout.spinrow, spin
            )
            mdView.mdSpin.adapter = adapter
        }
    }

    private fun parseListFind(json: JSONObject): String {
        var error = ""

        try {
            val jsonSrch = json.optJSONArray("srch_res")

            val jsonStatus = json.getString("status")
            val jObjectS = JSONObject(jsonStatus)
            val jsonCode = jObjectS.getString("code")


            if (jsonCode != "0") {
                val jsonMessage = jObjectS.getString("usermsg")
                error = "$jsonCode $jsonMessage"

                throw Exception(error)
            }

            for (i in 0 until jsonSrch.length()) {
                val jsonObject = jsonSrch.getJSONObject(i)
                artList[jsonObject.optString("matnr").toString()] =
                    jsonObject.optString("maktx").toString()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
            error = "Ошибка JSON поиска"
        }

        return error
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as DialogListener
        } catch (e: ClassCastException) {
        }
    }


    interface DialogListener {
        fun selectedMatnr(matnr: String)
    }

}