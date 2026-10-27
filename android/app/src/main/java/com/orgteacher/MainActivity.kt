package com.orgteacher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad)
            layoutDirection = View.LAYOUT_DIRECTION_RTL; setBackgroundColor(0xFF0F1217.toInt())
        }
        fun tv(t: String, size: Float = 15f, color: Int = Color.WHITE) =
            TextView(this).apply { text = t; textSize = size; setTextColor(color); setPadding(0, pad / 3, 0, pad / 3) }

        root.addView(tv("مدرّس الأورج", 26f, 0xFFD8B36A.toInt()))
        root.addView(tv("بيرسم التعليمات فوق تطبيق الأورج الحقيقي، وبينوّر الزرار المطلوب. لو ضغطت غلط بيصحّحلك ومبيبعتش الضغطة للأورج."))
        root.addView(tv("خطوات التشغيل", 18f, 0xFFD8B36A.toInt()))
        root.addView(tv("1) دوس «تفعيل الخدمة» وفعّل «مدرّس الأورج» من إمكانية الوصول.\n2) افتح تطبيق الأورج (وضع أفقي).\n3) هتلاقي لوحة الدرس فوقه. دوس «معايرة» مرة واحدة عشان الأماكن تتظبط على شاشتك.\n4) اتّبع الخطوات."))
        status = tv("")
        root.addView(status)
        root.addView(Button(this).apply {
            text = "تفعيل الخدمة"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "إيقاف المدرّس"
            setOnClickListener { TutorService.instance?.disableSelf(); refresh() }
        })
        root.addView(tv("مفتاح Claude API (اختياري، للردود الذكية)", 14f, 0xFF9AA4B2.toInt()))
        val key = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE); hint = "sk-ant-..."; setHintTextColor(0xFF5D6673.toInt())
            setText(getSharedPreferences("cfg", MODE_PRIVATE).getString("apikey", ""))
        }
        root.addView(key)
        root.addView(Button(this).apply {
            text = "حفظ المفتاح"
            setOnClickListener {
                getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("apikey", key.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "اتحفظ", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(ScrollView(this).apply { setBackgroundColor(0xFF0F1217.toInt()); addView(root) })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val on = TutorService.instance != null
        status.text = if (on) "الحالة: الخدمة شغالة" else "الحالة: الخدمة مش مفعّلة"
        status.setTextColor(if (on) 0xFF55D6A2.toInt() else 0xFFFF7A66.toInt())
    }
}
