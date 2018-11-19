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
package com.tencent.xiaowei.def;

/**
 * 通道层常量定义
 */
public class XWCommonDef {

    /**
     * 语音请求的事件定义
     */
    public interface XWEvent {
        /**
         * 空闲
         */
        int ON_IDLE = 0;
        /**
         * 请求开始
         */
        int ON_REQUEST_START = 1;
        /**
         * 检测到开始说话
         */
        int ON_SPEAK = 2;
        /**
         * 检测到静音(only@device has not txca_device_local_vad)
         */
        int ON_SILENT = 3;
        /**
         * 识别文本实时返回
         */
        int ON_RECOGNIZE = 4;
        /**
         * 请求收到响应
         */
        int ON_RESPONSE = 5;
        /**
         * 请求收到中间响应
         */
        int ON_RESPONSE_INTERMEDIATE = 7;
    }

    public interface DEVICE_INFO_PROP {
        long SUPPORT_LOCAL_VAD = 0x0000000000000001;
        long SUPPROT_GPS = 0x0000000000000002;
        long SUPPROT_WIFI_AP = 0x0000000000000004;
    }

    /**
     * 播放资源列表类型
     */
    public interface ResourceListType {

        /**
         * 默认的列表，当前列表。
         */
        int DEFAULT = 0;

        /**
         * 这个场景的历史列表
         */
        int HISTORY = 1;
    }

    /**
     * 播放资源入队行为定义
     */
    public interface PlayBehavior {
        /**
         * 中断当前播放，替换列表
         */
        int REPLACE_ALL = 0;
        /**
         * 拼接到列表队头
         */
        int ENQUEUE_FRONT = 1;
        /**
         * 拼接到列表队尾
         */
        int ENQUEUE_BACK = 2;
        /**
         * 不中断当前播放的资源，替换列表
         */
        int ENQUEUE_REPLACE = 3;
        /**
         * 不中断播放，更新列表中某些播放资源的url和quality字段信息
         */
        int ENQUEUE_UPDATE = 4;
        /**
         * 不中断播放，更新列表中某些播放资源的url和所有description
         */
        int ENQUEUE_UPDATE_ALL = 5;
        /**
         * 从当前列表中移除这些元素
         */
        int REMOVE = 6;

    }

    public interface RequestProtocolType {
        int MSG = 403;
        int CHAT = 404;
    }

    /**
     * 请求类型
     */
    public interface RequestType {
        /**
         * 语音请求
         */
        int VOICE = 0;
        /**
         * 文本请求
         */
        int TEXT = 1;
        /**
         * 请求TTS资源
         */
        int ONLY_TTS = 2;
        /**
         * 请求意图，未实现
         */
        int Intent = 3;
        /**
         * 唤醒校验
         */
        int WAKEUP_CHECK = 4;
    }


    /**
     * 唤醒类型
     */
    public interface WAKEUP_TYPE {
        /**
         * 默认，纯本地唤醒或者按键唤醒
         */
        public final static int WAKEUP_TYPE_DEFAULT = 0;
        /**
         * 需要云端校验唤醒词的请求
         */
        public final static int WAKEUP_TYPE_CLOUD_CHECK = 1;

        /**
         * 纯本地唤醒但是需要云端过滤掉唤醒词
         */
        public final static int WAKEUP_TYPE_LOCAL_WITH_TEXT = 2;

        /**
         * 免唤醒：不唤醒直接收音，上传数据，目前只限"闹钟触发场景"
         */
        public final static int WAKEUP_TYPE_LOCAL_WITH_FREE = 3;
    }

    /**
     * 远近场类型
     */
    public interface PROFILE_TYPE {
        /**
         * 远场
         */
        public final static int PROFILE_TYPE_FAR = 0;
        /**
         * 近场
         */
        public final static int PROFILE_TYPE_NEAR = 1;

    }

