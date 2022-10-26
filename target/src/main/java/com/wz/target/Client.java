package com.wz.target;

import static com.wz.base.NetUtil.LAYOUT_CLIENT;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.google.gson.Gson;
import com.wz.base.Msg;
import com.wz.base.NetUtil;
import com.wz.base.SkillLayoutConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    public static final int DEFAULT_TIMEOUT = 60 * 1000;
    public static int timeout = DEFAULT_TIMEOUT;
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
        System.out.println(Arrays.toString(args));
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
            socket.close();
            serverSocket.close();
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "连接超时");
            isProcessing = false;
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "com.wz.client exit with Exception");
            isProcessing = false;
            e.printStackTrace();
        } finally {
            Log.e(TAG, "com.wz.client exit");
        }
    }

    private static int offsetX, offsetY;
    private static void readLayoutConfig() {
        List<String> strings = NetUtil.readFileByLines(LAYOUT_CLIENT);
        if (strings.size() == 1) {
            SkillLayoutConfig skillLayoutConfig = gson.fromJson(strings.get(0), SkillLayoutConfig.class);
            offsetX = (int) skillLayoutConfig.getOffsetX();
            offsetY = (int) skillLayoutConfig.getOffsetY();
            Log.i(TAG, "readLayoutConfig: offsetX = " + offsetX + ", offsetY = " + offsetY);
        }
//        for (String config : strings) {
//            Log.i(TAG, "readLayoutConfig: " + config);
//        }
    }

    private static void processEvent(Msg msg) {
        long now = SystemClock.uptimeMillis();
        int action = msg.getAction();
        Point point = new Point(msg.getX() + offsetX, msg.getY() + offsetY);
        int pointerIndex = pointersState.getPointerIndex(-2);
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(action == MotionEvent.ACTION_UP ? 0f : 1f);
        pointer.setUp(action == MotionEvent.ACTION_UP);
        pointerCount = pointersState.update(pointerProperties, pointerCoords);
        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }
        MotionEvent message = generateEvent(now, action);
        Input.getInstance().getInputManager().injectInputEvent(message, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private static void initSockets() throws Exception {
        List<String> ipAddress1 = NetUtil.getIpAddress();
        for (String ip : ipAddress1) {
            System.out.println(ip);
        }
        serverSocket = new ServerSocket(55555, 50, InetAddress.getByName(ipAddress1.get(0)));
        Log.e(TAG, "serverSocket start ip=" + ipAddress1.get(0));
        serverSocket.setPerformancePreferences(-1, 10, -1);
        serverSocket.setSoTimeout(timeout);
        socket = serverSocket.accept();
        Log.e(TAG, "连接成功");
        printWriter = new PrintWriter(socket.getOutputStream());
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private static MotionEvent generateEvent(long now, int action) {
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
