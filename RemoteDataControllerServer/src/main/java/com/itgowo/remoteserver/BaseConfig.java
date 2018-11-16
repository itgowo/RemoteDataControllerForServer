package com.itgowo.remoteserver;

public class BaseConfig extends com.itgowo.actionframework.base.BaseConfig {
    private static final String CONFIG_WEB_SERVER_DEFAULT_URL = "WebServerDefaultUrl";
    private static final String CONFIG_PROXY_SERVER_MAXIMUM_SURVIVAL_TIME = "ProxyServerMaximumSurvivalTime";
    private static final String CONFIG_PROXY_SERVER_SLEEP_TIME = "ProxyServerSleepTime";
    private static final String CONFIG_RDC_SERVER_DEFAULT_PORT = "RDCServerDefaultPort";
    private static final String CONFIG_RDC_SERVER_WEB_ROOT = "RDCServerWebDir";
    private static final String CONFIG_RDC_SERVER_WEB_CACHE_CONTROL = "RDCServerWebCacheControl";

    public static String getWebServerDefaultUrl() {
        return getProperty(CONFIG_WEB_SERVER_DEFAULT_URL, "");
    }

    public static int getRDCServerDefaultPort() {
        return getProperty(CONFIG_RDC_SERVER_DEFAULT_PORT, 16671);
    }

    public static int getProxyServerMaximumSurvivalTime() {
        return getProperty(CONFIG_PROXY_SERVER_MAXIMUM_SURVIVAL_TIME, 1000 * 60 * 60 * 8);
    }

    public static long getProxyServerSleepTime() {
        return getProperty(CONFIG_PROXY_SERVER_SLEEP_TIME, 200l);
    }

    public static String getRDCServerWebRootDir() {
        return getProperty(CONFIG_RDC_SERVER_WEB_ROOT, "");
    }

    public static boolean getRDCServerWebCacheControl() {
        return getProperty(CONFIG_RDC_SERVER_WEB_CACHE_CONTROL, true);
    }
}
