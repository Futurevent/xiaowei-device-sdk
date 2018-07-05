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
#include "Player.hpp"
#include <stdio.h>
#include <string>
#include "AudioApp.hpp"
#include <string.h>
#include "logger.h"
#include "TXCSkillsDefine.h"
#include "TXCServices.hpp"

TXCPlayer::TXCPlayer(int app_id)
    : app_id_(app_id)
{
    memset(&info_, 0, sizeof(info_));
    info_.repeatMode = REPEAT_SEQUENCE;
    info_.status = STATUS_STOP;
    info_.session = app_id;
    info_.volume = 100;
    control_callback_ = TXCServices::instance()->GetAppManager()->callback_;
}

TXCPlayer::TXCPlayer(int app_id, REPEAT_MODE repeatMode)
    : app_id_(app_id)
{
    memset(&info_, 0, sizeof(info_));
    info_.repeatMode = repeatMode;
    info_.status = STATUS_STOP;
    info_.session = app_id;
    info_.volume = 100;
    control_callback_ = TXCServices::instance()->GetAppManager()->callback_;
}

void TXCPlayer::Stop()
{
    TriggerEvent(ACT_PLAYER_STOP, 0, 0);
}
void TXCPlayer::Pause()
{
    TriggerEvent(ACT_PLAYER_PAUSE, XWPARAM(1), 0);
}
void TXCPlayer::Resume()
{
    TriggerEvent(ACT_PLAYER_PAUSE, XWPARAM(0), 0);
}

void TXCPlayer::PlayMedia(const txc_media_t *media, bool withReleaseRes)
{
    TriggerEvent(ACT_MUSIC_PUSH_MEDIA, XWPARAM(media), XWPARAM(withReleaseRes));
}

int TXCPlayer::GetCurrentPosition()
{
    return 0;
}
int TXCPlayer::GetDuration()
{
    return 0;
}

void TXCPlayer::SeekTo(unsigned long long offset)
{
    TriggerEvent(ACT_PLAYER_SEEK_TO, XWPARAM(offset), 0);
}

bool TXCPlayer::IsPlaying()
{
    return info_.status == STATUS_PLAY;
}

void TXCPlayer::SetStatus(PLAYER_STATUS st)
{
    if (info_.status == st)
    {
        return;
    }
    TLOG_TRACE("TXCPlayer::SetStatus, old:%d, status:%d", info_.status, st);
    info_.status = st;
}

PLAYER_STATUS TXCPlayer::GetStatus() const
{
    return info_.status;
}

void TXCPlayer::SetRepeatMode(REPEAT_MODE mode)
{
    info_.repeatMode = mode;

    TriggerEvent(ACT_PLAYER_SET_REPEAT_MODE, XWPARAM(mode), 0);
}

REPEAT_MODE TXCPlayer::GetRepeatMode() const
{
    return info_.repeatMode;
}

void TXCPlayer::SetVolume(int volume)
{
    if (info_.volume == volume)
    {
        return;
    }
    info_.volume = volume;
    TLOG_TRACE("TXCPlayer::SetVolume volume[%d]", volume);
    TriggerEvent(ACT_CHANGE_VOLUME, XWPARAM((long)volume), 0);
}

int TXCPlayer::GetVolume() const
{
    return info_.volume;
}

txc_player_info_t *TXCPlayer::GetInfo()
{
    return &info_;
}

XWPARAM TXCPlayer::TriggerEvent(TXC_PLAYER_ACTION event, XWPARAM arg1, XWPARAM arg2)
{
    return control_callback_.control_callback(info_.session, event, arg1, arg2);
}
