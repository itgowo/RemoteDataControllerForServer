package com.itgowo.remoteserver;

import com.alibaba.fastjson.JSON;
import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Command;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.http.HttpServerHandler;
import com.itgowo.servercore.packagesocket.PackageMessage;
import io.netty.handler.codec.http.HttpMethod;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.itgowo.remoteserver.ControllerServiceServer.*;

public class RequestDispatcherWebClient {
    public static void doRequestWeb(HttpServerHandler handler, HashMap<String, HttpServerHandler> httpProxy, ConcurrentHashMap<String, Client> clients) throws IOException {
        if (handler.getHttpRequest().method() == HttpMethod.POST) {
            doRequestWebPOST(handler, httpProxy, clients);
        } else if (handler.getHttpRequest().method() == HttpMethod.GET) {
            if (handler.getParameters().containsKey("downloadFile")) {
                String clientID = handler.getParameters().get(CLIENTID);
                String downloadFile = handler.getParameters().get("downloadFile");
                Client client = clients.get(clientID);
                if (clientID == null || client == null || downloadFile == null) {
                    handler.sendData(new Response().setCode(Response.code_Error).setMsg("未找到设备").toJson(), true);
                    return;
                }
                httpProxy.put(downloadFile, handler);
                //向APP发送命令
                Command json = new Command().setAction(REQUEST_PROXY_DOWNLOAD).setData(downloadFile);
                sendCommand(client.getClientId(), json);
            }
        } else {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("请求不存在").toJson(), true);
        }
    }

    public static void doRequestWebPOST(HttpServerHandler handler, HashMap<String, HttpServerHandler> httpProxy, ConcurrentHashMap<String, Client> clients) throws UnsupportedEncodingException {
        //获取请求地址ClientID参数
        String clientID = handler.getParameters().get(CLIENTID);
        if (clientID == null || clientID.trim().length() < 3) {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("参数未找到").toJson(), true);
            return;
        }
        //获取设备
        Client client = clients.get(clientID);
        if (client == null) {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("未找到设备").toJson(), true);
            return;
        }
        if (handler.isMultipart()) {
            String uid = UUID.randomUUID().toString();
            httpProxy.put(uid, handler);
            //向APP发送命令
            Command json = new Command().setAction(REQUEST_PROXY_UPLOAD).setData(uid);
            sendCommand(client.getClientId(), json);
        } else {
            doRequestWeb2(handler, client);
        }
    }

    private static void doRequestWeb2(HttpServerHandler handler, Client client) throws UnsupportedEncodingException {
        String body = handler.getBody(Charset.defaultCharset());
        ServerManager.getLogger().info("fromWeb:" + body);
        Command command = JSON.parseObject(body, Command.class);
        if (command == null) {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("数据错误,无法解析" + handler).toJson(), true);
            return;
        }
        try {
            if (AUTH.equalsIgnoreCase(command.getAction())) {
                if (command.getClientId() == null || command.getData() == null || command.getClientId().trim().length() < 1 || command.getData().trim().length() < 1) {
                    handler.sendData(new Response().setCode(Response.code_Error).setMsg("数据错误").toJson(), true);
                    return;
                }
                if (client.isAuth()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("url", BaseConfig.getWebServerDefaultUrl());
                    map.put("clientId", client.getClientId());
                    String json = JSON.toJSONString(map);
                    Response response = new Response().setData(json);
                    handler.sendData(response.toJson(), true);
                } else {
                    auth(command.getClientId(), command.getData(), BaseConfig.getWebServerDefaultUrl());
                    client.setHttpHandler(handler);
                }
            } else {
                if (client.isAuth()) {
                    String uid = UUID.randomUUID().toString();
                    httpProxy.put(uid, handler);
                    //向APP发送命令
                    Command json = new Command().setAction(REQUEST_PROXY).setData(uid);
                    sendCommand(client.getClientId(), json);
                } else {
                    handler.sendData(new Response().setCode(Response.code_Error).setMsg("设备：" + client.getClientId() + " 未通过验证").toJson(), true);
                }
            }

        } catch (Exception e) {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("设备：" + client.getClientId() + " 代理操作失败").toJson(), true);
            return;
        }
    }

    public static boolean auth(String clientId, String code, String remoteUrl) {
        return sendCommand(clientId, new Command().setAction(AUTH).setData(code).setClientId(clientId).setRemoteUrl(remoteUrl));
    }

    public static boolean sendCommand(String clientId, Command command) {
        Client client = ControllerServiceServer.clients.get(clientId);
        if (client == null) {
            return false;
        }
        if (!client.getHandler().getCtx().channel().isActive()) {
            return false;
        }
        String json = JSON.toJSONString(command);
        PackageMessage packageMessage = PackageMessage.getPackageMessage();
        packageMessage.setDataSign(PackageMessage.DATA_TYPE_JSON).setData(json.getBytes());
        client.getHandler().sendData(packageMessage);
        return true;
    }
}
