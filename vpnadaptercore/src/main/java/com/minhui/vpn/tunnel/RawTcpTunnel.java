package com.minhui.vpn.tunnel;

import com.minhui.vpn.VPNLog;
import com.minhui.vpn.service.FirewallVpnService;
import com.minhui.vpn.ssl.SSLClientHelper;
import com.minhui.vpn.ssl.SSLServerHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

/**
 * Created by zengzheying on 15/12/30.
 */
public class RawTcpTunnel extends TcpTunnel {

	private static final String TAG = RawTcpTunnel.class.getSimpleName();

	private final boolean mIsServer;
	private String remoteAddress;
	private boolean isTunnelEstablished = false;

	private SSLServerHelper mSslServerHelper;
	private SSLClientHelper mSslClientHelper;

	public RawTcpTunnel(SocketChannel innerChannel, Selector selector) {
		super(innerChannel, selector);
		mIsServer = true;

	}

	public RawTcpTunnel(InetSocketAddress serverAddress, Selector selector, short portKey) throws IOException {
		super(serverAddress, selector, portKey);
		mIsServer = false;
		remoteAddress = serverAddress.getHostString();
	}

	@Override
	protected void onConnected() throws Exception {
		if(!isHttpsRequest()) {
			onTunnelEstablished(null);
			return ;
		}

		// Client
		mSslClientHelper = new SSLClientHelper("TLSv1.2", remoteAddress, portKey);
		isTunnelEstablished = mSslClientHelper.connect(mInnerChannel);
		if(!isTunnelEstablished) {
			throw new IllegalStateException("client ssl handshake has error");
		}

		VPNLog.d(this.getClass().getSimpleName(), "1111111 Client SSL Handshake success");

		onTunnelEstablished(mSslClientHelper.getEngine());
	}

	@Override
	protected void beginReceived(SSLEngine engine) throws Exception {
		super.beginReceived(engine);

		if(!isHttpsRequest() || !mIsServer) {
			return ;
		}

		// https请求，且是Server端，需要先阻塞式SSL握手
		mSslServerHelper = new SSLServerHelper(engine.getSession());
		isTunnelEstablished = mSslServerHelper.doHandshake(mInnerChannel);

		if(!isTunnelEstablished) {
			throw new IllegalStateException("server ssl handshake has error");
		}

		VPNLog.d(this.getClass().getSimpleName(), "22222222 Server SSL Handshake success");
	}

	@Override
	protected boolean isTunnelEstablished() {
		if(!isHttpsRequest()){
			return true;
		}
		return isTunnelEstablished;
	}

	@Override
	protected ByteBuffer beforeSend(ByteBuffer buffer) throws Exception {
		if(!isHttpsRequest()){
			return buffer;
		}

		SSLEngine engine = mIsServer ? mSslServerHelper.getEngine() : mSslClientHelper.getEngine();
		return wrap(buffer, engine);
	}

	private ByteBuffer wrap(ByteBuffer buffer, SSLEngine engine) throws SSLException {

		ByteBuffer wrapBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());

		while(true) {

			SSLEngineResult result = engine.wrap(buffer, wrapBuffer);
			VPNLog.d(TAG,"engine.wrap result " + result);

			switch (result.getStatus()) {
				case OK:
					wrapBuffer.flip();

					// 回收内存
					buffer.clear();
					return wrapBuffer;
				case CLOSED:
					this.dispose();
					return buffer;
				case BUFFER_OVERFLOW:
					// 再循环
					int bufferSize = engine.getSession().getPacketBufferSize();
					if (bufferSize > wrapBuffer.capacity()) {
						wrapBuffer = ByteBuffer.allocate(bufferSize);
					}
					VPNLog.d(TAG,"engine.wrap result BUFFER_OVERFLOW bufferSize=" + bufferSize + "；wrapBuffer=" + wrapBuffer);
					break;
				default:
					throw new IllegalStateException("wrap Invalid SSL status: " + result.getStatus());
			}
		}

	}


	@Override
	protected ByteBuffer afterReceived(ByteBuffer buffer) throws Exception {
		if(!isHttpsRequest()){
			return buffer;
		}

		SSLEngine engine = mIsServer ? mSslServerHelper.getEngine() : mSslClientHelper.getEngine();
		return unwrap(buffer, engine);
	}

	private ByteBuffer unwrap(ByteBuffer buffer, SSLEngine engine) throws SSLException {
		ByteBuffer unwrapBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
		unwrapBuffer.clear();

		SSLEngineResult result = engine.unwrap(buffer, unwrapBuffer);
		VPNLog.d(TAG,"engine.unwrap result " + result);

		switch (result.getStatus()) {
			case OK:
				unwrapBuffer.flip();
				// 回收内存
				buffer.clear();
				return unwrapBuffer;
			case CLOSED:
				this.dispose();
				return buffer;
			default:
				throw new IllegalStateException("unwrap Invalid SSL status: " + result.getStatus());
		}
	}

	@Override
	protected void onDispose() {
		if(mSslServerHelper != null) {
			mSslServerHelper.stop();
		}
		if(mSslClientHelper != null) {
			mSslClientHelper.shutdown();
		}
	}
}
