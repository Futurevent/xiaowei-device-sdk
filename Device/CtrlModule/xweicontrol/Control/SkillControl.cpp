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
#include <assert.h>
#include <memory.h>
#include "SkillControl.hpp"
#include "Playlist.hpp"
#include "TXCServices.hpp"
#include "TXCAppManager.hpp"
#include "logger.h"
#include "MediaTTS.hpp"
#include "MediaMusic.hpp"
#include "MediaText.hpp"
#include "TXCSkillsDefine.h"
#include "Player.h"
#include "Util.hpp"

CSkillControl::CSkillControl(SESSION id, REPEAT_MODE repeatMode)
    : id_(id), auto_resume_able(true)
{

    control_callback_ = TXCServices::instance()->GetAppManager()->callback_;

    player_ = TXCServices::instance()->GetPlayerManager()->NewPlayer(id, repeatMode);

    play_list_.reset(new CPlaylist(repeatMode, id_));

    GetMediaList();
}

CSkillControl::~CSkillControl()
{
}

bool CSkillControl::OnMessage(XWM_EVENT event, XWPARAM arg1, XWPARAM arg2)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::OnMessage event=%s, arg1=%ld, arg2=%ld.", id_, Util::ToString(event).c_str(), arg1, arg2);

    bool handled = false;
    switch (event)
    {
        //  resource
    case XWM_ALBUM_ADDED:
        handled = AddAlbum(reinterpret_cast<long>(arg1));
        break;
    case XWM_GET_MORE_LIST:
        {
            XWM_GET_MORE_LIST_TYPE get_list_type = (XWM_GET_MORE_LIST_TYPE)(reinterpret_cast<long>(arg1));
            TXCA_PLAYLIST_TYPE current_list_type = (TXCA_PLAYLIST_TYPE)(reinterpret_cast<long>(arg2));
            PtrMediaList mediaList = GetMediaList();
            txc_playlist_t playListInfo = mediaList->GetInfo();

            TLOG_DEBUG("sessionId=%d CSkillControl::OnMessage has_history=%d.", id_, playListInfo.has_history);
            if (!playListInfo.has_history &&  (get_list_type == TYPE_GET_HISTORY || current_list_type == txca_playlist_type_history))
            {
                return true;
            }
            std::string res_id;
            if(get_list_type == TYPE_GET_HISTORY || get_list_type == TYPE_GET_MORE_UP) {
                res_id = GetFirstPlayId(current_list_type);
            } else if(get_list_type == TYPE_GET_MORE) {
                res_id = GetLastPlayId(current_list_type);
            }
            if(!res_id.empty()){
                // 拉历史、上预拉取，都带上第一个url 的 resid，下预拉取，带上最后一个url 的 resid
                handled = control_callback_.control_callback(id_, ACT_NEED_GET_MORE_LIST, arg1, (XWPARAM)res_id.c_str());
            }
        }
        break;
    case XWM_LIST_ADDED:
        handled = AddList(txca_playlist_type_default, reinterpret_cast<long>(arg1), reinterpret_cast<long>(arg2));
        break;
    case XWM_LIST_HISTORY_ADDED:
        handled = AddList(txca_playlist_type_history, reinterpret_cast<long>(arg1), reinterpret_cast<long>(arg2));
        break;
    case XWM_LIST_REMOVED:
        handled = ClearList();
    break;

    case XWM_LIST_UPDATED:
        handled = control_callback_.control_callback(id_, ACT_PLAYLIST_UPDATE_ITEM, XWPARAM(arg1), XWPARAM(arg2));
        break;
    case XWM_MEDIA_REMOVED:
        handled = control_callback_.control_callback(id_, ACT_PLAYLIST_REMOVE_ITEM, XWPARAM(arg1), XWPARAM(arg2));
        break;
    case XWM_MEDIA_UPDATE:
    {
        const char *res_id = reinterpret_cast<const char *>(arg1);
        if (res_id && res_id[0])
        {
            OnMediaUpdated(res_id);
        }
        handled = true;
    }
    break;

        // control
    case XWM_PLAY:
    {
        long list_index = reinterpret_cast<long>(arg1);
        long list_type = reinterpret_cast<long>(arg2);
        Play(list_index, (int)list_type);
        handled = true;
    }
    break;
    case XWM_START:
    {
        long list_index = reinterpret_cast<long>(arg1);
        long list_type = reinterpret_cast<long>(arg2);
        Play(list_index, (int)list_type, false);
        handled = true;
    }
        break;
    case XWM_NEXT:
    {
        Next(reinterpret_cast<long>(arg1));
        handled = true;
    }
    break;
    case XWM_STOP:
        Stop();
        handled = true;
        break;
    case XWM_PAUSE:
        Pause(bool(arg1), bool(arg2));
        handled = true;
        break;
    case XWM_VOLUME:
        player_->SetVolume((int)reinterpret_cast<long>(arg1));
        handled = true;
        break;
    case XWM_REPEAT:
        SetRepeat(REPEAT_MODE(reinterpret_cast<long>(arg1)));
        handled = true;
        break;

    case XWM_PLAYER_STATUS_CHANGED:
        handled = OnPlayerStatusChanged(TXC_PLAYER_STATE(reinterpret_cast<long>(arg1)));
        break;

    case XWM_SUPPLEMENT_REQUEST:
    {
        const TXCA_PARAM_RESPONSE *response = reinterpret_cast<const TXCA_PARAM_RESPONSE *>(arg2);
        DelayEvent delayed = {event, arg1, arg2};
        delayed.resp.Copy(response);
        delay_events_.push_back(delayed);
        handled = true;
    }
    break;
    default:
        break;
    }

    return handled;
}

