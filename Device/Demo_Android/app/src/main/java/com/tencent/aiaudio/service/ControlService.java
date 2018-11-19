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
package com.tencent.aiaudio.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.tencent.aiaudio.CommonApplication;
import com.tencent.aiaudio.activity.ActivityManager;
import com.tencent.aiaudio.activity.FMActivity;
import com.tencent.aiaudio.activity.MusicActivity;
import com.tencent.aiaudio.activity.NewsActivity;
import com.tencent.aiaudio.activity.OtherActivity;
import com.tencent.aiaudio.activity.WeatherActivity;
import com.tencent.aiaudio.alarm.AlarmSkillHandler;
import com.tencent.aiaudio.alarm.DeviceSkillAlarmManager;
import com.tencent.aiaudio.demo.IControlService;
import com.tencent.aiaudio.msg.SkillMsgHandler;
import com.tencent.aiaudio.player.XWeiPlayerMgr;
import com.tencent.aiaudio.tts.TTSManager;
import com.tencent.utils.MusicPlayer;
import com.tencent.utils.ThreadManager;
import com.tencent.xiaowei.control.Constants;
import com.tencent.xiaowei.control.IXWeiPlayer;
import com.tencent.xiaowei.control.XWeiControl;
import com.tencent.xiaowei.control.info.XWeiMediaInfo;
import com.tencent.xiaowei.control.info.XWeiPlayerInfo;
import com.tencent.xiaowei.control.info.XWeiPlaylistInfo;
import com.tencent.xiaowei.control.info.XWeiSessionInfo;
import com.tencent.xiaowei.def.XWCommonDef;
import com.tencent.xiaowei.info.MediaMetaInfo;
import com.tencent.xiaowei.info.XWAppInfo;
import com.tencent.xiaowei.info.XWResponseInfo;
import com.tencent.xiaowei.sdk.XWSDK;
import com.tencent.xiaowei.util.JsonUtil;
import com.tencent.xiaowei.util.QLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.tencent.xiaowei.control.Constants.GET_MORE_LIST_TYPE.TYPE_GET_HISTORY;
import static com.tencent.xiaowei.control.Constants.GET_MORE_LIST_TYPE.TYPE_GET_MORE;
import static com.tencent.xiaowei.control.Constants.GET_MORE_LIST_TYPE.TYPE_GET_MORE_UP;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_ALARM;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_FM;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_MUSIC;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_New;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_QQ_MSG;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_TRIGGER_ALARM;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_Unknown;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WEATHER;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WECHAT_MSG;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WIKI_AI_LAB;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WIKI_Calculator;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WIKI_HISTORY;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WIKI_Time;
import static com.tencent.xiaowei.control.Constants.SkillIdDef.SKILL_ID_WX_Chat;
import static com.tencent.xiaowei.control.XWMediaType.TYPE_MUSIC_URL;

/**
 * 用于启动各个Skill场景
 */
