package com.teragrep.rlp_07;

import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;
import java.util.function.Function;

class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Consumer<byte[]> byteConsumer = bytes -> {
        String message = new String(bytes, StandardCharsets.UTF_8);
        LOGGER.info(message);
    };
    private static final FrameProcessor syslogFrameProcessor = new SyslogFrameProcessor(byteConsumer);
    static Config config;

    public static void main(String[] args) {
        config = new Config();
        if(config.loglevel != null) {
            LOGGER.info("Setting loglevel to <[{}]>", config.loglevel);
            Configurator.setAllLevels("com.teragrep", Level.valueOf(config.loglevel));
        }
        try {
            if (config.isTls) {
                tlsServer();
            } else {
                plainServer();
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to run: <{}>", e.getMessage(), e);
        }
    }

    private static void plainServer() throws IOException, InterruptedException {
        LOGGER.info("Starting plain server on port <[{}]>", config.port);
        Server relpServer = new Server(config.port, syslogFrameProcessor);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer() throws IOException, InterruptedException {
        LOGGER.info("Starting TLS server on port <[{}]>", config.port);

        InputStream keystoreStream;
        if(config.keystorePath != null) {
            LOGGER.info("Using user supplied keystore");
            Path path = Paths.get(config.keystorePath);
            if(!path.toFile().exists()) {
                throw new RuntimeException("File " + config.keystorePath + " doesn't exist");
            }
            keystoreStream = Files.newInputStream(path);
        }
        else {
            LOGGER.info("Using default keystore");
            // get server keyStore as inputstream, works on JAR packaging as well this way
            keystoreStream = Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
        }

        SSLContext sslContext;
        try {
            sslContext = TLSContextFactory.authenticatedContext(
                keystoreStream,
                config.keystorePassword,
                "TLSv1.3"
            );
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Can't create sslContext: " + e);
        }

        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };

        Server relpServer = new Server(config.port, syslogFrameProcessor, sslContext, sslEngineFunction);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }
}
