package com.yu.boot.jiami;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用一组X509证书链校验证书的合法性。
 *
 * {@code SSLContext}不支持多个{@code X509TrustManager}，详见{@link SSLContext#init}:
 *     Only the first instance of a particular key and/or trust
 *     manager implementation type in the array is used. (For example,
 *     only the first javax.net.ssl.X509TrustManager in the array will
 *     be used.)
 */
public class CompositeX509TrustManager implements X509TrustManager {

    private final List<X509TrustManager> trustManagers;

    public CompositeX509TrustManager(List<TrustManager> trustManagers) {
        this.trustManagers = new ArrayList();
        for (TrustManager km : trustManagers) {
            if (km instanceof X509TrustManager) {
                this.trustManagers.add((X509TrustManager) km);
            }
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;         // 证书被信任
            } catch (CertificateException e) {
                // 继续尝试其它trustManager
            }
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
        for (X509TrustManager trustManager : trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;         // 证书被信任
            } catch (CertificateException e) {
                // 继续尝试其它trustManager
            }
        }
        throw new CertificateException("None of the TrustManagers trust this certificate chain");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> certificates = new ArrayList();
        for (X509TrustManager trustManager : trustManagers) {
            for (X509Certificate cert : trustManager.getAcceptedIssuers()) {
                certificates.add(cert);
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }
}
