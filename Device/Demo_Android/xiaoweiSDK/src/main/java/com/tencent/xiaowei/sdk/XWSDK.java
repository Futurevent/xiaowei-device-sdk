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
package com.tencent.xiaowei.sdk;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.tencent.xiaowei.def.XWCommonDef;
import com.tencent.xiaowei.info.XWAccountInfo;
import com.tencent.xiaowei.info.XWAppInfo;
import com.tencent.xiaowei.info.XWBinderInfo;
import com.tencent.xiaowei.info.XWContextInfo;
import com.tencent.xiaowei.info.XWEventLogInfo;
import com.tencent.xiaowei.info.XWLoginInfo;
import com.tencent.xiaowei.info.XWLoginStatusInfo;
import com.tencent.xiaowei.info.XWPlayStateInfo;
import com.tencent.xiaowei.info.XWRequestInfo;
import com.tencent.xiaowei.info.XWResponseInfo;
import com.tencent.xiaowei.info.XWTTSDataInfo;
import com.tencent.xiaowei.util.QLog;
import com.tencent.xiaowei.util.Singleton;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音服务接口
 */
public class XWSDK {
    private static final String TAG = "XWSDK";

    private ConcurrentHashMap<String, RequestListener> mMapRequestListener = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, GetAlarmListRspListener> mDeviceGetAlarmListListener = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, SetAlarmRspListener> mDeviceSetAlarmListener = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, OnRspListener> mRspListenerMap = new ConcurrentHashMap<>();
    private OnSetWordsListListener mSetWordsListListener = null;
    static boolean online;
    private Context mContext;
    private Thread mUiThread;
    private Handler mainHandler;

    private AudioRequestListener audioRequestListener;
    private NetworkDelayListener mNetworkDelayListener;
    private OnReceiveTTSDataListener mOnReceiveTTSDataListener;

    XWLoginInfo mXWLoginInfo;

    private XWSDK() {

    }

    private static final Singleton<XWSDK> sSingleton = new Singleton<XWSDK>() {
        @Override
        protected XWSDK createInstance() {
            return new XWSDK();
        }
    };

    /**
     * 获取语音服务实例
     *
     * @return
     */
    public static XWSDK getInstance() {
        return sSingleton.getInstance();
    }

    /**
     * 初始化语音服务 use {@link #login} instead.
     *
     * @param context     上下文对象，不能为null
     * @param accountInfo 账户信息，使用小微App对接传null即可
     */
    @Deprecated
    public int init(Context context, XWAccountInfo accountInfo) {
        mContext = context.getApplicationContext();
        if (mainHandler == null) {
            mainHandler = new Handler(context.getMainLooper());
        }
        if (mUiThread == null) {
            mUiThread = context.getMainLooper().getThread();
        }
        XWSDKJNI.startXiaoweiService();
        XWSDKJNI.setXWAccountInfo(accountInfo);
        return 0;
    }

