package com.wz.tex.view.s

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException
import java.util.*

object S {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val socket = Socket("192.168.12.52", 8888)
            val br = BufferedReader(InputStreamReader(socket.getInputStream()))
            val pw = PrintWriter(socket.getOutputStream())
            val r = Runnable {
                while (true) {
                    // 发送信息
                    try {
                        val scan = Scanner(System.`in`)
                        val msg = scan.nextLine()
                        pw.println(msg)
                        pw.flush()
                    } catch (e: Exception) {
                    }
                }
            }
            val t1 = Thread(r)
            ServerReceive(br).start()
            t1.start()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
}