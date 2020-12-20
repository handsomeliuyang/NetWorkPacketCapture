package com.minhui.vpn;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by minhui.zhu on 2017/6/24.
 * Copyright © 2017年 minhui.zhu. All rights reserved.
 */

public final class VPNConstants {
    public static final int BUFFER_SIZE = 2560;
    public static final int MAX_PAYLOAD_SIZE = 2520;



    public static String BASE_DIR; // "/VpnCapture/Conversation/";
    public static String DATA_DIR;
    public static String CONFIG_DIR;
    public static final void initBaseDir(Context context){
        BASE_DIR = context.getExternalFilesDir("Conversation").getAbsolutePath() + "/";
        DATA_DIR = BASE_DIR + "data/";
        CONFIG_DIR = BASE_DIR+"config/";
    }

    public static final String VPN_SP_NAME="vpn_sp_name";
    public static final String IS_UDP_NEED_SAVE="isUDPNeedSave";
    public static final String IS_UDP_SHOW = "isUDPShow";
    public static final String IS_MOCK_LOCAL_DATA = "isMockLocalData";
    public static final String DEFAULT_PACKAGE_ID = "default_package_id";
    public static final String DEFAULT_PACAGE_NAME = "default_package_name";
}
