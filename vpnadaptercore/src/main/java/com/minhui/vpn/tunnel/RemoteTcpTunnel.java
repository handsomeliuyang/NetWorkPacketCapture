package com.minhui.vpn.tunnel;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.minhui.vpn.VPNConstants;
import com.minhui.vpn.VPNLog;
import com.minhui.vpn.http.HttpRequestHeaderParser;
import com.minhui.vpn.nat.NatSession;
import com.minhui.vpn.nat.NatSessionManager;
import com.minhui.vpn.processparse.PortHostService;
import com.minhui.vpn.utils.ACache;
import com.minhui.vpn.utils.TcpDataSaveHelper;
import com.minhui.vpn.utils.ThreadProxy;
import com.minhui.vpn.utils.TimeFormatUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zengzheying on 15/12/31.
 */
public class RemoteTcpTunnel extends RawTcpTunnel {
    TcpDataSaveHelper helper;
    NatSession session;
    private final Handler handler;
    private String namePre;
    private SimpleDateFormat DATA_FORMAT = new SimpleDateFormat("dd_HH_mm_ss_SSS");

    public RemoteTcpTunnel(InetSocketAddress serverAddress, Selector selector, short portKey) throws IOException {
        super(serverAddress, selector, portKey);
        session = NatSessionManager.getSession(portKey);
        String helperDir = new StringBuilder()
                .append(VPNConstants.DATA_DIR)
                .append(TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime))
//                .append("/")
//                .append(session.getUniqueName())
                .toString();

        helper = new TcpDataSaveHelper(helperDir);
        handler = new Handler(Looper.getMainLooper());

        VPNLog.d(this.getClass().getSimpleName(), "***** Save helper " + helper.toString());
    }


    @Override
    protected ByteBuffer afterReceived(ByteBuffer buffer) throws Exception {
        buffer = super.afterReceived(buffer);
        refreshSessionAfterRead(buffer.limit());
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .namePre(namePre)
                .isRequest(false)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();
        helper.addData(saveData);
        return buffer;
    }

    @Override
    protected ByteBuffer beforeSend(ByteBuffer buffer) throws Exception {
        // 如果是Https请求，只有ssl握手成功后，且解密后，才能得到请求的url
        if(session.isHttpsSession) {
            HttpRequestHeaderParser.parseHttpsHostAndRequestUrl(session, buffer.array());
        }

        namePre = session.getRequestUrl().split("\\?")[0]; // DATA_FORMAT.format(new Date());
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .isRequest(true)
                .namePre(namePre)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();

        // 走本地缓存，本地加载Response，直接返回，不再向RemoteServer发送数据
        if(helper.existResponse(saveData)) {
            // 加载本地response缓存，直接返回
            ByteBuffer responseBuffer = helper.loadCashedResponse(saveData);
            responseBuffer.flip();
            this.sendToBrother(null, responseBuffer);

            VPNLog.d(this.getClass().getSimpleName(), "***** has response " + responseBuffer);

            // 不再加密，也不发数据
            buffer.clear();
            return buffer;
        }

        helper.addData(saveData);
        refreshAppInfo();
        // 先保存再加密
        buffer = super.beforeSend(buffer);
        return buffer;
    }

    private void refreshAppInfo() {
        if (session.appInfo != null) {
            return;
        }
        if (PortHostService.getInstance() != null) {
            ThreadProxy.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    PortHostService.getInstance().refreshSessionInfo();
                }
            });
        }
    }

    private void refreshSessionAfterRead(int size) {

        session.receivePacketNum++;
        session.receiveByteNum += size;

    }

    @Override
    protected void onDispose() {
        super.onDispose();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ThreadProxy.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (session.receiveByteNum == 0 && session.bytesSent == 0) {
                            return;
                        }

                        String configFileDir = VPNConstants.CONFIG_DIR
                                +TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime) ;
                        File parentFile = new File(configFileDir);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        //说已经存了
                        File file = new File(parentFile, session.getUniqueName());
                        if (file.exists()) {
                            return;
                        }
                        ACache configACache = ACache.get(parentFile);
                        configACache.put(session.getUniqueName(), session);
                    }
                });
            }
        }, 1000);
    }
}
