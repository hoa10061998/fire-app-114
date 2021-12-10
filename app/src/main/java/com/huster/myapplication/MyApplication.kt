package com.huster.myapplication

import android.app.Application
import com.huster.myapplication.ui.personal.UserManager

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.init(applicationContext)
    }
}