    /**
     * 登录小微服务，所有的服务都应该在登录后进行
     *
     * @param context   上下文对象
     * @param loginInfo 登录信息
     * @return 错误码
     */
    public void login(Context context, XWLoginInfo loginInfo, OnXWLoginListener listener) {
        mContext = context.getApplicationContext();

        if (mContext == null) {
            throw new RuntimeException("context cannot be null.");
        }
        if (listener == null) {
            throw new RuntimeException("Init XWSDK failed,listener is null.");
        }
        // 先检查一遍loginInfo的基本格式
        if (loginInfo == null) {
            listener.onCheckParam(3, "loginInfo为空。");
            return;
        }
        if (TextUtils.isEmpty(loginInfo.serialNumber) || loginInfo.serialNumber.length() != 16) {
            listener.onCheckParam(3, "SN格式不正确，请传入符合规则的16位SN。");
            return;
        }
        if (TextUtils.isEmpty(loginInfo.license) || TextUtils.isEmpty(loginInfo.srvPubKey) || loginInfo.productId == 0) {
            listener.onCheckParam(3, "loginInfo参数非法。");
            return;
        }

        if (mainHandler == null) {
            mainHandler = new Handler(context.getMainLooper());
        }
        if (mUiThread == null) {
            mUiThread = context.getMainLooper().getThread();
        }

        mXWLoginInfo = loginInfo;
        QLog.init(context, context.getPackageName());
        XWSDKJNI.getInstance().initJNI(5);// 控制打印native 的日志级别 取值[0-5],0表示关闭日志，1-5对应[e,w,i,d,v]。数字越大打印的日志级别越多。

        /* err_failed                              = 0x00000001,       //failed 关键Service等对象不存在
         * err_unknown                             = 0x00000002,       //未知错误
         * err_invalid_param                       = 0x00000003,       //参数非法
         * err_mem_alloc                           = 0x00000005,       //分配内存失败
         * err_internal                            = 0x00000006,       //内部错误
         * err_device_inited                       = 0x00000007,       //设备已经初始化过了
         * err_invalid_device_info                 = 0x00000009,       //非法的设备信息
         * err_invalid_serial_number               = 0x0000000A,       //(10)      非法的设备序列号
         * err_invalid_system_path                 = 0x0000000E,       //(14)      非法的system_path
         * err_invalid_app_path                    = 0x0000000F,       //(15)      非法的app_path
         * err_invalid_temp_path                   = 0x00000010,       //(16)      非法的temp_path
         * err_invalid_device_name                 = 0x00000015,       //(21)      设备名没填，或者长度超过32byte
         * err_invalid_os_platform                 = 0x00000016,       //(22)      系统信息没填，或者长度超过32byte
         * err_invalid_license                     = 0x00000017,       //(23)      license没填，或者长度超过150byte
         * err_invalid_server_pub_key              = 0x00000018,       //(24)      server_pub_key没填，或者长度超过120byte
         * err_invalid_product_version             = 0x00000019,       //(25)      product_version非法
         * err_invalid_product_id                  = 0x0000001A,       //(26)      product_id非法
         * err_sys_path_access_permission          = 0x0000001D,       //(29)      system_path没有读写权限
         * err_invalid_network_type				= 0x0000001E,		//(30)		初始化时传入的网络类型非法
         * err_invalid_run_mode					= 0x0000001F,		//(31)      初始化时传入的SDK运行模式非法
         * 未找到设备信息，请确认设备是否开机或联网 34
         * guid和licence不匹配 46
         * licence长度超过255错误 57
         * 公钥长度有问题 71
         */
        int ret = XWSDKJNI.getInstance().init(loginInfo.deviceName, loginInfo.license.getBytes(), loginInfo.serialNumber, loginInfo.srvPubKey, loginInfo.productId, loginInfo.productVersion, loginInfo.networkType, loginInfo.runMode,
                loginInfo.sysPath, loginInfo.sysCapacity, loginInfo.appPath, loginInfo.appCapacity, loginInfo.tmpPath, loginInfo.tmpCapacity, 0);

        if (ret == 0) {
            int[] versions = XWSDKJNI.getInstance().getSDKVersion();
            QLog.setBuildNumber(versions[0] + "." + versions[1] + "." + versions[2]);
        } else {
            listener.onCheckParam(ret, "");
            return;
        }
        XWDeviceBaseManager.setOnXWLoginListener(listener);

        XWSDKJNI.startXiaoweiService();
        XWSDKJNI.initCCMsgModule();
    }

    /**
     * 退出登录，释放资源
     *
     * @param listener
     * @return
     */
    public int logout(OnXWLogoutListener listener) {
        mMapRequestListener.clear();
        mDeviceGetAlarmListListener.clear();
        mDeviceSetAlarmListener.clear();
        mRspListenerMap.clear();
        mXWLoginInfo = null;
        mContext = null;
        mainHandler = null;
        mUiThread = null;
        XWDeviceBaseManager.setOnXWLogoutListener(listener);
        return XWSDKJNI.getInstance().destroy();
    }

    /**
     * 监听设备在线状态
     *
     * @param listener
     */
    public void setOnXWOnlineStatusListener(OnXWOnlineStatusListener listener) {
        XWDeviceBaseManager.setOnXWOnlineStatusListener(listener);
    }