long CSkillControl::PlayListPushBack(const std::vector<txc_play_item_t> &list, int list_type)
{
    return play_list_->PushBack(list_type, list);
}

long CSkillControl::PlayListPushFront(const std::vector<txc_play_item_t> &list, int list_type)
{
    return play_list_->PushFront(list_type, list);
}

size_t CSkillControl::PlayListCount(int list_type)
{
    return play_list_->Count(list_type);
}

int CSkillControl::AddMediaItem(_In_ PtrMedia &media)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::AddMediaItem %s", id_, media.get()->GetInfo().res_id);
    return media_list_->Add(-1, media);
}

int CSkillControl::UpdateMediaItem(const TXCA_PARAM_RESOURCE *item, bool only_quality)
{
    int index = -1;

    if (item->format == txca_resource_url && item->content && item->extend_buffer && item->id)
    {
        const char *res_id = item->id;
        PtrMedia temp_media = TXCServices::instance()->GetMediaCenter()->GetMedia(res_id);
        index = media_list_->Find(res_id);
        if (temp_media.get() && index >= 0)
        {
            CMediaMusic *media_music = dynamic_cast<CMediaMusic *>(temp_media.get());
            if (media_music)
            {
                if(!only_quality) {
                    media_music->Update(item->content, item->extend_buffer, item->offset);
                } else {
                    std::string desc_item(item->extend_buffer);
                    std::string curr_desc(media_music->GetInfo().description);
                    std::string update_desc = desc_item.substr(desc_item.find("\"quality\""), 11);
                    curr_desc.replace(curr_desc.find("\"quality\""), 11, update_desc);
                    media_music->Update(item->content, curr_desc.c_str(), item->offset);
                }
            }
        }
    }

    return index;
}

std::string genSessionID2()
{
    std::string session_id;
    for (int i = 0; i < 32; ++i)
    {
        session_id += ('A' + rand() % 26);
    }
    return session_id;
}

namespace inner
{
enum ResponseType
{
    RESPONSE_TYPE_BEGIN = 0,
    RESPONSE_TYPE_CARD = 1,              // 卡片消息
    RESPONSE_TYPE_WEATHER = 2,           // 天气信息（json格式）
    RESPONSE_TYPE_GAME = 3,              // 游戏信息
    RESPONSE_TYPE_CLOCK = 4,             // 闹钟, json
    RESPONSE_TYPE_MEDIA = 5,             // 媒体类信息 JSON  和英语跟读冲突
    RESPONSE_TYPE_LOCAL_SKILL = 6,       // 本地SKILL：意图和槽位信息
    RESPONSE_TYPE_MSGBOX = 7,            // 消息盒子 JSON
    RESPONSE_TYPE_FETCH_DEVICE_INFO = 8, // 查询设备信息 JSON
    RESPONSE_TYPE_NEWS = 9,              // 新闻 JSON
    RESPONSE_TYPE_BAIKE = 10,            // 百科 JSON
};
}

