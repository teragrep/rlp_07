package com.teragrep.rlp_07;
public class Config {
    public final int port;
    public final boolean isTls;
    public final String keystorePassword;
    public final String keystorePath;
    public final String loglevel;
    public Config() {
        try {
            port = Integer.parseInt(System.getProperty("port", "1601"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Can't parse port: " + e.getMessage());
        }
        isTls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        keystorePassword = System.getProperty("tlsKeystorePassword", "changeit");
        keystorePath = System.getProperty("tlsKeystore", null);
        loglevel = System.getProperty("loglevel");
    }
}
