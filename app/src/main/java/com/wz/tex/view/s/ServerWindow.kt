package com.wz.tex.view.s

import android.R.attr.action
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import com.google.gson.Gson
import com.wz.tex.Device
import com.wz.tex.R
import com.wz.tex.bean.Msg
import com.wz.tex.bean.Point
import android.graphics.Point as SPoint
import com.wz.tex.bean.PointersState
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
//    var mInetAddress: InetAddress? = null
    var mPort = 55555
//    var mUdpSocket: DatagramSocket? = null
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
//        try {
//            mInetAddress = InetAddress.getByName(ip)
//            val myAddress = InetAddress.getByName(BitmapUtils.getIntranetIPAddress(this))
//            mUdpSocket = DatagramSocket(55554, myAddress)
//            ReceiveS(mUdpSocket!!, this).start()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
        MainScope().launch(Dispatchers.IO) {
        }
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
//        val datagramPacketToSend = DatagramPacket(
//            strToSend.toByteArray(),
//            strToSend.toByteArray().size,
//            mInetAddress,
//            mPort
//        )
        MainScope().launch(Dispatchers.IO) {
            try {
                if (::sSocket.isInitialized) {
                    pw?.println(strToSend)
                    pw?.flush()
                }
                Log.i("TAG_TIME", "send-sent: $strToSend}")
//                mUdpSocket?.send(datagramPacketToSend)
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

        view.setOnTouchListener(FloatingOnTouchListener(windowManager, lp))
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

    inner class FloatingOnTouchListener(
        private val windowManager: WindowManager,
        private val lp: WindowManager.LayoutParams
    ) : View.OnTouchListener {
//        private var x = 0
//        private var y = 0
//        private var firstX = 0
//        private var firstY = 0
        private val TAG = "ServerWindow"

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            sendToServer(gson.toJson(Msg.generateControlMsg(event)))
            return false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (::view.isInitialized) windowManager.removeViewImmediate(view)
//        mUdpSocket?.close()
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
                    if (string.contains(SocketEvents.ACTION_HELLO)) {
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

    class ReceiveS(private val ds: DatagramSocket, serverWindow: ServerWindow) : Thread() {

        private val buf = ByteArray(1024)
        private val ref: WeakReference<ServerWindow> = WeakReference(serverWindow)

        override fun run() {
            while (true) {
                buf.fill(0, 0)
                val receivedPacket = DatagramPacket(buf, buf.size)
                try {
                    //通过socket将接收内容置入 接收包 中
                    if (ds.isClosed) return
                    ds.receive(receivedPacket)
                    val string = String(receivedPacket.data, 0, receivedPacket.length)
                    println("接收内容：$string")
                    if (string.startsWith("answer")) {
                        ref.get()?.callback?.invoke(3, string)
                    }
                } catch (e: SocketException) {
                    ref.get()?.stopSelf()
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}