public class ControlService extends Service implements XWeiPlayerMgr.SkillUIEventListener {
    public static final String EXTRA_KEY_START_SKILL_DATA = "extra_key_start_skill_data";
    public static final String EXTRA_KEY_START_SKILL_SESSION_ID = "extra_key_start_skill_session_id";
    public static final String EXTRA_KEY_START_SKILL_NAME = "extra_key_start_skill_app_name";
    public static final String EXTRA_KEY_START_SKILL_ID = "extra_key_start_skill_app_id";
    public static final String EXTRA_KEY_START_SKILL_ANSWER = "extra_key_start_skill_answer";
    public static final String EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID = "extra_key_music_on_event_session_id";
    public static final String EXTRA_KEY_MUSIC_ON_EVENT_PLAY_ID = "extra_key_music_on_event_play_id";
    public static final String EXTRA_KEY_MUSIC_ON_UPDATE_ITEM_DETAIL = "extra_key_music_on_update_item_detail";
    public static final String ACTION_MUSIC_ON_PLAY = "action_music_on_play";
    public static final String ACTION_MUSIC_ON_PAUSE = "action_music_on_pause";
    public static final String ACTION_MUSIC_ON_RESUME = "action_music_on_resume";
    public static final String ACTION_MUSIC_ON_REPEAT_MODE = "action_music_on_repeat_mode";
    public static final String ACTION_MUSIC_ON_STOP = "action_music_on_stop";
    public static final String ACTION_MUSIC_ON_UPDATE_PLAY_LIST = "action_music_on_update_play_list";
    public static final String ACTION_MUSIC_ON_UPDATE_HISTORY_PLAY_LIST = "action_music_on_update_history_play_list";
    public static final String ACTION_MUSIC_ON_UNKEEP = "action_music_on_unkeep";
    public static final String ACTION_MUSIC_ON_KEEP = "action_music_on_keep";
    private static final String TAG = ControlService.class.getSimpleName();
    private SparseArray<String> session2CurPlayId = new SparseArray<>();
    private long lastUpdateTime = 0;
    private SparseArray<ArrayList<String>> session2PlayIdArray = new SparseArray<>();// sessionId 找到playlist的playid列表
    private SparseArray<ArrayList<String>> session2HistoryPlayIdArray = new SparseArray<>();// sessionId 找到history playlist的playid列表
    private HashMap<String, MediaMetaInfo> id2PlayInfo = new HashMap<>();
    private volatile boolean isLoadingMore;
    private Handler mHandler = new Handler();

    private AudioManager.OnAudioFocusChangeListener listener;
    private AlarmSkillHandler alarmSkillHandler; // 处理AlarmSkillHandler

    @Override
    public void onCreate() {
        super.onCreate();

        XWeiPlayerMgr.setPlayerEventListener(this);
        alarmSkillHandler = new AlarmSkillHandler(getApplicationContext());
        XWeiControl.getInstance().getXWeiOuterSkill().registerSkillIdOrSkillName(SKILL_ID_ALARM, alarmSkillHandler);
        XWeiControl.getInstance().getXWeiOuterSkill().registerSkillIdOrSkillName(SKILL_ID_TRIGGER_ALARM, alarmSkillHandler);
        XWeiControl.getInstance().getXWeiOuterSkill().registerSkillIdOrSkillName(SKILL_ID_QQ_MSG, SkillMsgHandler.getInstance());
        XWeiControl.getInstance().getXWeiOuterSkill().registerSkillIdOrSkillName(SKILL_ID_WECHAT_MSG, SkillMsgHandler.getInstance());

        DeviceSkillAlarmManager.instance().init(getApplication());
        DeviceSkillAlarmManager.instance().startDeviceAllAlarm();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new IControlServiceImpl();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XWeiPlayerMgr.setPlayerEventListener(null);
        alarmSkillHandler = null;
        XWeiControl.getInstance().getXWeiOuterSkill().unRegisterSkillIdOrSkillName(SKILL_ID_ALARM);
        XWeiControl.getInstance().getXWeiOuterSkill().unRegisterSkillIdOrSkillName(SKILL_ID_TRIGGER_ALARM);
        XWeiControl.getInstance().getXWeiOuterSkill().unRegisterSkillIdOrSkillName(SKILL_ID_QQ_MSG);
        XWeiControl.getInstance().getXWeiOuterSkill().unRegisterSkillIdOrSkillName(SKILL_ID_WECHAT_MSG);
    }

    @Override
    public void onPlaylistAddAlbum(int sessionId, XWeiMediaInfo mediaInfo) {
        XWeiPlaylistInfo playlistInfo = XWeiControl.getInstance().getMediaTool().txcGetPlaylistInfo(sessionId);
        startSkillUI(sessionId, mediaInfo);
    }

    @Override
    public void onPlaylistAddItem(int sessionId, int resourceListType, boolean isFront, XWeiMediaInfo[] mediaInfoArray) {
        addPlayListForSkill(sessionId, resourceListType, isFront, mediaInfoArray);
    }

    @Override
    public void onPlaylistUpdateItem(int sessionId, XWeiMediaInfo[] mediaInfoArray) {
        updatePlayListForSkill(sessionId, mediaInfoArray);
    }

