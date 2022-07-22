package com.teragrep.java_relp_server_demo;

import com.teragrep.rlp_03.FrameProcessor;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.SyslogFrameProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException,
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
}