    /**
     * 请求可带的参数，可以复合使用
     */
    public interface REQUEST_PARAM {
        /**
         * 不使用小微的Vad，使用本地的Vad，带上这个标记后，需要在说话结束后主动通知SDK voiceRequestEnd=true。
         */
        public static final int REQUEST_PARAM_USE_LOCAL_VAD = 0x1;

        /**
         * 使用GPS的位置，默认会使用IP所在的位置。暂未实现
         */
        public static final int REQUEST_PARAM_GPS = 0x2;

        /**
         * 使用本地的TTS，不接收小微的TTS push
         */
        public static final int REQUEST_PARAM_USE_LOCAL_TTS = 0x4;

        /**
         * 保存发送前的数据到 /sdcard/tencent/xiaowei/voice/send/voiceId.pcm ,格式为pcm
         */
        public static final int REQUEST_PARAM_DUMP_SEND_AUDIO_DATA = 0x8;

        /**
         * 这次请求不识别，只进行小微的Vad
         */
        public static final int REQUEST_PARAM_ONLY_VAD = 0x10;

    }

    /**
     * 资源格式定义
     */
    public interface ResourceFormat {
        /**
         * URL类型，
         */
        int URL = 0;
        /**
         * 文本类型
         */
        int TEXT = 1;
        /**
         * TTS类型，ID=resID
         */
        int TTS = 2;
        /**
         * 本地文件类型
         */
        int FILE = 3;
        /**
         * 位置类型
         */
        int LOCATION = 4;
        /**
         * 指令类型，ID=指令ID；content=指令内容
         */
        int COMMAND = 5;
        /**
         * 原始语义意图信息，content=语义json
         */
        int INTENT = 6;
        /**
         * 未知类型
         */
        int UNKNOW = 99;
    }

    /**
     * 上报状态类型定义
     */
    public interface PlayState {
        /**
         * 一首歌开始播放
         */
        int START = 1;
        /**
         * 暂停播放
         */
        int PAUSE = 2;
        /**
         * 一首歌播放完毕
         */
        int STOP = 3;
        /**
         * 歌单播放结束，停止播放了
         */
        int FINISH = 4;
        /**
         * 空闲
         */
        int IDLE = 5;
        /**
         * 继续播放
         */
        int RESUME = 6;
        /**
         * 中断
         */
        int ABORT = 11;
    }

    /**
     * QQ音乐品质定义
     */
    public interface PlayQuality {
        /**
         * 流畅
         */
        int PLAY_QUALITY_LOW = 0;

        /**
         * 标准
         */
        int PLAY_QUALITY_NORMAL = 1;

        /**
         * 高品质
         */
        int PLAY_QUALITY_HIGH = 2;

        /**
         * 无损
         */
        int PLAY_QUALITY_LOSSLESS = 3;
    }

    /**
     * 闹钟/提醒操作类型定义
     */
    public interface AlarmOptType {
        /**
         * 新增闹钟
         */
        int ALARM_OPT_TYPE_ADD = 1;
        /**
         * 更新闹钟
         */
        int ALARM_OPT_TYPE_UPDATE = 2;
        /**
         * 删除闹钟
         */
        int ALARM_OPT_TYPE_DELETE = 3;
    }

    /**
     * 可见可达词表类型
     */
    public interface WordListType {
        /**
         * 可见可答屏幕词表，常用于屏幕上的按钮，设置后是一次性的
         */
        int WORD_TYPE_COMMAND = 0;
        /**
         * 联系人词表，设置后，一直有效。重复调用将替换之前设置的联系人词表。
         */
        int WORD_TYPE_CONTACT = 1;
    }

