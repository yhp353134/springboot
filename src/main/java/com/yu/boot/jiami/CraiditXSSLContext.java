package com.yu.boot.jiami;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;


class CraiditXSSLContext {

    private KeyManager[] keyManager = null;
    private List<TrustManager> trustManager = new ArrayList();

    public CraiditXSSLContext() throws GeneralSecurityException, IOException {
        this.addTrustStore((KeyStore) null);
    }

    public SSLSocketFactory getSocketFactory()
        throws GeneralSecurityException, IOException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        TrustManager tm = this.getTrustManager();
        TrustManager[] tms = tm == null ? null : new TrustManager[] { tm };
        sslContext.init(this.keyManager, tms, null);
        return sslContext.getSocketFactory();
    }

    public X509TrustManager getTrustManager() {
        if (this.trustManager == null || this.trustManager.size() == 0) {
            return null;
        }
        return new CompositeX509TrustManager(this.trustManager);
    }

    public CraiditXSSLContext setClientCert(
        String clientKeyStorePath, String clientKeyStorePwd
    ) throws GeneralSecurityException, IOException {
        return this.setClientCert(clientKeyStorePath, clientKeyStorePwd, "PKCS12");
    }

    public CraiditXSSLContext setClientCert(
        String clientKeyStorePath, String clientKeyStorePwd, String keyType
    ) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(keyType);
        ks.load(new FileInputStream(clientKeyStorePath),
                clientKeyStorePwd.toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, clientKeyStorePwd.toCharArray());
        this.keyManager = kmf.getKeyManagers();
        return this;
    }

    public CraiditXSSLContext addServerCAFile(
        String serverTrustStorePath, String serverTrustStorePwd
    ) throws GeneralSecurityException, IOException {
        return this.addServerCAFile(serverTrustStorePath, serverTrustStorePwd, "JKS");
    }

    public CraiditXSSLContext addServerCAFile(
        String serverTrustStorePath, String serverTrustStorePwd, String keyType
    ) throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(keyType);
        ks.load(new FileInputStream(serverTrustStorePath),
                serverTrustStorePwd.toCharArray());
        return this.addTrustStore(ks);
    }

    public CraiditXSSLContext addTrustStore(KeyStore keyStore)
        throws GeneralSecurityException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);
        for (TrustManager tm : tmf.getTrustManagers()) {
            this.trustManager.add(tm);
        }
        return this;
    }
}
