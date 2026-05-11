package com.teragrep.rlp_07;

import ch.qos.logback.classic.Level;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.channel.socket.TLSFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import ch.qos.logback.classic.LoggerContext;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Consumer<FrameContext> syslogConsumer = frameContext -> LOGGER.info(frameContext.relpFrame().payload().toString());

    private static final Supplier<FrameDelegate> frameDelegateSupplier = () -> {
        LOGGER.debug("Providing frameDelegate for a connection");
        return new DefaultFrameDelegate(syslogConsumer);
    };

    private static final EventLoopFactory eventLoopFactory = new EventLoopFactory();

    static Config config;

    public static void main(String[] args) {

        config = new Config();
        if (config.loglevel != null) {
            LOGGER.debug("Setting loglevel to <[{}]>", config.loglevel);
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger logger = loggerContext.getLogger("com.teragrep");

            logger.setLevel(Level.toLevel(config.loglevel.toUpperCase()));
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        final EventLoop eventLoop;
        try {
            eventLoop = eventLoopFactory.create();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Thread eventLoopThread = new Thread(eventLoop);

        eventLoopThread.start();

        final SocketFactory socketFactory;

        if (config.isTls) {
            socketFactory = tlsServer();
        }
        else {
            LOGGER.debug("Starting plain server on port <[{}]>", config.port);
            socketFactory = new PlainFactory();
        }

        final ServerFactory serverFactory = new ServerFactory(
                eventLoop, executorService, socketFactory,
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );

        try {

            serverFactory.create(config.port);
        }
        catch (IOException e) {
            LOGGER.error("Failed to run: <[{}]>", e.getMessage(), e);
            throw new UncheckedIOException(e);
        }

        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.debug("Stopping server at port <[{}]>", config.port);

            latch.countDown();
        }));

        while (true)
            try {
                latch.await();
                break;
            }
            catch (InterruptedException e) {
                LOGGER.debug("Interruption in main thread latch.await(), retrying", e);
            }


        eventLoop.stop();
        try {
            eventLoopThread.join();
        }
        catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }

        LOGGER.debug("Server stopped at port <[{}]>", config.port);

        executorService.shutdown();
    }


    private static TLSFactory tlsServer() {
        LOGGER.debug("Starting TLS server on port <[{}]>", config.port);

        final InputStream keystoreStream;
        if(config.keystorePath != null) {
            LOGGER.debug("Using user supplied keystore");
            Path path = Paths.get(config.keystorePath);
            if(!path.toFile().exists()) {
                throw new RuntimeException("File " + config.keystorePath + " doesn't exist");
            }
            try {
                keystoreStream = Files.newInputStream(path);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        else {
            LOGGER.debug("Using default keystore");
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Function<SSLContext, SSLEngine> sslEngineFunction = sslCtx -> {
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            return sslEngine;
        };

        return new TLSFactory(sslContext, sslEngineFunction);
    }
}
