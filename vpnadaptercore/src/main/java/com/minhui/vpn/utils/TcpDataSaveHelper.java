package com.minhui.vpn.utils;

import android.net.Uri;

import com.minhui.vpn.VPNConstants;
import com.minhui.vpn.VPNLog;
import com.minhui.vpn.nat.NatSession;
import com.minhui.vpn.processparse.AppInfo;
import com.minhui.vpn.processparse.PortHostService;
import com.minhui.vpn.utils.ThreadProxy;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author minhui.zhu
 *         Created by minhui.zhu on 2018/5/7.
 *         Copyright © 2017年 Oceanwing. All rights reserved.
 */

public class TcpDataSaveHelper {
    private static final String TAG = "TcpDataSaveHelper";
    private String dir;
    private SaveData lastSaveData;
    private File lastSaveFile;
    int requestNum = 0;
    int responseNum = 0;
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";

    public TcpDataSaveHelper(String dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        return "TcpDataSaveHelper{" +
                "dir='" + dir + '\'' +
                ", lastSaveData=" + lastSaveData +
                ", lastSaveFile=" + lastSaveFile +
                ", requestNum=" + requestNum +
                ", responseNum=" + responseNum +
                '}';
    }

    public void addData(final SaveData data) {
        ThreadProxy.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                if (lastSaveData == null || (lastSaveData.isRequest ^ data.isRequest)) {
                    newFileAndSaveData(data);
                } else {
                    appendFileData(data);
                }
                lastSaveData = data;
            }
        });

    }

    private void appendFileData(SaveData data) {
        RandomAccessFile randomAccessFile;
        try {
            randomAccessFile = new RandomAccessFile(lastSaveFile.getAbsolutePath(), "rw");
            long length = randomAccessFile.length();
            randomAccessFile.seek(length);
            randomAccessFile.write(data.needParseData, data.offSet, data.playoffSize);
        } catch (Exception e) {

        }
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                VPNLog.d(TAG, "failed to close closeable");
            }
        }
    }

    private File fileName(String namePre, boolean isRequest){
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        String childName = namePre + "_" + (isRequest ? REQUEST : RESPONSE);
        return new File(file, childName);
    }

    private void newFileAndSaveData(SaveData data) {
        int saveNum;
        if (data.isRequest) {
            saveNum = requestNum;
            requestNum++;
        } else {
            saveNum = responseNum;
            responseNum++;
        }
        lastSaveFile = fileName(data.namePre, data.isRequest);
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(lastSaveFile);
            fileOutputStream.write(data.needParseData, data.offSet, data.playoffSize);
            fileOutputStream.flush();
        } catch (Exception e) {
            VPNLog.d(TAG, "failed to saveData" + e.getMessage());
        } finally {
            close(fileOutputStream);
        }

    }

    public boolean existResponse(SaveData saveData) {
        File responseFile = fileName(saveData.namePre, false);
        return responseFile.exists();
    }

    public ByteBuffer loadCashedResponse(SaveData saveData) {
        File responseFile = fileName(saveData.namePre, false);
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

    public static class SaveData {
        boolean isRequest;
        byte[] needParseData;
        int offSet;
        int playoffSize;
        String namePre;

        private SaveData(Builder builder) {
            isRequest = builder.isRequest;
            needParseData = builder.needParseData;
            offSet = builder.offSet;
            playoffSize = builder.length;
            namePre = builder.namePre;
        }


        public static final class Builder {
            private boolean isRequest;
            private byte[] needParseData;
            private int offSet;
            private int length;
            private String namePre;

            public Builder() {
            }

            public Builder isRequest(boolean val) {
                isRequest = val;
                return this;
            }

            public Builder needParseData(byte[] val) {
                needParseData = val;
                return this;
            }

            public Builder offSet(int val) {
                offSet = val;
                return this;
            }

            public Builder length(int val) {
                length = val;
                return this;
            }

            public Builder namePre(String val){
                namePre = val;
                return this;
            }

            public SaveData build() {
                return new SaveData(this);
            }
        }
    }
}
