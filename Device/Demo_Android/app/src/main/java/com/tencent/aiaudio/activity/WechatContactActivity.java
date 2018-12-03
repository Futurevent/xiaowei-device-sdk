/*
 * Tencent is pleased to support the open source community by making  XiaoweiSDK Demo Codes available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.tencent.aiaudio.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tencent.aiaudio.activity.base.BaseActivity;
import com.tencent.aiaudio.adapter.CommonListAdapter;
import com.tencent.aiaudio.demo.R;
import com.tencent.xiaowei.def.XWCommonDef;
import com.tencent.xiaowei.sdk.XWSDK;
import com.tencent.xiaowei.util.JsonUtil;

import java.util.ArrayList;

public class WechatContactActivity extends BaseActivity {
    static final String TAG = "WechatContactActivity";

    private GridView mGridView;
    protected CommonListAdapter<Contact> mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact_wechat);
        initViews();
        initAdapter();
        initDatas();
    }

    private void initDatas() {
        updateData();
    }

    private void initAdapter() {
        mAdapter = new CommonListAdapter<Contact>() {
            @Override
            protected View initListCell(int position, View convertView, ViewGroup parent) {
                convertView = getLayoutInflater().inflate(R.layout.item_contact, parent, false);
                ((TextView) convertView.findViewById(R.id.tv)).setText(mAdapter.getItem(position).remark);
                if (!TextUtils.isEmpty(mAdapter.getItem(position).head_url)) {
                    Picasso.with(convertView.getContext()).load(mAdapter.getItem(position).head_url).into((ImageView) convertView.findViewById(R.id.iv));
                }
                return convertView;
            }
        };
        mGridView.setAdapter(mAdapter);
    }

    private void initViews() {
        mGridView = (GridView) findViewById(R.id.gv);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String remark = mAdapter.getItem(position).remark;
                XWSDK.getInstance().request(XWCommonDef.RequestType.TEXT, ("给" + remark + "发消息").getBytes());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void updateData() {
        XWSDK.getInstance().request("WECHAT_CONTACT", "get_list", "", new XWSDK.OnRspListener() {
            @Override
            public void onRsp(String voiceId, int error, String json) {
                ArrayList<Contact> list = JsonUtil.getObjectList(json, Contact.class);
                mAdapter.addAll(list);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public static class Contact {
        public String open_id;
        public String remark;
        public String head_url;
        public int bind_type;// 0管理员 1普通成员
    }
}
