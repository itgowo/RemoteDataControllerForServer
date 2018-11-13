package com.itgowo.remoteserver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;

public class Test {
    public static void main(String[] args) {
        File file=new File("/Users/lujianchao/Downloads/111.png");
        file.renameTo(new File("/Users/lujianchao/Downloads/1112/22.png"));
    }
}
