package com.minhui.vpn.ssl;

import com.minhui.vpn.VPNLog;
import com.minhui.vpn.utils.VpnServiceHelper;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class SSLServerHelper extends NioSslPeer {

    private static final String TAG = SSLServerHelper.class.getSimpleName();

    private SSLContext context;
    private SSLEngine engine;

    public SSLServerHelper(SSLSession sslSession) throws Exception {

        X509Certificate upstreamCert = getCertificateFromSession(sslSession);
        String commonName = getCommonName(upstreamCert);
        SubjectAlternativeNameHolder san = new SubjectAlternativeNameHolder();
        san.addAll(upstreamCert.getSubjectAlternativeNames());

        Authority authority = new Authority(
            null,
                "server",
                "123456".toCharArray(),
                null,
                null,
                null,
                "ly proxy",
                "ly"
        );
        KeyStore ks = CertificateHelper.createServerCertificate(commonName,
                san, authority, VpnServiceHelper.getCaCert(), VpnServiceHelper.getCaPrivKey());
        KeyManager[] keyManagers = CertificateHelper.getKeyManagers(ks,
                authority);

        context = CertificateHelper.newServerContext(keyManagers);

        SSLSession dummySession = context.createSSLEngine().getSession();
        myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
        dummySession.invalidate();
    }

    private String getCommonName(X509Certificate c) {
        VPNLog.d(TAG, "Subject DN principal name: " + c.getSubjectDN().getName());
        for (String each : c.getSubjectDN().getName().split(",\\s*")) {
            if (each.startsWith("CN=")) {
                String result = each.substring(3);
                VPNLog.d(TAG,"Common Name: " + result);
                return result;
            }
        }
        throw new IllegalStateException("Missed CN in Subject DN: "
                + c.getSubjectDN());
    }

    private X509Certificate getCertificateFromSession(SSLSession sslSession)
            throws SSLPeerUnverifiedException {
        Certificate[] peerCerts = sslSession.getPeerCertificates();
        Certificate peerCert = peerCerts[0];
        if (peerCert instanceof X509Certificate) {
            return (X509Certificate) peerCert;
        }
        throw new IllegalStateException(
                "Required java.security.cert.X509Certificate, found: "
                        + peerCert);
    }

    public void stop() {
        executor.shutdown();
    }

    public boolean doHandshake(SocketChannel socketChannel) throws Exception {

        VPNLog.d(TAG, "New connection request!");
        engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();
        return doHandshake(socketChannel, engine);
    }

    public SSLEngine getEngine() {
        return engine;
    }
}
