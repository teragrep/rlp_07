package com.teragrep.java_relp_server_demo;

import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException,
            InterruptedException, GeneralSecurityException {
        // no arguments, use plain server on port 1601
       if (args.length == 0) {
           plainServer();
       }
       else if (args.length == 1){
           // if --tls argument given, use tls server on port 1602
           if (args[0].equalsIgnoreCase("--tls")) {
               tlsServer();
           }
       }
       else {
           throw new IllegalArgumentException("expected max 1 args, got: " + args.length);
       }
    }

    private static void plainServer() throws IOException,
            InterruptedException {
        Consumer<byte[]> byteConsumer = bytes -> {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOGGER.info(message);
        };

        FrameProcessor syslogFrameProcessor =
                new SyslogFrameProcessor(byteConsumer);

        Server relpServer = new Server(1601, syslogFrameProcessor);

        relpServer.start();

        Thread.sleep(Long.MAX_VALUE);
    }

    private static void tlsServer() throws
            InterruptedException, GeneralSecurityException {
        System.out.println("tls server");
        Consumer<byte[]> byteConsumer = bytes -> {
            String message = new String(bytes, StandardCharsets.UTF_8);
            LOGGER.info(message);
        };

        FrameProcessor syslogFrameProcessor =
                new SyslogFrameProcessor(byteConsumer);

        SSLContext sslContext;
        try {
            sslContext = SSLContextFactory.demoContext();//SSLContextFactory.authenticatedContext("serverkeystore.jks", "password", "TLSv1.2");
        } catch (IOException e) {
            throw new RuntimeException("SSL.demoContext Error: " + e);
        }

        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm(null);
            sslEngine.setSSLParameters(sslParameters);
            sslEngine.setNeedClientAuth(false);
            sslEngine.setUseClientMode(false);
            sslEngine.setEnabledProtocols(new String[]{"TLSv1.3"});
            sslEngine.setEnabledCipherSuites(new String[]{"TLS_AES_256_GCM_SHA384"});
            return sslEngine;
        };

        Server relpServer = new Server(1602, syslogFrameProcessor, sslContext, sslEngineFunction);

        try {
            relpServer.start();
        } catch (IOException e) {
            throw new RuntimeException("RelpServer.Start error: " + e);
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
