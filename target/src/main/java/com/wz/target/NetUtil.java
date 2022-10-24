package com.wz.target;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public static List<String> readFileByLines(String fileName) {
        File file = new File(fileName);
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
