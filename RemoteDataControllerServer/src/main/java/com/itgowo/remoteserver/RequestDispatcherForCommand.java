package com.itgowo.remoteserver;

import com.alibaba.fastjson.JSON;
import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Command;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.http.HttpServerHandler;
import com.itgowo.servercore.packagesocket.PackageMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.itgowo.remoteserver.ControllerServiceServer.CLOSE;

public class RequestDispatcherForCommand {
    public static final String DELETE_CLIENT = "DeleteClient";
    public static final String GETLIST_CLIENT = "GetListClient";


    public static void doRequestWebCommand(HttpServerHandler handler, HashMap<String, HttpServerHandler> httpProxy, ConcurrentHashMap<String, Client> clients) {
        String body = handler.getBody(Charset.forName("utf-8"));
        Response response = JSON.parseObject(body, Response.class);
        String clientId = response.getClientId();
        try {
            switch (response.getAction()) {
                case DELETE_CLIENT:
                    if (clientId != null) {
                        sendCommand(clientId, new Command().setAction(CLOSE).setData("此设备被服务通知断开"));
                        ControllerServiceServer.clients.remove(clientId);
                        ServerManager.getLogger().info("控制移除设备："+clientId);
                        handler.sendData(new Response().toJson(), true);
                    }else {
                        handler.sendData(new Response().setCode(Response.code_Error).setMsg("参数错误").toJson(), true);
                    }
                    break;
                case GETLIST_CLIENT:
                    handler.sendData(JSON.toJSONString(new ArrayList<>(ControllerServiceServer.clients.values())), true);
                    break;
                default:
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

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
        packageMessage.setDataType(PackageMessage.DATA_TYPE_JSON).setData(json.getBytes());
        client.getHandler().sendData(packageMessage);
        return true;
    }
}
