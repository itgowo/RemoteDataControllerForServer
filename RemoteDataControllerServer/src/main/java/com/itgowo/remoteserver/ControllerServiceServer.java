package com.itgowo.remoteserver;

import com.alibaba.fastjson.JSON;
import com.itgowo.actionframework.ServerManager;
import com.itgowo.remoteserver.entry.Command;
import com.itgowo.remoteserver.entry.Response;
import com.itgowo.servercore.ServerHandler;
import com.itgowo.servercore.SimpleServerListener;
import com.itgowo.servercore.http.HttpServerHandler;
import com.itgowo.servercore.http.HttpServerManager;
import com.itgowo.servercore.httpclient.HttpMethod;
import com.itgowo.servercore.packagesocket.PackageMessage;
import com.itgowo.servercore.packagesocket.PackageServerManager;
import com.itgowo.servercore.utils.LogU;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControllerServiceServer {
    public static final long ALARMINTERVAL = 30 * 60 * 1000;
    public static final String CONNECT = "Connect";
    public static final String CONNECTED = "Connected";
    public static final String AUTH = "Auth";
    public static final String HEART = "Heart";
    public static final String REQUEST_PROXY = "RequestProxy";
    public static final String REQUEST_PROXY_UPLOAD = "RequestProxyUpload";
    public static final String REQUEST_PROXY_DOWNLOAD = "RequestProxyDownload";
    public static final String CLIENT = "Client";
    public static final String WEB = "Web";
    public static final String UID = "Uuid";
    public static final String CLIENTID = "ClientId";
    public static final String DOWNLOADFILE = "downloadFile";
    private String webRootDir = BaseConfig.getRDCServerWebRootDir() + File.separator;
    private PackageServerManager packageServerManager = new PackageServerManager();
    private HttpServerManager httpServerManager = ServerManager.getHttpServerManager();
    private Thread alarmThread = new Thread(() -> {
        try {
            Thread.sleep(ALARMINTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cleanOfflineClient();
    });
    private Logger logger = LogU.getLogU("RDC HttpServer", Level.ALL);
    /**
     * key为clientId;
     */
    private ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    private HashMap<String, HttpServerHandler> httpProxy = new HashMap<>();
    private SocketPackageDispatcher socketDispatcher;

    public ControllerServiceServer setSocketDispatcher(SocketPackageDispatcher socketDispatcher) {
        this.socketDispatcher = socketDispatcher;
        return this;
    }

    public ConcurrentHashMap<String, Client> getClients() {
        return clients;
    }

    public void start() throws Exception {
        ServerManager.setLogger(logger);
        startHttpServer();
        startSocketServer();
        alarmThread.setDaemon(true);
    }

    private void startSocketServer() {
        socketDispatcher.setControllerServiceServer(ControllerServiceServer.this);
        packageServerManager.setOnServerListener(socketDispatcher);
        packageServerManager.setServerName("RDC HttpServer");
        packageServerManager.setThreadConfig(2, 4);
        try {
            packageServerManager.prepare(BaseConfig.getRDCServerDefaultPort());
            packageServerManager.startAsyn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startHttpServer() {
        httpServerManager = new HttpServerManager(webRootDir);
        httpServerManager.setThreadConfig(3, 10);
        httpServerManager.setServerName("RDC HttpServer");
        httpServerManager.setOnServerListener(new SimpleServerListener() {
            @Override
            public void onReceiveHandler(ServerHandler handler) throws Exception {
                doRequest((HttpServerHandler) handler);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

        });
        try {
            httpServerManager.prepare(BaseConfig.getServerPort());
            httpServerManager.startAsyn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doRequest(HttpServerHandler handler) {
        try {
            if (HttpMethod.OPTIONS.equalsIgnoreCase(handler.getHttpRequest().method().name())) {
                handler.addHeaderToResponse(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(), "Origin, X-Requested-With, Content-Type, Accept");
                handler.addHeaderToResponse(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), "*");
                handler.addHeaderToResponse(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE.toString(), "Origin, 3600");
                handler.sendData("", false);
            } else if (HttpMethod.POST.equalsIgnoreCase(handler.getHttpRequest().method().name())) {
                if (handler.getPath().startsWith(CLIENT)) {
                    doRequestClient(handler);
                } else if (handler.getPath().startsWith(WEB)) {
                    doRequestWeb(handler);
                } else {
                    handler.sendData(new Response().setCode(Response.code_Error).setMsg("请求不存在").toJson(), true);
                }
            } else if (HttpMethod.GET.equalsIgnoreCase(handler.getHttpRequest().method().name())) {
                String path = handler.getPath();

                if (handler.getParameters().get("Uuid") != null && handler.getParameters().containsKey("uploadPath")) {
                    //web文件上传后由app获取暂存文件
                    HttpServerHandler httpServerHandler = httpProxy.get(handler.getParameters().get("Uuid"));
                    if (httpServerHandler != null) {
                        if (httpServerHandler.getFileUploads().size() > 0) {
                            File file = httpServerHandler.getFileUploads().get(0);
                            String p = httpServerHandler.getParameters().get("uploadPath");
                            ServerManager.getLogger().info("ProxyFile:" + p);
                            handler.sendRedirect("/upload/" + URLEncoder.encode(file.getName(), "utf-8") + "?uploadPath=" + p);
                        }
                    }
                } else if (handler.getParameters().containsKey("downloadFile")) {
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
                } else {
                    if (path.equalsIgnoreCase("") || path.startsWith("index.html")) {
                        path = "index.html";
                    }
                    File file = new File(path);
                    if (!file.exists()) {
                        file = new File(webRootDir + path);
                    }
                    if (file.exists()) {
//                    handler.addHeaderToResponse(HttpHeaderNames.CACHE_CONTROL.toString(), "max-age=3600");
                        handler.addHeaderToResponse("oldUrl", handler.getUri());
                        handler.sendFile(file, true);
                    } else {
                        handler.sendData(HttpResponseStatus.NOT_FOUND, "404 资源未找到", false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doRequestClient(HttpServerHandler handler) {
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
                    httpServerHandler.sendRedirect("/upload/" + handler.getFileUploads().get(0).getName());
                }
                ServerManager.getLogger().info("fromClient:" + handler.getFileUploads());
            } else {
                ServerManager.getLogger().info("fromClient:不存在" + uid);
            }
        } else {
            String body = handler.getBody(Charset.defaultCharset());
            try {
                if (body == null || body.length() < 1) {
                    HttpServerHandler httpServerHandler = httpProxy.get(uid);
                    if (httpServerHandler != null) {
                        handler.sendData(httpServerHandler.getBody(Charset.defaultCharset()), true);
                    }
                } else {
                    HttpServerHandler httpServerHandler = httpProxy.remove(uid);
                    httpServerHandler.sendData(handler.getBody(Charset.defaultCharset()), true);
                    handler.sendData("", false);
                }
            } catch (Exception e) {
                ServerManager.getLogger().warning(e.getLocalizedMessage());
            }
            ServerManager.getLogger().info("fromClient:" + body);
        }
    }

    public void cleanOfflineClient() {
        for (Map.Entry<String, Client> stringClientEntry : clients.entrySet()) {
            if (stringClientEntry.getValue().isOffLine()) {
                clients.remove(stringClientEntry.getKey());
                ServerManager.getLogger().info("Clean Client:" + stringClientEntry.getValue().getClientId());
            }
        }
    }

    private void doRequestWeb(HttpServerHandler handler) throws UnsupportedEncodingException {
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

    private void doRequestWeb2(HttpServerHandler handler, Client client) throws UnsupportedEncodingException {
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

    public boolean auth(String clientId, String code, String remoteUrl) {
        return sendCommand(clientId, new Command().setAction(AUTH).setData(code).setClientId(clientId).setRemoteUrl(remoteUrl));
    }

    public boolean sendCommand(String clientId, Command command) {
        Client client = clients.get(clientId);
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

    public void stop() {
        packageServerManager.stop();
        alarmThread.stop();
    }
}
