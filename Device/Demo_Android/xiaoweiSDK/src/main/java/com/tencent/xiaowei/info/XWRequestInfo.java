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
package com.tencent.xiaowei.info;

import android.os.Parcel;
import android.os.Parcelable;

import com.tencent.xiaowei.util.JsonUtil;

import static com.tencent.xiaowei.def.XWCommonDef.WAKEUP_TYPE.WAKEUP_TYPE_DEFAULT;


/**
 * 请求附加参数
 */
public class XWRequestInfo implements Parcelable {


    /**
     * 上下文，上一次的结果存在多轮对话时，需要为这个字段赋值
     */
    public String contextId;

    /**
     * 等待用户说话的超时时间(单位:ms)
     */
    public int speakTimeout;

    /**
     * 用户说话的断句时间(单位:ms)
     */
    public int silentTimeout;

    /**
     * 声音请求的首包标志，首包时必须为true
     */
    public boolean voiceRequestBegin;

    /**
     * 当使用外部VAD时，声音尾包置成true
     */
    public boolean voiceRequestEnd;

    /**
     * 识别引擎是用近场还是远场 {@link com.tencent.xiaowei.def.XWCommonDef.PROFILE_TYPE}
     */
    public int profileType;

    /**
     * 唤醒词类型：{@link com.tencent.xiaowei.def.XWCommonDef.WAKEUP_TYPE}，如果是{@link com.tencent.xiaowei.def.XWCommonDef.RequestType#WAKEUP_CHECK}的请求并且值为 {@link com.tencent.xiaowei.def.XWCommonDef.WAKEUP_TYPE#WAKEUP_TYPE_CLOUD_CHECK} 将进行云端校验。
     */
    public int voiceWakeupType;

    /**
     * 识别结果中去掉唤醒次，如果是{@link com.tencent.xiaowei.def.XWCommonDef.RequestType#VOICE}的请求并且{@link #voiceWakeupType}值为 {@link com.tencent.xiaowei.def.XWCommonDef.WAKEUP_TYPE#WAKEUP_TYPE_LOCAL_WITH_TEXT} 将进行该操作。
     */
    public String voiceWakeupText;

    /**
     * 请求的一些参数{@link com.tencent.xiaowei.def.XWCommonDef.REQUEST_PARAM}
     */
    public long requestParam;

    public XWRequestInfo() {
        speakTimeout = 5000;
        silentTimeout = 500;
    }


    protected XWRequestInfo(Parcel in) {
        contextId = in.readString();
        speakTimeout = in.readInt();
        silentTimeout = in.readInt();
        voiceRequestBegin = in.readByte() != 0;
        voiceRequestEnd = in.readByte() != 0;
        profileType = in.readInt();
        voiceWakeupType = in.readInt();
        voiceWakeupText = in.readString();
        requestParam = in.readLong();
    }

    public void reset() {
        contextId = null;
        speakTimeout = 5000;
        silentTimeout = 500;
        voiceRequestBegin = false;
        voiceRequestEnd = false;
        voiceWakeupType = WAKEUP_TYPE_DEFAULT;
        voiceWakeupText = null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(contextId);
        dest.writeInt(speakTimeout);
        dest.writeInt(silentTimeout);
        dest.writeByte((byte) (voiceRequestBegin ? 1 : 0));
        dest.writeByte((byte) (voiceRequestEnd ? 1 : 0));
        dest.writeInt(profileType);
        dest.writeInt(voiceWakeupType);
        dest.writeString(voiceWakeupText);
        dest.writeLong(requestParam);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<XWRequestInfo> CREATOR = new Creator<XWRequestInfo>() {
        @Override
        public XWRequestInfo createFromParcel(Parcel in) {
            return new XWRequestInfo(in);
        }

        @Override
        public XWRequestInfo[] newArray(int size) {
            return new XWRequestInfo[size];
        }
    };

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}