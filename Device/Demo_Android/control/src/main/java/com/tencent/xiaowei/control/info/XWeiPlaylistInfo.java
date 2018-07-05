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
package com.tencent.xiaowei.control.info;

import android.os.Parcel;
import android.os.Parcelable;

import com.tencent.xiaowei.util.JsonUtil;

/**
 * 与某个场景关联的播放列表信息
 */
public class XWeiPlaylistInfo implements Parcelable {
    /**
     * 播放列表Id
     */
    public int playlistId;
    /**
     * 未定义
     */
    public int type;
    /**
     * 播放列表资源个数
     */
    public int count;
    /**
     * 当前列表往下有更多
     */
    public boolean hasMoreCurrent;
    /**
     * 当前列表往上有更多
     */
    public boolean hasMoreCurrentUp;
    /**
     * 历史列表往下有更多，并非每个Skill都有历史
     */
    public boolean hasMoreHistory;
    /**
     * 历史列表往上有更多，并非每个Skill都有历史
     */
    public boolean hasMoreHistoryUp;

    /**
     * 存在历史列表
     */
    public boolean hasHistory;


    protected XWeiPlaylistInfo(Parcel in) {
        playlistId = in.readInt();
        type = in.readInt();
        count = in.readInt();
        hasMoreCurrent = in.readByte() != 0;
        hasMoreCurrentUp = in.readByte() != 0;
        hasMoreHistory = in.readByte() != 0;
        hasMoreHistoryUp = in.readByte() != 0;
        hasHistory = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(playlistId);
        dest.writeInt(type);
        dest.writeInt(count);
        dest.writeByte((byte) (hasMoreCurrent ? 1 : 0));
        dest.writeByte((byte) (hasMoreCurrentUp ? 1 : 0));
        dest.writeByte((byte) (hasMoreHistory ? 1 : 0));
        dest.writeByte((byte) (hasMoreHistoryUp ? 1 : 0));
        dest.writeByte((byte) (hasHistory ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<XWeiPlaylistInfo> CREATOR = new Creator<XWeiPlaylistInfo>() {
        @Override
        public XWeiPlaylistInfo createFromParcel(Parcel in) {
            return new XWeiPlaylistInfo(in);
        }

        @Override
        public XWeiPlaylistInfo[] newArray(int size) {
            return new XWeiPlaylistInfo[size];
        }
    };

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
