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
package com.tencent.aiaudio.msg;

import android.util.Log;

import com.tencent.aiaudio.msg.data.OnOperationFinishListener;
import com.tencent.aiaudio.msg.data.OnQueryAllMsgListener;
import com.tencent.aiaudio.msg.data.MsgDbManager;
import com.tencent.aiaudio.msg.data.MsgEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MsgBoxManager implements OnQueryAllMsgListener {
    private static final String TAG = MsgBoxManager.class.getSimpleName();
    private static final MsgBoxManager ourInstance = new MsgBoxManager();

    private CopyOnWriteArrayList<MsgEntry> msgEntries = new CopyOnWriteArrayList<>();

    public static MsgBoxManager getInstance() {
        return ourInstance;
    }

    private MsgBoxManager() {

    }

    public void init() {
        MsgDbManager.getInstance().queryAllMsgItem(this);
    }

    /**
     * 添加一个消息
     *
     * @param entry    消息实体
     * @param listener 监听器
     */
    public void addMsg(MsgEntry entry, OnOperationFinishListener listener) {
        if (entry == null) {
            return;
        }

        msgEntries.add(entry);

        MsgDbManager.getInstance().addMsgItem(entry, listener);
    }


    /**
     * 设置消息已读
     *
     * @param msgId 消息id
     */
    public void setMsgRead(int msgId) {
        for (MsgEntry entry : msgEntries) {
            if (entry.getId() == msgId) {
                entry.setHasRead(true);
                MsgDbManager.getInstance().updateMsgItem(entry, null);
                break;
            }
        }
    }

    public void deleteAll() {
        msgEntries.clear();
        MsgDbManager.getInstance().deleteAll();
    }

    /**
     * 获取下一个未读消息
     *
     * @return 下一个未读消息或null
     */
    public MsgEntry getNextMsg() {

        for (int i = msgEntries.size() - 1; i >= 0; i--) {
            MsgEntry entry = msgEntries.get(i);
            if (!entry.isHasRead()) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public void onQueryAllMsg(List<MsgEntry> entries) {
        if (entries == null) {
            return;
        }

        for (MsgEntry entry : entries) {
            Log.d(TAG, "onQueryAllAlarm msgId: " + entry.getId());
            msgEntries.add(entry);
        }
    }
}
