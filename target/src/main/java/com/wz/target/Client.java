package com.wz.target;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private static Socket socket;
    private static ServerSocket serverSocket;
    private static PrintWriter printWriter;
    private static BufferedReader bufferedReader;
    private static boolean isProcessing = true;
    private static final String TAG = "ControllerClient";
    private static final Gson gson = new Gson();
    private static int pointerCount = 0;
    private static long lastTouchDown = 0L;
    private static PointersState pointersState = new PointersState();
    private static MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private static MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];

    public static void main(String[] args) {
        try {
            initPointers();
            readLayoutConfig();
            initSockets();
            do {
                String string = bufferedReader.readLine();
                Log.e(TAG, "bufferedReader.readLine()=" + string);
                if (string.contains(Constants.ACTION_EVENT)) {
                    Msg msg = gson.fromJson(string, Msg.class);
                    processEvent(msg);
                } else if (string.contains(Constants.ACTION_HELLO)) {
                    sendToServer(gson.toJson(Msg.generateAnswer(string)));
                } else if (string.contains(Constants.ACTION_END)) {
                    isProcessing = false;
                }
            } while(isProcessing);
        } catch (Exception e) {
            Log.e(TAG, "com.wz.client exit with Exception");
            isProcessing = false;
            e.printStackTrace();
        } finally {
            Log.e(TAG, "com.wz.client exit");
        }
    }

    private static void readLayoutConfig() {
        List<String> strings = NetUtil.readFileByLines("data/local/tmp/layout.txt");
        for (String config : strings) {
            Log.i(TAG, "readLayoutConfig: " + config);
        }
    }

    private static void processEvent(Msg msg) {
        long now = SystemClock.uptimeMillis();
        int action = msg.getAction();
    }

    private static void initSockets() throws Exception {
        List<String> ipAddress1 = NetUtil.getIpAddress();
        for (String ip : ipAddress1) {
            System.out.println(ip);
        }
        serverSocket = new ServerSocket(55555, 50, InetAddress.getByName(ipAddress1.get(0)));
        Log.e(TAG, "serverSocket start ip=" + ipAddress1.get(0));
        serverSocket.setPerformancePreferences(-1, 10, -1);
        serverSocket.setSoTimeout(60000);
        socket = serverSocket.accept();
        printWriter = new PrintWriter(socket.getOutputStream());
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private MotionEvent generateEvent(long now, int action, int pointCount) {
        return MotionEvent
                .obtain(
                        lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0,
                        0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
                );
    }

    private static void initPointers() {
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    private static final ExecutorService tp = Executors.newSingleThreadExecutor();
    public static void sendToServer(String msg) {
        tp.execute(() -> {
            printWriter.println(msg);
            printWriter.flush();
        });
    }
}
