package com.teragrep.java_relp_server_demo;

import com.teragrep.rlp_01.SSLContextFactory;
import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;
import java.util.function.Function;

class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException,
            InterruptedException, GeneralSecurityException {
        int port = 1601;
        boolean tlsMode = false;

        for (String arg : args) {
            if (arg.startsWith("port=")) {
                port = Integer.parseInt(arg.substring(arg.indexOf('=')+1));
                LOGGER.info("Got port parameter: " + port);
            }
            else if (arg.startsWith("tls=")) {
                String value = arg.substring(arg.indexOf('=')+1);
                if (value.equalsIgnoreCase("true")) {
                    tlsMode = true;
                    LOGGER.info("Got TLS mode parameter. Enabling TLS mode.");
                }
            }
        }

        if (tlsMode) {
            tlsServer(port);
        } else {
            plainServer(port);
        }
    }

    private static void plainServer(int port) throws IOException,
            InterruptedException {
        LOGGER.info("plain server on port " + port);
        Consumer<byte[]> byteConsumer = bytes -> {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOGGER.info(message);
        };

        FrameProcessor syslogFrameProcessor =
                new SyslogFrameProcessor(byteConsumer);

        Server relpServer = new Server(port, syslogFrameProcessor);

        relpServer.start();

        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer(int port) throws
            InterruptedException, GeneralSecurityException {
        LOGGER.info("tls server on port " + port);
        Consumer<byte[]> byteConsumer = bytes -> {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOGGER.info(message);
        };

        FrameProcessor syslogFrameProcessor =
                new SyslogFrameProcessor(byteConsumer);

        SSLContext sslContext;
        try {
            sslContext = SSLContextFactory.authenticatedContext(
                    "src/main/resources/keystore-server.jks", "changeit", "TLSv1.3");
        } catch (IOException e) {
            throw new RuntimeException("SSL.demoContext Error: " + e);
        }

        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };

        Server relpServer = new Server(port, syslogFrameProcessor, sslContext, sslEngineFunction);

        try {
            relpServer.start();
        } catch (IOException e) {
            throw new RuntimeException("RelpServer.Start error: " + e);
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