int CSkillControl::AddResponseData(int response_type, const char *response_data, _Out_ PtrMedia &_media)
{
    int index = -1;
    PtrMedia media;
    std::string res_id = genSessionID2();
    media = TXCServices::instance()->GetMediaCenter()->NewMedia<CMediaText>(res_id);
    CMediaText *media_text = dynamic_cast<CMediaText *>(media.get());
    if (media_text) {
        media_text->Init(TYPE_BEGIN_MISC, response_data);
        index = media_list_->Add(-1, media);
        if (0 <= index)
        {
            _media = media;
        }
    }

    return index;
}

void CSkillControl::SetMedialist(PtrMediaList &playList)
{
    media_list_ = playList;
}

PtrMediaList &CSkillControl::GetMediaList()
{
    if (!media_list_.get())
    {
        media_list_ = PtrMediaList(new TXCMediaList);

        TXCServices::instance()->GetMediaCenter()->AddMediaList(id_, media_list_);
    }
    return media_list_;
}

size_t CSkillControl::MediaListCount()
{
    return media_list_->Count();
}

void CSkillControl::ClearMediaList(int list_type)
{
    if (media_list_.get())
    {
        media_list_->Clear(list_type);
    }
}

PtrMedia CSkillControl::Remove(std::string resId)
{
    PtrMedia media;
    if (play_list_->Remove(resId))
    {
        int media_index = media_list_->Find(resId.c_str());
        if (media_index != -1)
        {
            media = media_list_->Get(media_index);
            media_list_->Remove(media_index);
        }
    }
    return media;
}

void CSkillControl::Release()
{
    if (media_list_.get() && media_list_->Count() > 0)
    {
        Stop();
        ClearList();
        ClearMediaList();
        delay_events_.clear();
        
        control_callback_.control_callback(id_, ACT_PLAYER_FINISH, 0, 0);
        TLOG_DEBUG("sessionId=%d CSkillControl::Release XWM_LIST_REMOVED", id_);
    }
}

PLAYER_STATUS CSkillControl::GetStatus()
{
    return player_->GetStatus();
}

bool CSkillControl::Play(bool isAuto)
{
    if (GetStatus() == STATUS_STOP)
    {
        send_message(id_, XWM_START, XWPARAM((long)0), XWPARAM((long)play_list_->GetCurPlayListType()));
    }
    else if (GetStatus() == STATUS_PAUSE)
    {
        send_message(id_, XWM_PAUSE, 0, XWPARAM(isAuto));
    }
    else if (GetStatus() == STATUS_PLAY)
    {
        // DO No Thing
    }

    return true;
}

void CSkillControl::ReportPlayState(TXCA_PLAYSTATE play_state)
{
    if (play_state == txca_playstate_idle)
    {
        if (GetStatus() == STATUS_STOP)
        {
            return;
        }
    }

    TXCA_PARAM_STATE state = {0};
    state.play_state = play_state == txca_playstate_idle ? GetStatus() == STATUS_PLAY ? txca_playstate_resume : txca_playstate_paused : play_state;

    const txc_session_info *app_info = TXCServices::instance()->GetAppManager()->GetSessionInfo(id_);

    if (app_info)
    {
        state.skill_info.name = app_info->skill_name;
        state.skill_info.id = app_info->skill_id;
    }

    PtrMedia media = TXCServices::instance()->GetMediaCenter()->GetMedia(play_res_id_);
    if (media.get())
    {
        if (media->GetInfo().type == TYPE_MUSIC_URL)
        {
            state.play_mode = play_list_->GetRepeat();
            state.play_id = media->GetInfo().res_id;
            state.play_content = media->GetInfo().content;
        }
    }
    TLOG_DEBUG("sessionId=%d CSkillControl::ReportPlayState skill_name[%s] skill_id[%s] play_state[%u] play_id[%s] play_mode[%u] play_content[%s]", id_, state.skill_info.name, state.skill_info.id, state.play_state, state.play_id, state.play_mode, state.play_content);
    control_callback_.control_callback(id_, ACT_REPORT_PLAY_STATE, (XWPARAM)(&state), NULL);
}

