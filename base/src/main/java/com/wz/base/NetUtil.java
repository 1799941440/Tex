package com.wz.base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
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

    public static final String LOCATE = "data/local/tmp/layout/";
    public static final String LOCATE_CONTROL = LOCATE + "s/";
    public static final String LOCATE_CLIENT = LOCATE + "c/";
    public static final String LAYOUT_CONTROL = LOCATE_CONTROL + "layout.txt";
    public static final String LAYOUT_CLIENT = LOCATE_CLIENT + "layout.txt";

//    public static void checkFile() {
//        try {
//            Process p = Runtime.getRuntime().exec("/system/bin/sh");
//            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            bufferedWriter.write("mkdir -R 777 /data/local/tmp/layout");
//            bufferedWriter.newLine();
//            bufferedWriter.close();
//            String s = bufferedReader.readLine();
//            while (s != null) {
//                s = bufferedReader.readLine();
//                System.out.println(s);
//            }
//            p.waitFor();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    static String[] cmds = {"/system/bin/sh", "CLASSPATH=/data/local/tmp/target", "app_process", "/data/local/temp", "com.wz.target.Client"};

    public static void checkFile() {
        try {
            Runtime.getRuntime().exec(cmds).waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

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
}
