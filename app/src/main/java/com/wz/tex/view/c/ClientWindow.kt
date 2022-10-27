package com.wz.tex.view.c

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.wz.base.NetUtil
import com.wz.base.SkillLayoutConfig
import com.wz.tex.R
import kotlinx.coroutines.*
import java.io.File
import java.lang.ref.WeakReference
import java.net.*

class ClientWindow : Service() {

    private var callback: ((Int, String) -> Unit)? = null
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val gson by lazy { Gson() }
    private lateinit var view: View

    override fun onBind(intent: Intent): IBinder {
        showFloatWindow(intent)
        return FloatingBind(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showFloatWindow(intent: Intent) {
        if (::view.isInitialized) {
            return
        }
        view = View.inflate(this, R.layout.window_float, null)
        val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.RGBA_8888
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val fileName = intent.getStringExtra("fileName") ?: NetUtil.DEFAULT_CONFIG_NAME
        val file = File(NetUtil.getDefaultClientLayoutDir(), fileName)
        if (!file.exists()) {
            file.createNewFile()
            NetUtil.writeDefault(file)
        }
        val readFileByLines = NetUtil.readFileByLines(file)
        if (readFileByLines.size > 0) {
            readFileByLines.forEach {
                try {
                    addButton(gson.fromJson(it, SkillLayoutConfig::class.java), view as ConstraintLayout)
                } catch (e: Exception) {
                    Toast.makeText(this, "配置文件解析失败", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
        windowManager.addView(view, layoutParams)
    }

    private var buttonIndex = 1

    private fun addButton(info: SkillLayoutConfig, view: ConstraintLayout) {
        val inflate = LayoutInflater.from(this).inflate(R.layout.item_button, view, false)
        inflate.tag = buttonIndex
        if (inflate is TextView) inflate.text = buttonIndex.toString()
        val layoutParams = inflate.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.startToStart = R.id.panel
        layoutParams.topToTop = R.id.panel
        layoutParams.width = ((info.width / 9) * 10).toInt()
        layoutParams.height = ((info.height / 9) * 10).toInt()
        layoutParams.marginStart = ((info.offsetX / 9) * 10).toInt()
        layoutParams.topMargin = ((info.offsetY / 9) * 10).toInt()
        inflate.layoutParams = layoutParams
        view.addView(inflate)
        buttonIndex++
    }

    class FloatingBind(temp: ClientWindow) : Binder() {

        val service: WeakReference<ClientWindow>
        init {
            service = WeakReference(temp)
        }

        fun setData(data: String) {
            service.get()?.upData(data)
        }
    }

    fun setCallback(call: ((Int, String) -> Unit)?) {
        callback = call
    }

    private val TAG = "RecorderWindow"
    fun upData(msg: String) {
        Log.i(TAG, "upData: 收到activity 数据:$msg")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind: ")
        if (::view.isInitialized) windowManager.removeViewImmediate(view)
        callback = null
        return super.onUnbind(intent)
    }

}

///dev/input/event4: 0003 0039 00000699 绝对点击 手指id 699
///dev/input/event4: 0001 014a 00000001 014a=touch  eventType = 1(down)
///dev/input/event4: 0001 0145 00000001 tool finger
///dev/input/event4: 0003 0035 000003be  x 坐标
///dev/input/event4: 0003 0036 00000268  y
///dev/input/event4: 0000 0000 00000000  同步信号量
///dev/input/event4: 0003 0039 ffffffff 释放手指？
///dev/input/event4: 0001 014a 00000000 014a=touch  eventType = 0(up)
///dev/input/event4: 0001 0145 00000000 tool finger up
///dev/input/event4: 0000 0000 00000000  同步信号量