bool CSkillControl::Play(long ui_index, int list_type, bool cannot_be_restart)
{
    bool result = false;

    long src_index = play_list_->Seek((int)ui_index, list_type, cannot_be_restart);
    TLOG_DEBUG("sessionId=%d CSkillControl::Play, ui_index=%ld, src_index=%ld", id_, ui_index, src_index);

    if (0 <= src_index)
    {
        auto_resume_able = true;
        result = PlayMediaIndex(src_index);
    }

    return result;
}

bool CSkillControl::Stop()
{
    SetStatus(STATUS_STOP);
    if (play_res_id_.length() > 0)
    {
        // 之前那首歌播放被打断
        ReportPlayState(txca_playstate_abort);
    }
    play_res_id_.clear();

    player_->Stop();
    return true;
}

bool CSkillControl::Next(long skip, bool isAuto)
{
    // 重置标记
    auto_resume_able = true;

    bool result = false;

    long src_index = play_list_->NextX(skip, isAuto);

    TLOG_DEBUG("sessionId=%d CSkillControl::Next, cur_index=%ld, src_index=%ld", id_, play_list_->GetCurIndex(), src_index);
    if (0 <= src_index)
    {
        result = PlayMediaIndex(src_index, isAuto);
    }
    else
    {
        // 自动的就回调播放结束，否则进行没有更多了提示
        if (isAuto)
        {

            // 播放结束了可能需要自动唤醒
            if (!delay_events_.empty())
            {
                std::vector<DelayEvent>::iterator itr = delay_events_.begin();
                bool retry = true;
                while (delay_events_.end() != itr && retry)
                {
                    if (itr->event == XWM_SUPPLEMENT_REQUEST)
                    {
                        XWPARAM response = reinterpret_cast<XWPARAM>(&(itr->resp.response()));
                        OnSupplementRequest(itr->arg1, response);
                    }
                    ++itr;
                }
                delay_events_.clear();
            }

            post_message(id_, XWM_PLAYER_STATUS_FINISH, NULL, NULL, 0);

//            control_callback_.control_callback(id_, ACT_PLAYER_FINISH, 0, 0);
        }
        else
        {
            control_callback_.control_callback(id_, ACT_NEED_TIPS, XWPARAM((int)skip > 0 ? PLAYER_TIPS_NEXT_FAILURE : PLAYER_TIPS_PREV_FAILURE), XWPARAM(skip));
        }
    }

    return result;
}

bool CSkillControl::Pause(bool pause, bool isAuto)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::Pause pause=%d, isAuto=%d, auto_resume_able=%d.", id_, pause, isAuto, auto_resume_able);
    if (!pause && isAuto && !auto_resume_able)
    {
        return false;
    }
    if (pause && !isAuto)
    {
        auto_resume_able = false;
    }
    if (!pause)
    {
        auto_resume_able = true;
    }
    TLOG_DEBUG("sessionId=%d CSkillControl::Pause auto_resume_able=%d.", id_, auto_resume_able);

    bool ret = SetStatus(pause ? STATUS_PAUSE : STATUS_PLAY);

    if (ret)
    {
        pause ? player_->Pause() : player_->Resume();
        ReportPlayState(pause ? txca_playstate_paused : txca_playstate_resume);
    }
    return true;
}

