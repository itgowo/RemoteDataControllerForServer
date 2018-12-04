package com.itgowo.remoteserver;

import com.itgowo.actionframework.ServerManager;
import com.itgowo.servercore.ServerHandler;
import com.itgowo.servercore.SimpleServerListener;
import com.itgowo.servercore.http.HttpServerHandler;
import com.itgowo.servercore.http.HttpServerManager;
import com.itgowo.servercore.httpclient.HttpMethod;
import com.itgowo.servercore.packagesocket.PackageServerManager;
import com.itgowo.servercore.utils.LogU;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControllerServiceServer {
    public static final long ALARMINTERVAL = 5 * 60;
    public static final String CONNECT = "Connect";
    public static final String CONNECTED = "Connected";
    public static final String RECONNECTED = "ReConnected";
    public static final String CLOSE = "Close";
    public static final String ERROR = "Error";
    public static final String AUTH = "Auth";
    public static final String HEART = "Heart";
    public static final String REQUEST_PROXY = "RequestProxy";
    public static final String REQUEST_PROXY_UPLOAD = "RequestProxyUpload";
    public static final String REQUEST_PROXY_DOWNLOAD = "RequestProxyDownload";
    public static final String CLIENT = "Client";
    public static final String WEB = "Web";
    public static final String COMMAND = "Command";
    public static final String UID = "Uuid";
    public static final String CLIENTID = "ClientId";
    public static final String DOWNLOADFILE = "downloadFile";
    public static String webRootDir = BaseConfig.getRDCServerWebRootDir() + File.separator;

    private PackageServerManager packageServerManager = new PackageServerManager();
    private HttpServerManager httpServerManager = ServerManager.getHttpServerManager();
    private ScheduledExecutorService scheduledExecutorService=Executors.newScheduledThreadPool(0);

    /**
     * key为clientId;
     */
    public static ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
    public static HashMap<String, HttpServerHandler> httpProxy = new HashMap<>();
    private Logger logger = LogU.getLogU("RDC HttpServer", Level.ALL);

    private SocketPackageDispatcher socketDispatcher;

    public ControllerServiceServer setSocketDispatcher(SocketPackageDispatcher socketDispatcher) {
        this.socketDispatcher = socketDispatcher;
        return this;
    }

    public ConcurrentHashMap<String, Client> getClients() {
        return clients;
    }

    public HashMap<String, HttpServerHandler> getHttpProxy() {
        return httpProxy;
    }

    public void start() throws Exception {
        ServerManager.setLogger(logger);
        startHttpServer();
        startSocketServer();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                BaseRequestDispatcher.cleanOfflineClinet();
            }
        },ALARMINTERVAL,ALARMINTERVAL,TimeUnit.SECONDS);
    }

    private void startSocketServer() {
        socketDispatcher.setControllerServiceServer(ControllerServiceServer.this);
        packageServerManager.setOnServerListener(socketDispatcher);
        packageServerManager.setServerName("RDC SocketPackageServer");
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
            } else {
                BaseRequestDispatcher.dispatcherTask(handler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        packageServerManager.stop();
        httpServerManager.stop();
        scheduledExecutorService.shutdownNow();
    }


}
