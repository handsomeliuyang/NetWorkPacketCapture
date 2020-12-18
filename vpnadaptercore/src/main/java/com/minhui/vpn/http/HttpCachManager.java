package com.minhui.vpn.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.minhui.vpn.VPNConstants;
import com.minhui.vpn.VPNLog;
import com.minhui.vpn.nat.NatSession;
import com.minhui.vpn.processparse.AppInfo;
import com.minhui.vpn.processparse.PortHostService;
import com.minhui.vpn.utils.ACache;
import com.minhui.vpn.utils.SerializeUtils;
import com.minhui.vpn.utils.TcpDataSaveHelper;
import com.minhui.vpn.utils.ThreadProxy;
import com.minhui.vpn.utils.TimeFormatUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HttpCachManager {
    private static final String TAG = HttpCachManager.class.getSimpleName();

    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";

    public static void saveHttpData(HttpData httpData) {

        // 如果是Request时，优先保存Http的简介信息
        if(httpData.isRequest) {
            saveHttpBrief(httpData);
        }

        // 保存Request数据
        File childFile = fileName(httpData.vpnStartTime, httpData.uniqueName, httpData.isRequest);
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(childFile);
            fileOutputStream.write(httpData.needParseData);
            fileOutputStream.flush();
        } catch (Exception e) {
            VPNLog.d(TAG, "failed to saveData" + e.getMessage());
        } finally {
            if(fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    VPNLog.d(TAG, "failed to close closeable");
                }
            }
        }
    }

    private static void saveHttpBrief(HttpData httpData) {
        String fileDir = VPNConstants.CONFIG_DIR
                + TimeFormatUtil.formatYYMMDDHHMMSS(httpData.vpnStartTime);

        SerializeUtils serializeUtils = new SerializeUtils(fileDir);
        serializeUtils.serialize(httpData.getUniqueName(), httpData);
    }

    public static List<HttpData> loadHttpData(String fileDir) {
        ArrayList<HttpData> result = new ArrayList<HttpData>();
        File file = new File(fileDir);
        String[] list = file.list();
        if (list == null || list.length == 0) {
            return result;
        }

        SerializeUtils serializeUtils = new SerializeUtils(fileDir);
        for (String fileName : list) {
            HttpData httpData = (HttpData) serializeUtils.deserializeAsObject(fileName);
            result.add(httpData);
        }
        Collections.sort(result, new HttpData.Comparator());
        return result;
    }

    public static boolean existResponse(HttpData httpData) {
        File childFile = fileName(httpData.vpnStartTime, httpData.uniqueName, httpData.isRequest);
        return childFile.exists();
    }

    private static File fileName(long vpnStartTime, String uniqueName, boolean isRequest){
        // 保存Request数据
        String dir = VPNConstants.DATA_DIR + TimeFormatUtil.formatYYMMDDHHMMSS(vpnStartTime);
        return fileName(dir, uniqueName, isRequest);
    }
    public static File fileName(String dir, String uniqueName, boolean isRequest){
        // 保存Request数据
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }

        String childName = uniqueName + "_" + (isRequest ? REQUEST : RESPONSE);
        return new File(file, childName);
    }

    public static ByteBuffer loadCashedResponse(HttpData httpData) {
        File responseFile = fileName(httpData.vpnStartTime, httpData.uniqueName, false);
        RandomAccessFile aFile = null;
        try {
            aFile = new RandomAccessFile(responseFile, "r");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buf = ByteBuffer.allocate((int) inChannel.size() + 5);
            inChannel.read(buf);
            return buf;
        } catch (Exception e) {
            VPNLog.w(TAG, "failed to loadCashedResponse", e);
            return null;
        } finally {
            if(aFile != null) {
                try {
                    aFile.close();
                } catch (IOException e) {
                    VPNLog.w(TAG, "failed to close", e);
                }
            }
        }
    }

    public static class HttpData implements Serializable {
        final String requestUrl;
        final String ipAndPort;
        final long httpStartTime;
        final long vpnStartTime;
        final short localPort;
        final String remoteHost;
        final boolean isHttps;
        final String uniqueName; // 保存的文件名
        final byte[] needParseData;
        final boolean isRequest;
        final int length;

        public HttpData(HttpData.Builder builder) {
            this.requestUrl = builder.requestUrl;
            this.ipAndPort = builder.ipAndPort;
            this.vpnStartTime = builder.vpnStartTime;
            this.localPort = builder.localPort;
            this.remoteHost = builder.remoteHost;
            this.isHttps = builder.isHttps;
            this.uniqueName = builder.uniqueName;
            this.httpStartTime = System.currentTimeMillis();
            this.needParseData = builder.needParseData;
            this.isRequest = builder.isRequest;
            this.length = builder.length;
        }

        public String getRequestUrl() {
            return requestUrl;
        }

        public String getIpAndPort() {
            return ipAndPort;
        }

//        public AppInfo getAppInfo() {
//            return appInfo;
//        }

        public long getHttpStartTime() {
            return httpStartTime;
        }

        public long getVpnStartTime() {
            return vpnStartTime;
        }

        public short getLocalPort() {
            return localPort;
        }

        public String getRemoteHost() {
            return remoteHost;
        }

        public boolean isHttps() {
            return isHttps;
        }

        public String getUniqueName() {
            return uniqueName;
        }

        public static final class Builder {
            private String requestUrl;
            private String ipAndPort;
//            private AppInfo appInfo;
            private long vpnStartTime;
            private short localPort;
            private String remoteHost;
            private boolean isHttps;
            private String uniqueName;
            private byte[] needParseData;
            private boolean isRequest;
            private int length;

            public Builder requestUrl(String val) {
                requestUrl = val;
                return this;
            }
            public Builder ipAndPort(String val) {
                ipAndPort = val;
                return this;
            }
            public Builder vpnStartTime(long val) {
                vpnStartTime = val;
                return this;
            }
            public Builder localPort(short val) {
                localPort = val;
                return this;
            }
            public Builder remoteHost(String val) {
                remoteHost = val;
                return this;
            }
            public Builder isHttps(boolean val) {
                isHttps = val;
                return this;
            }
            public Builder uniqueName(String val) {
                uniqueName = val;
                return this;
            }
            public Builder needParseData(byte[] val) {
                needParseData = val;
                return this;
            }
            public Builder isRequest(boolean val) {
                isRequest = val;
                return this;
            }
            public Builder length(int val) {
                length = val;
                return this;
            }

            public HttpData build() {
                return new HttpData(this);
            }
        }

        public static class Comparator implements java.util.Comparator<HttpData> {

            @Override
            public int compare(HttpData o1, HttpData o2) {
                if (o1 == o2) {
                    return 0;
                }
                return Long.compare(o2.httpStartTime, o1.httpStartTime);
            }
        }
    }

}
