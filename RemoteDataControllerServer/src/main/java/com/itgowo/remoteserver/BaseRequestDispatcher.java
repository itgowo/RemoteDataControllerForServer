package com.itgowo.remoteserver;

import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.http.HttpServerHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.itgowo.remoteserver.ControllerServiceServer.*;

public class BaseRequestDispatcher {

    public static void cleanOfflineClient() {
        for (Map.Entry<String, Client> stringClientEntry : clients.entrySet()) {
            if (stringClientEntry.getValue().isOffLine()) {
                clients.remove(stringClientEntry.getKey());
                ServerManager.getLogger().info("Clean Client:" + stringClientEntry.getValue().getClientId());
            }
        }
    }


    public static void dispatcherTask(HttpServerHandler handler) throws IOException {

        if (handler.getPath().startsWith(CLIENT)) {
            RequestDispatcherForClient.doRequestClient(handler, httpProxy);
        } else if (handler.getPath().startsWith(WEB)) {
            RequestDispatcherWebClient.doRequestWeb(handler, httpProxy, clients);
        } else if (handler.getHttpRequest().method() == io.netty.handler.codec.http.HttpMethod.GET) {
            String path = handler.getPath();
            if (path.equalsIgnoreCase("") || path.startsWith("index.html")) {
                path = "index.html";
            }
            File file = new File(path);
            if (!file.exists()) {
                file = new File(webRootDir, path);
            }
            if (file.exists()) {
                if (BaseConfig.getRDCServerWebCacheControl()) {
                    handler.addHeaderToResponse(HttpHeaderNames.CACHE_CONTROL.toString(), "max-age=3600");
                }
                handler.addHeaderToResponse("oldUrl", handler.getUri());
                handler.sendFile(file, true);
            } else {
                handler.sendData(HttpResponseStatus.NOT_FOUND, "404 资源未找到", false);
            }
        } else {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("不支持的请求类型").toJson(), true);
        }
    }
}
