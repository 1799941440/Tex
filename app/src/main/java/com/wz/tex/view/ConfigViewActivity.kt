package com.wz.tex.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.google.gson.Gson
import com.wz.base.NetUtil
import com.wz.base.SkillLayoutConfig
import com.wz.tex.BitmapUtils
import com.wz.tex.R
import com.wz.tex.databinding.LayoutConfigViewBinding
import java.io.*

class ConfigViewActivity : AppCompatActivity() {

    private val binding by lazy { LayoutConfigViewBinding.inflate(layoutInflater) }
    private val gson by lazy { Gson() }
    private val REQUEST_GET_IMAGE = 1
    private var from = FROM_CONTROL
    private var fileName: String? = ""
    private var name = "control.jpeg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContentView(binding.root)
        fileName = intent?.getStringExtra("fileName")
        if (fileName == null) {
            Toast.makeText(this, "传参fileName丢失", Toast.LENGTH_SHORT).show()
        }
        from = intent?.getIntExtra("from", FROM_CONTROL) ?: FROM_CONTROL
        if (from == FROM_CLIENT) name = "client.jpeg"
        loadTempBg()
        NetUtil.LOCATE = application.filesDir.path
        loadConfigFile()
        with(binding.panel) {
            post {
                val widthPixels = resources.displayMetrics.widthPixels
                val screenResolution = getScreenResolution()
                binding.etWidthHeight.setText("可用${widthPixels},最大${screenResolution}")
                layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
                    dimensionRatio = "$widthPixels:${resources.displayMetrics.heightPixels}"
                }
            }
        }
        binding.selectBg.setOnClickListener {
            selectImage()
        }
        binding.add.setOnClickListener {
            addButton(SkillLayoutConfig.getBlankShow())
        }
        binding.save.setOnClickListener {
            save()
        }
    }

    private val defaultLayoutDir by lazy {
        if (from == FROM_CONTROL)
            NetUtil.getDefaultControlLayoutDir()
        else
            NetUtil.getDefaultClientLayoutDir()
    }

    private fun loadConfigFile() {
        loadConfigFile(defaultLayoutDir, fileName!!)
    }

    private fun loadConfigFile(dirName: String, fileName: String) {
        binding.panel.removeAllViews()
        val dir = File(dirName)
        if (!dir.exists()) {
            val mkdirs = dir.mkdirs()
            if (!mkdirs) {
                throw Exception("无法创建目录")
            }
        }
        val file = File(dir, fileName)
        if (!file.exists()) {
            file.createNewFile()
            writeDefault(file)
        }
        val readFileByLines = NetUtil.readFileByLines(file)
        if (readFileByLines.size > 0) {
            readFileByLines.forEach {
                addButton(gson.fromJson(it, SkillLayoutConfig::class.java))
            }
        }
    }

    private fun writeDefault(file: File) {
        NetUtil.coverFile(file, NetUtil.DEFAULT_CONFIG_CONTENT)
    }

    private var index: Int = 1

    private fun addButton(info: SkillLayoutConfig) {
        val inflate = layoutInflater.inflate(R.layout.item_button, binding.panel, false)
        inflate.tag = index
        if (inflate is TextView) inflate.text = index.toString()
        val layoutParams = inflate.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.startToStart = R.id.panel
        layoutParams.topToTop = R.id.panel
        layoutParams.width = info.width.toInt()
        layoutParams.height = info.height.toInt()
        layoutParams.marginStart = info.offsetX.toInt()
        layoutParams.topMargin = info.offsetY.toInt()
        inflate.layoutParams = layoutParams
        binding.panel.addView(inflate)
        index++
    }

    private fun save() {
        if (binding.panel.childCount == 0) {
            Toast.makeText(this, "不能保存空布局", Toast.LENGTH_SHORT).show()
        }
        val list = mutableListOf<String>()
        binding.panel.children.forEach { view ->
            val layoutParams = view.layoutParams as? ConstraintLayout.LayoutParams
            layoutParams?.let {
                list.add(gson.toJson(SkillLayoutConfig.getBlankSave().apply {
                    index = view.tag as? Int ?: 0
                    offsetX = layoutParams.marginStart.toFloat()
                    offsetY = layoutParams.topMargin.toFloat()
                    width = view.measuredWidth.toFloat()
                    height = view.measuredHeight.toFloat()
                }))
            }
        }
        NetUtil.coverFile(File(defaultLayoutDir + NetUtil.DEFAULT_CONFIG_NAME), list)
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
    }

    private fun getScreenResolution(): Int { // 获取屏幕真实分辨率
        val outSize = Point()
        windowManager.defaultDisplay.getRealSize(outSize)
        return Math.max(outSize.x, outSize.y)
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Browser Image..."), REQUEST_GET_IMAGE)
    }

    @Throws
    private fun loadTempBg() {
        val value = filesDir.absolutePath + "/images/"
        val file = File(value)
        if (file.exists()) {
            val exists = File(value + name)
            if (exists.exists()) {
                val inputStream = exists.inputStream()
                val decodeStream = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                })
                binding.panel.background = BitmapDrawable(this@ConfigViewActivity.resources, decodeStream)
                inputStream.close()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            data.data?.let {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                    options.inJustDecodeBounds = false
                    val selectedBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it), null, options)
                    val name = if (from == FROM_CONTROL) "control.jpeg" else "client.jpeg"
                    BitmapUtils.saveBitmap(name, selectedBitmap, this)
                    binding.panel.background = BitmapDrawable(this@ConfigViewActivity.resources, selectedBitmap)
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val FROM_CONTROL = 0
        const val FROM_CLIENT = 1
    }
}