    @Override
    public void onPlayListRemoveItem(int sessionId, XWeiMediaInfo[] mediaInfoArray) {
        removePlayListForSkill(sessionId, mediaInfoArray);
    }

    @Override
    public void onPlay(int sessionId, XWeiMediaInfo mediaInfo, boolean fromUser) {

        Log.d(TAG, "onPlay resId: " + mediaInfo.resId);
        XWeiSessionInfo sessionInfo = XWeiControl.getInstance().getAppTool().txcGetSession(sessionId);
        if (sessionInfo == null || (TextUtils.isEmpty(sessionInfo.skillName) || TextUtils.isEmpty(sessionInfo.skillId))) {
            return;
        }

        if (sessionInfo.skillId.equals(SKILL_ID_MUSIC) || sessionInfo.skillId.equals(SKILL_ID_FM)) {

            if (mediaInfo.mediaType == TYPE_MUSIC_URL) {
                MediaMetaInfo currentPlayInfo = JsonUtil.getObject(mediaInfo.description, MediaMetaInfo.class);


                if (currentPlayInfo != null) {
                    session2CurPlayId.put(sessionId, currentPlayInfo.playId);
                    Bundle bundle = new Bundle();
                    bundle.putInt(EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID, sessionId);
                    sendBroadcast(ACTION_MUSIC_ON_PLAY, bundle);

                    getDetailInfoIfNeed(sessionInfo.skillName, sessionInfo.skillId, id2PlayInfo.get(currentPlayInfo.playId));

                    refreshPlayListIfNeed(sessionId, false);
                }
            }
        } else if (sessionInfo.skillId.equals(SKILL_ID_New)) {
            if (mediaInfo.mediaType == TYPE_MUSIC_URL) {
                MediaMetaInfo currentPlayInfo = JsonUtil.getObject(mediaInfo.description, MediaMetaInfo.class);


                if (currentPlayInfo != null) {
                    session2CurPlayId.put(sessionId, currentPlayInfo.playId);
                    Bundle bundle = new Bundle();
                    bundle.putInt(EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID, sessionId);
                    sendBroadcast(ACTION_MUSIC_ON_PLAY, bundle);
                }
            }
        }
    }

