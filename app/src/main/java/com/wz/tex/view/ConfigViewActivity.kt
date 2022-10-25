package com.wz.tex.view

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.wz.tex.BitmapUtils
import com.wz.tex.databinding.LayoutConfigViewBinding
import java.io.File
import java.io.IOException
import java.io.InputStream


class ConfigViewActivity : AppCompatActivity() {

    private val binding by lazy { LayoutConfigViewBinding.inflate(layoutInflater) }
    private val REQUEST_GET_IMAGE = 1
    private var from = FROM_CONTROL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContentView(binding.root)
        from = intent?.getIntExtra("from", FROM_CONTROL) ?: FROM_CONTROL
        loadTempFile()
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
    private fun loadTempFile() {
        val value = filesDir.absolutePath + "/images/"
        val file = File(value)
        if (file.exists()) {
            val exists = File(value + "temp.jpeg")
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
                    val selectdBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(it), null, options)
                    val name = if (from == FROM_CONTROL) "control.jpeg" else "client.jpeg"
                    BitmapUtils.saveBitmap(name, selectdBitmap, this)
                    binding.panel.background = BitmapDrawable(this@ConfigViewActivity.resources, selectdBitmap)
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