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
package com.tencent.aiaudio;

import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import com.tencent.aiaudio.bledemo.BLEManager;
import com.tencent.aiaudio.bledemo.BLEService;
import com.tencent.aiaudio.chat.AVChatManager;
import com.tencent.aiaudio.demo.BuildConfig;
import com.tencent.aiaudio.demo.R;
import com.tencent.aiaudio.player.XWeiPlayerMgr;
import com.tencent.aiaudio.service.AIAudioService;
import com.tencent.aiaudio.service.WakeupAnimatorService;
import com.tencent.aiaudio.tts.TTSManager;
import com.tencent.aiaudio.utils.AssetsUtil;
import com.tencent.aiaudio.utils.PcmBytesPlayer;
import com.tencent.aiaudio.utils.UIUtils;
import com.tencent.aiaudio.wakeup.RecordDataManager;
import com.tencent.av.XWAVChatAIDLService;
import com.tencent.xiaowei.control.XWeiAudioFocusManager;
import com.tencent.xiaowei.control.XWeiControl;
import com.tencent.xiaowei.info.XWAccountInfo;
import com.tencent.xiaowei.info.XWBinderInfo;
import com.tencent.xiaowei.info.XWCCMsgInfo;
import com.tencent.xiaowei.info.XWLoginInfo;
import com.tencent.xiaowei.info.XWTTSDataInfo;
import com.tencent.xiaowei.sdk.XWCCMsgManager;
import com.tencent.xiaowei.sdk.XWSDK;
import com.tencent.xiaowei.util.QLog;

