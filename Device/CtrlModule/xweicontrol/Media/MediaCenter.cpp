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
#include "MediaCenter.hpp"
#include <stdio.h>
#include <string>
#include "AudioApp.hpp"
#include "MediaTTS.hpp"
#include "MediaMusic.hpp"
#include "MediaText.hpp"
#include "TXCServices.hpp"
#include "TXCSkillsDefine.h"
#include <string.h>
#include "logger.h"

TXCMediaCenter::TXCMediaCenter()
    : m_lastActiveTime(0)
{
}

size_t TXCMediaCenter::AddMedia(PtrMedia &media)
{
    if (media.get() && media->info_.res_id && media->info_.res_id[0])
    {
        map_media_[media->info_.res_id] = media;
    }

    return map_media_.size();
}

PtrMedia TXCMediaCenter::GetMedia(const std::string &res_id)
{
    PtrMedia result;
    std::map<std::string, PtrMedia>::iterator itr = map_media_.find(res_id);
    if (map_media_.end() != itr)
    {
        result = itr->second;
    }

    return result;
}

int TXCMediaCenter::ReadMedia(_In_ const char *res_id, _Out_ const void **data, _Out_ size_t *data_size, _In_ size_t offset)
{
    int result = EINVAL;
    if (res_id && res_id[0] && data && data_size)
    {
        PtrMedia media = GetMedia(res_id);
        if (media.get())
        {
            result = media->Read(data, data_size, offset);
        }
        else
        {
            result = EBADF;
        }
    }

    return result;
}

bool TXCMediaCenter::RemoveMedia(const std::string &res_id)
{
    bool exists = false;
    if (!res_id.empty())
    {
        std::map<std::string, PtrMedia>::iterator itr = map_media_.find(res_id);
        if (map_media_.end() != itr)
        {
            map_media_.erase(itr);
            exists = true;
        }
    }

    return exists;
}

bool TXCMediaCenter::DecMediaTipCnt(const std::string &res_id)
{
    bool handled = false;
    PtrMedia media = GetMedia(res_id);
    if (media.get())
    {
        media.get()->DecPlayCnt();
    }

    return handled;
}

bool TXCMediaCenter::IsMediaNeedPlay(const std::string &res_id)
{
    bool needPlay = true;

    PtrMedia media = GetMedia(res_id);
    if (media.get())
    {
        TLOG_DEBUG("TXCMediaCenter::IsMediaNeedPlay res_id=%s play_count=%d", res_id.c_str(), media.get()->GetPlayCnt());
        needPlay = (media.get()->GetPlayCnt() > 0 || media.get()->GetPlayCnt() == -1);
    }

    return needPlay;
}

std::string TXCMediaCenter::GenResourceId()
{
    std::string session_id;
    for (int i = 0; i < 32; ++i)
    {
        session_id += ('A' + rand() % 26);
    }
    return session_id;
}

int TXCMediaCenter::AddMediaList(SESSION id, PtrMediaList &playList)
{
    map_playlist_[id] = playList;

    return (int)map_playlist_.size();
}

PtrMediaList TXCMediaCenter::GetMediaList(SESSION id)
{
    return map_playlist_[id];
}

int TXCMediaCenter::TriggerMediaUpdated(const PtrMedia &media)
{
    int count = 0;
    if (media.get())
    {
        const char *res_id = media->GetInfo().res_id;
        if (res_id && res_id[0])
        {

            std::map<SESSION, PtrMediaList>::iterator itr = map_playlist_.begin();
            for (; map_playlist_.end() != itr; ++itr)
            {
                SESSION session_id = itr->first;
                PtrMediaList &playlist = itr->second;
                if (playlist.get())
                {
                    PtrMedia find_media = playlist->Get(res_id);
                    if (find_media.get())
                    {
                        post_message(session_id, XWM_MEDIA_UPDATE, XWPARAM(res_id), NULL, 0);
                        count++;
                    }
                }
            }
        }
    }

    return count;
}

void TXCMediaCenter::SetLastActiveTime()
{
    m_lastActiveTime = time(NULL);
}

long TXCMediaCenter::GetLastActiveTime()
{
    time_t now = time(NULL);
    return now - m_lastActiveTime;
}

void TXCMediaCenter::AddVoiceData(const char *data, int length)
{
    if (data == NULL || length == 0)
    {
        return;
    }

    std::string strBuffer;
    strBuffer.assign(data, length);
    m_strVoiceData.append(strBuffer);
}

void TXCMediaCenter::ResetVoiceData()
{
    m_strVoiceData.clear();
}

std::string TXCMediaCenter::GetVoiceData()
{
    return m_strVoiceData;
}
