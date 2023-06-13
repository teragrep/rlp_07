package com.teragrep.rlp_07;
public class Config {
    private final int port;

    public int getPort() {
        return port;
    }

    private final boolean isTls;

    public boolean isTls() {
        return isTls;
    }

    private final String keystorePassword;

    public String getKeystorePassword() {
        return keystorePassword;
    }

    private final String keystorePath;
    public String getKeystorePath() {
        return keystorePath;
    }
    public Config() {
        try {
            port = Integer.parseInt(System.getProperty("port", "1601"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Can't parse port: " + e.getMessage());
        }
        isTls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        keystorePassword = System.getProperty("tlsKeystorePassword", "changeit");
        keystorePath = System.getProperty("tlsKeystore", null);
    }
}
