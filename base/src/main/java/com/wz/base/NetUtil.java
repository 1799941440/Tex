package com.wz.base;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class NetUtil {
    public static List<String> getIpAddress() throws SocketException, IllegalStateException {
        List<String> list = new LinkedList<>();
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface network = (NetworkInterface) enumeration.nextElement();
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = (InetAddress) addresses.nextElement();
                if ((address instanceof Inet4Address)) {
                    String hostAddress = address.getHostAddress();
                    if (hostAddress == null) continue;
                    if (hostAddress.startsWith("10.") || hostAddress.startsWith("172.") || hostAddress.startsWith("192.168")) {
                        list.add(hostAddress);
                    }
                }
            }
        }
        if (list.size() == 0) {
            throw new IllegalStateException("没有本地环回ipv4地址(10.x.x.x或172.x.x.x或192.168.x.x)");
        }
        return list;
    }

    public static String LOCATE = "";
    public static final String LOCATE_CONTROL = "/s/";
    public static final String LOCATE_CLIENT = "/c/";
    public static final String DEFAULT_CONFIG_NAME = "layout.txt";
    public static final String DEFAULT_CONFIG_CONTENT = "{\"index\":1,\"mapType\":1,\"offsetX\":0,\"offsetY\":0,\"orientation\":2,\"width\":100,\"height\":100}";

    public static String getDefaultControlLayoutDir() {
        return LOCATE + LOCATE_CONTROL;
    }

    public static String getDefaultClientLayoutDir() {
        return LOCATE + LOCATE_CLIENT;
    }

    public static String[] getSConfigFileList() throws Exception {
        File dir = new File(getDefaultControlLayoutDir());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new Exception("创建ControlLayoutDir失败");
            }
        }
        String[] list = dir.list((file1, s) -> s.endsWith(".txt"));
        if (list == null || list.length == 0) {
            File defaultFile = new File(dir, DEFAULT_CONFIG_NAME);
            if (!defaultFile.createNewFile()) {
                throw new Exception("创建ControlLayoutDir/layout.txt失败");
            }
            coverFile(defaultFile, DEFAULT_CONFIG_CONTENT);
            return new String[]{ DEFAULT_CONFIG_NAME };
        }
        return list;
    }

    public static String[] getCConfigFileList() throws Exception {
        File dir = new File(getDefaultClientLayoutDir());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new Exception("创建ClientLayoutDir失败");
            }
        }
        String[] list = dir.list((file1, s) -> s.endsWith(".txt"));
        if (list == null || list.length == 0) {
            File defaultFile = new File(dir, DEFAULT_CONFIG_NAME);
            if (!defaultFile.createNewFile()) {
                throw new Exception("创建ClientLayoutDir/layout.txt失败");
            }
            coverFile(defaultFile, DEFAULT_CONFIG_CONTENT);
            return new String[]{ DEFAULT_CONFIG_NAME };
        }
        return list;
    }

    public static void coverFile(File file, List<String> content) {
        writeFile(file, content, false);
    }

    public static void appendFile(File file, List<String> content) {
        writeFile(file, content, true);
    }

    public static void coverFile(File file, String content) {
        writeFile(file, Collections.singletonList(content), false);
    }

    public static void appendFile(File file, String content) {
        writeFile(file, Collections.singletonList(content), true);
    }

    public static void writeDefault(File file) {
        writeFile(file, Collections.singletonList(DEFAULT_CONFIG_CONTENT), false);
    }

    private static void writeFile(File file, List<String> content, boolean isAppend) {
        if (file == null || content == null || content.size() == 0) return;
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, isAppend));
            for (String str : content) {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String[] cmds = {"/system/bin/sh", "CLASSPATH=/data/local/tmp/target", "app_process", "/data/local/temp", "com.wz.target.Client"};

    @Deprecated
    public static List<String> readFileByLines(String fileName) {
        File file = new File(fileName);
        List<String> result = new ArrayList<>();
        BufferedReader reader = null;
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                result.add(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return result;
    }

    public static List<String> readFileByLines(File file) {
        List<String> result = new ArrayList<>();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                result.add(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return result;
    }

    public static Bitmap adjustPhotoRotation(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }
}
