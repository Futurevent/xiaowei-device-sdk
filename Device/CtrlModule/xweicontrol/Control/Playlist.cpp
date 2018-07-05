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
#include <stdlib.h>
#include "Playlist.hpp"
#include "Playlist.h"
#include "Player.h"
#include "Media.h"
#include "TXCServices.hpp"
#include <sstream>
#include "logger.h"

CPlaylist::CPlaylist()
{
    repeat_ = REPEAT_SEQUENCE;
    x_ = -1;
    y_ = -1;
    id_ = -1;
    x_in_ui_ = -1;
    current_list_type_ = txca_playlist_type_default;
}

CPlaylist::CPlaylist(REPEAT_MODE repeatMode, SESSION id)
{
    repeat_ = repeatMode;
    x_ = -1;
    y_ = -1;
    id_ = id;
    x_in_ui_ = -1;
    current_list_type_ = txca_playlist_type_default;
}

long CPlaylist::PushBack(int list_type, const std::vector<txc_play_item_t> &list)
{
    size_t count = list.size();
    if (list_type == txca_playlist_type_default)
    {
        size_t org_count = list_.size();
        list_.insert(list_.end(), list.begin(), list.end());

        if (count > 0 && org_count == 0)
        {
            x_ = 0;
            x_in_ui_ = 0;
        }

        if (REPEAT_RANDOM == repeat_)
        {
            for (int i = (int)org_count; i < list_.size(); i++)
            {
                un_random_list_.push_back(i);
            }
        }

        TLOG_DEBUG("sessionId=%d CPlaylist::PushBack list[size]=%d list_[size]=%d x_=%d ui_x_=%d", id_, count, org_count, x_, x_in_ui_);
        return count;
    }
    else if (list_type == txca_playlist_type_history)
    {
        size_t org_count = list_history_.size();
        list_history_.insert(list_history_.end(), list.begin(), list.end());
        TLOG_DEBUG("sessionId=%d CPlaylist::PushBack list[size]=%d list_history_[size]=%d x_=%d ui_x_=%d", id_, count, org_count, x_, x_in_ui_);
        return count;
    }
    return 0;
}

long CPlaylist::PushFront(int list_type, const std::vector<txc_play_item_t> &list)
{
    size_t count = list.size();
    if (list_type == txca_playlist_type_default)
    {
        size_t org_count = list_.size();
        list_.insert(list_.begin(), list.begin(), list.end());

        if (current_list_type_ == txca_playlist_type_default && count > 0)
        {
            x_ += count;
            x_in_ui_ += count;
        }

        if (REPEAT_RANDOM == repeat_)
        {
            ResetRandomList();
        }

        TLOG_DEBUG("sessionId=%d CPlaylist::PushFront list[size]=%d list_[size]=%d x_=%d ui_x_=%d", id_, count, org_count, x_, x_in_ui_);
        return count;
    }
    else if (list_type == txca_playlist_type_history)
    {
        size_t org_count = list_history_.size();
        list_history_.insert(list_history_.begin(), list.begin(), list.end());
        if (current_list_type_ == txca_playlist_type_history && count > 0)
        {
            x_ += count;
            x_in_ui_ += count;
        }
        TLOG_DEBUG("sessionId=%d CPlaylist::PushFront list[size]=%d list_history_[size]=%d x_=%d ui_x_=%d", id_, count, org_count, x_, x_in_ui_);
    }
    return 0;
}

void CPlaylist::print()
{
    std::stringstream ss;
    ss << "random_list_:[";
    for (std::vector<UI_INDEX>::iterator iter = random_list_.begin(); iter != random_list_.end(); iter++)
    {
        UI_INDEX index = *iter;
        ss << index;
        ss << ",";
    }
    TLOG_DEBUG("CPlaylist::print %s", ss.str().c_str());
}

