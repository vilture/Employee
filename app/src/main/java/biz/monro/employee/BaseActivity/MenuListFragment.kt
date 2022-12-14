package biz.monro.employee.BaseActivity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import biz.monro.employee.BuildConfig
import biz.monro.employee.Functions.CommonFun
import biz.monro.employee.R
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat


class MenuListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_menu, container,
            false
        )


        val vNavigation = view.findViewById<View>(R.id.menuNavigation) as NavigationView
        if (BuildConfig.DEBUG) {
            vNavigation.menu.getItem(5).isVisible = true
        }

        vNavigation.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_mservip -> {
                    activity?.let { CommonFun.savePrefWIP(it) }
                }
                R.id.action_help -> {
                    val intent = Intent(activity, Help::class.java)
                    startActivity(intent)
                }
                R.id.action_info -> {
                    info()
                }
                R.id.action_update -> {
                    // проверка обновления
                    activity?.let { CommonFun.checkUpdate(it) }
                }
                R.id.action_userChange -> {
                    AlertDialog.Builder(activity, R.style.AlertDialogCustomW)
                        .setTitle("Подтвердите выход")
                        .setMessage("Вы уверены что хотите изменить пользователя?")
                        .setPositiveButton("Да") { dialog, _ ->
                            activity?.let { CommonFun.deletePref(it) }

                            val intent = Intent(activity, AuthActivity::class.java)
                            startActivity(intent)
                            activity?.finish()
                        }

                        .setNegativeButton("Нет")
                        { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
                R.id.action_debug -> {
                    // val cb = menuItem.actionView as CheckBox
                    menuItem.isChecked = !menuItem.isChecked

                    if (!menuItem.isChecked) {
                        CommonFun.API_URLUPD_Debug = CommonFun.API_URLUPD_Release
                        CommonFun.API_URL_Debug = CommonFun.API_URL_Release
                    } else {
                        CommonFun.API_URLUPD_Debug = CommonFun.API_URLUPD_DebugC
                        CommonFun.API_URL_Debug = CommonFun.API_URL_DebugC
                    }
                }
            }

            false
        }

        return view
    }

    /**
     * опция меню Информация
     */
    @SuppressLint("SimpleDateFormat", "ResourceType")
    private fun info() {
        val sb = StringBuilder()
        sb.append(
            "Вы вошли под: " + activity?.let { CommonFun.readPrefValue(it, CommonFun.PREF_WERKS) }
        )

        sb.append("\n\nВерсия приложения: " + BuildConfig.VERSION_NAME)
        sb.append("\nСборка: " + SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(BuildConfig.TIMESTAMP))
        sb.append("\nЧто нового?: " + BuildConfig.CHANGELOG)

        AlertDialog.Builder(activity, R.style.AlertDialogCustomW)
            .setTitle("Сотрудник Монро.\n MONRO©")
            .setMessage(sb.toString())
            .setIcon(R.raw.information)
            .setNegativeButton(
                "Назад"
            ) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }
}