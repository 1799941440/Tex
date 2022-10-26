package com.wz.tex

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wz.tex.databinding.ActivityMainBinding
import com.wz.tex.view.c.ClientActivity
import com.wz.tex.view.s.ServerActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.send.setOnClickListener {
        }
        binding.server.setOnClickListener {
            startActivity(Intent(this, ServerActivity::class.java))
        }
        binding.client.setOnClickListener {
            startActivity(Intent(this, ClientActivity::class.java))
        }
        binding.lastConfig.setOnClickListener {
        }
    }
}