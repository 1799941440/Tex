package com.wz.tex.view.c

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point as SPoint
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.view.MotionEvent.ACTION_CANCEL
import com.google.gson.Gson
import com.wz.tex.BitmapUtils
import com.wz.tex.Device
import com.wz.tex.R
import com.wz.tex.bean.Msg
import com.wz.tex.bean.Point
import com.wz.tex.bean.PointersState
import com.wz.tex.view.SocketEvents.ACTION_EVENT
import com.wz.tex.view.SocketEvents.ACTION_HELLO
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.*
import kotlin.concurrent.thread
import kotlin.math.abs

class ClientWindow : Service() {

    private var callback: ((Int, String) -> Unit)? = null
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
//    private lateinit var udpSocketServer: DatagramSocket
    private lateinit var jSocket: Socket
    private lateinit var server: ServerSocket
    private lateinit var view: View
    private var pw: PrintWriter? = null
    private val gson by lazy { Gson() }

    private var remoteAddress: InetAddress? = null
    private var remotePort: Int? = null

    override fun onBind(intent: Intent): IBinder {
        showFloatWindow()
        val port = intent.getIntExtra("port", 55555)
//        udpSocketServer = DatagramSocket(port, InetAddress.getByName(BitmapUtils.getIntranetIPAddress(this)))
//        ReceiveC(udpSocketServer, this).start()
        MainScope().launch(Dispatchers.IO) {
        }
        thread {
            try {
                server = ServerSocket(port, 50, InetAddress.getByName(
                    BitmapUtils.getIntranetIPAddress(
                        this@ClientWindow
                    )
                ))
                callback?.invoke(1, "启动成功,等待连接")
                server.setPerformancePreferences(-1, 10, -1)
                jSocket = server.accept()
                callback?.invoke(1, "启动成功,连接成功")
                pw = PrintWriter(jSocket.getOutputStream())
                ClientReceive(BufferedReader(InputStreamReader(jSocket.getInputStream())), this).start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return FloatingBind(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
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
        view.setOnClickListener {

        }
    }

    inner class FloatingBind(temp: ClientWindow) : Binder() {

        val service: WeakReference<ClientWindow>
        init {
            service = WeakReference(temp)
        }

        fun setData(data: String) {
            service.get()?.upData(data)
        }
    }

    private fun sendAnswer(ask: String?) {
        sendToServer(gson.toJson(Msg.generateAnswer(ask)))
    }

    private fun sendToServer(strToSend: String) {
//        if (remotePort == null || remotePort == 0 || remoteAddress == null) {
//            return
//        }
//        val datagramPacketToSend = DatagramPacket(
//            strToSend.toByteArray(),
//            strToSend.toByteArray().size,
//            remoteAddress,
//            remotePort ?:0
//        )
        MainScope().launch(Dispatchers.IO) {
            try {
                pw?.println(strToSend)
                pw?.flush()
//                udpSocketServer.send(datagramPacketToSend)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
//        udpSocketServer.close()
        if (::jSocket.isInitialized) jSocket.close()
        if (::server.isInitialized) server.close()
        callback = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: ")
    }

    inner class FloatingOnTouchListener(
        private val windowManager: WindowManager,
        private val lp: WindowManager.LayoutParams
    ) : View.OnTouchListener {
        private var x = 0
        private var y = 0
        private var firstX = 0
        private var firstY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(TAG, "onTouch: ACTION_DOWN ${event.getPointerId(event.actionIndex)}")
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                    firstX = event.rawX.toInt()
                    firstY = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowx = event.rawX.toInt()
                    val nowy = event.rawY.toInt()
                    val movedx = nowx - x
                    val movedy = nowy - y
                    x = nowx
                    y = nowy

                    lp.x = lp.x - movedx
                    lp.y = lp.y - movedy
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(view, lp)
                }
                MotionEvent.ACTION_UP -> {
                    Log.i(TAG, "onTouch: ACTION_UP ${event.getPointerId(event.actionIndex)}")
                    val minSlop = 3
                    if (abs(event.rawX.toInt() - firstX) < minSlop && abs(event.rawY.toInt() - firstY) < minSlop) {
                        view.performClick()
                    }
                }
            }
            return false
        }
    }

    inner class ClientReceive(private val br: BufferedReader, clientWindow: ClientWindow) : Thread() {

        private val runtime by lazy { Runtime.getRuntime() }
        private val ref: WeakReference<ClientWindow> = WeakReference(clientWindow)
        private var pointerCount = 0
        private var lastTouchDown = 0L
        private val pointersState: PointersState =
            PointersState()
        private val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(PointersState.MAX_POINTERS)
        private val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(PointersState.MAX_POINTERS)
        private val device by lazy { Device(null) }

        init {
            for (i in 0 until PointersState.MAX_POINTERS) {
                val props = MotionEvent.PointerProperties()
                props.toolType = MotionEvent.TOOL_TYPE_FINGER
                val coords = MotionEvent.PointerCoords()
                coords.orientation = 0f
                coords.size = 0f
                pointerProperties[i] = props
                pointerCoords[i] = coords
            }
        }

        override fun run() {
            while (!isInterrupted) {
                try {
                    val string = br.readLine()
                    println("接收 S 的信息$string ${System.currentTimeMillis()}")
                    if (string.contains(ACTION_EVENT)) {
                        processEvent(string)
                    } else if (string.contains(ACTION_HELLO)) {
                        val fromJson = gson.fromJson(string, Msg::class.java)
                        ref.get()?.sendAnswer(fromJson.msg)
                        println(fromJson)
                    }

//                    ref.get()?.callback?.invoke(2, string)
//                    if (string.startsWith("input tap")) {
//                        runtime.exec(string)
//                        Log.i("TAG_TIME", "执行点击: ${System.currentTimeMillis()}")
//                    } else if (string.startsWith("ask")) {
//                        ref.get()?.sendAnswer(string)
//                    }
                } catch (s: SocketException) {
                    s.printStackTrace()
                    interrupt()
                }
            }
        }

        private fun processEvent(event: String) {
            val fromJson = gson.fromJson(event, Msg::class.java)
            val now = SystemClock.uptimeMillis()
            var action1 = fromJson.action ?: ACTION_CANCEL
            val point = Point(fromJson.x ?: 0, fromJson.y ?: 0)
            val pointerIndex = pointersState.getPointerIndex(-2)
            val pointer = pointersState[pointerIndex]
            pointer.point = point
            pointer.pressure = if (fromJson.action == MotionEvent.ACTION_UP)  0f else 1f
            pointer.isUp = android.R.attr.action == MotionEvent.ACTION_UP

            val pointerCount = pointersState.update(pointerProperties, pointerCoords)
            if (pointerCount == 1) {
                if (action1 == MotionEvent.ACTION_DOWN) {
                    lastTouchDown = now
                }
            } else {
                // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
                if (action1 == MotionEvent.ACTION_UP) {
                    action1 =
                        MotionEvent.ACTION_POINTER_UP or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                } else if (android.R.attr.action == MotionEvent.ACTION_DOWN) {
                    action1 =
                        MotionEvent.ACTION_POINTER_DOWN or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
            }
            println(generateEvent(now, action1, pointerCount))
//            device.injectEvent(generateEvent(now, action1, pointerCount), Device.INJECT_MODE_ASYNC)
        }

        private fun generateEvent(
            now: Long,
            action1: Int,
            pointerCount: Int
        ) = MotionEvent
            .obtain(
                lastTouchDown, now, action1, pointerCount, pointerProperties, pointerCoords, 0,
                0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
    }

    class ReceiveC(private val ds: DatagramSocket, clientWindow: ClientWindow) : Thread() {

        private val buf = ByteArray(1024)
        private val runtime by lazy { Runtime.getRuntime() }
        private val ref: WeakReference<ClientWindow> = WeakReference(clientWindow)

        override fun run() {
            while (!isInterrupted) {
                buf.fill(0, 0)
                val receivedPacket = DatagramPacket(buf, buf.size)
                try {
                    //通过socket将接收内容置入 接收包 中
                    if (ds.isClosed) interrupt()
                    ds.receive(receivedPacket)
                    ref.get()?.remoteAddress = receivedPacket.address
                    ref.get()?.remotePort = receivedPacket.port
                    val string = String(receivedPacket.data, 0, receivedPacket.length)
                    println("接收内容：$string")
                    Log.i("TAG_TIME", "接收内容: ${System.currentTimeMillis()}")
                    ref.get()?.callback?.invoke(2, "${receivedPacket.address}:${receivedPacket.port} = $string")
                    if (string.startsWith("input tap")) {
                        runtime.exec(string)
                        Log.i("TAG_TIME", "执行点击: ${System.currentTimeMillis()}")
                    } else if (string.startsWith("ask")) {
                        ref.get()?.sendAnswer(string)
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