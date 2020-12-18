package com.minhui.networkcapture;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.minhui.vpn.VPNLog;
import com.minhui.vpn.utils.ThreadProxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author minhui.zhu
 *         Created by minhui.zhu on 2018/2/27.
 *         Copyright © 2017年 minhui.zhu. All rights reserved.
 */

public class PackageListActivity extends Activity {

    public static final String SELECT_PACKAGE = "package_select";
    public static final String SELECT_PACKAGE_NAME = "package_select_name";
    private static final String TAG = "PackageListActivity";
    private ListView packageListView;
    // 置顶显示的App
    private static final List<String> TopAppArray = Arrays.asList(
            "com.wuba", // 58同城
            "com.docker", // ADocker
            "com.wuba.town.client", // 本地版
            "com.wuba.wbtown", // 58同镇站长
            "com.ly.studydemo" // 测试Demo
    );
    private ProgressBar pg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_list);
        pg = (ProgressBar) findViewById(R.id.pg);

        packageListView = (ListView) findViewById(R.id.package_list);
        ThreadProxy.getInstance().execute(new Runnable() {

            private ShowPackageAdapter showPackageAdapter;

            @Override
            public void run() {
                List<PackageShowInfo> topPackageShowInfo = new ArrayList<PackageShowInfo>();
                List<PackageShowInfo> packageShowInfo = new ArrayList<PackageShowInfo>();

                List<PackageShowInfo> allInstallPackageList = PackageShowInfo.getPackageShowInfo(getApplicationContext());
                for(PackageShowInfo info : allInstallPackageList){
                    if(TopAppArray.contains(info.packageName)) {
                        topPackageShowInfo.add(info);
                    } else {
                        packageShowInfo.add(info);
                    }
                }
                showPackageAdapter = new ShowPackageAdapter(PackageListActivity.this, topPackageShowInfo, packageShowInfo);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        packageListView.setAdapter(showPackageAdapter);
                        pg.setVisibility(View.GONE);
                    }
                });
            }
        });


        packageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(view.getTag() instanceof ShowPackageAdapter.Holder) {
                    ShowPackageAdapter.Holder holder = (ShowPackageAdapter.Holder) view.getTag();

                    Intent intent = new Intent();
                    if (position != 0) {
                        intent.putExtra(SELECT_PACKAGE, holder.packageShowInfo.packageName);
                        intent.putExtra(SELECT_PACKAGE_NAME, holder.packageShowInfo.appName);
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private static class ShowPackageAdapter extends BaseAdapter {

        private enum ItemViewType {
            TYPE_CATEGORY,
            TYPE_PACKAGE
        }

        private Activity mActivity;
        private Drawable mDefaultDrawable;
        private final List<PackageShowInfo> mTopPackageShowInfos;
        private final List<PackageShowInfo> mPackageShowInfos;

        public ShowPackageAdapter(Activity activity, List<PackageShowInfo> topPackageShowInfos, List<PackageShowInfo> packageShowInfos) {
            mActivity = activity;
            mDefaultDrawable = activity.getResources().getDrawable(R.drawable.sym_def_app_icon);
            this.mTopPackageShowInfos = topPackageShowInfos;
            this.mPackageShowInfos = packageShowInfos;
        }

        @Override
        public int getCount() {
            return 1 + mTopPackageShowInfos.size() + 1 + mPackageShowInfos.size();
        }

        @Override
        public Object getItem(int position) {
            // 第一个类别Item
            if(position == 0) {
                return null;
            }

            position = position - 1;

            // 置顶App
            if(position < mTopPackageShowInfos.size()) {
                return mTopPackageShowInfos.get(position);
            }

            position = position - mTopPackageShowInfos.size();

            // 第二个类别
            if(position == 0) {
                return null;
            }

            position = position - 1;

            // 其他App
            return mPackageShowInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {

            // 第一个类别Item
            if(position == 0) {
                return ItemViewType.TYPE_CATEGORY.ordinal();
            }

            position = position - 1;

            // 置顶App
            if(position < mTopPackageShowInfos.size()) {
                return ItemViewType.TYPE_PACKAGE.ordinal();
            }

            position = position - mTopPackageShowInfos.size();

            // 第二个类别
            if(position == 0) {
                return ItemViewType.TYPE_CATEGORY.ordinal();
            }

            position = position - 1;

            // 其他App
            return ItemViewType.TYPE_PACKAGE.ordinal();
        }

        @Override
        public int getViewTypeCount() {
            return ItemViewType.values().length;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ItemViewType viewType = ItemViewType.values()[getItemViewType(position)];

            // 初始化View
            if(convertView == null) {
                switch (viewType) {
                    case TYPE_CATEGORY:
                        convertView = LayoutInflater.from(mActivity).inflate(R.layout.item_category, parent, false);
                        convertView.setTag(convertView.findViewById(R.id.category_name));
                        break;
                    case TYPE_PACKAGE:
                        convertView = LayoutInflater.from(mActivity).inflate(R.layout.item_select_package, parent, false);
                        convertView.setTag(new Holder(convertView, position));
                        break;
                }
            }

            // 绑定类别数据
            if(viewType == ItemViewType.TYPE_CATEGORY) {
                TextView categoryName = (TextView) convertView.getTag();
                categoryName.setText(position == 0 ? "置顶App" : "其他App");

                return convertView;
            }

            // 绑定包数据
            if(viewType == ItemViewType.TYPE_PACKAGE) {
                Holder holder = (Holder) convertView.getTag();
                holder.packageShowInfo = (PackageShowInfo) getItem(position);
                holder.holderPosition = position;

                if (holder.packageShowInfo.appName == null) {
                    holder.appName.setText(holder.packageShowInfo.packageName);
                } else {
                    holder.appName.setText(holder.packageShowInfo.appName);
                }

//                holder.icon.setImageDrawable(mDefaultDrawable);
//                final View alertIconView = convertView;
                holder.icon.setImageDrawable(holder.packageShowInfo.iconDrawable == null ? mDefaultDrawable : holder.packageShowInfo.iconDrawable);
//                ThreadProxy.getInstance().execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        Holder iconHolder = (Holder) alertIconView.getTag();
//                        if (iconHolder.holderPosition != position) {
//                            return;
//                        }
//                        final Drawable drawable = holder.packageShowInfo.applicationInfo.loadIcon(mActivity.getPackageManager());
//                        mActivity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Holder iconHolder = (Holder) alertIconView.getTag();
//                                if (iconHolder.holderPosition != position) {
//                                    return;
//                                }
//                                holder.icon.setImageDrawable(drawable);
//                            }
//                        });
//                    }
//                });
                return convertView;
            }

            return convertView;
        }

        public static class Holder {
            TextView appName;
            ImageView icon;
            View baseView;
            PackageShowInfo packageShowInfo;
            int holderPosition;

            Holder(View view, int position) {
                baseView = view;
                appName = (TextView) view.findViewById(R.id.app_name);
                icon = (ImageView) view.findViewById(R.id.select_icon);
                this.holderPosition = position;
            }
        }
    }

}