    /**
     * 全局通用错误码
     */
    public interface ErrorCode {
        /**
         * 成功
         */
        int ERROR_NULL_SUCC = 0x00000000;       // success
        /**
         * 失败
         */
        int ERROR_FAILED = 0x00000001;       // failed
        /**
         * 未知错误
         */
        int ERROR_UNKNOWN = 0x00000002;       // 未知错误
        /**
         * 参数非法
         */
        int ERROR_INVALID_PARAM = 0x00000003;       // 参数非法
        /**
         * buffer长度不足
         */
        int ERROR_BUFFER_NOTENOUGH = 0x00000004;       // buffer长度不足
        /**
         * 分配内存失败
         */
        int ERROR_MEM_ALLOC = 0x00000005;       // 分配内存失败
        /**
         * 内部错误
         */
        int ERROR_INTERNAL = 0x00000006;       // 内部错误
        /**
         * 设备已经初始化过了
         */
        int ERROR_DEVICE_INITED = 0x00000007;       // 设备已经初始化过了
        /**
         * av service 已经启动了
         */
        int ERROR_AV_SERVICE_STARTED = 0x00000008;       // av_service已经启动了
        /**
         * 非法的设备信息
         */
        int ERROR_INVALID_DEVICE_INFO = 0x00000009;       // 非法的设备信息
        /**
         * 非法的设备序列号
         */
        int ERROR_INVALID_SERIAL_NUMBER = 0x0000000A;       // (10)      非法的设备序列号
        /**
         * 非法的读写的回调
         */
        int ERROR_INVALID_FS_HANDLER = 0x0000000B;       // (11)      非法的读写回调
        /**
         * 非法的设备通知回调
         */
        int ERROR_INVALID_DEVICE_NOTIFY = 0x0000000C;       // (12)      非法的设备通知回调
        /**
         * 非法的音视频回调
         */
        int ERROR_INVALID_AV_CALLBACK = 0x0000000D;       // (13)      非法的音视频回调
        /**
         * 非法的system path
         */
        int ERROR_INVALID_SYSTEM_PATH = 0x0000000E;       // (14)      非法的system_path
        /**
         * 非法的app path
         */
        int ERROR_INVALID_APP_PATH = 0x0000000F;       // (15)      非法的app_path
        /**
         * 非法的temp path
         */
        int ERROR_INVALID_TEMP_PATH = 0x00000010;       // (16)      非法的temp_path
        /**
         * 未实现
         */
        int ERROR_NOT_IMPL = 0X00000011;       // (17)      未实现
        /**
         * 正在向后台获取数据中
         */
        int ERROR_FETCHING = 0x00000012;       // (18)      正在向后台获取数据中
        /**
         * 正在向后台获取数据中 & buffer长度不足
         */
        int ERROR_FETCHING_BUFF_NOT_ENOUGH = 0x00000013;       // (19)      正在向后台获取数据中 & buffer长度不足
        /**
         * 当前设备处于离线状态
         */
        int ERROR_OFF_LINE = 0x00000014;       // (20)      当前设备处于离线状态
        /**
         * 设备名没填，或者长度超过32byte
         */
        int ERROR_INVALID_DEVICE_NAME = 0x00000015;       // (21)      设备名没填，或者长度超过32byte
        /**
         * 系统信息没填，或者长度超过32byte
         */
        int ERROR_INVALID_OS_PLATFORM = 0x00000016;       // (22)      系统信息没填，或者长度超过32byte
        /**
         * license没填，或者长度超过150byte
         */
        int ERROR_INVALID_LICENSE = 0x00000017;       // (23)      license没填，或者长度超过150byte
        /**
         * server_pub_key没填，或者长度超过120byte
         */
        int ERROR_INVALID_SERVER_PUB_KEY = 0x00000018;       // (24)      server_pub_key没填，或者长度超过120byte
        /**
         * product_version非法
         */
        int ERROR_INVALID_PRODUCT_VERSION = 0x00000019;       // (25)      product_version非法
        /**
         * product_id非法
         */
        int ERROR_INVALID_PRODUCT_ID = 0x0000001A;       // (26)      product_id非法
        /**
         * socket connect失败
         */
        int ERROR_CONNECT_FAILED = 0x0000001B;       // (27)      socket connect失败
        /**
         * 调用的太频繁了
         */
        int ERROR_CALL_TOO_FREQUENTLY = 0x0000001C;       // (28)      调用的太频繁了
        /**
         * system_path没有读写权限
         */
        int ERROR_SYS_PATH_ACCESS_PERMISSION = 0x0000001D;       // (29)      system_path没有读写权限
        /**
         * 初始化时传入的网络类型非法
         */
        int ERROR_INVALID_NETWORK_TYPE = 0x0000001E;       // (30)	   初始化时传入的网络类型非法
        /**
         * 初始化时传入的SDK运行模式非法
         */
        int ERROR_INVALID_RUN_MODE = 0x0000001F;       // (31)      初始化时传入的SDK运行模式非法
        /**
         * lanav_service已经启动了
         */
        int ERROR_LANAV_SERVICE_STARTED = 0x00000020;       // (32)	   lanav_service已经启动了
        /**
         * 非法的局域网音视频回调
         */
        int ERROR_INVALID_LANAV_CALLBACK = 0x00000021;       // (33)      非法的局域网音视频回调

