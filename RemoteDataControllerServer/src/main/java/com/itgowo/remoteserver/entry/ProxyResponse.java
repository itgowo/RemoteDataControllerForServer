package com.itgowo.remoteserver.entry;

import com.alibaba.fastjson.JSON;

public class ProxyResponse {
    public static final int code_OK = 200;
    public static final int code_Error = 201;
    private Integer code = code_OK;
    private String msg = "success";
    private String action;
    private String clientId;
    private String remoteUrl;
    private String data;
    private String token;

    public String getToken() {
        return token;
    }

    public ProxyResponse setToken(String token) {
        this.token = token;
        return this;
    }

    public Integer getCode() {
        return code;
    }

    public ProxyResponse setCode(Integer code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public ProxyResponse setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public String getAction() {
        return action;
    }

    public ProxyResponse setAction(String action) {
        this.action = action;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public ProxyResponse setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public ProxyResponse setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        return this;
    }

    public String getData() {
        return data;
    }

    public ProxyResponse setData(String data) {
        this.data = data;
        return this;
    }
    public String toJson() {
        return JSON.toJSONString(this);
    }
}