bool CSkillControl::SetRepeat(REPEAT_MODE repeatMode)
{
    const txc_session_info *session = txc_get_session(id_);
    std::string strSkillId = session != NULL ? session->skill_id : "";

    if (strSkillId == DEF_TXCA_SKILL_ID_NEWS)
    {
        if (REPEAT_SEQUENCE != REPEAT_MODE(repeatMode))
        {
            // 新闻只支持顺序播放
            return true;
        }
    }
    if (strSkillId == DEF_TXCA_SKILL_ID_FM)
    {
        if (REPEAT_LOOP != REPEAT_MODE(repeatMode))
        {
            // FM一般都是有序的，只支持列表循环播放(循环播放尾部可以回首部，首部提示没有更多了，存在后台控制的，以后台为准。)
            return true;
        }
    }

    // 其余场景不支持顺序播放，只能循环，随机，单曲

    if (REPEAT_SEQUENCE == REPEAT_MODE(repeatMode))
    {
        repeatMode = REPEAT_LOOP;
    }

    TLOG_DEBUG("sessionId=%d CSkillControl::SetRepeat repeatMode=%d", id_, repeatMode);

    player_->SetRepeatMode(repeatMode);
    play_list_->SetRepeat(repeatMode);
    ReportPlayState();

    return true;
}

bool CSkillControl::AddAlbum(long src_index)
{
    bool handled = false;
    const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
    if (playlist_info && playlist_info->count > 0)
    {
        const txc_media_t *media = txc_get_media(id_, src_index);
        if (media)
        {
            control_callback_.control_callback(id_, ACT_ADD_ALBUM, XWPARAM(media), XWPARAM(src_index));
            handled = true;
        }
    }

    return handled;
}

bool CSkillControl::AddList(TXCA_PLAYLIST_TYPE resource_list_type, long begin_index, long count)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::AddList resource_list_type:%d, begin_index:%d, count:%d", resource_list_type, id_, begin_index, count);
    const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
    if (playlist_info && playlist_info->count > 0)
    {
        CAutoBuffer<const txc_media_t *> msg_list(count);
        long add_count = 0;
        for (long i = 0; i < count; i++)
        {
            const txc_play_item_t *item = play_list_->GetItem(resource_list_type, (int)(begin_index + i));
            const txc_media_t *media = NULL;
            for (int j = 0; j < item->count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
            {
                long src_index = item->group[j];

                const txc_media_t *temp = txc_get_media(id_, src_index);
                if (temp && temp->type == TYPE_MUSIC_URL)
                {
                    media = temp;
                    j = item->count;
                }

                if (media)
                {
                    msg_list[add_count] = media;
                    add_count++;
                }
            }
        }

        if (add_count > 0)
        {
            if (resource_list_type == txca_playlist_type_default)
            {
                control_callback_.control_callback(id_, begin_index == 0 ? ACT_PLAYLIST_ADD_ITEM_FRONT : ACT_PLAYLIST_ADD_ITEM, XWPARAM(msg_list.Get()), XWPARAM(add_count));
            }
            else if (resource_list_type == txca_playlist_type_history)
            {
                control_callback_.control_callback(id_, begin_index == 0 ? ACT_PLAYLIST_HISTORY_ADD_ITEM_FRONT : ACT_PLAYLIST_HISTORY_ADD_ITEM, XWPARAM(msg_list.Get()), XWPARAM(add_count));
            }
        }
    }

    return true;
}

bool CSkillControl::ClearList(int list_type)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::ClearList list_type=%d.", id_, list_type);

    const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
    if (playlist_info && playlist_info->count > 0)
    {
        size_t count = play_list_->Count(list_type);
        CAutoBuffer<const txc_media_t *> msg_list(count);
        long remove_count = 0;
        for (long i = 0; i < count; i++)
        {
            const txc_play_item_t *item = play_list_->GetItem(list_type, (int)(i));
            const txc_media_t *media = NULL;
            for (int j = 0; j < item->count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
            {
                long src_index = item->group[j];

                const txc_media_t *temp = txc_get_media(id_, src_index);
                if (temp && temp->type == TYPE_MUSIC_URL)
                {
                    media = temp;
                    j = item->count;
                }

                if (media)
                {
                    msg_list[remove_count] = media;
                    remove_count++;
                }
            }
        }

        if (remove_count > 0)
        {
            control_callback_.control_callback(id_, ACT_PLAYLIST_REMOVE_ITEM, XWPARAM(msg_list.Get()), XWPARAM(remove_count));
        }
    }
    if (play_list_.get())
    {
        play_list_->Clear(list_type);
    }
    return true;
}

