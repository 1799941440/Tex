package com.wz.tex.view.c

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wz.tex.BitmapUtils
import com.wz.tex.databinding.LayoutClientBinding

class ClientActivity : AppCompatActivity() {

    private var floatingBind: ClientWindow.FloatingBind? = null
    private val binding by lazy { LayoutClientBinding.inflate(layoutInflater) }
    private val mPort = 55555

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.myIp.text = BitmapUtils.getIntranetIPAddress(this)
        binding.etPort.setText(mPort.toString())
        binding.refreshIP.setOnClickListener {
            binding.myIp.text = BitmapUtils.getIntranetIPAddress(this)
        }
        binding.myConfig.setOnClickListener {
            startServer()
        }
        binding.closeClient.setOnClickListener {
            stopService()
        }
    }

    private var conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            floatingBind = service as ClientWindow.FloatingBind
            val service1 = floatingBind?.service?.get()
            service1?.setCallback { code, msg ->
                runOnUiThread {
                    Log.i(TAG, "onServiceConnected: from service: $msg")
                    if (code == 1) {
                        binding.myState.text = "本机状态：$msg"
                    } else if (code == 2) {
                        binding.myState.text = "本机状态：开启并连通"
                        Toast.makeText(this@ClientActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "onServiceDisconnected:$name ")
        }
    }

    private val TAG = "ClientActivity"

    private fun sendData(count: String) {
        floatingBind?.setData("activity返回数据${count}")
    }

    private fun isAllowAlertWindow(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            intent.data = Uri.fromParts("package", this.packageName, null)
            startActivity(intent)
        }
        return Settings.canDrawOverlays(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }

    private fun stopService() {
        try {
            unbindService(conn)
            floatingBind = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startServer() {
        try {
            if (isAllowAlertWindow()) {
                bindService(Intent(this, ClientWindow::class.java).apply {
                    putExtra("port", mPort)
                }, conn, BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}