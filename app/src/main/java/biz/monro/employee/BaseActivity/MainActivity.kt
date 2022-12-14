@file:Suppress("DEPRECATION")

package biz.monro.employee.BaseActivity

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import biz.monro.employee.BuildConfig
import biz.monro.employee.Functions.*
import biz.monro.employee.Functions.InfoKiosk.InfoKiosk
import biz.monro.employee.R
import biz.monro.employee.Service.UpdateTask
import biz.monro.employee.databinding.ActivityMainBinding
import com.mxn.soul.flowingdrawer_core.ElasticDrawer
import kotlin.system.exitProcess


/**
 * основная активность
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var PERMISSION_REQUEST_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // разрешение на установку обновлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                        Uri.parse(String.format("package:%s", packageName))
                    ), 1007
                )
            }
        } else {
            @Suppress("DEPRECATION")
            if (Settings.Secure.getInt(
                    contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS
                ) != 1
            ) {
                Toast.makeText(this, "Включите неизвестные источники", Toast.LENGTH_LONG).show()
                startActivityForResult(Intent(Settings.ACTION_SECURITY_SETTINGS), 1007)
            }
        }


        startService(Intent(this, UpdateTask::class.java))
        binding.mainHeader.text = CommonFun.readPrefValue(
            this,
            CommonFun.PREF_WNAME
        )

        // запросим разрешения
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.REQUEST_INSTALL_PACKAGES
            ),
            PERMISSION_REQUEST_CODE
        )

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            CommonFun.filesPermission(this)


        // frame с окном Что Нового после обновления
        if (CommonFun.readPrefValue(this, CommonFun.VERSION) != BuildConfig.VERSION_NAME) {
            CommonFun.savePrefValue(this, CommonFun.VERSION, BuildConfig.VERSION_NAME)
            binding.shadow.visibility = View.VISIBLE
            binding.frameWhatsnew.visibility = View.VISIBLE
            binding.whatsnew.text = CommonFun.whatIsNew()
        }

        binding.btnFrclose.setOnClickListener {
            binding.shadow.visibility = View.GONE
            binding.frameWhatsnew.visibility = View.GONE
        }

        // вытягиваемое меню
        binding.mainLayout.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL)
        setupMenu()

        binding.options.setOnClickListener {
            val anim = ScaleAnimation(0F, 1F, 0F, 1F)
            anim.fillBefore = true
            anim.fillAfter = true
            anim.isFillEnabled = true
            anim.duration = 300
            anim.interpolator = OvershootInterpolator()
            binding.options.startAnimation(anim)

            binding.mainLayout.toggleMenu()
        }

        // кнопки функций экрана
        binding.btnInfokiosk.setOnClickListener {
            val intent = Intent(this, InfoKiosk::class.java)
            startActivity(intent)
        }

        binding.btnCheckkm.setOnClickListener {
            val intent = Intent(this, CheckDataMatrix::class.java)
            startActivity(intent)
        }

        binding.btnCheckprice.setOnClickListener {
            val intent = Intent(this, CheckPrice::class.java)
            startActivity(intent)
        }

        binding.btnInfodk.setOnClickListener {
            Toast.makeText(this,"Функционал в разработке",Toast.LENGTH_SHORT).show()
            return@setOnClickListener

            val intent = Intent(this, InfoDk::class.java)
            startActivity(intent)
        }

        binding.btnFotorep.setOnClickListener {
            val intent = Intent(this, RepFoto::class.java)
            startActivity(intent)
        }
    }

    private fun setupMenu() {
        val fm = supportFragmentManager
        var menuFragment = fm.findFragmentById(R.id.mainMenuFrame) as MenuListFragment?
        if (menuFragment == null) {
            menuFragment = MenuListFragment()
            fm.beginTransaction().add(R.id.mainMenuFrame, menuFragment).commit()
        }
    }


    /**
     * Обработка полученных Интентов. Обновление программы
     */
    private val updReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.getBooleanExtra("updatetask", false)) {
                if (!intent.getStringExtra("newvers").isNullOrBlank()) {
                    update(intent.getStringExtra("newvers")!!, intent.getStringExtra("upderror"))
                }
            }
        }
    }

    private fun update(newvers: String, error: String?) {
        if (error.isNullOrEmpty())
            AlertDialog.Builder(this, R.style.AlertDialogCustomR)
                .setTitle("Есть новые обновления")
                .setMessage("Вы хотите обновить приложение сейчас?")
                .setPositiveButton("Обновить") { dialog, _ ->
                    CommonFun.update(this, newvers)
                }
                .setNegativeButton("Позже") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        else
            Toast.makeText(
                this,
                error,
                Toast.LENGTH_LONG
            ).show()
    }


    /**
     * выход из приложения
     */
    private fun exit() {
        val aInfo = AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustomR)
        aInfo.setTitle("Подтвердить выход?")
            .setNegativeButton(
                "Нет"
            ) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(
                "Да"
            ) { _: DialogInterface, _: Int ->
                stopService(Intent(this@MainActivity, UpdateTask::class.java))
                moveTaskToBack(true)
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(1)
            }
        aInfo.create()
        aInfo.show()
    }


    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onPause() {
        // отменить регистрацию, если активность не видна
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(updReceiver)
        super.onPause()
    }


    override fun onResume() {
        super.onResume()
        if (!isMyServiceRunning(UpdateTask::class.java)) {
            startService(Intent(this, UpdateTask::class.java))
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                updReceiver,
                IntentFilter("Update")
            )

    }

    /**
     * перенезначаем кнопку назад
     */
    override fun onBackPressed() {
        exit()
    }

    /**
     * разрешение на установку обновлений
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1007 && resultCode != RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    startActivityForResult(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                            Uri.parse(String.format("package:%s", packageName))
                        ), 1007
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                if (Settings.Secure.getInt(
                        contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS
                    ) != 1
                ) {
                    Toast.makeText(this, "Включите неизвестные источники", Toast.LENGTH_LONG).show()
                    startActivityForResult(Intent(Settings.ACTION_SECURITY_SETTINGS), 1007)
                }
            }
        }
    }
}