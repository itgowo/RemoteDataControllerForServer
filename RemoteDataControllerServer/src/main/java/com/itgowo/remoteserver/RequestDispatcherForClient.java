package com.itgowo.remoteserver;

import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.remoteserver.utils.URLEncoder;
import com.itgowo.servercore.http.HttpServerHandler;
import io.netty.handler.codec.http.HttpMethod;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.itgowo.remoteserver.ControllerServiceServer.UID;


public class RequestDispatcherForClient extends BaseRequestDispatcher {
    public static void doRequestClient(HttpServerHandler handler, HashMap<String, HttpServerHandler> httpProxy) throws UnsupportedEncodingException {
        if (handler.getHttpRequest().method() == HttpMethod.POST) {
            doRequestClientPOST(handler, httpProxy);
        } else if (handler.getHttpRequest().method() == HttpMethod.GET) {
            if (handler.getParameters().get("Uuid") != null) {
                //web文件上传后由app获取暂存文件
                HttpServerHandler httpServerHandler = httpProxy.get(handler.getParameters().get("Uuid"));
                if (httpServerHandler != null) {
                    if (httpServerHandler.getFileUploads().size() > 0) {
                        File file = httpServerHandler.getFileUploads().get(0);
                        String p = httpServerHandler.getParameters().get("uploadPath");
                        ServerManager.getLogger().info("客户端取走Web上传文件:" + p);
                        handler.sendRedirect("/upload/" + URLEncoder.encode(file.getName(),"utf-8") + "?uploadPath=" + URLEncoder.encode(p,"utf-8"));
                    }
                }
            }
        } else {
            handler.sendData(new Response().setCode(Response.code_Error).setMsg("请求不存在").toJson(), true);
        }
    }

    public static void doRequestClientPOST(HttpServerHandler handler, HashMap<String, HttpServerHandler> httpProxy) {
        String uid = null;
        if (handler.getParameters() != null) {
            uid = handler.getParameters().get(UID);
        }
        if (uid == null || uid.trim().length() < 3) {
            return;
        }
        if (handler.isMultipart()) {
            HttpServerHandler httpServerHandler = httpProxy.remove(uid);
            if (httpServerHandler != null) {
                if (handler.getFileUploads() != null && !handler.getFileUploads().isEmpty()) {
                    try {
                        httpServerHandler.sendRedirect("/upload/" + URLEncoder.encode(handler.getFileUploads().get(0).getName(),"utf-8"));
                        ServerManager.getLogger().info("客户端上传Web需求文件:" + handler.getFileUploads());
                    } catch (UnsupportedEncodingException e) {
                        ServerManager.getLogger().info("客户端上传Web需求文件:" + e.getLocalizedMessage());
                    }

                }

            } else {
                ServerManager.getLogger().info("fromClient:不存在" + uid);
            }
            try {
                handler.sendData("", false);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            //body为null为请求web请求信息，不为null为回复web请求
            String body = handler.getBody(Charset.defaultCharset());
            try {
                if (body == null || body.length() < 1) {
                    HttpServerHandler httpServerHandler = httpProxy.get(uid);
                    if (httpServerHandler != null) {
                        String s = httpServerHandler.getBody(Charset.defaultCharset());
                        ServerManager.getLogger().info("客户端请求代理数据包:" + httpProxy.size() + "  " + s);
                        handler.sendData(s, true);
                    }
                } else {
                    ServerManager.getLogger().info("客户端回复web:" + httpProxy.size());
                    HttpServerHandler httpServerHandler = httpProxy.remove(uid);
                    httpServerHandler.sendData(handler.getBody(Charset.defaultCharset()), true);
                    ServerManager.getLogger().info("HttpProxyRemove:" + httpProxy.size());
                    handler.sendData("", false);
                }
            } catch (Exception e) {
                ServerManager.getLogger().warning(e.getLocalizedMessage());
            }
            ServerManager.getLogger().info("fromClient:" + body);
        }
    }
}
