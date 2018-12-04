package com.itgowo.remoteserver;

import com.alibaba.fastjson.JSON;
import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Command;
import com.itgowo.remoteserver.entry.ProxyResponse;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.onServerListener;
import com.itgowo.servercore.packagesocket.PackageMessage;
import com.itgowo.servercore.packagesocket.PackageServerHandler;
import io.netty.channel.ChannelHandlerContext;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static com.itgowo.remoteserver.ControllerServiceServer.*;

public class SocketPackageDispatcher implements onServerListener<PackageServerHandler> {
    private HashMap<String, String> clients = new HashMap<>();
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
        String clientId = clients.get(handler.getCtx().channel().id());
        if (clientId != null) {
            Client client = controllerServiceServer.getClients().get(clientId);
            if (client != null) {
                client.refreshLastMsgTime();
            }
        }
        if (handler.getPackageMessage().getDataType() == PackageMessage.DATA_TYPE_HEART) {
            ServerManager.getLogger().info("收到来自一个心跳");
            return;
        }
        String string = new String(handler.getPackageMessage().getData().readableBytesArray());

        if (string.trim().startsWith("{") && string.trim().endsWith("}")) {
            ProxyResponse proxyResponse = null;
            try {
                proxyResponse = JSON.parseObject(string, ProxyResponse.class);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (proxyResponse == null) {
                return;
            }
            ServerManager.getLogger().finest(string);
            switch (proxyResponse.getAction()) {
                case CONNECT:
                    if (proxyResponse.getClientId() != null && proxyResponse.getClientId().trim().length() > 0) {
                        handler.sendData(handler.getPackageMessage());
                    }
                    break;
                case CONNECTED:
                    if (proxyResponse.getClientId() != null && proxyResponse.getClientId().trim().length() > 0
                            && proxyResponse.getToken() != null && proxyResponse.getToken().trim().length() > 0) {
                        if (controllerServiceServer.getClients().containsKey(proxyResponse.getClientId())) {
                            Command command = new Command().setAction(CLOSE).setClientId(proxyResponse.getClientId())
                                    .setData("ID为" + proxyResponse.getClientId() + "的设备已存在");
                            ServerManager.getLogger().info("设备已存在：" + proxyResponse.getClientId());
                            handler.sendData(command.toJson().getBytes());
                        } else {
                            controllerServiceServer.getClients().put(proxyResponse.getClientId(), new Client(proxyResponse.getClientId(), handler).setToken(proxyResponse.getToken()));
                            ServerManager.getLogger().info("添加设备：" + proxyResponse.getClientId());
                            if (listener != null) {
                                listener.onAddCLient(proxyResponse.getClientId());
                            }
                            handler.sendData(handler.getPackageMessage());
                        }
                    }
                    break;
                case AUTH:
                    if (proxyResponse.getClientId() != null && proxyResponse.getClientId().length() > 1) {
                        Client client = controllerServiceServer.getClients().get(proxyResponse.getClientId());
                        if (client != null) {
                            if (proxyResponse.getCode() == Response.code_OK) {
                                client.setAuth(true);
                                clients.put(handler.getCtx().channel().id().asLongText(), client.getClientId());
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
                                    controllerServiceServer.getClients().get(proxyResponse.getClientId()).getHttpHandler().sendData(string, true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                case RECONNECTED:
                    ServerManager.getLogger().info("设备请求重连：" + proxyResponse.getClientId());
                    if (ControllerServiceServer.clients.containsKey(proxyResponse.getClientId())) {
                        Client client = controllerServiceServer.getClients().get(proxyResponse.getClientId());
                        if (client != null) {
                            if (client.getToken().equals(proxyResponse.getData())) {
                                client.setHandler(handler);
                                ServerManager.getLogger().info("设备重连：" + client.getClientId());
                                handler.sendData(handler.getPackageMessage());
                            } else {
                                Command command = new Command().setAction(CLOSE).setData("设备验证失败，无法建立连接");
                                handler.sendData(command.toJson().getBytes());
                            }
                        } else {
                            Command command = new Command().setAction(CLOSE).setData("设备不存在，无法建立连接");
                            handler.sendData(command.toJson().getBytes());
                        }
                    } else {
                        Command command = new Command().setAction(CLOSE).setData("设备不存在，无法建立连接");
                        handler.sendData(command.toJson().getBytes());
                    }
                    break;
                case CLOSE:
                    Client client = controllerServiceServer.getClients().get(proxyResponse.getClientId());
                    if (client != null) {
                        if (handler.getCtx().channel() == client.getHandler().getCtx().channel()) {
                            controllerServiceServer.getClients().remove(proxyResponse.getClientId());
                            ServerManager.getLogger().info("设备主动断开：" + proxyResponse.getClientId());
                        } else {
                            ServerManager.getLogger().info("设备主动断开但连接不同，拒绝断开请求：" + proxyResponse.getClientId());
                        }
                    } else {
                        ServerManager.getLogger().info("设备主动断开但设备未连接：" + proxyResponse.getClientId());
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