bool CSkillControl::OnMediaUpdated(const char *res_id)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::OnMediaUpdated, %s, %s", id_, res_id, play_res_id_.c_str());
    if (res_id && res_id[0] && play_res_id_ == res_id)
    {
        SetStatus(STATUS_PLAY);
        const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
        if (playlist_info && playlist_info->count > 0)
        {
            play_list_->Seek(play_list_->GetCurIndex(), play_list_->GetCurPlayListType());
        }
    }

    return true;
}

bool CSkillControl::OnPlayerStatusChanged(TXC_PLAYER_STATE state_code)
{
    TLOG_DEBUG("sessionId=%d CSkillControl::OnPlayerStatusChanged state_code=%s", id_, Util::ToString(state_code).c_str());

    if (TXC_PLAYER_STATE_COMPLETE == state_code || TXC_PLAYER_STATE_ERR == state_code)
    {
        ReportPlayState(TXC_PLAYER_STATE_COMPLETE == state_code ? txca_playstate_stopped : txca_playstate_err);

        if (STATUS_PLAY == GetStatus() && -1 == play_list_->GetCurIndex() && play_list_->Count() == 0)
        {
            play_res_id_.clear();
        }
        else
        {
            bool handled = false;
            long list_index = play_list_->NextY();
            while (0 <= list_index && !handled)
            {
                handled = PlayMediaIndex(list_index, true);
                if (!handled)
                {
                    list_index = play_list_->NextY();
                }
            }
            if (!handled)
            {
                Next(1, true);
            }
        }
    }
    else if (TXC_PLAYER_STATE_CONTINUE == state_code)
    {
        //Next(1);
    }
    else if (TXC_PLAYER_STATE_START == state_code)
    {
    }

    return true;
}

bool CSkillControl::OnSupplementRequest(XWPARAM arg1, XWPARAM arg2)
{
    bool handled = true;
    control_callback_.control_callback(id_, ACT_NEED_SUPPLEMENT, arg1, arg2);

    return handled;
}

bool CSkillControl::SetStatus(PLAYER_STATUS status)
{
    player_->SetStatus(status);
    return true;
}

bool CSkillControl::PlayMediaIndex(long src_index, bool isAuto)
{
    bool handled = false;
    if (0 <= src_index)
    {
        const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
        if (playlist_info && playlist_info->count > 0)
        {
            const txc_media_t *media = txc_get_media(id_, src_index);

            if (media)
            {
                SetStatus(STATUS_PLAY);

                if (!isAuto && play_res_id_.length() > 0)
                {
                    // 之前那首歌播放被打断
                    ReportPlayState(txca_playstate_abort);
                }

                if (media->res_id && media->res_id[0])
                {
                    play_res_id_ = media->res_id;
                }

                handled = PlayMedia(media);
            }
        }
    }

    return handled;
}

bool CSkillControl::PlayMedia(const txc_media_t *media)
{
    bool result = true;

    TLOG_DEBUG("sessionId=%d CSkillControl::PlayMedia, res_id:%s content:%s offset:%d", id_, media->res_id, media->content, media->offset);
    TXCServices::instance()->GetMediaCenter()->DecMediaTipCnt(play_res_id_);

    player_->PlayMedia(media, (bool)(media->play_count == 0));

    ReportPlayState(txca_playstate_start);

    return result;
}

int CSkillControl::GetCurPlayListType()
{
    return play_list_->GetCurPlayListType();
}
void CSkillControl::SetCurPlayListType(TXCA_PLAYLIST_TYPE type)
{
    play_list_->SetCurPlayListType(type);
}