        /**
         * 登录失败
         */
        int ERROR_LOGIN_FAILED = 0x00010001;       // (65537)   登录失败
        /**
         * 设备信息非法
         */
        int ERROR_LOGIN_INVALID_DEVICEINFO = 0x00010002;       // (65538)   设备信息非法
        /**
         * 连接Server失败
         */
        int ERROR_LOGIN_CONNECT_FAILED = 0x00010003;       // (65539)   连接Server失败
        /**
         * 登录超时
         */
        int ERROR_LOGIN_TIMEOUT = 0x00010004;       // (65540)   登录超时
        /**
         * 擦除设备信息
         */
        int ERROR_LOGIN_ERASEINFO = 0x00010005;       // (65541)   擦除设备信息
        /**
         * 登录Server返回错误
         */
        int ERROR_LOGIN_SERVERERROR = 0x00010006;       // (65542)   登录Server返回错误

        /**
         * 消息发送失败
         */
        int ERROR_MSG_SENDFAILED = 0x00020001;       // (131073)  消息发送失败
        /**
         * 消息发送超时
         */
        int ERROR_MSG_SENDTIMEOUT = 0x00020002;       // (131074)  消息发送超时

        /**
         * 未登录的情况下启动音视频服务
         */
        int ERROR_AV_UNLOGIN = 0x00030001;       // (196609)  未登录的情况下启动音视频服务
        /**
         * 文件传输(发送/接收)失败
         */
        int ERROR_FT_TRANSFER_FAILED = 0x00040001;       // (262145)  文件传输(发送/接收)失败
        /**
         * 发送的文件太大
         */
        int ERROR_FT_FILE_TOO_LARGE = 0x00040002;       // (262146)  发送的文件太大
        /**
         * 小文件通道上传完毕后，后台没有返回key
         */
        int ERROR_FT_UPLOAD_FAILED_KEY_EMPTY = 0x00040003;       // (262147)  小文件通道上传完毕后，后台没有返回key
    }

    /**
     * 错误码定义
     */
    public interface XWeiErrorCode {
        /**
         * 成功
         */
        int SUCCESS = 0;

        /**
         * 失败
         */
        int ERR_FAILED = 1;

        /**
         * 不明白
         */
        int DONOT_UNDERSTAND = 20001;

        /**
         * 请求解包错误
         */
        int PARSE_REQ_ERR = 10000;

        /**
         * 空的语音数据
         */
        int EMPTY_VOICE_DATA = 10001;

        /**
         * 语音转文本失败
         */
        int VOICE_TO_TEXT_ERR = 10002;

        /**
         * 语义分析失败
         */
        int TEXT_ANALY_ERR = 10003;

        /**
         * 文本转语音失败
         */
        int TEXT_TO_VOICE_ERR = 10004;

        /**
         * 社平后台返回错误
         */
        int SP_SERVER_ERR = 10005;

        /**
         * 被限制接入的设备
         */
        int INVALID_DEVICE = 10006;

        /**
         * 语音输入过长
         */
        int VOICE_TOO_LONG = 10007;

        /**
         * 唤醒后一直没说话，触发超时
         */
        int VOICE_TIMEOUT = 10008;

