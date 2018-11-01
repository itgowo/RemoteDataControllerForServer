package com.itgowo.remoteserver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

public class Test {
    public static void main(String[] args) {
        try {
            Socket socket=new Socket("rdc.itgowo.com",80);
            socket.getOutputStream().write("sssss".getBytes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
