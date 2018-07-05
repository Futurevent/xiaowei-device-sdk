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

#ifndef Util_hpp
#define Util_hpp

#include "TXSDKCommonDef.h"
#include "AudioApp.hpp"
#include <stdio.h>
#include <string>

namespace Util
{
unsigned long long GetCurTimeMsLocal();

// 没有当前场景和上一次场景，并且没有播放资源和自动唤醒标Î记
bool IsInvaild(const TXCA_PARAM_RESPONSE &cRsp);
// 有当前场景，或者有播放资源或自动唤醒标记
bool IsVaild(const TXCA_PARAM_RESPONSE &cRsp);
bool IsTempRsp(const TXCA_PARAM_RESPONSE &cRsp);
bool IsCommandRsp(const TXCA_PARAM_RESPONSE &cRsp);
int GetNewProcessId();
std::string ToString(const TXCA_PARAM_RESPONSE &cRsp);
std::string ToString(XWM_EVENT e);
std::string ToString(DURATION_HINT e);
std::string ToString(TXC_PLAYER_ACTION e);
std::string ToString(TXC_PLAYER_STATE e);
std::string ToString(player_control e);
std::string ToString(txc_playlist_t t);
}

#endif
