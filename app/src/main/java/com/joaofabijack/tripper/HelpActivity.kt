package com.joaofabijack.tripper

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Button

class HelpActivity : TripperActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        }
        setContentView(R.layout.activity_help)
        findViewById<View>(R.id.help_root).setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViewById<View>(R.id.help_root).requestApplyInsets()
        findViewById<Button>(R.id.help_back).setOnClickListener { finish() }
    }
}