void CPlaylist::ResetRandomList()
{
    un_random_list_.clear();
    random_list_.clear();
    for (int i = 0; i < list_.size(); i++)
    {
        un_random_list_.push_back(i);
    }
    if (current_list_type_ == txca_playlist_type_default)
    {
        AddRandomList(GetCurIndex());
        x_ = 0;
        GetMorePlayListIfNeed();
    }
}
bool CPlaylist::Remove(std::string resId)
{
    //  先从list中寻找res_id所在的txc_media_t，再确认txc_media_t是否处于正在播放状态，如果是，返回false。否则，移除list_元素。  如果是随机播放模式，从random_list_和un_random_list_移除对应的ui_index，并fix x_,x_in_ui_.其余模式只需要直接 fix x_，x_in_ui_。
    const txc_player_info_t *player_info = txc_get_player_info(id_);
    if (player_info == NULL)
    {
        return false;
    }

    int ui_index = 0;
    int belong_list_type = -1;
    print();
    TLOG_DEBUG("sessionId=%d CPlaylist::Remove before list[size]=%d list_history_[size]=%d x_=%d ui_x_=%d ui_index=%d, list_type=%d", id_, list_.size(), list_history_.size(), x_, x_in_ui_, ui_index, belong_list_type);
    for (std::vector<txc_play_item_t>::iterator iter = list_.begin(); iter != list_.end() && belong_list_type == -1; iter++)
    {
        txc_play_item_t item = *iter;
        for (int j = 0; j < item.count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
        {
            long src_index = item.group[j];
            const txc_media_t *temp = txc_get_media(player_info->session, src_index);
            if (temp != NULL && resId == temp->res_id)
            {
                if (current_list_type_ == txca_playlist_type_default && ui_index == x_in_ui_)
                {
                    // 正在播放这首歌
                    return false;
                }
                list_.erase(iter++);
                if (ui_index < x_in_ui_)
                {
                    x_in_ui_--;
                }
                belong_list_type = txca_playlist_type_default;
                break;
            }
        }
        if (belong_list_type == -1)
        { // 没找到
            ui_index++;
        }
    }
    if (belong_list_type == -1)
    {
        ui_index = 0;
        for (std::vector<txc_play_item_t>::iterator iter = list_history_.begin(); iter != list_history_.end() && belong_list_type == -1; iter++)
        {
            txc_play_item_t item = *iter;
            for (int j = 0; j < item.count && j < PLAY_ITEM_GROUP_MAX_SIZE; j++)
            {
                long src_index = item.group[j];
                const txc_media_t *temp = txc_get_media(player_info->session, src_index);
                if (temp != NULL && resId == temp->res_id)
                {
                    if (current_list_type_ == txca_playlist_type_history && ui_index == x_in_ui_)
                    {
                        // 正在播放这首歌
                        return false;
                    }
                    list_history_.erase(iter++);
                    if (ui_index < x_in_ui_)
                    {
                        x_in_ui_--;
                    }
                    belong_list_type = txca_playlist_type_history;
                    break;
                }
            }
            if (belong_list_type == -1)
            { // 没找到
                ui_index++;
            }
        }
    }
    if (belong_list_type != -1)
    {
        if (REPEAT_RANDOM == repeat_ && belong_list_type == txca_playlist_type_default)
        {
            ResetRandomList();
        }
        else
        {
            // 如果删掉了正在播放的之前的元素，fix一下当前播放的下标。
            if (ui_index < x_)
            {
                x_--;
            }
        }
    }
    TLOG_DEBUG("sessionId=%d CPlaylist::Remove list[size]=%d list_history_[size]=%d x_=%d ui_x_=%d ui_index=%d, list_type=%d", id_, list_.size(), list_history_.size(), x_, x_in_ui_, ui_index, belong_list_type);
    print();
    return belong_list_type != -1;
}

void CPlaylist::Clear(int list_type)
{
    if (list_type == txca_playlist_type_default || list_type == -1)
    {
        list_.clear();
        random_list_.clear();
        un_random_list_.clear();
    }
    if (list_type == txca_playlist_type_history || list_type == -1)
    {
        list_history_.clear();
    }
    if (current_list_type_ == list_type || list_type == -1)
    {
        x_ = -1;
        y_ = -1;
        current_list_type_ = txca_playlist_type_default;
    }
}

size_t CPlaylist::Count(int list_type)
{
    if (list_type == txca_playlist_type_default)
    {
        return list_.size();
    }
    else if (list_type == txca_playlist_type_history)
    {
        return list_history_.size();
    }
    return list_.size() + list_history_.size();
}

void CPlaylist::SetRepeat(REPEAT_MODE mode)
{
    if (mode != repeat_)
    {
        un_random_list_.clear();
        random_list_.clear();
        if (REPEAT_RANDOM == mode)
        {
            ResetRandomList();
        }
        else
        {
            x_ = x_in_ui_;
        }
        repeat_ = mode;
    }
}

REPEAT_MODE CPlaylist::GetRepeat()
{
    return repeat_;
}

const txc_play_item_t *CPlaylist::GetItem(int list_type, UI_INDEX index)
{
    txc_play_item_t *item = NULL;
    if (index < 0)
    {
        return item;
    }
    if (list_type == txca_playlist_type_default)
    {
        if (index < (int)list_.size())
        {
            item = &(list_[index]);
        }
    }
    else if (list_type == txca_playlist_type_history)
    {
        if (index < (int)list_history_.size())
        {
            item = &(list_history_[index]);
        }
    }
    else
    {
        if (index < (int)list_history_.size())
        {
            item = &(list_history_[index]);
        }
        else
        {
            index = index - (UI_INDEX)list_history_.size();
            if (index < (int)list_.size())
            {
                item = &(list_[index]);
            }
        }
    }
    return item;
}

CPlaylist::SRC_INDEX CPlaylist::Seek(UI_INDEX index, int list_type, bool cannot_be_restart)
{ //  seek item in the list
    TLOG_DEBUG("sessionId=%d CPlaylist::Seek index: %d", id_, index);
    if(x_in_ui_ == index && list_type == current_list_type_ && cannot_be_restart) {
        return -2;
    }
    CPlaylist::SRC_INDEX src_index = -1;
    y_ = -1;

    if (list_type == txca_playlist_type_default)
    {
        if (0 <= index && index < (int)list_.size())
        {
            x_in_ui_ = index;
            if (repeat_ != REPEAT_RANDOM)
            {
                x_ = index;
            }
            else
            {
                // 从random中找到这个ui_index，如果不存在，加进去。
                x_ = RandomIndexOf(index);
                if (x_ < 0)
                {
                    AddRandomList(index);
                    x_ = (UI_INDEX)random_list_.size() - 1;
                }
            }

            const txc_play_item_t &item = list_[index];
            if (item.count > 0)
            {
                current_list_type_ = txca_playlist_type_default;
                src_index = NextY();
            }
        }
    }
    else if (list_type == txca_playlist_type_history)
    {
        if (0 <= index && index < (int)list_history_.size())
        {
            x_in_ui_ = index;
            x_ = index;

            const txc_play_item_t &item = list_history_[index];
            if (item.count > 0)
            {
                current_list_type_ = txca_playlist_type_history;
                src_index = NextY();
            }
        }
    }
    GetMorePlayListIfNeed();
    return src_index;
}

// 如果之后切歌切到第一首，主动拉下历史记录
CPlaylist::SRC_INDEX CPlaylist::NextX(long offset, bool isAuto)
{
    CPlaylist::SRC_INDEX src_index = -1;
    CPlaylist::UI_INDEX ui_index = -1;
    UI_INDEX old_y = y_;
    y_ = -1;
    TXCA_PLAYLIST_TYPE list_type = current_list_type_;
    const size_t list_count = list_.size();
    if(list_count == 0) {
        return src_index;
    }
    print();
    TLOG_DEBUG("sessionId=%d CPlaylist::NextX x_: %d x_ui:%d random_list_.size()=%d", id_, x_, x_in_ui_, random_list_.size());
    if (REPEAT_RANDOM == repeat_)
    {
        long index = x_ + offset;

        if (index < 0)
        {
            if (current_list_type_ == txca_playlist_type_default && list_history_.size() > 0)
            {
                list_type = txca_playlist_type_history;
                index = (UI_INDEX)list_history_.size() + ui_index;
            }
            else if (current_list_type_ == txca_playlist_type_default)
            {
                // 创建一个，添加到random_list_的末尾。
                int idx = CreateRandomNum((int)un_random_list_.size());
                if (idx >= 0)
                {
                    AddRandomList(un_random_list_[idx]);
                }
                index = random_list_.size() - 1;
            }
        }
        else
        {
            if (current_list_type_ == txca_playlist_type_history && index >= list_history_.size())
            {
                list_type = txca_playlist_type_default;
                index -= list_history_.size();
            }
            if (list_type == txca_playlist_type_default)
            {
                if (index >= random_list_.size())
                {
                    int idx = CreateRandomNum((int)un_random_list_.size());
                    if (idx >= 0)
                    {
                        AddRandomList(un_random_list_[idx]);
                        index = random_list_.size() - 1;
                    }
                    else
                    {
                        index = 0;
                    }
                }
            }
        }

        x_ = (UI_INDEX)index;
        if (list_type == txca_playlist_type_history)
        {
            ui_index = (UI_INDEX)index;
        }
        else if (index >= 0 && index < random_list_.size())
        {
            ui_index = random_list_[index];
        }
    }
    else if (REPEAT_SINGLE == repeat_)
    {
        if (isAuto)
        {
            ui_index = x_;
        }
        else
        {
            // if user do next, just link REPEAT_LOOP mode
            ui_index = x_ + (int)offset;

            if (ui_index < 0)
            {
                if (current_list_type_ == txca_playlist_type_default && list_history_.size() > 0)
                {
                    list_type = txca_playlist_type_history;
                    ui_index = (UI_INDEX)list_history_.size() + ui_index;
                }
                else if (current_list_type_ == txca_playlist_type_default)
                {
                    ui_index = -ui_index % list_count;
                    ui_index = (UI_INDEX)list_count - ui_index;
                }
            }
            else
            {
                if (current_list_type_ == txca_playlist_type_history && ui_index >= list_history_.size())
                {
                    list_type = txca_playlist_type_default;
                    ui_index -= list_history_.size();
                }
                if (list_type == txca_playlist_type_default)
                {
                    ui_index = ui_index % list_count;
                }
            }
        }
    }
    else if (REPEAT_LOOP == repeat_)
    {
        ui_index = x_ + (int)offset;

        if (ui_index < 0)
        {
            if (current_list_type_ == txca_playlist_type_default && list_history_.size() > 0)
            {
                list_type = txca_playlist_type_history;
                ui_index = (UI_INDEX)list_history_.size() + ui_index;
            }
        }
        else
        {
            if (current_list_type_ == txca_playlist_type_history && ui_index >= list_history_.size())
            {
                list_type = txca_playlist_type_default;
                ui_index -= list_history_.size();
            }
            if (list_type == txca_playlist_type_default)
            {
                ui_index = ui_index % list_count;
            }
        }
    }
    else if (REPEAT_SEQUENCE == repeat_)
    {
        ui_index = x_ + (int)offset;

        if (ui_index < 0 || ui_index >= (int)list_count)
        {
            ui_index = -1;
        }
    }

    if (0 <= ui_index && ((list_type == txca_playlist_type_default && (int)list_count > ui_index) || (list_type == txca_playlist_type_history && (int)list_history_.size() > ui_index)))
    {

        current_list_type_ = list_type;
        x_ = (repeat_ != REPEAT_RANDOM ? ui_index : x_);
        x_in_ui_ = ui_index;
        const txc_play_item_t &item = current_list_type_ == txca_playlist_type_default ? list_[ui_index] : list_history_[ui_index];

        if (0 < item.count)
        {
            src_index = NextY(); // 取这个Group的第一个可播放元素
        }
    }

    if (src_index == -1)
    {
        y_ = old_y; // 切换失败了恢复y
    } else {
        GetMorePlayListIfNeed();
    }
    return src_index;
}

void CPlaylist::GetMorePlayListIfNeed()
{
    if(x_ !=0 && (current_list_type_ == txca_playlist_type_default && x_ != list_.size() - 1) && (current_list_type_ == txca_playlist_type_history && x_ != list_history_.size() - 1))
    {
        // 不是列表的首尾，就不执行了
        return;
    }
    const txc_player_info_t *player_info = txc_get_player_info(id_);
    const txc_playlist_t *playlist_info = NULL;
    if (player_info)
    {
        playlist_info = txc_get_medialist_info(player_info->session);
    }
    if(current_list_type_ == txca_playlist_type_default && (x_ == 0 || x_ == list_.size() - 1))
    {
        if(x_ == 0) {
            // 如果能上拉取，先上拉取，否则就拉历史
            if(playlist_info && playlist_info->has_more_current_up) {
                send_message(id_, XWM_GET_MORE_LIST, XWPARAM(TYPE_GET_MORE_UP), XWPARAM(txca_playlist_type_default));
            } else {
                send_message(id_, XWM_GET_MORE_LIST, XWPARAM(TYPE_GET_HISTORY), XWPARAM(txca_playlist_type_default));
            }
        }
        if(x_ == list_.size() - 1) {
            if(playlist_info && playlist_info->has_more_current) {
                send_message(id_, XWM_GET_MORE_LIST, XWPARAM(TYPE_GET_MORE), XWPARAM(txca_playlist_type_default));
            }
        }
    } else if(current_list_type_ == txca_playlist_type_history && (x_ == 0 || x_ == list_history_.size() - 1))
    {
        if(x_ == 0) {
            if(playlist_info && playlist_info->has_more_history_up) {
                send_message(id_, XWM_GET_MORE_LIST, XWPARAM(TYPE_GET_MORE_UP), XWPARAM(txca_playlist_type_history));
            }
        }
        if(x_ == list_history_.size() - 1) {
            if(playlist_info && playlist_info->has_more_history) {
                send_message(id_, XWM_GET_MORE_LIST, XWPARAM(TYPE_GET_MORE), XWPARAM(txca_playlist_type_history));
            }
        }
    }
}

CPlaylist::UI_INDEX CPlaylist::GetCurIndex()
{
    return x_in_ui_;
}

int CPlaylist::GetCurPlayListType()
{
    return current_list_type_;
}

void CPlaylist::SetCurPlayListType(TXCA_PLAYLIST_TYPE type)
{
    current_list_type_ = type;
}

CPlaylist::SRC_INDEX CPlaylist::NextY()
{ //  next index in current item/group;
    TLOG_DEBUG("sessionId=%d CPlaylist::NextY y_:%d", id_, y_);

    CPlaylist::SRC_INDEX src_index = -1;

    if (0 <= x_in_ui_ && ((current_list_type_ == txca_playlist_type_default && x_in_ui_ < (int)list_.size()) || (current_list_type_ == txca_playlist_type_history && x_in_ui_ < (int)list_history_.size())))
    {
        const txc_play_item_t &item = current_list_type_ == txca_playlist_type_default ? list_[x_in_ui_] : list_history_[x_in_ui_];
        TLOG_DEBUG("sessionId=%d CPlaylist::NextY item.count:%d", id_, item.count);
        int i = (y_ >= 0 ? y_ + 1 : 0);

        for (; i < item.count; i++)
        {
            const txc_player_info_t *player_info = txc_get_player_info(id_);
            if (player_info)
            {
                const txc_playlist_t *playlist_info = txc_get_medialist_info(player_info->session);
                if (playlist_info && playlist_info->count > 0)
                {
                    const txc_media_t *media = txc_get_media(player_info->session, item.group[i]);
                    if (media != NULL && TXCServices::instance()->GetMediaCenter()->IsMediaNeedPlay(media->res_id))
                    {
                        src_index = item.group[i];
                        y_ = i;
                        break;
                    }
                }
            }
        }
    }

    return src_index;
}

int CPlaylist::RandomIndexOf(UI_INDEX ui_index)
{
    std::vector<UI_INDEX>::iterator iter = std::find(random_list_.begin(), random_list_.end(), ui_index);

    if (iter != random_list_.end())
    {
        return (int)std::distance(random_list_.begin(), iter);
    }
    return -1;
}

int CPlaylist::CreateRandomNum(int num)
{
    if (num <= 0)
    {
        return -1;
    }
    return rand() % num;
}

void CPlaylist::AddRandomList(UI_INDEX index)
{
    std::vector<UI_INDEX>::iterator iter = std::find(un_random_list_.begin(), un_random_list_.end(), index);
    if (iter != un_random_list_.end())
    {
        UI_INDEX i = *iter;
        random_list_.push_back(i);
        un_random_list_.erase(iter);
    }
}
