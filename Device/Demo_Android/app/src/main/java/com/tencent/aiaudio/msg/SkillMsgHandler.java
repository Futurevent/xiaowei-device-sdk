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

import android.content.Context;
import android.media.AudioManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.aiaudio.CommonApplication;
import com.tencent.aiaudio.msg.data.OnOperationFinishListener;
import com.tencent.aiaudio.msg.data.QQMsgEntry;
import com.tencent.aiaudio.wakeup.RecordDataManager;
import com.tencent.utils.MusicPlayer;
import com.tencent.xiaowei.control.XWeiAudioFocusManager;
import com.tencent.xiaowei.control.XWeiOuterSkill;
import com.tencent.xiaowei.def.XWCommonDef;
import com.tencent.xiaowei.info.XWFileTransferInfo;
import com.tencent.xiaowei.info.XWRequestInfo;
import com.tencent.xiaowei.info.XWResGroupInfo;
import com.tencent.xiaowei.info.XWResourceInfo;
import com.tencent.xiaowei.info.XWResponseInfo;
import com.tencent.xiaowei.info.XWeiMessageInfo;
import com.tencent.xiaowei.sdk.XWFileTransferManager;
import com.tencent.xiaowei.sdk.XWSDK;
import com.tencent.xiaowei.sdk.XWeiMsgManager;
import com.tencent.xiaowei.util.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.kvh.media.amr.AmrEncoder;

import static com.tencent.aiaudio.msg.data.QQMsgEntry.MSG_TYPE_AUDIO;
import static com.tencent.aiaudio.msg.data.QQMsgEntry.MSG_TYPE_TEXT;

public class SkillMsgHandler implements XWeiOuterSkill.OuterSkillHandler, RecordDataManager.AudioDataListener {


    private static final String TAG = SkillMsgHandler.class.getSimpleName();
    private static final String MSG_RING = "http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/msg.ring.mp3";
    private static final int MSG_RECORD_SPEAK_TIMEOUT = 5000;  // 用户5s没有说话，则超时，并取消发送消息
    private static final int MSG_RECORD_SILENT = 2000;         // VAD尾点时间改为2000ms，防止用户没有说完
    private static final String COMMAND_SEND_AUDIO_MSG = "11018";
    private static final String COMMAND_RCV_TEXT_MSG = "10000";        // 收到文本消息
    private static final String COMMAND_RCV_AUDIO_MSG = "10003";       // 收到语音消息
    private static final String COMMAND_PLAY_MSG = "700125";       // 播放消息

    private Context mContext;
    private int mSessionId = -1;


    private MusicPlayer player = new MusicPlayer();
    private ArrayList<XWResourceInfo> playList = new ArrayList<>();
    private int curPlayIndex = 0;
    private MusicPlayer.OnPlayListener mOnPlayListener = new MusicPlayer.OnPlayListener() {
        @Override
        public void onCompletion(int error) {
            curPlayIndex++;
            if (curPlayIndex >= playList.size()) {
                if (mRequestInfo != null) {
                    RecordDataManager.getInstance().onWakeup(mRequestInfo, SkillMsgHandler.this);
                    mRequestInfo = null;
                } else {
                    Log.e(TAG, "onCompletion mContextInfo is null");
                }
                CommonApplication.mAudioManager.abandonAudioFocus(listener);
            } else {
                player.playMediaInfo(playList.get(curPlayIndex), this);
            }
        }
    };
    private long mTargetId;
    private long duration;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private XWRequestInfo mRequestInfo = null;


    private AudioManager.OnAudioFocusChangeListener listener;
    private AudioManager.OnAudioFocusChangeListener transientListener;


    private static final int STATE_IDLE = 0x0;
    private static final int STATE_ONCE_PLAYING = 0x1;
    private static final int STATE_LOOP_PLAYING = 0x2;

    private volatile int mPlayState = STATE_IDLE;


