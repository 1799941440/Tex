package com.wz.tex;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.FileUtils;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class BitmapUtils {

    /**
     * Save Bitmap
     *
     * @param name file name
     * @param bm   picture to save
     */

    public static void saveBitmap(String targetDir, String name, Bitmap bm) {
        //判断指定文件夹的路径是否存在
        if (!fileIsExist(targetDir)) {
            Log.d("Save Bitmap", "TargetPath isn't exist");
        } else {
            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作
            File saveFile = new File(targetDir, name);
            try {
                FileOutputStream saveImgOut = new FileOutputStream(saveFile);
                // compress - 压缩的意思
                bm.compress(Bitmap.CompressFormat.JPEG, 100, saveImgOut);
                //存储完成后需要清除相关的进程
                saveImgOut.flush();
                saveImgOut.close();
                Log.d("Save Bitmap", "The picture is save to your phone!");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    static boolean fileIsExist(String fileName) {
        //传入指定的路径，然后判断路径是否存在
        File file = new File(fileName);
        if (file.exists())
            return true;
        else {
            //file.mkdirs() 创建文件夹的意思
            return file.mkdirs();
        }
    }

    public static String getIntranetIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                return null;
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return intIP2StringIP(wifiInfo.getIpAddress());
            }
        } else {
            return null;
        }
        return null;
    }

    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    public static boolean saveAssetsToSDCard(Context context, String assetsFileName) {
        boolean isSave;
        String fileDir = context.getFilesDir() + File.separator;
        Log.i("saveAssetsToSDCard", "saveAssetsToSDCard: fileDir = " + fileDir);
        File fileD = new File(fileDir);
        if (!fileD.exists() || !fileD.isDirectory()) {
            if (!fileD.mkdirs()) { // 文件夹创建失败
                Log.i("saveAssetsToSDCard", "saveAssetToSDCard: file make dir file");
            }
        }
        String filePath = fileDir + assetsFileName;
        File file = new File(filePath);
        //如果文件存在，则不拷贝
        if (file.exists()) {
            return true;
        }
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = context.getAssets().open(assetsFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            fos = new FileOutputStream(file);
            fos.write(buffer);
            isSave = true;
//            Log.i("saveAssetsToSDCard", "saveAssetToSDCard: finish");
        } catch (Exception e) {
            isSave = false;
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return isSave;
    }
}