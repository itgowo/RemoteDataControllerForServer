package com.itgowo.remoteserver;

import com.itgowo.servercore.http.HttpServerHandler;
import com.itgowo.servercore.packagesocket.PackageServerHandler;
import com.itgowo.servercore.socket.SocketServerHandler;

public class Client {
    private String clientId;
    private PackageServerHandler handler;
    private HttpServerHandler httpHandler;
    private boolean auth = false;
    private String token;
    private long lastMsgTime=System.currentTimeMillis();

    public void refreshLastMsgTime() {
        lastMsgTime = System.currentTimeMillis();
    }

    public boolean isOffLine() {
        return System.currentTimeMillis() - lastMsgTime > 5 * 60 * 1000;
    }

    public HttpServerHandler getHttpHandler() {
        return httpHandler;
    }

    public Client setHttpHandler(HttpServerHandler httpHandler) {
        this.httpHandler = httpHandler;
        return this;
    }

    public Client(String clientId, PackageServerHandler handler) {
        this.clientId = clientId;
        this.handler = handler;
    }

    public String getToken() {
        return token;
    }

    public Client setToken(String token) {
        this.token = token;
        return this;
    }

    public boolean isAuth() {
        return auth;
    }

    public Client setAuth(boolean auth) {
        this.auth = auth;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public Client setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public PackageServerHandler getHandler() {
        return handler;
    }

    public Client setHandler(PackageServerHandler handler) {
        this.handler = handler;
        return this;
    }
}