    private ConcurrentLinkedQueue<XWResourceInfo> msgPlayList = new ConcurrentLinkedQueue<>();
    private AudioManager.OnAudioFocusChangeListener msgPlayAudioFocusListener;
    private MusicPlayer.OnPlayListener msgOnPlayListener = new MusicPlayer.OnPlayListener() {
        @Override
        public void onCompletion(int error) {
            XWResourceInfo resourceInfo = msgPlayList.poll();

            if (resourceInfo != null) {
                player.playMediaInfo(resourceInfo, this);

                if (resourceInfo.format == XWCommonDef.ResourceFormat.TEXT
                        || resourceInfo.format == XWCommonDef.ResourceFormat.FILE) {
                    MsgBoxManager.getInstance().setMsgRead(Integer.valueOf(resourceInfo.ID));
                }
            } else if (mPlayState == STATE_LOOP_PLAYING) {
                QQMsgEntry entry = MsgBoxManager.getInstance().getNextMsg();
                if (entry != null) {
                    XWResourceInfo[] resourceInfos = msgEntryToMedias(entry);
                    for (XWResourceInfo res : resourceInfos) {
                        msgPlayList.offer(res);
                    }

                    player.playMediaInfo(msgPlayList.poll(), this);
                } else {
                    XWResourceInfo tmpRes = new XWResourceInfo();
                    tmpRes.format = XWCommonDef.ResourceFormat.TEXT;
                    tmpRes.ID = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    tmpRes.content = "没有更多消息了";
                    playTipMsg(tmpRes, XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT);

                    mPlayState = STATE_IDLE;
                    CommonApplication.mAudioManager.abandonAudioFocus(msgPlayAudioFocusListener);
                }

            } else if (mPlayState == STATE_ONCE_PLAYING) {
                mPlayState = STATE_IDLE;
                CommonApplication.mAudioManager.abandonAudioFocus(msgPlayAudioFocusListener);
            }
        }
    };

    public SkillMsgHandler(Context context) {
        this.mContext = context;

        MsgBoxManager.getInstance().init();
    }

    @Override
    public boolean handleResponse(int sessionId, XWResponseInfo responseInfo) {
        Log.d(TAG, "handleResponse： " + responseInfo.toString());

        if (mSessionId == -1) {
            mSessionId = sessionId;
        }

        // 1. 处理消息接收逻辑
        boolean handled = processRcvMsg(responseInfo);

        // 2. 处理消息发送逻辑
        if (!handled) {
            handled = processSendMsg(responseInfo);
        }

        // 3. 处理消息播放
        if (!handled) {
            handled = processPlayMsg(responseInfo);
        }

        // 4. 处理异常逻辑
        if (!handled) {
            handled = processException(responseInfo);
        }

        return handled;
    }

    /**
     * 处理QQ消息接收
     *
     * @param rspInfo 资源信息
     * @return 是否已处理响应
     */
    private boolean processRcvMsg(XWResponseInfo rspInfo) {
        if (rspInfo.resources != null && rspInfo.resources.length >= 1
                && rspInfo.resources[0].resources != null
                && rspInfo.resources[0].resources.length >= 1
                && rspInfo.resources[0].resources[0].format == XWCommonDef.ResourceFormat.COMMAND
                && (rspInfo.resources[0].resources[0].ID.equals(COMMAND_RCV_TEXT_MSG)
                || rspInfo.resources[0].resources[0].ID.equals(COMMAND_RCV_AUDIO_MSG))) {

            if (rspInfo.resources[0].resources[0].ID.equals(COMMAND_RCV_TEXT_MSG)) {
                processTextMsg(rspInfo.resources[0].resources[0]);
            } else {
                processAudioMsg(rspInfo.resources[0].resources[0]);
            }

            return true;
        }


        return false;
    }