    @Override
    public void onPause(int sessionId) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID, sessionId);
        sendBroadcast(ACTION_MUSIC_ON_PAUSE, bundle);
    }

    @Override
    public void onResume(int sessionId) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID, sessionId);
        sendBroadcast(ACTION_MUSIC_ON_RESUME, bundle);
    }

    @Override
    public void onSetPlayMode(int sessionId, int repeatMode) {
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_MUSIC_ON_EVENT_SESSION_ID, sessionId);
        sendBroadcast(ACTION_MUSIC_ON_REPEAT_MODE, bundle);
    }

    @Override
    public void onFinish(int sessionId) {
        ActivityManager.getInstance().finish(sessionId);
        TTSManager.getInstance().release(sessionId);
    }

    @Override
    public void onFavoriteEvent(String event, String playId) {
        boolean isKeep = event.equals("收藏");
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_KEY_MUSIC_ON_EVENT_PLAY_ID, playId);
        sendBroadcast(isKeep ? ACTION_MUSIC_ON_KEEP : ACTION_MUSIC_ON_UNKEEP, bundle);

        MediaMetaInfo mediaMetaInfo = id2PlayInfo.get(playId);
        if (mediaMetaInfo != null) {
            mediaMetaInfo.favorite = isKeep;
        }
    }

    @Override
    public void onTips(int tipsType) {
        switch (tipsType) {
            case Constants.TXPlayerTipsType.PLAYER_TIPS_NEXT_FAILURE:
                XWSDK.getInstance().requestTTS("当前列表没有更多了，您可以重新点播".getBytes(), new XWSDK.RequestListener() {
                    @Override
                    public boolean onRequest(int event, final XWResponseInfo rspData, byte[] extendData) {
                        CommonApplication.mAudioManager.abandonAudioFocus(listener);
                        listener = new AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                                if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
                                    MusicPlayer.getInstance().playMediaInfo(rspData.resources[0].resources[0], new MusicPlayer.OnPlayListener() {
                                        @Override
                                        public void onCompletion(int error) {
                                            CommonApplication.mAudioManager.abandonAudioFocus(listener);
                                        }

                                    });
                                } else {
                                    MusicPlayer.getInstance().stop();
                                }
                            }
                        };
                        int ret = CommonApplication.mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        }
                        return true;
                    }
                });
                break;
            case Constants.TXPlayerTipsType.PLAYER_TIPS_PREV_FAILURE:
                XWSDK.getInstance().requestTTS("当前列表没有上一首了".getBytes(), new XWSDK.RequestListener() {
                    @Override
                    public boolean onRequest(int event, final XWResponseInfo rspData, byte[] extendData) {
                        CommonApplication.mAudioManager.abandonAudioFocus(listener);
                        listener = new AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                                if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
                                    MusicPlayer.getInstance().playMediaInfo(rspData.resources[0].resources[0], new MusicPlayer.OnPlayListener() {
                                        @Override
                                        public void onCompletion(int error) {
                                            CommonApplication.mAudioManager.abandonAudioFocus(listener);
                                        }

                                    });
                                } else {
                                    MusicPlayer.getInstance().stop();
                                }
                            }
                        };
                        int ret = CommonApplication.mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        }
                        return true;
                    }
                });
                break;
        }
    }

    @Override
    public void onAutoWakeup(int sessionId, String contextId, int speakTimeout, int silentTimeout, long requestParam) {
        AIAudioService service = AIAudioService.getInstance();
        if (service != null) {
            service.wakeup(contextId, speakTimeout, silentTimeout, requestParam);
        }
    }

    @Override
    public void onGetMoreList(int sessionId, final int type, final String playId) {
        XWeiSessionInfo sessionInfo = XWeiControl.getInstance().getAppTool().txcGetSession(sessionId);
        if (sessionInfo == null) {
            return;
        }
        final XWAppInfo appInfo = new XWAppInfo();
        appInfo.name = sessionInfo.skillName;
        appInfo.ID = sessionInfo.skillId;

        if (type == TYPE_GET_HISTORY) {
            // 可以获取到历史播放过的歌单，后台会存储一部分数据
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    XWSDK.getInstance().request("PLAY_RESOURCE", "get_history",
                            "{\"skill_info\":{\"id\":\"" + appInfo.ID + "\",\"name\":\"" + appInfo.name + "\"},\"cur_play_id\":\"" + playId + "\"}", new XWSDK.OnRspListener() {
                                @Override
                                public void onRsp(String voiceId, int error, String json) {
                                    QLog.d(TAG, "get_history:" + json);
                                    XWResponseInfo rspData = XWResponseInfo.fromCmdJson(json);
                                    if (rspData.resources != null && rspData.resources.length > 0)
                                        XWeiControl.getInstance().processResponse(voiceId, rspData, null);
                                }
                            });
                }
            });
        } else if (type == TYPE_GET_MORE || type == TYPE_GET_MORE_UP) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    XWSDK.getInstance().request("PLAY_RESOURCE", "get_more",
                            "{\"skill_info\":{\"id\":\"" + appInfo.ID + "\",\"name\":\"" + appInfo.name + "\"},\"play_id\":\"" + playId + "\",\"is_up\":" + (type == 1) + "}", new XWSDK.OnRspListener() {
                                @Override
                                public void onRsp(String voiceId, int error, String json) {
                                    QLog.d(TAG, "get_more:" + json);
                                    XWResponseInfo rspData = XWResponseInfo.fromCmdJson(json);
                                    XWeiControl.getInstance().processResponse(voiceId, rspData, null);
                                }
                            });
                }
            }, 300);

        }
    }

    /**
     * 根据响应结果启动通用SKill场景界面，例如天气，新闻，百科
     *
     * @param sessionId 与场景关联的sessionId
     * @param mediaInfo 封面媒体信息
     */
    private void startSkillUI(int sessionId, XWeiMediaInfo mediaInfo) {
        XWeiSessionInfo sessionInfo = XWeiControl.getInstance().getAppTool().txcGetSession(sessionId);
        if (sessionInfo == null || (TextUtils.isEmpty(sessionInfo.skillName) || TextUtils.isEmpty(sessionInfo.skillId))) {
            return;
        }

        switch (sessionInfo.skillId) {
            case SKILL_ID_WEATHER: {
                Intent intent = new Intent(getApplicationContext(), WeatherActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_KEY_START_SKILL_DATA, mediaInfo.description);
                intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                startActivity(intent);
                break;
            }
            case SKILL_ID_MUSIC: {
                lastUpdateTime = System.currentTimeMillis();
                Intent intent = new Intent(getApplicationContext(), MusicActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                intent.putExtra(EXTRA_KEY_START_SKILL_DATA, mediaInfo.description);
                intent.putExtra(EXTRA_KEY_START_SKILL_NAME, sessionInfo.skillName);
                intent.putExtra(EXTRA_KEY_START_SKILL_ID, sessionInfo.skillId);
                startActivity(intent);
                break;
            }
            case SKILL_ID_FM: {
                Intent intent = new Intent(getApplicationContext(), FMActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                intent.putExtra(EXTRA_KEY_START_SKILL_DATA, mediaInfo.description);
                intent.putExtra(EXTRA_KEY_START_SKILL_NAME, sessionInfo.skillName);
                intent.putExtra(EXTRA_KEY_START_SKILL_ID, sessionInfo.skillId);
                startActivity(intent);
                break;
            }
            case SKILL_ID_New: {
                XWeiPlaylistInfo playlistInfo = XWeiControl.getInstance().getMediaTool().txcGetPlaylistInfo(sessionId);

                if (playlistInfo.count <= 1) {
                    Intent intent = new Intent(getApplicationContext(), OtherActivity.class);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                    intent.putExtra(EXTRA_KEY_START_SKILL_NAME, sessionInfo.skillName);
                    intent.putExtra(EXTRA_KEY_START_SKILL_ANSWER, mediaInfo.content);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                    intent.putExtra(EXTRA_KEY_START_SKILL_NAME, sessionInfo.skillName);
                    intent.putExtra(EXTRA_KEY_START_SKILL_ID, sessionInfo.skillId);
                    startActivity(intent);
                }
                break;
            }
            case SKILL_ID_WIKI_HISTORY:
            case SKILL_ID_WIKI_AI_LAB:
            case SKILL_ID_WIKI_Time:
            case SKILL_ID_WX_Chat:
            case SKILL_ID_Unknown:
            case SKILL_ID_WIKI_Calculator: {
                Intent intent = new Intent(getApplicationContext(), OtherActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
                intent.putExtra(EXTRA_KEY_START_SKILL_NAME, sessionInfo.skillName);
                intent.putExtra(EXTRA_KEY_START_SKILL_ID, sessionInfo.skillId);
                intent.putExtra(EXTRA_KEY_START_SKILL_ANSWER, mediaInfo.content);
                startActivity(intent);
                break;
            }
            default:
                if (!TextUtils.isEmpty(mediaInfo.content))
                    CommonApplication.showToast(mediaInfo.content);
                break;
        }
    }

    /**
     * 添加播放资源
     *
     * @param sessionId      场景sessionId
     * @param mediaInfoArray 播放资源
     */
    private void addPlayListForSkill(int sessionId, int resourceListType, boolean isFront, XWeiMediaInfo[] mediaInfoArray) {
        QLog.d(TAG, "addPlayListForSkill sessionId=" + sessionId + ",resourceListType=" + resourceListType + ",isFront=" + isFront + ",mediaInfoArray.length :" + mediaInfoArray.length);
        SparseArray<ArrayList<String>> list = resourceListType == XWCommonDef.ResourceListType.DEFAULT ? session2PlayIdArray : session2HistoryPlayIdArray;
        ArrayList<String> playIdArray = list.get(sessionId);
        if (playIdArray == null) {
            playIdArray = new ArrayList<>();
        }

        int i = 0;
        for (XWeiMediaInfo info : mediaInfoArray) {
            MediaMetaInfo item = JsonUtil.getObject(info.description, MediaMetaInfo.class);
            if (item != null) {
                if (isFront) {
                    playIdArray.add(i, item.playId);
                } else {
                    playIdArray.add(item.playId);
                }
                id2PlayInfo.put(item.playId, item);
                i++;
            }
        }

        list.put(sessionId, playIdArray);

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
        sendBroadcast(resourceListType == XWCommonDef.ResourceListType.DEFAULT ? ACTION_MUSIC_ON_UPDATE_PLAY_LIST : ACTION_MUSIC_ON_UPDATE_HISTORY_PLAY_LIST, bundle);
    }

    /**
     * 更新列表资源项
     *
     * @param sessionId      场景sessionId
     * @param mediaInfoArray 播放资源
     */
    private void updatePlayListForSkill(int sessionId, XWeiMediaInfo[] mediaInfoArray) {
        Log.d(TAG, "updatePlayListForSkill mediaInfoArray.length :" + mediaInfoArray.length);
        for (XWeiMediaInfo info : mediaInfoArray) {
            try {
                MediaMetaInfo item = JsonUtil.getObject(info.description, MediaMetaInfo.class);
                if (item != null) {
                    id2PlayInfo.put(item.playId, item);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
        sendBroadcast(ACTION_MUSIC_ON_UPDATE_PLAY_LIST, bundle);
    }

    /**
     * 删减播放资源
     *
     * @param sessionId      场景sessionId
     * @param mediaInfoArray 播放资源
     */
    private void removePlayListForSkill(int sessionId, XWeiMediaInfo[] mediaInfoArray) {
        ArrayList<String> removePlayIdArray = new ArrayList<>();
        for (XWeiMediaInfo info : mediaInfoArray) {
            MediaMetaInfo item = JsonUtil.getObject(info.description, MediaMetaInfo.class);
            if (item != null) {
                removePlayIdArray.add(item.playId);
                id2PlayInfo.remove(item.playId);
            }
        }

        ArrayList<String> playIdArray = session2PlayIdArray.get(sessionId);
        boolean updateDefaultPlayList = false;
        boolean updateHistoryPlayList = false;
        if (playIdArray != null) {
            updateDefaultPlayList = playIdArray.removeAll(removePlayIdArray);

        }
        playIdArray = session2HistoryPlayIdArray.get(sessionId);
        if (playIdArray != null) {
            updateHistoryPlayList = playIdArray.removeAll(removePlayIdArray);
        }
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_KEY_START_SKILL_SESSION_ID, sessionId);
        if (updateDefaultPlayList) {
            sendBroadcast(ACTION_MUSIC_ON_UPDATE_PLAY_LIST, bundle);
        }
        if (updateHistoryPlayList) {
            sendBroadcast(ACTION_MUSIC_ON_UPDATE_HISTORY_PLAY_LIST, bundle);
        }
    }

    private void sendBroadcast(String action, Bundle extra) {
        Intent intent = new Intent(action);
        if (extra != null)
            intent.putExtras(extra);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * 拉取播放资源的详情信息
     *
     * @param skillName     场景名
     * @param skillId       场景Id
     * @param mediaMetaInfo 播放资源
     */
    private void getDetailInfoIfNeed(final String skillName, final String skillId, final MediaMetaInfo mediaMetaInfo) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (mediaMetaInfo != null && (TextUtils.isEmpty(mediaMetaInfo.lyric) || mediaMetaInfo.duration == 0)) {
                    XWAppInfo appInfo = new XWAppInfo();
                    appInfo.name = skillName;
                    appInfo.ID = skillId;
                    ArrayList<String> list = new ArrayList<>(1);
                    list.add(mediaMetaInfo.playId);
                    XWSDK.getInstance().request("PLAY_RESOURCE", "get_detail",
                            "{\"skill_info\":{\"id\":\"" + appInfo.ID + "\",\"name\":\"" + appInfo.name + "\"},\"play_ids\":" + JsonUtil.toJson(list) + "}", new XWSDK.OnRspListener() {
                                @Override
                                public void onRsp(String voiceId, int error, String json) {
                                    QLog.d(TAG, "get_detail:" + json);
                                    XWResponseInfo rspData = XWResponseInfo.fromCmdJson(json);
                                    // 让控制层处理具体的数据
                                    if (rspData.resources != null && rspData.resources.length > 0)
                                        XWeiControl.getInstance().processResponse(voiceId, rspData, null);

                                }
                            });
                }
            }
        };

        ThreadManager.getInstance().start(runnable);
    }

    /**
     * url过期或者需要切换品质则刷新列表
     *
     * @param sessionId 场景id
     */
    private void refreshPlayListIfNeed(final int sessionId, boolean isForce) {
        final XWeiSessionInfo sessionInfo = XWeiControl.getInstance().getAppTool().txcGetSession(sessionId);

        if (!isForce && (System.currentTimeMillis() - lastUpdateTime) < 24 * 3600 * 1000) {
            return;
        }


        ThreadManager.getInstance().start(new Runnable() {
            @Override
            public void run() {
                XWAppInfo appInfo = new XWAppInfo();
                appInfo.name = sessionInfo.skillName;
                appInfo.ID = sessionInfo.skillId;
                ArrayList<String> playIdArray = session2PlayIdArray.get(sessionId);
                ArrayList<String> historyPlayIdArray = session2HistoryPlayIdArray.get(sessionId);
                ArrayList<String> playIds = new ArrayList<>();
                if (playIdArray != null)
                    playIds.addAll(playIdArray);
                if (historyPlayIdArray != null)
                    playIds.addAll(historyPlayIdArray);
                ArrayList<String> list = new ArrayList<>();
                while (playIds.size() > 0) {
                    // 需要拆分请求
                    list.clear();
                    int count = Math.min(30, playIds.size());
                    for (int i = 0; i < count; i++) {
                        list.add(playIds.remove(0));
                    }
                    XWSDK.getInstance().request("PLAY_RESOURCE", "refresh",
                            "{\"skill_info\":{\"id\":\"" + appInfo.ID + "\",\"name\":\"" + appInfo.name + "\"},\"play_ids\":" + JsonUtil.toJson(list) + "}", new XWSDK.OnRspListener() {
                                @Override
                                public void onRsp(String voiceId, int error, String json) {
                                    QLog.d(TAG, "refresh:" + json);
                                    XWResponseInfo rspData = XWResponseInfo.fromCmdJson(json);
                                    // 让控制层处理具体的数据
                                    if (rspData.resources != null && rspData.resources.length > 0)
                                        XWeiControl.getInstance().processResponse(voiceId, rspData, null);
                                }
                            });
                }
            }
        });
    }

    private class IControlServiceImpl extends IControlService.Stub {

        @Override
        public int getCurrentPlayMode(int sessionId) {
            XWeiPlayerInfo playerInfo = XWeiControl.getInstance().getMediaTool().txcGetPlayerInfo(sessionId);
            if (playerInfo != null) {
                return playerInfo.repeatMode;
            } else {
                return Constants.RepeatMode.REPEAT_MODE_SEQUENCE;
            }
        }

        @Override
        public boolean isPlaying(int sessionId) {

            XWeiPlayerInfo playerInfo = XWeiControl.getInstance().getMediaTool().txcGetPlayerInfo(sessionId);
            return playerInfo != null && playerInfo.status == Constants.XWeiInnerPlayerStatus.STATUS_PLAY;
        }

        @Override
        public int getCurrentPosition(int sessionId) {
            IXWeiPlayer player = XWeiControl.getInstance().getXWeiPlayerMgr().getXWeiPlayer(sessionId);

            return player != null ? player.getCurrentPosition() : 0;
        }

        @Override
        public int getDuration(int sessionId) {
            IXWeiPlayer player = XWeiControl.getInstance().getXWeiPlayerMgr().getXWeiPlayer(sessionId);

            return player != null ? player.getDuration() : 0;
        }

        @Override
        public void seekTo(int sessionId, int position) {
            IXWeiPlayer player = XWeiControl.getInstance().getXWeiPlayerMgr().getXWeiPlayer(sessionId);

            if (player != null) {
                player.seekTo(position);
            }
        }

        @Override
        public List<MediaMetaInfo> getCurrentMediaList(int sessionId) {
            ArrayList<MediaMetaInfo> playList = new ArrayList<>();

            ArrayList<String> playIdArray = session2PlayIdArray.get(sessionId);

            if (playIdArray != null) {
                for (String playId : playIdArray) {
                    playList.add(id2PlayInfo.get(playId));
                }
            }

            return playList;
        }

        @Override
        public List<MediaMetaInfo> getCurrentHistoryMediaList(int sessionId) {
            ArrayList<MediaMetaInfo> playList = new ArrayList<>();

            ArrayList<String> playIdArray = session2HistoryPlayIdArray.get(sessionId);

            if (playIdArray != null) {
                for (String playId : playIdArray) {
                    playList.add(id2PlayInfo.get(playId));
                }
            }

            return playList;
        }

        @Override
        public MediaMetaInfo getCurrentMediaInfo(int sessionId) {
            String playId = session2CurPlayId.get(sessionId);

            return id2PlayInfo.get(playId);
        }

        @Override
        public void getMoreList(final int sessionId, final boolean isUp) {
            // UI滑动到底 根据sessionId预加载播放资源，如果已经在拉了，就不重复加载，并判断是否需要拉取。
            final ArrayList<String> playIdList = session2PlayIdArray.get(sessionId);
            if (playIdList == null || playIdList.size() == 0) {
                return;
            }

            XWeiPlaylistInfo playlistInfo = XWeiControl.getInstance().getMediaTool().txcGetPlaylistInfo(sessionId);
            XWeiSessionInfo sessionInfo = XWeiControl.getInstance().getAppTool().txcGetSession(sessionId);

            QLog.d(TAG, "playlistInfo: " + playlistInfo + " isLoadingMore: " + isLoadingMore);
            if (isLoadingMore) {
                return;
            }
            // 没上拉取了，并且没有历史，就查一下历史列表
            if (isUp && !playlistInfo.hasMoreCurrentUp && playlistInfo.hasHistory && session2HistoryPlayIdArray.get(sessionId) != null && session2HistoryPlayIdArray.get(sessionId).size() == 0) {
                onGetMoreList(sessionId, TYPE_GET_HISTORY, playIdList.get(0));
            }
            if (((playlistInfo.hasMoreCurrent && !isUp) || (playlistInfo.hasMoreCurrentUp && isUp))) {
                isLoadingMore = true;

                XWAppInfo skill = new XWAppInfo();
                skill.name = sessionInfo.skillName;
                skill.ID = sessionInfo.skillId;

                XWSDK.getInstance().request("PLAY_RESOURCE", "get_more",
                        "{\"skill_info\":{\"id\":\"" + skill.ID + "\",\"name\":\"" + skill.name + "\"},\"play_id\":\"" + (isUp ? playIdList.get(0) : playIdList.get(playIdList.size() - 1)) + "\",\"is_up\":" + isUp + "}", new XWSDK.OnRspListener() {
                            @Override
                            public void onRsp(String voiceId, int error, String json) {
                                QLog.d(TAG, "get_more ui:" + json);
                                XWResponseInfo rspData = XWResponseInfo.fromCmdJson(json);
                                // 让控制层处理具体的数据
                                isLoadingMore = false;
                                XWeiControl.getInstance().processResponse(voiceId, rspData, null);
                            }
                        });
            }
        }

        @Override
        public void refreshPlayList(final int sessionId) {
            refreshPlayListIfNeed(sessionId, true);
        }

    }
}
