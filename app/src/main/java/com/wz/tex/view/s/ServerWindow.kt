package com.wz.tex.view.s

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.wz.base.Msg
import com.wz.base.NetUtil
import com.wz.base.SkillLayoutConfig
import com.wz.tex.R
import com.wz.tex.view.SocketEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.*
import kotlin.concurrent.thread

class ServerWindow : Service() {

    private val viewList = mutableListOf<View>()
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private var mPort = 55555
    private lateinit var sSocket: Socket
    private val gson by lazy { Gson() }
    private var pw: PrintWriter? = null
    private var configOrientation = -999

    override fun onBind(intent: Intent?): IBinder? {
        val ip = intent?.getStringExtra("ip")
        mPort = intent?.getIntExtra("port",  0) ?: 0
        if (ip?.isEmpty() == true && mPort == 0) {
            stopSelf()
            return null
        } else {
            showFloatWindow(intent)
            initUdpClient(ip!!)
            return FloatingBind(this)
        }
    }

    private fun initUdpClient(ip: String) {
        thread {
            try {
                sSocket = Socket(ip, mPort)
                pw = PrintWriter(sSocket.getOutputStream())
                ServerReceive(BufferedReader(InputStreamReader(sSocket.getInputStream())), this).start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendToServer(strToSend: String?) {
        MainScope().launch(Dispatchers.IO) {
            try {
                if (::sSocket.isInitialized) {
                    pw?.println(strToSend)
                    pw?.flush()
                }
                Log.i("TAG_TIME", "send-sent: $strToSend")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showFloatWindow(intent: Intent?) {
        clearWindow()
        val fileName = intent?.getStringExtra("fileName") ?: NetUtil.DEFAULT_CONFIG_NAME
        val file = File(NetUtil.getDefaultControlLayoutDir(), fileName)
        if (!file.exists()) {
            file.createNewFile()
            NetUtil.writeDefault(file)
        }
        val readFileByLines = NetUtil.readFileByLines(file)
        if (readFileByLines.size > 0) {
            readFileByLines.forEach {
                try {
                    viewList.add(addButton(gson.fromJson(it, SkillLayoutConfig::class.java)))
                } catch (e: Exception) {
                    Toast.makeText(this, "配置文件解析失败", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            addTips()
        }
    }

    private var tipView: View? = null
    private fun addTips() {
        tipView = View.inflate(this, R.layout.window_float_server_tips, null)
        val show = configOrientation == resources.configuration.orientation
        if (!show) Toast.makeText(this, "当前配置方向与屏幕方向不一致，配置按钮不做展示", Toast.LENGTH_SHORT).show()
        windowManager.addView(tipView, generateLP(-2, -2, 0, 0))
    }

    private fun addButton(info: SkillLayoutConfig): View {
        if (configOrientation == -999) configOrientation = info.orientation
        val inflate = View.inflate(this, R.layout.window_float_server, null)
        val buttonIndex = info.index.toString()
        inflate.tag = buttonIndex
        if (inflate is TextView) inflate.text = buttonIndex
        val width = ((info.width / 9) * 10).toInt()
        val height = ((info.height / 9) * 10).toInt()
        val marginStart = ((info.offsetX / 9) * 10).toInt()
        val topMargin = ((info.offsetY / 9) * 10).toInt()
        inflate.visibility = if (configOrientation == resources.configuration.orientation) View.VISIBLE else View.GONE
        windowManager.addView(inflate, generateLP(width, height, marginStart, topMargin))
        return inflate
    }

    private fun generateLP(w: Int, h: Int, x: Int, y: Int): WindowManager.LayoutParams {
        val lp: WindowManager.LayoutParams = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            PixelFormat.RGBA_8888
        )
        lp.x = x
        lp.y = y
        lp.gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return lp
    }

    private fun clearWindow() {
        viewList.forEach {
            try {
                windowManager.removeViewImmediate(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewList.clear()
        tipView?.let {
            windowManager.removeViewImmediate(it)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewList.forEach {
            it.visibility = if (newConfig.orientation == configOrientation) View.VISIBLE else View.GONE
        }
    }

    class FloatingBind(temp: ServerWindow) : Binder() {

        val service: WeakReference<ServerWindow>
        init {
            service = WeakReference(temp)
        }

        fun setData(data: String) {
            service.get()?.upData(data)
        }

        fun sayHello() {
            service.get()?.upData(service.get()?.gson?.toJson(Msg.generateHello()))
        }
    }

    private fun upData(data: String?) {
        sendToServer(data)
        callback?.invoke(2, "")
    }

    private var callback: ((Int, String) -> Unit)? = null
    fun setCallback(call: ((Int, String) -> Unit)?) {
        callback = call
        callback?.invoke(1, "启动成功")
    }

    inner class FloatingOnTouchListener(private val tag: Int) : View.OnTouchListener {

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            sendToServer(gson.toJson(Msg.generateControlMsg(event, tag)))
            return false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clearWindow()
        callback = null
        if (::sSocket.isInitialized) sSocket.close()
        return super.onUnbind(intent)
    }

    class ServerReceive(private val br: BufferedReader, serverWindow: ServerWindow) : Thread() {

        private val ref: WeakReference<ServerWindow> = WeakReference(serverWindow)

        override fun run() {
            while (!isInterrupted) {
                try {
                    val string = br.readLine()
                    if (string.contains(SocketEvents.ACTION_ANSWER)) {
                        ref.get()?.callback?.invoke(3, string)
                    }
                    println("接受 C 的信息:$string")
                } catch (e: Exception) {
                    e.printStackTrace()
                    interrupt()
                }
            }
        }
    }
}