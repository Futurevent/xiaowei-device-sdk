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
package com.tencent.aiaudio.wakeup;

public class RingBuffer {
    private byte[] buf;
    private int count;
    private int capacity;

    /**
     * @param capacity 允许的最大长度
     */
    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            capacity = 32;
        }
        this.capacity = capacity;
        buf = new byte[capacity];
    }

    /**
     * 更新RingBuffer中的数据
     *
     * @param buffer 将buffer追加到最后，如果长度超过capacity，将最前面的移除
     */
    public synchronized void write(byte[] buffer) {
        if (buffer == null || buffer.length == 0) {
            return;
        }
        int removeSize = count + buffer.length - capacity;

        if (removeSize > 0) {
            byte[] bf = new byte[capacity];
            System.arraycopy(buf, removeSize, bf, 0, count - removeSize);
            count -= removeSize;
            buf = bf;
        }
        System.arraycopy(buffer, 0, buf, count, buffer.length);
        count += buffer.length;
    }

    /**
     * 获得RingBuffer中的所有数据
     *
     * @return
     */
    public synchronized byte[] toByteArray() {
        byte[] newArray = new byte[count];
        System.arraycopy(buf, 0, newArray, 0, count);
        return newArray;
    }

    /**
     * 获得RingBuffer中最后的指定长度的数据
     *
     * @param length
     * @return
     */
    public synchronized byte[] getLastByteArray(int length) {
        if (length == 0) {
            return null;
        }
        byte[] newArray = new byte[Math.min(count, length)];
        System.arraycopy(buf, count - newArray.length, newArray, 0, newArray.length);
        return newArray;
    }

}
