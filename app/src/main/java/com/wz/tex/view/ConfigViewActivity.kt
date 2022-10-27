package com.wz.tex.view

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
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
    private var imageName: String? = ""
    private var buttonIndex: Int = 1
    private val layoutDir by lazy {
        if (from == FROM_CONTROL) {
            NetUtil.getDefaultControlLayoutDir()
        } else {
            NetUtil.getDefaultClientLayoutDir()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fullWindow()
        setContentView(binding.root)
        initParams()
        loadTempBg()
        loadConfigFile()
        initUI()
    }

    @Throws
    private fun loadTempBg() {
        val file = File(layoutDir)
        if (file.exists()) {
            val exists = File(layoutDir + imageName)
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

    private fun loadConfigFile() {
        loadConfigFile(layoutDir, fileName!!)
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
            NetUtil.writeDefault(file)
        }
        val readFileByLines = NetUtil.readFileByLines(file)
        if (readFileByLines.size > 0) {
            readFileByLines.forEach {
                try {
                    addButton(gson.fromJson(it, SkillLayoutConfig::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "配置文件解析失败", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addButton(info: SkillLayoutConfig) {
        val inflate = layoutInflater.inflate(R.layout.item_button, binding.panel, false)
        inflate.tag = buttonIndex
        if (inflate is TextView) inflate.text = buttonIndex.toString()
        val layoutParams = inflate.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.startToStart = R.id.panel
        layoutParams.topToTop = R.id.panel
        layoutParams.width = info.width.toInt()
        layoutParams.height = info.height.toInt()
        layoutParams.marginStart = info.offsetX.toInt()
        layoutParams.topMargin = info.offsetY.toInt()
        inflate.layoutParams = layoutParams
        binding.panel.addView(inflate)
        buttonIndex++
    }

    private fun initUI() {
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
        NetUtil.coverFile(File(layoutDir + fileName), list)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            data.data?.let {
                try {
                    val isPreDecode = contentResolver.openInputStream(it)
                    val onlyBoundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
                    }
                    BitmapFactory.decodeStream(isPreDecode, null, onlyBoundsOptions)
                    isPreDecode?.close()

                    val isDecode = contentResolver.openInputStream(it)
                    val selectedBitmap = BitmapFactory.decodeStream(isDecode, null, null) ?: return
                    if (onlyBoundsOptions.outWidth < onlyBoundsOptions.outHeight) {
                        val rotate = NetUtil.adjustPhotoRotation(selectedBitmap, -90f)
                        BitmapUtils.saveBitmap(layoutDir, imageName, rotate)
                        binding.panel.background = BitmapDrawable(this@ConfigViewActivity.resources, rotate)
                    } else {
                        BitmapUtils.saveBitmap(layoutDir, imageName, selectedBitmap)
                        binding.panel.background = BitmapDrawable(this@ConfigViewActivity.resources, selectedBitmap)
                    }
                    isDecode?.close()
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
        }
    }

    private fun initParams() {
        fileName = intent?.getStringExtra("fileName")
        if (fileName == null) {
            Toast.makeText(this, "传参fileName丢失", Toast.LENGTH_SHORT).show()
        }
        binding.attrs.text = fileName
        imageName = if (fileName!!.contains(".")) {
            "${fileName!!.split(".")[0]}.jpeg"
        } else {
            "${fileName}.jpeg"
        }
        from = intent?.getIntExtra("from", FROM_CONTROL) ?: FROM_CONTROL
    }

    private fun fullWindow() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        const val FROM_CONTROL = 0
        const val FROM_CLIENT = 1
    }
}