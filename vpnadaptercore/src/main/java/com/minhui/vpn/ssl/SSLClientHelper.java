package com.minhui.vpn.ssl;

import android.content.Context;

import com.minhui.vpn.VPNLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLClientHelper extends NioSslPeer {

    private static final String TAG = SSLClientHelper.class.getSimpleName();

    private String remoteAddress;
    private int port;
    private SSLEngine engine;
//    private SocketChannel socketChannel;

    public SSLClientHelper(String protocol, String remoteAddress, int port) throws Exception {

        this.remoteAddress = remoteAddress;
        this.port = port;

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
        myAppData = ByteBuffer.allocate(BUFFER_SIZE);
        myNetData = ByteBuffer.allocate(BUFFER_SIZE);

        peerAppData = ByteBuffer.allocate(BUFFER_SIZE);
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

//    public void read() throws Exception {
//        read(socketChannel, engine);
//    }
//
//    public void write(String message) throws IOException {
//        write(socketChannel, engine, message);
//    }

//    @Override
//    protected void write(SocketChannel socketChannel, SSLEngine engine, String message) throws IOException {
//
//        VPNLog.d(TAG, "About to write to the server...");
//
//        myAppData.clear();
//        myAppData.put(message.getBytes());
//        myAppData.flip();
//        while (myAppData.hasRemaining()) {
//            // The loop has a meaning for (outgoing) messages larger than 16KB.
//            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
//            myNetData.clear();
//            SSLEngineResult result = engine.wrap(myAppData, myNetData);
//            switch (result.getStatus()) {
//                case OK:
//                    myNetData.flip();
//                    while (myNetData.hasRemaining()) {
//                        socketChannel.write(myNetData);
//                    }
//                    VPNLog.d(TAG,"Message sent to the server: " + message);
//                    break;
//                case BUFFER_OVERFLOW:
//                    myNetData = enlargePacketBuffer(engine, myNetData);
//                    break;
//                case BUFFER_UNDERFLOW:
//                    throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
//                case CLOSED:
//                    closeConnection(socketChannel, engine);
//                    return;
//                default:
//                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
//            }
//        }
//
//    }

//    @Override
//    protected void read(SocketChannel socketChannel, SSLEngine engine) throws Exception {
//        VPNLog.d(TAG, "About to read from the server...");
//
//        peerNetData.clear();
//        int waitToReadMillis = 50;
//        boolean exitReadLoop = false;
//        while (!exitReadLoop) {
//            int bytesRead = socketChannel.read(peerNetData);
//
//            VPNLog.d(TAG,"read data bytesRead=" + bytesRead);
//
//            if (bytesRead > 0) {
//                peerNetData.flip();
//                while (peerNetData.hasRemaining()) {
//                    VPNLog.d(TAG,"peerNetData hasRemaining " + peerNetData);
//
//                    peerAppData.clear();
//                    SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
//
//                    VPNLog.d(TAG,"engine.unwrap result " + result);
//                    VPNLog.d(TAG, "unwrap after peerNetData=" + peerNetData + "; peerAppData=" + peerAppData);
//
//                    switch (result.getStatus()) {
//                        case OK:
//                            peerAppData.flip();
//                            VPNLog.d(TAG, "Server response: " + new String(peerAppData.array()));
//                            exitReadLoop = true;
//                            break;
//                        case BUFFER_OVERFLOW:
//                            peerAppData = enlargeApplicationBuffer(engine, peerAppData);
//                            break;
//                        case BUFFER_UNDERFLOW:
////                            peerNetData = handleBufferUnderflow(engine, peerNetData);
//                            break;
//                        case CLOSED:
//                            closeConnection(socketChannel, engine);
//                            return;
//                        default:
//                            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
//                    }
//                }
//            } else if (bytesRead < 0) {
//                handleEndOfStream(socketChannel, engine);
//                return;
//            }
//            Thread.sleep(waitToReadMillis);
//        }
//    }

    public void shutdown() {
        executor.shutdown();
        VPNLog.d(TAG, "Goodbye!");
    }

}
