package com.wz.base;

import android.view.MotionEvent;

public class Msg {
    private String socketAction;
    private String msg;
    //<editor-fold desc="socketAction = event">
    private int action;
    private int x;
    private int y;
    //</editor-fold>

    public String getSocketAction() {
        return socketAction;
    }

    public void setSocketAction(String socketAction) {
        this.socketAction = socketAction;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public static Msg generateControlMsg(MotionEvent e) {
        Msg temp = new Msg();
        temp.setSocketAction("event");
        temp.setAction(e.getAction());
        temp.setX((int) e.getX());
        temp.setY((int) e.getY());
        return temp;
    }

    public static Msg generateHello() {
        Msg temp = new Msg();
        temp.setSocketAction("hello");
        temp.setMsg("hello from server");
        return temp;
    }

    public static Msg generateAnswer(String ask) {
        Msg temp = new Msg();
        temp.setSocketAction("answer");
        temp.setMsg("answer for: " + ask);
        return temp;
    }
}
