package com.wz.tex.view.c

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.SocketException
import java.util.*

object C {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val server = ServerSocket(8888)
            println("----程序已经连接++++1")
            val socket = server.accept()
            println("----程序已经连接++++2")
            val br = BufferedReader(InputStreamReader(socket.getInputStream()))
            val pw = PrintWriter(socket.getOutputStream())
            val r2 = Runnable {
                // 返回信息
                while (true) {
                    val scan = Scanner(System.`in`)
                    val msg = scan.nextLine()
                    pw.println(msg)
                    pw.flush()
                }
            }
            ClientReceive(br).start()
            val t2 = Thread(r2)
            t2.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class ClientReceive(private val br: BufferedReader) : Thread() {
        override fun run() {
            while (!isInterrupted) {
                try {
                    val str = br.readLine()
                    println("接收 S 的信息$str")
                } catch (s: SocketException) {
                    s.printStackTrace()
                    interrupt()
                }
            }
        }
    }
}