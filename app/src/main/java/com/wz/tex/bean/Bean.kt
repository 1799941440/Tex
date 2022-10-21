package com.wz.tex.bean

import android.view.MotionEvent

//MotionEvent { action=ACTION_DOWN, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=2493072910, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=216516680 }
//MotionEvent { action=ACTION_MOVE, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=1, eventTime=2493072920, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=849214663 }
//MotionEvent { action=ACTION_UP, actionButton=0, id[0]=0, x[0]=40.25, y[0]=50.25, toolType[0]=TOOL_TYPE_FINGER, buttonState=0, classification=NONE, metaState=0, flags=0x40000, edgeFlags=0x0, pointerCount=1, historySize=0, eventTime=2493072922, downTime=2493072910, deviceId=4, source=0x1002, displayId=0, eventId=155553049 }

data class Msg(
    var socketAction: String? = "",
    var msg: String? = "",
    //<editor-fold desc="socketAction = event">
    var action: Int? = 0,
    var x: Int? = 0,
    var y: Int? = 0,
    //</editor-fold>
) {
    companion object {
        fun generateControlMsg(e: MotionEvent): Msg {
            return Msg().apply {
                socketAction = "event"
                action = e.action
                x = e.x.toInt()
                y = e.y.toInt()
            }
        }

        fun generateHello(): Msg {
            return Msg().apply {
                socketAction = "hello"
                msg = "hello from server"
            }
        }

        fun generateAnswer(ask: String?): Msg {
            return Msg().apply {
                socketAction = "answer"
                msg = "answer for '$ask'"
            }
        }
    }
}
