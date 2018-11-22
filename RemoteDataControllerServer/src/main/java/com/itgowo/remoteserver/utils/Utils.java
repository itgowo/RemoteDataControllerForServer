package com.itgowo.remoteserver.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    /**
     * 将zip包内文件与目录下对应文件比对，是否有更新，有则表示zip最新
     *
     * @param inZip
     * @return
     * @throws IOException
     */
    public static void updateWebFiles(ZipInputStream inZip, File dir) throws IOException {
        ZipEntry zipEntry;
        File file = null;
        while ((zipEntry = inZip.getNextEntry()) != null) {
            if (zipEntry.isDirectory()) {
                file = new File(dir, zipEntry.getName());
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                file = new File(dir, zipEntry.getName());
                if (file.exists()) {
                    if (file.lastModified() < zipEntry.getTime()) {
                        saveToFile(inZip, file);
                    }
                } else {
                    saveToFile(inZip, file);
                }
            }
        }
    }

    /**
     * 将制定zip中文件写入File
     *
     * @param zipInputStream
     * @param file
     * @throws IOException
     */
    protected static void saveToFile(ZipInputStream zipInputStream, File file) throws IOException {
        // 获取文件的输出流
        FileOutputStream out = new FileOutputStream(file);
        int len;
        byte[] buffer = new byte[1024];
        // 读取（字节）字节到缓冲区
        while ((len = zipInputStream.read(buffer)) != -1) {
            // 从缓冲区（0）位置写入（字节）字节
            out.write(buffer, 0, len);
            out.flush();
        }
        out.close();
    }
}
