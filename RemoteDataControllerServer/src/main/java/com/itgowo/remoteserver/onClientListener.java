package com.itgowo.remoteserver;

public interface onClientListener {
    void onAddCLient(String clientId);
    void onAuthClient(String clientId,String token);
}
