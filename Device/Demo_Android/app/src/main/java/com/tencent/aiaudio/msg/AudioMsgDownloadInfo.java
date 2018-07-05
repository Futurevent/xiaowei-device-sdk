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

public class AudioMsgDownloadInfo {

    /**
     * duration : 2
     * file_key : 305902010004523050020100041231343431313531393739303534333536343202037a1db9020438fd03b702045afe99d7042037304635434141323933434542423741313441413346363141384231453334310201000201000400
     * fkey2 : ChBw9cqik867ehSqP2GoseNBEhtzdHJlYW1fMjAxODA1MTgxNzE2MDUyOS5hbXI=
     * msg_time : 0
     * to_din : 144115192393501304
     */

    private int duration;
    private String file_key;
    private String fkey2;
    private int msg_time;
    private long to_din;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getFile_key() {
        return file_key;
    }

    public void setFile_key(String file_key) {
        this.file_key = file_key;
    }

    public String getFkey2() {
        return fkey2;
    }

    public void setFkey2(String fkey2) {
        this.fkey2 = fkey2;
    }

    public int getMsg_time() {
        return msg_time;
    }

    public void setMsg_time(int msg_time) {
        this.msg_time = msg_time;
    }

    public long getTo_din() {
        return to_din;
    }

    public void setTo_din(long to_din) {
        this.to_din = to_din;
    }
}
