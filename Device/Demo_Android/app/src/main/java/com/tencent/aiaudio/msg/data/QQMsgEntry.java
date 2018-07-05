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
package com.tencent.aiaudio.msg.data;


import android.os.Parcel;
import android.os.Parcelable;

import com.tencent.aiaudio.msg.AudioMsgDownloadInfo;
import com.tencent.aiaudio.msg.TextMsgInfo;
import com.tencent.xiaowei.util.JsonUtil;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "msg_list")
public class QQMsgEntry implements Parcelable {


    /**
     * 文本消息
     */
    public static final int MSG_TYPE_TEXT = 0;
    /**
     * 语音消息
     */
    public static final int MSG_TYPE_AUDIO = 1;

    public QQMsgEntry() {
        this(0, false, false, MSG_TYPE_TEXT, "", 0);
    }


    public QQMsgEntry(long sender, TextMsgInfo textMsgInfo) {
        this.sender = sender;
        this.receiver = textMsgInfo.getSenderDin();
        this.timeStamp = textMsgInfo.getMsg_time() * 1000L;
        this.hasRead = false;
        this.isRcv = true;
        this.content = textMsgInfo.getText();
        this.msgType = MSG_TYPE_TEXT;
        this.duration = 0;
    }

    public QQMsgEntry(long sender, AudioMsgDownloadInfo downloadInfo) {
        this.sender = sender;
        this.receiver = downloadInfo.getTo_din();
        this.timeStamp = downloadInfo.getMsg_time() * 1000L;
        this.hasRead = false;
        this.isRcv = true;
        this.msgType = MSG_TYPE_AUDIO;
        this.duration = downloadInfo.getDuration();
    }

    public QQMsgEntry(long sender, boolean hasRead, boolean isRcv, String content, int msgType) {
        this(sender, hasRead, isRcv, msgType, content, 0);
    }

    public QQMsgEntry(long sender, boolean hasRead, boolean isRcv, int msgType, String content, int duration) {
        this.sender = sender;
        this.hasRead = hasRead;
        this.isRcv = isRcv;
        this.content = content;
        this.duration = duration;
        this.msgType = msgType;
        this.timeStamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getSender() {
        return sender;
    }

    public void setSender(long sender) {
        this.sender = sender;
    }

    public boolean isHasRead() {
        return hasRead;
    }

    public void setHasRead(boolean hasRead) {
        this.hasRead = hasRead;
    }

    public boolean isRcv() {
        return isRcv;
    }

    public void setRcv(boolean rcv) {
        isRcv = rcv;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    /**
     * 消息id，自增
     */
    @Column(name = "id", isId = true, autoGen = true, property = "NOT NULL")
    private int id;

    /**
     * 发送者id
     */
    @Column(name = "sender")
    private long sender;

    /**
     * 发送者id
     */
    @Column(name = "receiver")
    private long receiver;

    /**
     * 是否已读
     */
    @Column(name = "hasRead")
    private boolean hasRead;

    /**
     * 是否为收到的消息
     */
    @Column(name = "isRcv")
    private boolean isRcv;

    /**
     * 消息时间戳
     */
    @Column(name = "timeStamp")
    private long timeStamp;

    /**
     * 文本消息就是消息的内容，语音消息为音频路径
     */
    @Column(name = "content")
    private String content;

    /**
     * 语音消息的时长
     */
    @Column(name = "duration")
    private int duration;

    /**
     * 消息类型
     */
    @Column(name = "msgType")
    private int msgType;


    protected QQMsgEntry(Parcel in) {
        id = in.readInt();
        sender = in.readLong();
        hasRead = in.readByte() != 0;
        isRcv = in.readByte() != 0;
        timeStamp = in.readLong();
        content = in.readString();
        duration = in.readInt();
        msgType = in.readInt();
    }

    public static final Creator<QQMsgEntry> CREATOR = new Creator<QQMsgEntry>() {
        @Override
        public QQMsgEntry createFromParcel(Parcel in) {
            return new QQMsgEntry(in);
        }

        @Override
        public QQMsgEntry[] newArray(int size) {
            return new QQMsgEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeLong(sender);
        dest.writeByte((byte) (hasRead ? 1 : 0));
        dest.writeByte((byte) (isRcv ? 1 : 0));
        dest.writeLong(timeStamp);
        dest.writeString(content);
        dest.writeInt(duration);
        dest.writeInt(msgType);
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