        /**
         * 触发的闹钟后台没找到
         */
        int CLOCK_NOT_EXIST = 10009;

        /**
         * 没有命中Skill（不明白）{@link #DONOT_UNDERSTAND}
         */
        @Deprecated
        int NOT_MATCH_SKILL = 10010;

        /**
         * 没有找到授权的登录态信息
         */
        int NOT_FIND_LOGIN_INFO = 10011;

        /**
         * 未登录用户，点播歌曲
         */
        int MUSIC_NOT_LOGIN_USER = 10012;

        /**
         * 登录用户非会员触发付费内容
         */
        int MUSIC_NOT_VIP_ASK_VIP_CONTENT = 10013;

        /**
         * 没有找到某歌曲
         */
        int MUSIC_NOT_FIND_SONG = 10014;

        /**
         * 歌曲没有版权
         */
        int MUSIC_NOT_HAS_LAW = 10015;

        /**
         * 无结果
         */
        int MUSIC_NOT_FIND_RESULT = 10016;

        /**
         * 开启消息代收错误码
         */
        int QQ_MSG_PROXY_OPEN = 10017;

        /**
         * 代收消息关闭失败
         */
        int QQ_MSG_PROXY_CLOSE = 10018;

        /**
         * 代收消息获取设备绑定者失败
         */
        int QQ_MSG_PROXY_GET_BINDER = 10019;

        /**
         * 代收消息关闭其他设备代收标志失败
         */
        int QQ_MSG_PROXY_CLOSE_OTHER_DEVICE = 10020;
        /**
         * 代收消息获取设备登录信息失败
         */
        int QQ_MSG_PROXY_GET_LOGIN_INFO = 10021;
        /**
         * 代收消息设备无消息代收权限
         */
        int QQ_MSG_PROXY_AUTHORITY = 10022;
        /**
         * 代收消息向手Q推送授权消息失败
         */
        int QQ_MSG_PROXY_PUSH_TO_QQ = 10023;
        /**
         * 代收消息获取代收标志失败
         */
        int QQ_MSG_PROXY_GET_FLAG = 10025;
        /**
         * 代收消息设置代收标志失败
         */
        int QQ_MSG_PROXY_SET_FLAG = 10026;
        /**
         * 代收消息未开启代收无需关闭
         */
        int QQ_MSG_PROXY_NEEDNOT_CLOSE = 10027;

        /**
         * 重新授予登录态不是主人
         */
        int LOGIN_STATUS_NOT_BINDER = 10029;

        /**
         * open登录态已过期
         */
        int MUSIC_TOKEN_EXPIRED = 10038;
        /**
         * 设备已解绑，不能再分享了
         */
        int MUSIC_CANNOT_SHARE = 10039;
        /**
         * 设备类型无领取音乐会员权限
         */
        int MUSIC_GIFT_VIP_NO_PERMISSION = 10045;
        /**
         * 设备已领取过赠送音乐会员
         */
        int MUSIC_GIFT_VIP_HAD_RECEIVED = 10046;
        /**
         * 海外用户播放受阻
         */
        int MUSIC_UNPLAYABLE_ABROAD_USER = 10050;
        /**
         * 数字专辑需要购买才能听
         */
        int MUSIC_UNPLAYABLE_NEED_BUY = 10051;
        /**
         * 不能播放其他原因
         */
        int MUSIC_UNPLAYABLE_OTHERS = 10052;
        /**
         * 找不到对应的FM资源
         */
        int FM_CANNOT_FIND = 10101;

        /**
         * 后台系统错误
         */
        int SERVER_ERR = 11000;

        /**
         * 没有找到联系人，不是好友或代收好友
         */
        int NO_SUCH_CONTACT = 11030;

        /**
         * 不能分享给自己
         */
        int MUSIC_CANNOT_SHARE_TO_SELF = 21030;
        /**
         * 频繁查详情，被加入黑名单。
         */
        int GET_MUSIC_DETAIL_FORBID = 21031;
    }

}