    /**
     * 处理QQ文本消息
     *
     * @param resourceInfo 资源信息
     */
    private void processTextMsg(XWResourceInfo resourceInfo) {
        if (TextUtils.isEmpty(resourceInfo.content) || TextUtils.isEmpty(resourceInfo.extendInfo)) {
            Log.e(TAG, "processTextMsg content is null or extendInfo is null");
            return;
        }

        long sender = 0;

        TextMsgInfo textMsgInfo = JsonUtil.getObject(resourceInfo.content, TextMsgInfo.class);

        try {
            JSONObject jsonObject = new JSONObject(resourceInfo.extendInfo);
            sender = jsonObject.getLong("sender");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (sender == 0 || textMsgInfo == null || TextUtils.isEmpty(textMsgInfo.getText())) {
            Log.e(TAG, "processTextMsg sender is 0 or text is null");
            return;
        }

        QQMsgEntry entry = new QQMsgEntry(sender, textMsgInfo);
        entry.setTimeStamp(System.currentTimeMillis() / 1000);

        MsgBoxManager.getInstance().addMsg(entry, new OnOperationFinishListener() {
            @Override
            public void onOperationFinish(QQMsgEntry entry, String action) {
                playOnceMsg(entry);
            }
        });
    }


    /**
     * 处理QQ语音消息，语音消息需要先下载
     *
     * @param resourceInfo 资源信息
     */
    private void processAudioMsg(XWResourceInfo resourceInfo) {
        if (TextUtils.isEmpty(resourceInfo.content) || TextUtils.isEmpty(resourceInfo.extendInfo)) {
            Log.e(TAG, "processAudioMsg content is null or extendInfo is null");
            return;
        }

        long sender = 0;

        try {
            JSONObject jsonObject = new JSONObject(resourceInfo.extendInfo);
            sender = jsonObject.getLong("sender");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AudioMsgDownloadInfo downloadInfo = JsonUtil.getObject(resourceInfo.content, AudioMsgDownloadInfo.class);

        if (sender == 0 || downloadInfo == null || TextUtils.isEmpty(downloadInfo.getFile_key())) {
            Log.e(TAG, "processAudioMsg sender is 0 or download is null or file key is null");
            return;
        }

        final QQMsgEntry entry = new QQMsgEntry(sender, downloadInfo);
        entry.setTimeStamp(System.currentTimeMillis() / 1000);

        XWFileTransferManager.downloadMiniFile(downloadInfo.getFile_key(),
                XWFileTransferInfo.TYPE_TRANSFER_FILE_AUDIO, downloadInfo.getFkey2(), new XWFileTransferManager.OnFileTransferListener() {

                    @Override
                    public void onProgress(long transferProgress, long maxTransferProgress) {

                    }

                    @Override
                    public void onComplete(XWFileTransferInfo info, int errorCode) {
                        if (errorCode == 0) {
                            Log.d(TAG, "downloadMiniFile success. local file: " + info.filePath);
                            entry.setContent(info.filePath);
                            MsgBoxManager.getInstance().addMsg(entry, new OnOperationFinishListener() {
                                @Override
                                public void onOperationFinish(QQMsgEntry entry, String action) {
                                    playOnceMsg(entry);
                                }
                            });

                        } else {
                            Log.e(TAG, "downloadMiniFile failed. errCode: " + errorCode);
                        }
                    }
                });
    }

    /**
     * 将消息实体转换为播放资源
     *
     * @param entry 消息实体
     * @return 播放资源列表
     */
    private XWResourceInfo[] msgEntryToMedias(QQMsgEntry entry) {
        XWResourceInfo[] resList = new XWResourceInfo[2];

        String resId = XWSDK.getInstance().request("TTS_TIPS", "qq_msg", "{\"tiny_id\":" + entry.getSender() + ",\"timestamp\":" + entry.getTimeStamp() + "}", null);
        XWResourceInfo resourceInfo = new XWResourceInfo();
        resourceInfo.ID = resId;
        resourceInfo.format = XWCommonDef.ResourceFormat.TTS;


        resList[0] = resourceInfo;

        resourceInfo = new XWResourceInfo();
        if (entry.getMsgType() == MSG_TYPE_TEXT) {
            resourceInfo.ID = String.valueOf(entry.getId());
            resourceInfo.format = XWCommonDef.ResourceFormat.TEXT;
            resourceInfo.content = entry.getContent();
            resList[1] = resourceInfo;
        } else {
            resourceInfo.ID = String.valueOf(entry.getId());
            resourceInfo.format = XWCommonDef.ResourceFormat.FILE;
            resourceInfo.content = entry.getContent();
            resList[1] = resourceInfo;
        }


        return resList;
    }

    /**
     * 处理收到的消息
     *
     * @param entry 消息实体
     */
    private void playOnceMsg(QQMsgEntry entry) {
        Log.d(TAG, "playOnceMsg: " + entry.toString());

        if (RecordDataManager.getInstance().isDeviceActive()) {
            XWResourceInfo[] resList = msgEntryToMedias(entry);
            for (XWResourceInfo item : resList) {
                msgPlayList.offer(item);
            }

            if (mPlayState == STATE_IDLE) {
                mPlayState = STATE_ONCE_PLAYING;

                msgPlayAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                            XWResourceInfo resourceInfo = msgPlayList.poll();
                            if (resourceInfo != null) {
                                player.playMediaInfo(resourceInfo, msgOnPlayListener);
                            }
                        } else if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_LOSS) {
                            player.stop();
                            msgPlayList.clear();
                            mPlayState = STATE_IDLE;
                        }
                    }
                };
                int ret = CommonApplication.mAudioManager.requestAudioFocus(msgPlayAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    msgPlayAudioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
            }

        } else {
            playTipMsg(MSG_RING, XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    /**
     * 处理消息发送逻辑
     *
     * @param rspInfo 响应数据
     * @return 是否已经处理该响应
     */
    private boolean processSendMsg(XWResponseInfo rspInfo) {
        if (rspInfo.resources != null && rspInfo.resources.length >= 1
                && rspInfo.resources[0].resources != null
                && rspInfo.resources[0].resources.length >= 1
                && rspInfo.resources[0].resources[0].format == XWCommonDef.ResourceFormat.COMMAND
                && rspInfo.resources[0].resources[0].ID.equals(COMMAND_SEND_AUDIO_MSG)) {

            if (TextUtils.isEmpty(rspInfo.resources[0].resources[0].content)) {
                Log.e(TAG, "processSendMsg targetId is empty");
            }

            mTargetId = Long.valueOf(rspInfo.resources[0].resources[0].content);

            mRequestInfo = new XWRequestInfo();
            mRequestInfo.requestParam |= XWCommonDef.REQUEST_PARAM.REQUEST_PARAM_ONLY_VAD;
            mRequestInfo.speakTimeout = MSG_RECORD_SPEAK_TIMEOUT;
            mRequestInfo.silentTimeout = MSG_RECORD_SILENT;

            playList.clear();
            curPlayIndex = 0;

            for (int i = 0; i < rspInfo.resources.length; i++) {
                XWResGroupInfo groupInfo = rspInfo.resources[i];
                for (int j = 0; j < groupInfo.resources.length; j++) {
                    if (groupInfo.resources[j].format == XWCommonDef.ResourceFormat.TTS
                            || groupInfo.resources[j].format == XWCommonDef.ResourceFormat.URL) {
                        playList.add(groupInfo.resources[j]);
                    }
                }
            }

            listener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                        if (curPlayIndex <= playList.size() - 1) {
                            player.playMediaInfo(playList.get(curPlayIndex), mOnPlayListener);
                        }
                    } else if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_LOSS) {
                        player.stop();
                        playList.clear();
                        curPlayIndex = 0;
                    }
                }
            };
            int ret = CommonApplication.mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }

            return true;
        }

        return false;
    }


    /**
     * 播放未读消息
     *
     * @param rspInfo 响应信息
     * @return 是否已处理响应
     */
    private boolean processPlayMsg(XWResponseInfo rspInfo) {
        if (rspInfo.resources != null && rspInfo.resources.length >= 1
                && rspInfo.resources[0].resources != null
                && rspInfo.resources[0].resources.length >= 1
                && rspInfo.resources[0].resources[0].format == XWCommonDef.ResourceFormat.COMMAND
                && rspInfo.resources[0].resources[0].ID.equals(COMMAND_PLAY_MSG)) {

            QQMsgEntry entry = MsgBoxManager.getInstance().getNextMsg();
            if (entry != null) {
                XWResourceInfo[] resList = msgEntryToMedias(entry);
                for (XWResourceInfo item : resList) {
                    msgPlayList.offer(item);
                }

                mPlayState = STATE_LOOP_PLAYING;

                msgPlayAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                            XWResourceInfo resourceInfo = msgPlayList.poll();
                            if (resourceInfo != null) {
                                player.playMediaInfo(resourceInfo, msgOnPlayListener);
                            }
                        } else if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_LOSS) {
                            player.stop();
                            msgPlayList.clear();
                            mPlayState = STATE_IDLE;
                        }
                    }
                };
                int ret = CommonApplication.mAudioManager.requestAudioFocus(msgPlayAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    msgPlayAudioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }

            } else {
                XWResourceInfo tmpRes = new XWResourceInfo();
                tmpRes.format = XWCommonDef.ResourceFormat.TEXT;
                tmpRes.ID = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                tmpRes.content = "没有更多消息了";
                playTipMsg(tmpRes, XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }

            return true;
        }


        return false;
    }

    /**
     * 处理异常场景，例如没有找到联系人
     *
     * @param rspInfo 响应信息
     * @return 是否已处理响应
     */
    private boolean processException(final XWResponseInfo rspInfo) {
        if (rspInfo.resources != null && rspInfo.resources.length >= 1
                && rspInfo.resources[0].resources != null
                && rspInfo.resources[0].resources.length >= 1
                && rspInfo.resources[0].resources[0].format == XWCommonDef.ResourceFormat.TTS) {

            playTipMsg(rspInfo.resources[0].resources[0], XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT);

            return true;
        }

        return false;
    }

    /**
     * 播放单个提示性资源
     *
     * @param url      播放url
     * @param duration 焦点类型
     */
    private void playTipMsg(final String url, final int duration) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        transientListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == duration) {
                    MusicPlayer.getInstance().playMediaInfo(url, new MusicPlayer.OnPlayListener() {
                        @Override
                        public void onCompletion(int error) {
                            CommonApplication.mAudioManager.abandonAudioFocus(transientListener);
                        }
                    });
                } else if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_LOSS) {
                    MusicPlayer.getInstance().stop();
                }
            }
        };
        int ret = CommonApplication.mAudioManager.requestAudioFocus(transientListener, AudioManager.STREAM_MUSIC, duration);
        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            transientListener.onAudioFocusChange(duration);
        }
    }

    /**
     * 播放单个提示性资源
     *
     * @param resourceInfo 播放资源
     * @param duration     焦点类型
     */
    private void playTipMsg(final XWResourceInfo resourceInfo, final int duration) {
        if (resourceInfo == null) {
            return;
        }

        transientListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == duration) {
                    MusicPlayer.getInstance().playMediaInfo(resourceInfo, new MusicPlayer.OnPlayListener() {
                        @Override
                        public void onCompletion(int error) {
                            CommonApplication.mAudioManager.abandonAudioFocus(transientListener);
                        }
                    });
                } else if (focusChange == XWeiAudioFocusManager.AUDIOFOCUS_LOSS) {
                    MusicPlayer.getInstance().stop();
                }
            }
        };
        int ret = CommonApplication.mAudioManager.requestAudioFocus(transientListener, AudioManager.STREAM_MUSIC, duration);
        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            transientListener.onAudioFocusChange(duration);
        }
    }

    /**
     * AMR压缩处理
     *
     * @param pcm     PCM数据
     * @param amrPath AMR压缩后存储路径
     */
    private void encodeVoiceData2Amr(byte[] pcm, String amrPath) {
        AmrEncoder.init(0);
        int mode = AmrEncoder.Mode.MR515.ordinal();

        try {
            OutputStream out = new FileOutputStream(amrPath);
            //下面的AMR的文件头,缺少这几个字节是不行的
            out.write(0x23);
            out.write(0x21);
            out.write(0x41);
            out.write(0x4D);
            out.write(0x52);
            out.write(0x0A);

            int voiceLength = pcm.length;

            if (pcm.length > 0) {
                String tmpFile = Environment.getExternalStoragePublicDirectory("tencent") + "/device/file/" + "pcm_" + voiceLength + ".pcm";
                OutputStream out2 = new FileOutputStream(tmpFile);
                out2.write(pcm, 0, pcm.length);
                out2.close();
            }

            int pcmSize = 640;
            int amrSize = 320;

            int voiceOffset = 0;
            while (voiceLength > 0 && voiceLength >= pcmSize) {

                byte[] range = Arrays.copyOfRange(pcm, voiceOffset, voiceOffset + pcmSize);

                byte[] newPcm = new byte[range.length / 2];
                for (int i = 0; i < newPcm.length; i += 2) {
                    if (i >= range.length / 2)
                        break;

                    System.arraycopy(range, 2 * i, newPcm, i, 2);
                }

                short[] shortin = new short[newPcm.length / 2];
                ByteBuffer.wrap(newPcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortin);


                byte[] outData = new byte[pcmSize / 2];
                int len = AmrEncoder.encode(mode, shortin, outData);
                if (len > 0) {
                    out.write(outData, 0, len);
                }

                voiceOffset += pcmSize;
                voiceLength -= pcmSize;
            }

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AmrEncoder.exit();

    }


    @Override
    public void onFeedAudioData(byte[] audioData) {
        try {
            baos.write(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyRequestEvent(int event, int errCode) {
        Log.d(TAG, "notifyRequestEvent event: " + event + " errCode: " + errCode);
        switch (event) {
            case XWCommonDef.XWEvent.ON_REQUEST_START:
                duration = System.currentTimeMillis();
                baos.reset();
                break;
            case XWCommonDef.XWEvent.ON_SILENT:
                //计算录音时长
                duration = (System.currentTimeMillis() - duration) / 1000;
                break;
            case XWCommonDef.XWEvent.ON_RESPONSE:
                if (errCode == XWCommonDef.XWeiErrorCode.VOICE_TIMEOUT) {
                    // 用户没有说话，就取消发送语音消息
                } else {
                    //编码语音文件amr
                    final String amrPath = Environment.getExternalStoragePublicDirectory("tencent") + "/device/file/" + "qqmsg_send_" + duration + ".amr";
                    encodeVoiceData2Amr(baos.toByteArray(), amrPath);

                    baos.reset();

                    //发送文件
                    final XWeiMessageInfo msg = new XWeiMessageInfo();
                    msg.type = XWeiMessageInfo.TYPE_AUDIO;
                    msg.receiver = new ArrayList<>();
                    msg.receiver.add(String.valueOf(mTargetId));
                    msg.content = amrPath;

                    XWeiMsgManager.sendMessage(msg, new XWeiMsgManager.OnSendMessageListener() {

                        @Override
                        public void onProgress(long transferProgress, long maxTransferProgress) {

                        }

                        @Override
                        public void onComplete(int errCode) {
                            Log.d(TAG, "sendMessage errCode " + errCode);
                            if (errCode == 0) {
                                QQMsgEntry entry = new QQMsgEntry();
                                entry.setRcv(false);
                                entry.setTimeStamp(System.currentTimeMillis());
                                entry.setHasRead(true);
                                entry.setDuration((int) duration);
                                entry.setContent(amrPath);
                                entry.setMsgType(MSG_TYPE_AUDIO);
                                Log.d(TAG, "sendMessage msg: " + entry.toString());
                                MsgBoxManager.getInstance().addMsg(entry, null);
                                playTipMsg(MSG_RING, XWeiAudioFocusManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                            }
                        }
                    });
                }
                break;
            default:
                break;
        }
    }
}