    /**
     * 更新绑定者登录态
     *
     * @param accountInfo
     */
    public void setXWAccountInfo(XWAccountInfo accountInfo) {

        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.setXWAccountInfo(accountInfo);
    }

    /**
     * 小微登录相关事件
     */
    public interface OnXWLoginListener {
        /**
         * 检查参数，这里会检测XWLoginInfo格式是否正确，如果出现错误码，后面就不会继续了，请自行检查参数
         *
         * @param errorCode
         * @param info
         */
        void onCheckParam(int errorCode, String info);

        /**
         * 连接服务器
         *
         * @param errorCode 连接失败需要检查网络，以及是否屏蔽了腾讯的ip
         */
        void onConnectedServer(int errorCode);

        /**
         * 注册结果，pid+sn会换到设备唯一标识din，din很重要，反馈问题的时候都需要提供
         *
         * @param errorCode
         * @param subCode
         * @param din
         */
        void onRegister(int errorCode, int subCode, long din);

        /**
         * 登录结果
         *
         * @param errorCode
         */
        void onLogin(int errorCode, String erCodeUrl);

        /**
         * 登录后会收到绑定者列表，绑定者列表变化也会收到它
         *
         * @param errorCode
         * @param arrayList
         */
        void onBinderListChange(int errorCode, ArrayList<XWBinderInfo> arrayList);
    }

    /**
     * 小微登出相关事件
     */
    public interface OnXWLogoutListener {
        void onLogout();
    }

    public interface OnXWOnlineStatusListener {
        /**
         * 上线，在登录成功和平时网络恢复会被调用。在线才能发出去请求
         */
        void onOnline();

        /**
         * 离线，断网后调用。
         */
        void onOffline();
    }

    /**
     * 设置语音请求状态回调接口
     */
    public void setAudioRequestListener(AudioRequestListener listener) {
        this.audioRequestListener = listener;
    }

    public String request(int type, byte[] requestData) {
        return request(type, requestData, (XWRequestInfo) null);
    }

    public String request(int type, byte[] requestData, XWRequestInfo param) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        String strVoiceID = XWSDKJNI.request(type, requestData, param);
        if (strVoiceID.isEmpty()) {
            QLog.e(TAG, "request voiceID is null.");
            return null;
        }

