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
package com.tencent.aiaudio.alarm;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tencent.aiaudio.CommonApplication;
import com.tencent.utils.MusicPlayer;
import com.tencent.xiaowei.control.Constants;
import com.tencent.xiaowei.control.XWeiOuterSkill;
import com.tencent.xiaowei.def.XWCommonDef;
import com.tencent.xiaowei.info.XWAppInfo;
import com.tencent.xiaowei.info.XWPlayStateInfo;
import com.tencent.xiaowei.info.XWResGroupInfo;
import com.tencent.xiaowei.info.XWResponseInfo;
import com.tencent.xiaowei.sdk.XWSDK;
import com.tencent.xiaowei.util.QLog;

import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_ALARM;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_TRIGGER_ALARM;

public class AlarmSkillHandler implements XWeiOuterSkill.OuterSkillHandler {

    public static final String TAG = AlarmSkillHandler.class.getSimpleName();
    public static final String EXTRA_KEY_CLOSE_ALARM = "extra_key_alarm_close";
    private Context mContext;
    private AudioManager.OnAudioFocusChangeListener listener;
    private static MusicPlayer player = new MusicPlayer();

    public AlarmSkillHandler(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean handleResponse(int sessionId, XWResponseInfo responseInfo) {
        boolean handled = false;
        switch (responseInfo.appInfo.ID) {
            case SKILL_ID_ALARM:
                handled = handleAlarm(responseInfo);
                break;
            case SKILL_ID_TRIGGER_ALARM:
                handled = handlerTriggerAlarm(responseInfo);
                break;
            default:
                break;
        }

        return handled;
    }

    // 处理提醒类
    private boolean handleAlarm(final XWResponseInfo responseInfo) {
        boolean handled = DeviceSkillAlarmManager.instance().isSetAlarmOperation(responseInfo);
        final boolean isSnooze = DeviceSkillAlarmManager.instance().isSnoozeAlarm(responseInfo);

        Log.d(TAG, "handleAlarm isSnooze: " + isSnooze);
        if (handled) {
            handled = DeviceSkillAlarmManager.instance().operationAlarmSkill(responseInfo);
        }

        // 除了增删改闹钟场景外，有可能还有存在单TTS播放资源
        if (handled || (responseInfo.resources.length > 0 && responseInfo.resources[0].resources.length > 0)) {
            CommonApplication.mAudioManager.abandonAudioFocus(listener);
            listener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) {
                        XWPlayStateInfo stateInfo = new XWPlayStateInfo();
                        stateInfo.appInfo = new XWAppInfo();
                        stateInfo.appInfo.ID = Constants.SkillIdDef.SKILL_ID_ALARM;
                        stateInfo.appInfo.name = Constants.SKILL_NAME.SKILL_NAME_ALARM;
                        stateInfo.state = XWCommonDef.PlayState.START;
                        XWSDK.getInstance().reportPlayState(stateInfo);

                        if (responseInfo.resources != null
                                && responseInfo.resources.length == 1
                                && responseInfo.resources[0].resources[0].format == XWCommonDef.ResourceFormat.TTS) {
                            Log.d(TAG, "onAudioFocusChange playMediaInfo");

                            player.playMediaInfo(responseInfo.resources[0].resources[0], new MusicPlayer.OnPlayListener() {
                                @Override
                                public void onCompletion(int error) {
                                    if (isSnooze) {
                                        sendBroadcast(EXTRA_KEY_CLOSE_ALARM, null);
                                    }
                                    CommonApplication.mAudioManager.abandonAudioFocus(listener);
                                }

                            });
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        XWPlayStateInfo stateInfo = new XWPlayStateInfo();
                        stateInfo.appInfo = new XWAppInfo();
                        stateInfo.appInfo.ID = Constants.SkillIdDef.SKILL_ID_ALARM;
                        stateInfo.appInfo.name = Constants.SKILL_NAME.SKILL_NAME_ALARM;
                        stateInfo.state = XWCommonDef.PlayState.FINISH;
                        XWSDK.getInstance().reportPlayState(stateInfo);

                        if (isSnooze) {
                            sendBroadcast(EXTRA_KEY_CLOSE_ALARM, null);
                        }
                        player.stop();
                        CommonApplication.mAudioManager.abandonAudioFocus(listener);
                    }
                }
            };

            int ret = CommonApplication.mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        }

        return handled || (responseInfo.resources.length > 0 && responseInfo.resources[0].resources.length > 0);
    }

    // 处理闹钟触发场景
    private boolean handlerTriggerAlarm(XWResponseInfo responseInfo) {
        boolean handled = false;

        int command = checkCommandId(responseInfo.resources);
        switch (command) {
            case Constants.PROPERTY_ID.PAUSE:
            case Constants.PROPERTY_ID.STOP:
            case Constants.PROPERTY_ID.EXIT:
                // 停止闹钟
                sendBroadcast(EXTRA_KEY_CLOSE_ALARM, null);
                handled = true;
                break;
        }

        return handled;

    }

    private int checkCommandId(XWResGroupInfo[] resources) {
        if (resources != null && resources.length > 0 && resources[0].resources.length > 0
                && resources[0].resources[0].format == XWCommonDef.ResourceFormat.COMMAND) {
            String id = resources[0].resources[0].ID;
            try {
                return Integer.valueOf(id);
            } catch (Exception e) {
            }
        }
        return 0;
    }

    private String getCommandValue(XWResGroupInfo[] resources) {
        if (resources != null && resources.length > 0 && resources[0].resources.length > 0
                && resources[0].resources[0].format == XWCommonDef.ResourceFormat.COMMAND) {
            return resources[0].resources[0].content;
        }
        return null;
    }

    private void sendBroadcast(String action, Bundle extra) {
        Intent intent = new Intent(action);
        if (extra != null)
            intent.putExtras(extra);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
