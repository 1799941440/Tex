package com.wz.tex.view.s

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import com.google.gson.Gson
import com.wz.base.Msg
import com.wz.tex.R
import android.graphics.Point as SPoint
import com.wz.tex.view.SocketEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.*
import kotlin.concurrent.thread

class ServerWindow : Service() {

    private lateinit var view: View
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    var mPort = 55555
    private lateinit var sSocket: Socket
    private val gson by lazy { Gson() }
    private var pw: PrintWriter? = null

    override fun onBind(intent: Intent?): IBinder? {
        val ip = intent?.getStringExtra("ip")
        mPort = intent?.getIntExtra("port",  0) ?: 0
        if (ip?.isEmpty() == true && mPort == 0) {
            stopSelf()
            return null
        } else {
            showFloatWindow()
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

    private fun sendToServer(strToSend: String) {
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

    private fun showFloatWindow() {
        if (::view.isInitialized) {
            return
        }
        view = View.inflate(this, R.layout.window_float, null)
        val lp = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            lp.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        lp.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        lp.format = PixelFormat.RGBA_8888
        lp.gravity = Gravity.BOTTOM or Gravity.END
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        val outSize = SPoint()
        windowManager.defaultDisplay.getRealSize(outSize)
        lp.y = (outSize.y * 0.5).toInt()
        windowManager.addView(view, lp)

        view.setOnTouchListener(FloatingOnTouchListener())
    }

    inner class FloatingBind(temp: ServerWindow) : Binder() {

        val service: WeakReference<ServerWindow>
        init {
            service = WeakReference(temp)
        }

        fun setData(data: String) {
            service.get()?.upData(data)
        }

        fun sayHello() {
            service.get()?.upData(gson.toJson(Msg.generateHello()))
        }
    }

    private fun upData(data: String) {
        sendToServer(data)
        callback?.invoke(2, "")
    }

    private var callback: ((Int, String) -> Unit)? = null
    fun setCallback(call: ((Int, String) -> Unit)?) {
        callback = call
        callback?.invoke(1, "启动成功")
    }

    inner class FloatingOnTouchListener() : View.OnTouchListener {

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            sendToServer(gson.toJson(Msg.generateControlMsg(event)))
            return false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (::view.isInitialized) windowManager.removeViewImmediate(view)
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