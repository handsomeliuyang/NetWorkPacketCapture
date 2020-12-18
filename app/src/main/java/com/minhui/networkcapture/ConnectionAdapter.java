package com.minhui.networkcapture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.minhui.vpn.http.HttpCachManager;
import com.minhui.vpn.nat.NatSession;
import com.minhui.vpn.processparse.AppInfo;
import com.minhui.vpn.utils.TimeFormatUtil;

import java.util.List;

/**
 * @author minhui.zhu
 *         Created by minhui.zhu on 2018/2/28.
 *         Copyright © 2017年 minhui.zhu. All rights reserved.
 */

public class ConnectionAdapter extends BaseAdapter {
    private final Context context;
    private List<HttpCachManager.HttpData> httpDataList;

    ConnectionAdapter(Context context, List<HttpCachManager.HttpData> httpDataList) {
        this.context = context;
        this.httpDataList = httpDataList;
    }

    public void setNetConnections(List<HttpCachManager.HttpData> httpDataList) {
        this.httpDataList = httpDataList;
    }

    @Override
    public int getCount() {
        return httpDataList==null?0:httpDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_connection, parent, false);
            holder = new Holder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        HttpCachManager.HttpData httpData = httpDataList.get(position);

        holder.hostName.setVisibility(httpData.getRequestUrl() != null || httpData.getRemoteHost() != null ?
                View.VISIBLE : View.GONE);
        holder.hostName.setText(httpData.getRequestUrl() != null ? httpData.getRequestUrl() : httpData.getRemoteHost());

//        holder.hostName.setText(null);
//        holder.hostName.setVisibility(View.GONE);
//        if (NatSession.TCP.equals(connection.getType())) {
//            if (connection.getRequestUrl() != null) {
//                holder.hostName.setText(connection.getRequestUrl());
//            } else {
//                holder.hostName.setText(connection.getRemoteHost());
//            }
//            if(connection.getRequestUrl()!=null||connection.getRemoteHost()!=null){
//                holder.hostName.setVisibility(View.VISIBLE);
//            }
//        }

        holder.netState.setText(httpData.getIpAndPort());
        holder.refreshTime.setText(TimeFormatUtil.formatHHMMSSMM(httpData.getHttpStartTime()));
//        int sumByte = (int) (connection.bytesSent + connection.getReceiveByteNum());

//        String showSum;
//        if (sumByte > 1000000) {
//            showSum = String.valueOf((int) (sumByte / 1000000.0 + 0.5)) + "mb";
//        } else if (sumByte > 1000) {
//            showSum = String.valueOf((int) (sumByte / 1000.0 + 0.5)) + "kb";
//        } else {
//            showSum = String.valueOf(sumByte) + "b";
//        }
//
//        holder.size.setText(showSum);

        return convertView;
    }

    class Holder {
        TextView netState;
        TextView refreshTime;
        TextView hostName;
        View baseView;

        Holder(View view) {
            baseView = view;
            refreshTime = (TextView) view.findViewById(R.id.refresh_time);
            netState = (TextView) view.findViewById(R.id.net_state);
            hostName = (TextView) view.findViewById(R.id.url);
        }

    }

}
