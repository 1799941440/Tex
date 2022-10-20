package com.wz.tex

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wz.tex.view.c.ClientActivity
import com.wz.tex.view.s.ServerActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val send = findViewById<View>(R.id.send)
        val server = findViewById<View>(R.id.server)
        val client = findViewById<View>(R.id.client)
        val lastConfig = findViewById<View>(R.id.lastConfig)
        send.setOnClickListener {
        }
        server.setOnClickListener {
            startActivity(Intent(this, ServerActivity::class.java))
        }
        client.setOnClickListener {
            startActivity(Intent(this, ClientActivity::class.java))
        }
        lastConfig.setOnClickListener {
        }
    }
}