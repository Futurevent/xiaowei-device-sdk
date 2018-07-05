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

public class TextMsgInfo {
    /**
     * msg_time : 1526636971
     * msgSeq : 1526636971
     * text : text
     * senderDin : 144115192393501304
     */

    private int msg_time;
    private int msgSeq;
    private String text;
    private long senderDin;

    public int getMsg_time() {
        return msg_time;
    }

    public void setMsg_time(int msg_time) {
        this.msg_time = msg_time;
    }

    public int getMsgSeq() {
        return msgSeq;
    }

    public void setMsgSeq(int msgSeq) {
        this.msgSeq = msgSeq;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getSenderDin() {
        return senderDin;
    }

    public void setSenderDin(long senderDin) {
        this.senderDin = senderDin;
    }
}
