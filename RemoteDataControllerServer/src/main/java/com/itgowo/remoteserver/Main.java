package com.itgowo.remoteserver;

public class Main {
    public static void main(String[] args) {

        initServer();
    }

    private static void initServer() {
        ControllerServiceServer serviceServer = new ControllerServiceServer();
        SocketPackageDispatcher dispatcher = new SocketPackageDispatcher();
        dispatcher.setListener(new onClientListener() {
            @Override
            public void onAddCLient(String clientId) {
                System.out.println("设备添加： " + clientId);
//                serviceServer.auth(clientId, "111111");
            }

            @Override
            public void onAuthClient(String clientId, String token) {
                System.out.println("设备通过认证： " + clientId);
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