import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class CommonApplication extends Application {

    public static final String ACTION_LOGIN_SUCCESS = "ACTION_LOGIN_SUCCESS_DEMO";
    public static final String ACTION_LOGIN_FAILED = "ACTION_LOGIN_FAILED";

    public static final String ACTION_ON_BINDER_LIST_CHANGE = "BinderListChange_DEMO";   //绑定列表变化

    public static final String AIC2CBusiness_GetVolume = "ai.internal.xiaowei.GetVolumeMsg";    //获取当前音量
    public static final String AIC2CBusiness_SetVolume = "ai.internal.xiaowei.SetVolumeMsg";    //设置音量
    public static final String AIC2CBusiness_ReturnVolume = "ai.internal.xiaowei.Cur100VolMsg"; //cc消息返回音量，返回GetVolumeMsg

    private static final String TAG = "CommonApplication";

    ////////////////////////////////////////////
    protected final static String URI_DEVICE_ICON_FORMAT = "http://i.gtimg.cn/open/device_icon/%s/%s/%s/%s/%s.png";
    ////////////////////////////////////////////

    static Handler mHandler = new Handler();

    public static boolean isLogined;// 用来标记成功登录过
    public static boolean isOnline;// 用来标记当前是否在线

    public static Context mApplication;

    public static String mStoragePath;
    public static String mReceiveFileMenuPath;
    private static Toast mToast;
    public static AudioManager mAudioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mStoragePath = Environment.getExternalStorageDirectory().toString();
        mReceiveFileMenuPath = Environment.getExternalStoragePublicDirectory("tencent") + "/device/file";

        AVChatManager.setBroadcastPermissionDeviceSdkEvent("com.tencent.xiaowei.demo.chat");
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance());// 保存java层的日志
        if (!getPackageName().equals(getProcessName(this, android.os.Process.myPid()))) {
            return;
        }
        mApplication = this;

        AssetsUtil.init(this);

        if (!PidInfoConfig.init()) {
            Toast.makeText(this, "请先运行sn生成工具，再重新打开Demo", Toast.LENGTH_LONG).show();
            return;
        }

        if (!UIUtils.isNetworkAvailable(this)) {
            PcmBytesPlayer.getInstance().play(AssetsUtil.getRing("network_disconnected.pcm"), null);
        }

        // 构造登录信息
        XWLoginInfo login = new XWLoginInfo();
        login.deviceName = getString(R.string.app_name);
        login.license = PidInfoConfig.licence;
        login.serialNumber = PidInfoConfig.sn;
        login.srvPubKey = PidInfoConfig.srvPubKey;
        login.productId = PidInfoConfig.pid;
        login.productVersion = UIUtils.getVersionCode(this);// build.gradle中的versionCode，用来检测更新
        login.networkType = XWLoginInfo.TYPE_NETWORK_WIFI;
        login.runMode = XWLoginInfo.SDK_RUN_MODE_DEFAULT;
        login.sysPath = getCacheDir().getAbsolutePath();
        login.sysCapacity = 1024000l;
        login.appPath = getCacheDir().getAbsolutePath();
        login.appCapacity = 1024000l;
        login.tmpPath = Environment.getExternalStoragePublicDirectory("tencent") + "/device/file/";
        login.tmpCapacity = 1024000l;

        XWSDK.getInstance().login(getApplicationContext(), login, new XWSDK.OnXWLoginListener() {
            @Override
            public void onCheckParam(int errorCode, String info) {
                // 这里会检测XWLoginInfo格式是否正确，如果出现错误码，后面就不会继续了，请自行检查参数
                if (errorCode != 0)
                    showToast("初始化失败：" + info);
            }

            @Override
            public void onConnectedServer(int errorCode) {
                if (errorCode != 0)
                    showToast("连接服务器失败 " + errorCode);
            }

            @Override
            public void onRegister(int errorCode, int subCode, long din) {
                // errorCode为1需要关注XWLoginInfo的参数和配置平台的配置是否都正确。其他错误可以联系我们。
                if (errorCode != 0)
                    showToast("注册失败 " + subCode + ",请检查网络以及登录的相关信息是否正确。");
                else {
                    QLog.i(TAG, "onRegister: din =  " + din);
                }
            }

            @Override
            public void onLogin(int errorCode, String erCodeUrl) {
                QLog.i(TAG, "onLogin: error =  " + errorCode + " " + erCodeUrl);
                if (errorCode == 0) {
                    isLogined = true;
                    showToastMessage("登录成功");
                    sendBroadcast(ACTION_LOGIN_SUCCESS);
                } else {
                    // 往往需要检查网络，断网了？有没有可能把腾讯的ip地址屏蔽了？
                    sendBroadcast(ACTION_LOGIN_FAILED);
                    showToastMessage("登录失败");
                }
            }

            @Override
            public void onBinderListChange(int errorCode, ArrayList<XWBinderInfo> arrayList) {
                // 刷新联系人的列表以及判断是否需要关闭已经打开的Activity。在绑定者列表变化时会收到回调，例如在手Q或者小微APP那边解绑了。
                if (errorCode == 0) {
                    sendBroadcast(ACTION_ON_BINDER_LIST_CHANGE);
                    if (arrayList.size() == 0) {
                        XWeiAudioFocusManager.getInstance().abandonAllAudioFocus();// 解绑了应该停止所有的资源的播放
                    }
                }
            }
        });// 登录小微sdk

        XWSDK.getInstance().setOnXWOnlineStatusListener(new XWSDK.OnXWOnlineStatusListener() {
            @Override
            public void onOnline() {
                isOnline = true;
                RecordDataManager.getInstance().setHalfWordsCheck(true);

                QLog.i(TAG, "onOnlineSuccess");
                showToastMessage("上线成功");
                sendBroadcast("ONLINE");
            }

            @Override
            public void onOffline() {
                isOnline = false;
                RecordDataManager.getInstance().setHalfWordsCheck(false);

                QLog.i(TAG, "onOfflineSuccess ");
                showToastMessage("离线");
                sendBroadcast("OFFLINE");
            }
        });

        if (!BuildConfig.IS_NEED_VOICE_LINK) {
            // 不需要配网，打开唤醒按钮悬浮窗
            startService(new Intent(this, WakeupAnimatorService.class));
        }

        // 第三方App需要设置登录态
        XWAccountInfo accountInfo = new XWAccountInfo();
        XWSDK.getInstance().setXWAccountInfo(accountInfo);

        QLog.d(TAG, "onCreate");

        // 初始化控制层
        XWeiControl.getInstance().init();
        XWeiAudioFocusManager.getInstance().init(this);
        mAudioManager = XWeiAudioFocusManager.getInstance().getAudioManager();
        XWeiControl.getInstance().setXWeiPlayerMgr(new XWeiPlayerMgr(getApplicationContext()));

        // 设置接受TTS数据的监听
        XWSDK.getInstance().setOnReceiveTTSDataListener(new XWSDK.OnReceiveTTSDataListener() {
            @Override
            public boolean onReceive(String voiceId, XWTTSDataInfo ttsData) {
                // TTS数据交给TTSManager管理
                TTSManager.getInstance().write(ttsData);
                return true;
            }
        });

        //处理cc消息
        XWCCMsgManager.setOnReceiveC2CMsgListener(new XWCCMsgManager.OnReceiveC2CMsgListener() {
            @Override
            public void onReceiveC2CMsg(long from, XWCCMsgInfo msg) {
                if (msg.businessName.equals("蓝牙")) {
                    BLEManager.onCCMsg(from, new String(msg.msgBuf));
                } else if (msg.businessName.equals(AIC2CBusiness_GetVolume)) {
                    AIAudioService service = AIAudioService.getInstance();
                    if (service != null) {
                        int volume = service.getVolume();
                        QLog.d(TAG, "GetVolume " + volume);

                        XWCCMsgInfo ccMsgInfo = new XWCCMsgInfo();
                        ccMsgInfo.businessName = AIC2CBusiness_ReturnVolume;
                        ccMsgInfo.msgBuf = Integer.toString(volume).getBytes();
                        XWCCMsgManager.sendCCMsg(from, ccMsgInfo, new XWCCMsgManager.OnSendCCMsgListener() {

                            @Override
                            public void onResult(long to, int errCode) {
                                QLog.d(TAG, "sendCCMsg result to: " + to + " errCode: " + errCode);
                            }
                        });
                    }
                } else if (msg.businessName.equals(AIC2CBusiness_SetVolume)) {
                    if (msg.msgBuf != null) {
                        int volume = Integer.valueOf(new String(msg.msgBuf));
                        AIAudioService service = AIAudioService.getInstance();
                        if (service != null) {
                            service.setVolume(volume);
                        }
                    }
                }
            }
        });

        // App控制设备设备蓝牙功能，需要自行适配
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            startService(new Intent(this, BLEService.class));
        }

        // 初始化音视频服务Service，用于QQ通话功能。
        startService(new Intent(this, XWAVChatAIDLService.class));
        AVChatManager.getInstance().init(this);

        // XUtils 初始化
        x.Ext.init(this);
    }

    private void sendBroadcast(String action) {
        sendBroadcast(action, null);
    }

    private void sendBroadcast(String action, Bundle bundle) {
        Intent intent = new Intent(action);
        if (bundle != null)
            intent.putExtras(bundle);
        sendBroadcast(intent);
        QLog.d(TAG, "send a broadcast:" + action);
    }

    public static void showToastMessage(final String text) {
        showToastMessage(text, true);
    }

    public static void showToastMessage(final String text, boolean show) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mApplication, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getProcessName(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps != null && !runningApps.isEmpty()) {
            for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
                if (procInfo.pid == pid) {
                    return procInfo.processName;
                }
            }
        }
        return null;
    }

    public static void showToast(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(mApplication, text, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

}
