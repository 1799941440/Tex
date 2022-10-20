package com.wz.tex.view.s

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wz.tex.databinding.LayoutServerBinding

class ServerActivity : AppCompatActivity() {

    private val binding by lazy { LayoutServerBinding.inflate(layoutInflater) }
    private var floatingBind: ServerWindow.FloatingBind? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.targetConfig.setOnClickListener {
            floatingBind?.setData("ask from k40") ?: Toast.makeText(this, "服务未启动", Toast.LENGTH_SHORT).show()
        }
        binding.startServer.setOnClickListener {
            startServer()
        }
        binding.stopServer.setOnClickListener {
            stopService()
        }
    }

    private fun startServer() {
        val etIp = binding.etIp.text.toString()
        val etPort = binding.etPort.text.toString()
        if (etIp.isEmpty()) {
            Toast.makeText(this, "ip不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (etPort.isEmpty()) {
            Toast.makeText(this, "port不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (isAllowAlertWindow()) {
                bindService(Intent(this, ServerWindow::class.java).apply {
                    putExtra("ip", etIp)
                    putExtra("port", etPort.toInt())
                }, conn, BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            floatingBind = service as ServerWindow.FloatingBind
            val service1 = floatingBind?.service?.get()
            service1?.setCallback { code, msg ->
                runOnUiThread {
                    if (code == 1) {
                        binding.myState.text = "本机状态：开启"
                    } else if (code == 2) {
                        binding.myState.text = "本机状态：开启并发送"
                        Toast.makeText(this@ServerActivity, msg, Toast.LENGTH_SHORT).show()
                    } else if (code == 3) {
                        binding.myState.text = "本机状态：开启并发送且回应 $msg"
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}