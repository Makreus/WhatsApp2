package com.example.whatsappkayit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var durumText: TextView
    private lateinit var kayitButton: Button

    private val REQ_PERMS = 1
    private val REQ_PROJECTION = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        val baslik = TextView(this).apply {
            text = "WhatsApp Görüntülü Arama Kaydı"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val ipucu = TextView(this).apply {
            text = "Önce kaydı başlat, sonra WhatsApp'a geç ve aramayı hoparlörde yap. " +
                "Kayıt Filmler/WhatsAppKayit klasörüne düşer. " +
                "Karşı tarafa ve (ders ise) hocaya haber vermeyi unutma."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
        }
        durumText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, pad)
        }
        kayitButton = Button(this).apply {
            setOnClickListener { onKayitButonu() }
        }

        kok.addView(baslik)
        kok.addView(ipucu)
        kok.addView(durumText)
        kok.addView(kayitButton)
        setContentView(kok)

        RecorderService.onDurumDegisti = { runOnUiThread { arayuzuGuncelle() } }
        arayuzuGuncelle()
    }

    override fun onResume() {
        super.onResume()
        arayuzuGuncelle()
    }

    override fun onDestroy() {
        RecorderService.onDurumDegisti = null
        super.onDestroy()
    }

    private fun arayuzuGuncelle() {
        if (RecorderService.kayitVar) {
            durumText.text = "● Kayıt yapılıyor"
            kayitButton.text = "Kaydı durdur"
        } else {
            durumText.text = "Hazır"
            kayitButton.text = "Kaydı başlat"
        }
    }

    private fun onKayitButonu() {
        if (RecorderService.kayitVar) {
            startService(Intent(this, RecorderService::class.java).setAction(RecorderService.ACTION_STOP))
        } else {
            izinleriIste()
        }
    }

    private fun izinleriIste() {
        val gerekli = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) gerekli.add(Manifest.permission.POST_NOTIFICATIONS)
        val eksik = gerekli.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (eksik.isEmpty()) ekranIzniIste() else requestPermissions(eksik.toTypedArray(), REQ_PERMS)
    }

    @Deprecated("Basit tutmak için klasik API")
    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code != REQ_PERMS) return
        val mikrofonVar = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (mikrofonVar) {
            ekranIzniIste()
        } else {
            Toast.makeText(this, "Mikrofon izni olmadan ses kaydedilemez", Toast.LENGTH_LONG).show()
        }
    }

    private fun ekranIzniIste() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJECTION)
    }

    @Deprecated("Basit tutmak için klasik API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PROJECTION) return
        if (resultCode == RESULT_OK && data != null) {
            val i = Intent(this, RecorderService::class.java)
                .setAction(RecorderService.ACTION_START)
                .putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(RecorderService.EXTRA_DATA, data)
            startForegroundService(i)
        } else {
            Toast.makeText(this, "Ekran kaydı izni verilmedi", Toast.LENGTH_SHORT).show()
        }
    }
}
