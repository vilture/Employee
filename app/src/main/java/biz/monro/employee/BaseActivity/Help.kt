package biz.monro.employee.BaseActivity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import biz.monro.employee.databinding.HelpBinding

class Help : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = HelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pdfView.fromAsset("инструкция.pdf").load()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}