std::string CSkillControl::GetFirstPlayId(TXCA_PLAYLIST_TYPE list_type)
{
    std::string res_id;
    const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
    if (playlist_info && playlist_info->count > 0)
    {
        for (long i = 0; i < PlayListCount(list_type); i++)
        {
            const txc_play_item_t *item = play_list_->GetItem(list_type, (int)i);
            for (int j = 0; j < item->count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
            {
                long src_index = item->group[j];

                const txc_media_t *temp = txc_get_media(id_, src_index);
                if (temp && temp->type == TYPE_MUSIC_URL)
                {
                    res_id = temp->res_id ? temp->res_id : "";
                    return res_id;
                }
            }
        }
    }
    return res_id;
}

std::string CSkillControl::GetLastPlayId(TXCA_PLAYLIST_TYPE list_type)
{
    std::string res_id;
    const txc_playlist_t *playlist_info = txc_get_medialist_info(id_);
    if (playlist_info && playlist_info->count > 0)
    {
        for (long i = PlayListCount(list_type) - 1; i >= 0 ; i--)
        {
            const txc_play_item_t *item = play_list_->GetItem(list_type, (int)i);
            for (int j = 0; j < item->count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
            {
                long src_index = item->group[j];

                const txc_media_t *temp = txc_get_media(id_, src_index);
                if (temp && temp->type == TYPE_MUSIC_URL)
                {
                    res_id = temp->res_id ? temp->res_id : "";
                    return res_id;
                }
            }
        }
    }
    return res_id;
}

TXCPlayerManager::TXCPlayerManager()
{
}

PtrPlayer TXCPlayerManager::NewPlayer(int id)
{
    PtrPlayer player(new TXCPlayer(id));
    vPlayers_[id] = player;

    return player;
}

PtrPlayer TXCPlayerManager::NewPlayer(int id, REPEAT_MODE repeatMode)
{
    PtrPlayer player(new TXCPlayer(id, repeatMode));
    vPlayers_[id] = player;

    return player;
}

PtrPlayer TXCPlayerManager::GetPlayer(int app_id)
{
    PtrPlayer result;
    std::map<int, PtrPlayer>::iterator itr = vPlayers_.find(app_id);
    if (vPlayers_.end() != itr)
    {
        result = itr->second;
    }

    return result;
}

bool txc_player_control(SESSION id, TXC_PLAYER_CONTROL control_code, int arg1, int arg2)
{
    PtrPlayer player = TXCServices::instance()->GetPlayerManager()->GetPlayer(id);
    if (!player.get())
    {
        return false;
    }
    bool handled = false;
    switch (control_code)
    {
    case PLAYER_STOP:
        handled = send_message(id, XWM_STOP, 0, 0);
        break;
    case PLAYER_PLAY:
        post_message(id, XWM_REQUEST_AUDIO_FOCUS, XWPARAM((long)id), NULL);
        handled = send_message(id, XWM_PLAY, XWPARAM((long)arg1), XWPARAM((long)arg2));
        break;
    case PLAYER_PAUSE:
        handled = send_message(id, XWM_PAUSE, XWPARAM(1), XWPARAM(false));
        break;
    case PLAYER_RESUME:
        post_message(id, XWM_REQUEST_AUDIO_FOCUS, XWPARAM((long)id), NULL);
        handled = send_message(id, XWM_PAUSE, 0, XWPARAM(false));
        break;
    case PLAYER_VOLUME:
        handled = send_message(id, XWM_VOLUME, XWPARAM((long)arg1), 0);
        break;
    case PLAYER_REPEAT:
        handled = send_message(id, XWM_REPEAT, XWPARAM((long)arg1), 0);
        break;
    case PLAYER_NEXT:
        post_message(id, XWM_REQUEST_AUDIO_FOCUS, XWPARAM((long)id), NULL);
        handled = send_message(id, XWM_NEXT, XWPARAM((long)arg1), 0);
        break;
    case PLAYER_SKIP:
        break;
    default:
        break;
    }

    return handled;
}

void txc_player_statechange(SESSION id, TXC_PLAYER_STATE state_code)
{
    post_message(id, XWM_PLAYER_STATUS_CHANGED, XWPARAM(state_code), NULL, 0);
}

const txc_player_info_t *txc_get_player_info(SESSION id)
{
    PtrPlayer player = TXCServices::instance()->GetPlayerManager()->GetPlayer(id);
    if (player.get())
    {
        return player->GetInfo();
    }

    return NULL;
}
