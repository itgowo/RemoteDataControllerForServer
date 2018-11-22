package com.itgowo.remoteserver;

import com.alibaba.fastjson.JSON;
import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.onServerListener;
import com.itgowo.servercore.packagesocket.PackageServerHandler;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static com.itgowo.remoteserver.ControllerServiceServer.*;

public class SocketPackageDispatcher implements onServerListener<PackageServerHandler> {
    private HashMap<ChannelHandlerContext, String> clients = new HashMap<>();
    private onClientListener listener;
    private ControllerServiceServer controllerServiceServer;

    public onClientListener getListener() {
        return listener;
    }

    public SocketPackageDispatcher setControllerServiceServer(ControllerServiceServer controllerServiceServer) {
        this.controllerServiceServer = controllerServiceServer;
        return this;
    }

    public SocketPackageDispatcher setListener(onClientListener listener) {
        this.listener = listener;
        return this;
    }

    private void delay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onReceived(PackageServerHandler handler) {
        if (handler.getPackageMessage() == null || handler.getPackageMessage().getData() == null || handler.getPackageMessage().getData().readableBytes() < 2) {
            return;
        }
        String string = new String(handler.getPackageMessage().getData().readableBytesArray());

        if (string.trim().startsWith("{") && string.trim().endsWith("}")) {
            Response response = null;
            try {
                response = JSON.parseObject(string, Response.class);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (response == null) {
                return;
            }
            ServerManager.getLogger().finest(string);
            switch (response.getAction()) {
                case CONNECT:
                    if (response.getClientId() != null && response.getClientId().trim().length() > 0) {
                        handler.sendData(handler.getPackageMessage());
                    }
                    break;
                case CONNECTED:
                    if (response.getClientId() != null && response.getClientId().trim().length() > 0) {
                        controllerServiceServer.getClients().put(response.getClientId(), new Client(response.getClientId(), handler));
                        ServerManager.getLogger().info("添加设备：" + response.getClientId());
                        if (listener != null) {
                            listener.onAddCLient(response.getClientId());
                        }
                        handler.sendData(handler.getPackageMessage());
                    }
                    break;
                case AUTH:
                    if (response.getClientId() != null && response.getClientId().length() > 1) {
                        Client client = controllerServiceServer.getClients().get(response.getClientId());
                        if (client != null) {
                            if (response.getCode() == Response.code_OK) {
                                client.setAuth(true);
                                client.setToken(response.getData());
                                clients.put(handler.getCtx(), client.getClientId());
                                if (client.getHttpHandler() != null) {
                                    try {
                                        client.getHttpHandler().sendData(new Response().toJson(), true);
                                        client.setHttpHandler(null);
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                ServerManager.getLogger().info("设备通过认证：" + client.getClientId());
                                if (listener != null) {
                                    listener.onAuthClient(client.getClientId(), client.getToken());
                                }
                            } else {
                                try {
                                    controllerServiceServer.getClients().get(response.getClientId()).getHttpHandler().sendData(string, true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                default:
            }
        } else {

        }
    }

    @Override
    public void onChannelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void onChannelInactive(ChannelHandlerContext ctx) {
        controllerServiceServer.cleanOfflineClinet();
    }

    @Override
    public void onReceiveHandler(PackageServerHandler handler) throws Exception {
        onReceived(handler);
    }

    @Override
    public void onUserEventTriggered(Object event) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onServerStarted(int serverPort) {

    }

    @Override
    public void onServerStop() {

    }
}
