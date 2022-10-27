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
import com.wz.base.NetUtil
import com.wz.tex.BitmapUtils
import com.wz.tex.SharedPreferencesHelper
import com.wz.tex.databinding.LayoutClientBinding
import com.wz.tex.view.ConfigSelectDialog
import com.wz.tex.view.ConfigViewActivity
import com.wz.tex.view.ConfigViewActivity.Companion.FROM_CLIENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ClientActivity : AppCompatActivity() {

    private var floatingBind: ClientWindow.FloatingBind? = null
    private val binding by lazy { LayoutClientBinding.inflate(layoutInflater) }
    private val mPort = 55555
    private var currentFileName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        currentFileName = SharedPreferencesHelper.getInstance().getString("lastClientConfig", NetUtil.DEFAULT_CONFIG_NAME)
        binding.myIp.text = BitmapUtils.getIntranetIPAddress(this)
        binding.etPort.setText(mPort.toString())
        binding.refreshIP.setOnClickListener {
            binding.myIp.text = BitmapUtils.getIntranetIPAddress(this)
        }
        binding.shell.setOnClickListener {
            MainScope().launch(Dispatchers.IO) {
                execShell(run2)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ClientActivity, "停止", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.shellStop.setOnClickListener {
            process?.destroy()
        }
        binding.startPreview.setOnClickListener {
            startServer()
        }
        binding.closePreview.setOnClickListener {
            stopService()
        }
        binding.chooseLayout.setOnClickListener {
            ConfigSelectDialog.newInstance(FROM_CLIENT, currentFileName) { fileName, isEdit ->
                if (isEdit) {
                    startActivity(Intent(this, ConfigViewActivity::class.java).apply {
                        putExtra("from", FROM_CLIENT)
                        putExtra("fileName", fileName)
                    })
                } else {
                    SharedPreferencesHelper.getInstance().put("lastClientConfig", fileName)
                    currentFileName = fileName
                }
            }.show(supportFragmentManager)
        }
        if (BitmapUtils.saveAssetsToSDCard(this, "target")) {
            Toast.makeText(this, "检测服务状态成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show()
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

    private val run by lazy { "CLASSPATH=${application.filesDir.path}/target app_process ${application.filesDir.path} com.wz.target.Client" }
    private val run2 by lazy { arrayOf("/system/bin/sh", "-c", run) }

    private var process: Process? = null

    private fun execShell(cmd: Array<String>): String {
        var result = ""
        try {
            process = Runtime.getRuntime().exec(cmd)
            val mReader = InputStreamReader(process?.inputStream)
            result = mReader.readText()
            mReader.close()
            process?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
            println("YM========执行Linux命令异常==========${e.message}")
        }
        return result
    }

    fun sendCmd(cmd: String){
        var process: Process? = null
        try {
            val p = ProcessBuilder("/system/bin/sh", "-c", cmd)
            p.redirectErrorStream(true)
            process = p.start()
            process.waitFor()
            toStings(process?.inputStream)
        } catch ( e: InterruptedException) {
            e.printStackTrace();
        } catch ( e: IOException) {
            e.printStackTrace()
        } finally {
            process?.destroy()
        }
    }

    private fun toStings(inputStream: InputStream?){
        val stringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var firstLine = true
        var line: String? = null
        while (bufferedReader.readLine().also { line = it } != null) {
            if (!firstLine) {
                stringBuilder.append(System.getProperty("line.separator"))
            } else {
                firstLine = false
            }
            stringBuilder.append(line)
        }
        Log.e("YM","result:$stringBuilder")
    }
}