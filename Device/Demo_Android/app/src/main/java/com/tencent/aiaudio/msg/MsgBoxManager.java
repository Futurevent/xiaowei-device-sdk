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
import com.tencent.aiaudio.msg.data.QQMsgDbManager;
import com.tencent.aiaudio.msg.data.QQMsgEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MsgBoxManager implements OnQueryAllMsgListener {
    private static final String TAG = MsgBoxManager.class.getSimpleName();
    private static final MsgBoxManager ourInstance = new MsgBoxManager();

    private CopyOnWriteArrayList<QQMsgEntry> msgEntries = new CopyOnWriteArrayList<>();

    public static MsgBoxManager getInstance() {
        return ourInstance;
    }

    private MsgBoxManager() {

    }

    public void init() {
        QQMsgDbManager.getInstance().queryAllMsgItem(this);
    }

    /**
     * 添加一个QQ消息
     *
     * @param entry QQ消息实体
     * @param listener 监听器
     */
    public void addMsg(QQMsgEntry entry, OnOperationFinishListener listener) {
        if (entry == null) {
            return;
        }

        msgEntries.add(entry);

        QQMsgDbManager.getInstance().addMsgItem(entry, listener);
    }


    /**
     * 设置消息已读
     * @param msgId 消息id
     */
    public void setMsgRead(int msgId) {
        for (QQMsgEntry entry : msgEntries) {
            if (entry.getId() == msgId) {
                entry.setHasRead(true);
                QQMsgDbManager.getInstance().updateMsgItem(entry, null);
                break;
            }
        }
    }

    /**
     * 获取下一个未读消息
     *
     * @return 下一个未读消息或null
     */
    public QQMsgEntry getNextMsg() {

        for (int i = msgEntries.size() - 1; i >= 0; i--) {
            QQMsgEntry entry = msgEntries.get(i);
            if (!entry.isHasRead()) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public void onQueryAllMsg(List<QQMsgEntry> entries) {
        if (entries == null) {
            return;
        }

        for (QQMsgEntry entry : entries) {
            Log.d(TAG, "onQueryAllAlarm msgId: " + entry.getId());
            msgEntries.add(entry);
        }
    }
}
