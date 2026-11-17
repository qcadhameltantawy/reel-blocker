package com.reelsblocker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            setBackgroundColor(Color.WHITE)
        }
        val text = TextView(this).apply {
            text = "Reels Blocker\n\n1) اضغط الزرار تحت\n2) افتح Installed apps / Downloaded apps\n3) اختار Reels Blocker وفعّله\n\nبعدها افتح انستجرام وجرب."
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        val btn = Button(this).apply {
            text = "فتح إعدادات Accessibility"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        layout.addView(text)
        layout.addView(btn)
        setContentView(layout)
    }
}
