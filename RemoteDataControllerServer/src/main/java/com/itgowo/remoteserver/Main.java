package com.itgowo.remoteserver;

import com.itgowo.remoteserver.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class Main {
    public static void main(String[] args) {

        initServer();
    }

    private static void initServer() {
        try {
            File file = new File(BaseConfig.getRDCServerWebAppFile());
            if (file.exists()) {
                ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
                Utils.updateWebFiles(zipInputStream, new File(BaseConfig.getRDCServerWebRootDir()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ControllerServiceServer serviceServer = new ControllerServiceServer();
        SocketPackageDispatcher dispatcher = new SocketPackageDispatcher();
        dispatcher.setListener(new onClientListener() {
            @Override
            public void onAddCLient(String clientId) {
            }

            @Override
            public void onAuthClient(String clientId, String token) {
            }
        });
        serviceServer.setSocketDispatcher(dispatcher);
        try {
            serviceServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