        return strVoiceID;
    }

    /**
     * 语音请求  use {@link #request} instead.
     *
     * @param type        请求类型 {@link XWCommonDef.RequestType}
     * @param requestData 请求数据
     * @param context     上下文，用于携带额外的会话信息
     * @return 本次请求对应的voiceID
     */
    @Deprecated
    public String request(int type, byte[] requestData, XWContextInfo context) {
        return request(type, requestData, context.toRequestInfo());
    }


    /**
     * 取消语音请求
     *
     * @param voiceId 要取消的voiceID，当为null的时候，表示取消所有请求
     */
    public int requestCancel(String voiceId) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return XWSDKJNI.cancelRequest(voiceId);
    }

    /**
     * 根据文本转TTS
     *
     * @param strText  请求的文本
     * @param listener 回调监听
     * @return 本次请求对应的VoiceID
     */
    public String requestTTS(@NonNull byte[] strText, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        String strVoiceID = XWSDKJNI.request(XWCommonDef.RequestType.ONLY_TTS, strText, null);

        if (strVoiceID.isEmpty()) {
            QLog.e(TAG, "request voiceID is null.");
            return strVoiceID;
        }

        if (listener != null) {
            mMapRequestListener.put(strVoiceID, listener);
        }

        return strVoiceID;
    }

    /**
     * 根据文本转TTS use {@link #requestTTS} instead.
     *
     * @param strText     请求的文本
     * @param contextInfo 上下文
     * @param listener    回调监听
     * @return 本次请求对应的VoiceID
     */
    @Deprecated
    public String requestTTS(@NonNull byte[] strText, XWContextInfo contextInfo, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return requestTTS(strText, listener);
    }

    /**
     * 取消TTS的传输
     *
     * @param resId
     */
    public void cancelTTS(String resId) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.cancelTTS(resId);
    }

    /**
     * 拉取更多列表请求，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     * 在Response.hasMorePlaylist，且当前列表已经快播放完成或者用户滑动到底部时调用
     *
     * @param appInfo     场景信息，表示在哪个场景下，该接口暂时只支持音乐场景
     * @param playID      当前正在播放的playID
     * @param maxListSize 要拉取的最大数量，暂时不支持自定义，向下是6，向上是20
     * @param isUp        往前面查询
     * @param listener    请求回调
     * @return 本次请求对应的voiceID
     */
    public String getMorePlaylist(XWAppInfo appInfo, String playID, int maxListSize, boolean isUp, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        if (null == listener) {
            return "";
        }

        String strVoiceID = XWSDKJNI.getMorePlaylist(appInfo, playID, maxListSize, isUp);
        if (strVoiceID.isEmpty()) {
            return strVoiceID;
        }

        mMapRequestListener.put(strVoiceID, listener);
        return strVoiceID;
    }


    /**
     * 拉取播放资源详情，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     * 用于拉取歌词、是否收藏等信息时调用
     *
     * @param appInfo    场景信息，表示在哪个场景下，该接口暂时只支持音乐场景
     * @param listPlayID 要拉取详情的playID
     * @param listener   请求回调
     * @return 本次请求对应的voiceID
     */
    public String getPlayDetailInfo(XWAppInfo appInfo, String[] listPlayID, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        if (null == listener) {
            return "";
        }

        String strVoiceID = XWSDKJNI.getPlayDetailInfo(appInfo, listPlayID);
        if (strVoiceID.isEmpty()) {
            return strVoiceID;
        }

        mMapRequestListener.put(strVoiceID, listener);
        return strVoiceID;
    }

    /**
     * 更新播放列表url信息，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param appInfo    场景信息，表示在哪个场景下，该接口暂时只支持音乐场景
     * @param listPlayID 要拉取详情的playID
     * @param listener   请求回调
     * @return 本次请求对应的voiceID
     */
    public String refreshPlayList(XWAppInfo appInfo, String[] listPlayID, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        String strVoiceID = XWSDKJNI.refreshPlayList(appInfo, listPlayID);
        if (strVoiceID.isEmpty()) {
            return strVoiceID;
        }

        if (listener != null) {
            mMapRequestListener.put(strVoiceID, listener);
        }

        return strVoiceID;
    }

    /**
     * 设置QQ音乐品质
     *
     * @param quality {@link XWCommonDef.PlayQuality}
     * @return 返回值请参考 {@link XWCommonDef.ErrorCode}
     */
    public int setMusicQuality(int quality) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return XWSDKJNI.setQuality(quality);
    }

    void runOnMainThread(Runnable runnable) {
        if (mUiThread == Thread.currentThread()) {
            runnable.run();
            return;
        }

        if (mainHandler != null) {
            mainHandler.post(runnable);
        }
    }

    boolean onRequest(final String voiceID, final int event, final XWResponseInfo rspData, final byte[] extendData) {
        RequestListener listener = mMapRequestListener.remove(voiceID);
        if (listener != null) {
            return listener.onRequest(event, rspData, extendData);
        } else if (audioRequestListener != null) {
            return audioRequestListener.onRequest(voiceID, event, rspData, extendData);
        }

        return false;
    }

    boolean onTTSPushData(String voiceId, XWTTSDataInfo tts) {
        if (mOnReceiveTTSDataListener != null) {
            return mOnReceiveTTSDataListener.onReceive(voiceId, tts);
        }
        return false;
    }

    long lastNetDelayTime;

    boolean onNetWorkDelay(final String voiceID, final long time) {
        if (mNetworkDelayListener != null) {
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    mNetworkDelayListener.onDelay(voiceID, time);
                }
            });
        }
        lastNetDelayTime = time;
        return true;
    }

    /**
     * 事件上报
     *
     * @param log
     */
    public void reportEvent(XWEventLogInfo log) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.reportEvent(log);
    }

    /**
     * 上报播放状态
     *
     * @param stateInfo 要上报的当前状态
     */
    public int reportPlayState(XWPlayStateInfo stateInfo) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        QLog.d(TAG, "reportPlayState " + stateInfo);
        return XWSDKJNI.reportPlayState(stateInfo);
    }


    /**
     * 通用请求回调通知
     */
    public interface RequestListener {
        /**
         * 收到通知
         * 根据event分为多种事件
         *
         * @param event      当前通知事件 {@link XWCommonDef.XWEvent}
         * @param rspData    标准信息
         * @param extendData 扩展信息
         */
        boolean onRequest(int event, XWResponseInfo rspData, byte[] extendData);
    }

    /**
     * 语音服务状态回调接口
     */
    public interface AudioRequestListener {
        /**
         * 收到通知
         * 根据event分为多种事件
         *
         * @param voiceId    当次请求的VoiceId
         * @param event      当前通知事件 {@link XWCommonDef.XWEvent}
         * @param rspData    标准信息
         * @param extendData 扩展信息
         * @return 收到状态通知后，上层是否需要处理
         */
        boolean onRequest(String voiceId, int event, XWResponseInfo rspData, byte[] extendData);
    }

    /**
     * 网络延迟的监听，每次小微的网络请求都会回调，从开始发送到收到响应的时差
     */
    public interface NetworkDelayListener {
        void onDelay(String voiceId, long ms);
    }

    /**
     * 设置网络延迟的监听
     *
     * @param listener {@link NetworkDelayListener}
     */
    public void setNetworkDelayListener(NetworkDelayListener listener) {
        mNetworkDelayListener = listener;
    }

    /**
     * 网络延迟的监听，每次小微的网络请求都会回调，从开始发送到收到响应的时差
     */
    public interface OnReceiveTTSDataListener {
        boolean onReceive(String voiceId, XWTTSDataInfo ttsData);
    }

    /**
     * 设置接受TTS数据的监听
     *
     * @param listener {@link OnReceiveTTSDataListener}
     */
    public void setOnReceiveTTSDataListener(OnReceiveTTSDataListener listener) {
        mOnReceiveTTSDataListener = listener;
    }

    /**
     * 拉取提醒的回调定义
     */
    public interface GetAlarmListRspListener {
        /**
         * 拉取提醒类型回调方法
         *
         * @param errCode        返回码，请参考 {@link XWCommonDef.ErrorCode}
         * @param strVoiceID     请求VoiceID
         * @param arrayAlarmList 提醒列表
         */
        void onGetAlarmList(int errCode, String strVoiceID, String[] arrayAlarmList);
    }

    /**
     * 设置/更新/删除提醒的回调定义
     */
    public interface SetAlarmRspListener {
        /**
         * 设置/更新/删除提醒的回调方法
         *
         * @param errCode    返回码，请参考 {@link XWCommonDef.ErrorCode}
         * @param strVoiceID 请求VoiceID
         * @param alarmId    设置/更新/删除的闹钟ID
         */
        void onSetAlarmList(int errCode, String strVoiceID, int alarmId);
    }


    /**
     * 获取提醒列表，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param listener 响应回调接口 定义请参考{@link XWSDK.GetAlarmListRspListener}
     * @return 接口调用结果，请参考{@link XWCommonDef.ErrorCode}
     */
    public int getDeviceAlarmList(GetAlarmListRspListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }

        String voiceId = XWSDKJNI.getDeviceAlarmList();

        if (!TextUtils.isEmpty(voiceId)) {
            mDeviceGetAlarmListListener.put(voiceId, listener);
        } else {
            listener.onGetAlarmList(XWCommonDef.ErrorCode.ERROR_FAILED, voiceId, null);
        }

        return (TextUtils.isEmpty(voiceId) ? XWCommonDef.ErrorCode.ERROR_FAILED : XWCommonDef.ErrorCode.ERROR_NULL_SUCC);
    }

    /**
     * 回调通知提醒列表
     *
     * @param errCode        错误码
     * @param strVoiceID     请求id
     * @param arrayAlarmList 提醒列表
     */
    void onGetAIAudioAlarmList(final int errCode, final String strVoiceID, final String[] arrayAlarmList) {

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                GetAlarmListRspListener listener = mDeviceGetAlarmListListener.remove(strVoiceID);
                if (listener != null) {
                    listener.onGetAlarmList(errCode, strVoiceID, arrayAlarmList);
                }
            }
        });
    }

    /**
     * 设置闹钟或提醒，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param opType       操作类型 1.增加 2.修改 3.删除 {@link XWCommonDef.AlarmOptType}
     * @param strAlarmJson 操作对应的json结构
     * @param listener     设置结果的回调通知 定义请参考{@link XWSDK.SetAlarmRspListener}
     * @return 接口调用返回结果 请参考{@link XWCommonDef.ErrorCode}
     */
    public int setDeviceAlarmInfo(int opType, String strAlarmJson, SetAlarmRspListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }

        String voiceId = XWSDKJNI.setDeviceAlarm(opType, strAlarmJson);

        if (!TextUtils.isEmpty(voiceId)) {
            mDeviceSetAlarmListener.put(voiceId, listener);
        } else {
            listener.onSetAlarmList(XWCommonDef.ErrorCode.ERROR_FAILED, null, 0);
        }

        return (TextUtils.isEmpty(voiceId) ? XWCommonDef.ErrorCode.ERROR_FAILED : XWCommonDef.ErrorCode.ERROR_NULL_SUCC);
    }

    /**
     * 拉取定时播放任务资源，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param strAlarmId 定时
     * @param listener   响应回调
     * @return 接口调用返回结果 请参考{@link XWCommonDef.ErrorCode}
     */
    public int getTimingSkillResource(String strAlarmId, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }

        String voiceId = XWSDKJNI.getTimingSkillResource(strAlarmId);

        if (!TextUtils.isEmpty(voiceId)) {
            mMapRequestListener.put(voiceId, listener);
        }

        return (TextUtils.isEmpty(voiceId) ? XWCommonDef.ErrorCode.ERROR_FAILED : XWCommonDef.ErrorCode.ERROR_NULL_SUCC);
    }

    /**
     * 上报日志文件
     */
    public void uploadLogs(String start, String end) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.getInstance().uploadLogs(start, end);
    }

    /**
     * 用户不满意上次的识别，将上一次的记录上报到后台
     */
    public void errorFeedBack() {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.errorFeedBack();
    }

    /**
     * 设置主人登录态，建议使用{@link #request(String, String, String, OnRspListener)}
     *
     * @param info
     * @param listener
     */
    public void setLoginStatus(XWLoginStatusInfo info, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        if (null == listener) {
            return;
        }

        String strVoiceID = XWSDKJNI.setLoginStatus(info);
        if (strVoiceID.isEmpty()) {
            return;
        }
        mMapRequestListener.put(strVoiceID, listener);
    }

    /**
     * 获取主人登录态，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param skillId
     * @param listener
     */
    public void getLoginStatus(String skillId, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        if (null == listener) {
            return;
        }

        String strVoiceID = XWSDKJNI.getLoginStatus(skillId);
        if (strVoiceID.isEmpty()) {
            return;
        }
        mMapRequestListener.put(strVoiceID, listener);
    }

    /**
     * 获得音乐会员信息，建议使用{@link #request(String, String, String, OnRspListener)}获取结果
     *
     * @param listener
     */
    @Deprecated
    public void getMusicVipInfo(RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        if (null == listener) {
            return;
        }

        String strVoiceID = XWSDKJNI.getMusicVipInfo();
        if (strVoiceID.isEmpty()) {
            return;
        }
        mMapRequestListener.put(strVoiceID, listener);
    }


    /**
     * 设置词表的结果回调通知
     */
    public interface OnSetWordsListListener {
        /**
         * 回调
         *
         * @param op      设置词表的操作 0:上传 1:删除
         * @param errCode 0:成功 非0:失败
         */
        void OnReply(int op, int errCode);
    }

    /**
     * 开启可见可答
     *
     * @param enable true:open, false:close
     * @return 0:success else failed
     */
    public int enableV2A(boolean enable) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return XWSDKJNI.enableV2A(enable);
    }

    void onSetWordsListRet(int op, int errCode) {
        QLog.v(TAG, "onSetWordsListRet op:" + op + " errCode:" + errCode);
        if (mSetWordsListListener != null) {
            mSetWordsListListener.OnReply(op, errCode);
        }
    }

    /**
     * 设置词库列表
     *
     * @param type       词库类型 {@link XWCommonDef.WordListType}
     * @param words_list 词库
     * @param listener   回调
     * @return 0:success else failed
     */
    public int setWordslist(int type, String[] words_list, OnSetWordsListListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        mSetWordsListListener = listener;
        return XWSDKJNI.setWordslist(type, words_list);
    }

    /**
     * 通知命令执行结果
     *
     * @param errCode    错误码
     * @param strVoiceID 请求id
     */
    void onSetAlarmCallback(final int errCode, final String strVoiceID, final int clockId) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                SetAlarmRspListener listener = mDeviceSetAlarmListener.remove(strVoiceID);
                if (listener != null) {
                    listener.onSetAlarmList(errCode, strVoiceID, clockId);
                }
            }
        });
    }

    /**
     * 收藏FM或者QQ音乐，收藏结果需要关注700126的propertyId
     *
     * @param appInfo    场景信息
     * @param playId     资源ID
     * @param isFavorite true表示收藏，false表示取消收藏
     */
    public void setFavorite(XWAppInfo appInfo, String playId, boolean isFavorite) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        XWSDKJNI.setFavorite(appInfo, playId, isFavorite);
    }

    public boolean isOnline() {
        return online;
    }

    /**
     * 请求指定格式的TTS，给视频通话、消息、导航等特殊场景使用
     *
     * @param tinyid    目标用户id，电话和消息需要填写
     * @param timestamp 时间 ,消息需要填，其余填0
     * @param type      类别 {@link XWCommonDef.RequestProtocolType}
     * @return TTS的resId
     */
    public String requestProtocolTTS(long tinyid, long timestamp, int type, RequestListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        String strVoiceID = XWSDKJNI.requestProtocolTTS(tinyid, timestamp, type);
        if (!strVoiceID.isEmpty() && listener != null) {
            QLog.e(TAG, "requestProtocolTTS voiceId: " + strVoiceID);
            mMapRequestListener.put(strVoiceID, listener);
        }
        return strVoiceID;
    }

    /**
     * 在某些场景下，可设置设备状态，正常退出场景后，需要调用clearUserState清除
     *
     * @param stateInfo 自定义状态
     */
    public int setUserState(XWPlayStateInfo stateInfo) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return XWSDKJNI.setUserState(stateInfo);
    }

    /**
     * 清除自定义设备状态, 与setUserState配合使用
     */
    public int clearUserState() {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        return XWSDKJNI.clearUserState();
    }

    /**
     * 通用请求资源的回调
     */
    public interface OnRspListener {
        /**
         * @param voiceId 请求唯一标识
         * @param error   错误码
         * @param json    结果
         */
        void onRsp(String voiceId, int error, String json);
    }

    /**
     * 通用请求资源的接口，参考{https://xiaowei.qcloud.com/wiki/#OpenSrc_Cmd_Interface}
     *
     * @param cmd      大命令
     * @param subCmd   子命令
     * @param params   参数
     * @param listener {@link OnRspListener}
     */
    public String request(String cmd, String subCmd, String params, OnRspListener listener) {
        if (mContext == null) {
            throw new RuntimeException("You need to call login at first.");
        }
        String strVoiceID = XWSDKJNI.requestCmd(cmd, subCmd, params);
        if (!strVoiceID.isEmpty() && listener != null) {
            QLog.d(TAG, "request voiceId: " + strVoiceID);
            mRspListenerMap.put(strVoiceID, listener);
        }
        return strVoiceID;
    }

    public void onRequest(final String voiceId, final int error, final String json) {
        final OnRspListener listener = mRspListenerMap.get(voiceId);
        if (listener != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRsp(voiceId, error, json);
                }
            });
        }
    }

}
