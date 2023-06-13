package com.teragrep.rlp_07;

import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = Integer.parseInt(System.getProperty("port", "1601"));
        boolean tlsMode = Boolean.parseBoolean(System.getProperty("tls", "false"));
        try {
            if (tlsMode) {
                tlsServer(port);
            } else {
                plainServer(port);
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to run: " + e.getMessage());
        }
    }

    private static void plainServer(int port) throws IOException, InterruptedException {
        LOGGER.info("Starting plain server on port " +port);
        Server relpServer = new Server(port, syslogFrameProcessor);
        relpServer.start();
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer(int port) throws IOException, InterruptedException {
        LOGGER.info("Starting TLS server on port " + port);
        String keystorePath = System.getProperty("tlsKeystore");
        InputStream keyStoreStream;
        if(keystorePath != null) {
            LOGGER.info("Using user supplied keystore");
            Path path = Paths.get(keystorePath);
            if(!path.toFile().exists()) {
                throw new RuntimeException("File " + keystorePath + " doesn't exist");
            }
            keyStoreStream = Files.newInputStream(path);
        }
        else {
            LOGGER.info("Using default keystore");
            // get server keyStore as inputstream, works on JAR packaging as well this way
            keyStoreStream = Main.class.getClassLoader().getResourceAsStream("keystore-server.jks");
        }
        SSLContext sslContext;
        try {
            sslContext = TLSContextFactory.authenticatedContext(
                keyStoreStream,
                "changeit",
                "TLSv1.3"
            );
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("SSL.demoContext Error: " + e);
        }

        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };

        Server relpServer = new Server(port, syslogFrameProcessor, sslContext, sslEngineFunction);

        relpServer.start();

        Thread.sleep(Long.MAX_VALUE);
    }
}
