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
import com.tencent.aiaudio.msg.TextQQMsgInfo;
import com.tencent.aiaudio.msg.WechatMsgInfo;
import com.tencent.xiaowei.util.JsonUtil;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "msg_list")
public class MsgEntry implements Parcelable {


    /**
     * 文本消息
     */
    public static final int MSG_TYPE_QQ_TEXT = 0;
    /**
     * 语音Url消息
     */
    public static final int MSG_TYPE_QQ_AUDIO_URL = 1;
    /**
     * 语音文件消息
     */
    public static final int MSG_TYPE_QQ_AUDIO_FILE = 2;
    /**
     * 文本消息
     */
    public static final int MSG_TYPE_WECHAT_TEXT = 3;
    /**
     * 语音Url消息,有效期为7天，所以在收到的时候应该存储在本地，变成MSG_TYPE_WECHAT_AUDIO_FILE
     */
    public static final int MSG_TYPE_WECHAT_AUDIO_URL = 4;
    /**
     * 语音文件消息
     */
    public static final int MSG_TYPE_WECHAT_AUDIO_FILE = 5;

    public MsgEntry() {
        this(0, false, false, MSG_TYPE_QQ_TEXT, "", 0);
    }


    public MsgEntry(long sender, TextQQMsgInfo textQQMsgInfo) {
        this.sender = sender;
        this.receiver = textQQMsgInfo.getSenderDin();
        this.timeStamp = textQQMsgInfo.getMsg_time() * 1000L;
        this.hasRead = false;
        this.isRcv = true;
        this.content = textQQMsgInfo.getText();
        this.msgType = MSG_TYPE_QQ_TEXT;
        this.duration = 0;
    }

    public MsgEntry(WechatMsgInfo textWechatMsgInfo) {
        this.senderOpenId = textWechatMsgInfo.from;
        this.timeStamp = textWechatMsgInfo.timestamp * 1000L;
        this.hasRead = false;
        this.isRcv = true;
        this.content = textWechatMsgInfo.content;
        this.msgType = WechatMsgInfo.MSG_TYPE_TEXT.equals(textWechatMsgInfo.msgtype) ? MSG_TYPE_WECHAT_TEXT : MSG_TYPE_WECHAT_AUDIO_URL;
        this.duration = 0;
    }

    public MsgEntry(long sender, AudioMsgDownloadInfo downloadInfo) {
        this.sender = sender;
        this.receiver = downloadInfo.getTo_din();
        this.timeStamp = downloadInfo.getMsg_time() * 1000L;
        this.hasRead = false;
        this.isRcv = true;
        this.msgType = MSG_TYPE_QQ_AUDIO_FILE;
        this.duration = downloadInfo.getDuration();
    }

    public MsgEntry(long sender, boolean hasRead, boolean isRcv, String content, int msgType) {
        this(sender, hasRead, isRcv, msgType, content, 0);
    }

    public MsgEntry(long sender, boolean hasRead, boolean isRcv, int msgType, String content, int duration) {
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

    public String getSenderOpenId() {
        return senderOpenId;
    }

    public void setSenderOpenId(String senderOpenId) {
        this.senderOpenId = senderOpenId;
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
     * QQ发送者id
     */
    @Column(name = "sender")
    private long sender;

    /**
     * 微信发送者id
     */
    @Column(name = "senderOpenId")
    private String senderOpenId;

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

    protected MsgEntry(Parcel in) {
        id = in.readInt();
        sender = in.readLong();
        hasRead = in.readByte() != 0;
        isRcv = in.readByte() != 0;
        timeStamp = in.readLong();
        content = in.readString();
        duration = in.readInt();
        msgType = in.readInt();
    }

    public static final Creator<MsgEntry> CREATOR = new Creator<MsgEntry>() {
        @Override
        public MsgEntry createFromParcel(Parcel in) {
            return new MsgEntry(in);
        }

        @Override
        public MsgEntry[] newArray(int size) {
            return new MsgEntry[size];
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
