package com.lianghao.myapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen: SplashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 条件为 true 时系统 Splash 一直显示，直到进入 Dashboard
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            isReady = true
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }, SPLASH_DURATION_MS)
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1200L
    }
}
