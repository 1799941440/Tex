package com.wz.tex.base

import android.app.Application
import com.wz.base.NetUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        NetUtil.LOCATE = filesDir.path
    }

    companion object {
        lateinit var instance: App
    }
}