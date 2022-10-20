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
import com.wz.tex.Device
import com.wz.tex.R
import com.wz.tex.bean.Point
import android.graphics.Point as SPoint
import com.wz.tex.bean.PointersState
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
                ServerReceive(BufferedReader(InputStreamReader(sSocket.getInputStream()))).start()
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
                Log.i("TAG_TIME", "before send: ${System.currentTimeMillis()}")
                if (::sSocket.isInitialized) {
                    pw?.println(strToSend)
                    pw?.flush()
                }
//                mUdpSocket?.send(datagramPacketToSend)
                Log.i("TAG_TIME", "after send: ${System.currentTimeMillis()}")
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
        private var pointerCount = 0
        private var lastTouchDown = 0L
        private val pointersState: PointersState =
            PointersState()
        private val pointerProperties = arrayOfNulls<PointerProperties>(PointersState.MAX_POINTERS)
        private val pointerCoords = arrayOfNulls<PointerCoords>(PointersState.MAX_POINTERS)
        private val device by lazy { Device(null) }

        init {
            for (i in 0 until PointersState.MAX_POINTERS) {
                val props = PointerProperties()
                props.toolType = MotionEvent.TOOL_TYPE_FINGER
                val coords = PointerCoords()
                coords.orientation = 0f
                coords.size = 0f
                pointerProperties[i] = props
                pointerCoords[i] = coords
            }
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val now = SystemClock.uptimeMillis()
            var action1 = event.action
            val point = Point(event.x.toInt(), event.y.toInt())
            val pointerIndex = pointersState.getPointerIndex(-2)
            val pointer = pointersState[pointerIndex]
            pointer.point = point
            pointer.pressure = if (event.action == MotionEvent.ACTION_UP)  0 else 1f
            pointer.isUp = action == MotionEvent.ACTION_UP

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
                } else if (action == MotionEvent.ACTION_DOWN) {
                    action1 =
                        MotionEvent.ACTION_POINTER_DOWN or (pointerIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
            }
            val event = MotionEvent
                .obtain(
                    lastTouchDown,
                    now,
                    action1,
                    pointerCount,
                    pointerProperties,
                    pointerCoords,
                    0,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                    InputDevice.SOURCE_TOUCHSCREEN,
                    0
                )
            device.injectEvent(event, Device.INJECT_MODE_ASYNC);
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    x = event.rawX.toInt()
//                    y = event.rawY.toInt()
//                    firstX = event.rawX.toInt()
//                    firstY = event.rawY.toInt()
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val nowx = event.rawX.toInt()
//                    val nowy = event.rawY.toInt()
//                    val movedx = nowx - x
//                    val movedy = nowy - y
//                    x = nowx
//                    y = nowy
//
//                    lp.x = lp.x - movedx
//                    lp.y = lp.y - movedy
//                    // 更新悬浮窗控件布局
////                    windowManager.updateViewLayout(view, lp)
//                }
//                MotionEvent.ACTION_UP -> {
////                    Log.i(TAG, "onTouch: ACTION_UP ${event.getPointerId(event.actionIndex)}")
////                    val minSlop = 3
////                    if (abs(event.rawX.toInt() - firstX) < minSlop && abs(event.rawY.toInt() - firstY) < minSlop) {
////                        Log.i("TAG_TIME", "onTouch: ${System.currentTimeMillis()}")
////                        sendToServer("input tap 500 1500")
////                        view.performClick()
////                    }
//                }
//            }
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

    class ServerReceive(private val br: BufferedReader) : Thread() {
        override fun run() {
            while (!isInterrupted) {
                var str: String
                try {
                    str = br.readLine()
                    println("接受 C 的信息:$str")
                } catch (e: Exception) {
                    e.printStackTrace()
                    interrupt()
                }
            }
        }
    }

    //MotionEvent { action=ACTION_DOWN, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=2493072910, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=216516680 }
    //MotionEvent { action=ACTION_MOVE, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=1, eventTime=2493072920, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=849214663 }
    //MotionEvent { action=ACTION_UP, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=2493072922, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=155553049 }

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