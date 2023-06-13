package com.teragrep.rlp_07;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static int port;
    public int getPort() {
        return port;
    }
    private static boolean isTls;
    public boolean isTls() {
        return isTls;
    }
    private static String keystorePassword;
    public String getKeystorePassword() {
        return keystorePassword;
    }
    public Config() {
        try {
            port = Integer.parseInt(System.getProperty("port", "1601"));
        }
        catch(NumberFormatException e) {
            throw new RuntimeException("Can't parse port: " + e.getMessage());
        }
        isTls = Boolean.parseBoolean(System.getProperty("tls", "false"));
        keystorePassword = System.getProperty("tlsKeystorePassword", "changeit");
    }

    public InputStream getKeystoreStream() throws IOException {
        String keystorePath = System.getProperty("tlsKeystore");
        if(keystorePath != null) {
            LOGGER.info("Using user supplied keystore");
            Path path = Paths.get(keystorePath);
            if(!path.toFile().exists()) {
                throw new RuntimeException("File " + keystorePath + " doesn't exist");
            }
            return Files.newInputStream(path);
        }
        else {
            LOGGER.info("Using default keystore");
            // get server keyStore as inputstream, works on JAR packaging as well this way
            return Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
        }
    }
}
