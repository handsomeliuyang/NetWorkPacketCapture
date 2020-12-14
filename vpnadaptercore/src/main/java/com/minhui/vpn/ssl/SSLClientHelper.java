package com.minhui.vpn.ssl;

import com.minhui.vpn.VPNLog;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLClientHelper extends NioSslPeer {

    private static final String TAG = SSLClientHelper.class.getSimpleName();

    private SSLEngine engine;

    public SSLClientHelper(String protocol, String remoteAddress, int port) throws Exception {

        SSLContext sslContext = SSLContext.getInstance(protocol);
        // 客户端不验证证书，信任所有的证书
        sslContext.init(null,
                new TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        //
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        //
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }},
                new SecureRandom());
        engine = sslContext.createSSLEngine(remoteAddress, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        myNetData = ByteBuffer.allocate(BUFFER_SIZE);
        // 扩大内存，解决读数据失败问题
        peerNetData = ByteBuffer.allocate(BUFFER_SIZE*2);
    }

    public SSLEngine getEngine(){
        return engine;
    }

    public boolean connect(SocketChannel socketChannel) throws Exception {
//        socketChannel = SocketChannel.open();
//        socketChannel.configureBlocking(false);
//        socketChannel.connect(new InetSocketAddress(remoteAddress, port));
//        // 等待TCP连接建立，即三次握手完成
//        while (!socketChannel.finishConnect()) {
//            // can do something here...
//        }
        // TCP建立连接后，进行SSL握手
        engine.beginHandshake();
        return doHandshake(socketChannel, engine);
    }

    public void shutdown() {
        executor.shutdown();
        VPNLog.d(TAG, "Goodbye!");
    }

}
