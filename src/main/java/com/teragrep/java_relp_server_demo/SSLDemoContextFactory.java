package com.teragrep.java_relp_server_demo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SSLDemoContextFactory {
    public static SSLContext authenticatedContext(
            InputStream keyStoreStream,
            String keystorePassword,
            String protocol
    ) throws GeneralSecurityException, IOException {

        SSLContext sslContext = SSLContext.getInstance(protocol);
        KeyStore ks = KeyStore.getInstance("JKS");

        ks.load(keyStoreStream, keystorePassword.toCharArray());
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keystorePassword.toCharArray());
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext;

    }
}
