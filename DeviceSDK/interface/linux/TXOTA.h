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
#ifndef __OTA_H__
#define __OTA_H__

#include "TXSDKCommonDef.h"

CXX_EXTERN_BEGIN

// OTA来源定义
typedef enum _tx_from_def {
    OTA_FROM_DEF_TIMER  = 0,    // 定时自动检测
    OTA_FROM_DEF_APP    = 1,    // App端操作
    OTA_FROM_DEF_SERVER = 2,    // 服务端推送
    OTA_FROM_DEF_DEVICE = 3,    // 设备主动查询
} tx_from_def;

// OTA信息结构
typedef struct _tx_ota_update_info
{
    bool        upgrade;  // 是否强制升级
    unsigned int  version; // 版本号
    const char*   title;  // 更新标题
    const char*   desc;   // 更新详情
    const char*   url;    // 下载链接
    const char*   md5;    // 升级包Md5值
} tx_ota_update_info;

// 新的OTA通知
typedef struct _tx_ota_result
{
    /**
     * 收到OTA更新信息(本回调方法废弃，请使用on_ota_result_ex来实现回调)
     *
     * @param from    来源 0 定时自动检测 1 App操作 2 ServerPush 3 设备主动查询，请参考tx_from_def
     * @param force   是否强制升级
     * @param version 版本号
     * @param title   更新标题
     * @param desc    更新详情
     * @param url     下载链接
     * @param md5     升级包Md5值
     */
    void (*on_ota_result)(int from, bool upgrade, unsigned int version, const char* title, const char* desc, const char* url, const char* md5);                  //SDK状态回调
    
    /**
     * 收到OTA更新信息
     *
     * @param error 错误码
     * @param from 来源 0 定时自动检测 1 App操作 2 ServerPush 3 设备主动查询
     * @param custom_id 自定义信息
     * @param info ota升级信息，如果没有升级信息或查询失败，该字段为NULL
     */
    void (*on_ota_result_ex)(int error, int from, unsigned long long custom_id, tx_ota_update_info *info);
} TX_OTA_RESULT;


/**
 * 主动查询OTA升级信息
 */
SDK_API int tx_query_ota_update();

/**
 * 主动查询OTA升级信息
 *
 * @param custom_id 自定义信息
 */
SDK_API int tx_query_ota_update_ex(unsigned long long custom_id);

/**
 * 配置ota信息的回调
 *
 * @param callback 回调接口，请参考结构体tx_ota_result定义
 */
SDK_API void tx_config_ota(TX_OTA_RESULT* callback);


/**
 * 配置ota信息的回调
 *
 * @param callback 回调接口，请参考结构体tx_ota_result定义
 * @param auto_query 是否开启定时query任务
 */
SDK_API void tx_config_ota_ex(TX_OTA_RESULT* callback, bool auto_query);

CXX_EXTERN_END

#endif
