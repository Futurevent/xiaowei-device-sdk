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
#ifndef AudioFocusManager_cpp
#define AudioFocusManager_cpp

#include "logger.h"
#include "AudioFocusManager.hpp"
#include "Util.hpp"

#include "TXCServices.hpp"
#include <sstream>

#define MAX_FOCUS 3

static int s_cookie = 0;
SDK_API tcx_xwei_audio_focus_interface audio_focus_interface = {0};

CFocusItem::CFocusItem()
    : id(0), cookie(0), listener(NULL)
{
}

CFocusItem::~CFocusItem()
{
    listener = NULL;
}

std::string CFocusItem::ToString()
{
    std::stringstream str;
    str << "cookie=";
    str << cookie;
    str << ",hint=";
    str << Util::ToString(hint);
    str << ",listener=";
    str << listener;
    return str.str();
}

TXCAudioFocusManager::TXCAudioFocusManager()
{
    audio_focus_manager_impl_ = new TXCAudioFocusManagerImpl;
    audio_focus_manager_impl_->SetAudioFocusChangeCallback(this);
}

TXCAudioFocusManager::~TXCAudioFocusManager()
{
    AbandonAllAudioFocus();
    delete audio_focus_manager_impl_;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::RequestAudioFocus(SESSION id)
{
    CFocusItem *item = GetFocusItem(id);
    if (item != NULL)
    {
        return RequestAudioFocus(id, item->listener, item->hint);
    }
    return AUDIOFOCUS_REQUEST_FAILED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::RequestAudioFocus(SESSION id, OnAudioFocusChangeListener *listener, DURATION_HINT duration)
{
    if (duration < AUDIOFOCUS_GAIN || duration > AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE || listener == NULL)
    {
        return AUDIOFOCUS_REQUEST_FAILED;
    }
    CFocusItem *item = GetFocusItem(id);
    if (item == NULL)
    {
        item = new CFocusItem;
        item->id = id;
        item->cookie = CreateCookie(id);
        m_id_item[id] = item;
        m_cookie_item[item->cookie] = item;
    }
    item->listener = listener;
    item->hint = duration;
    m_lis_item[item->listener] = item;

    // 请求焦点
    if (audio_focus_interface.on_request_audio_focus)
    {
        AUDIOFOCUS_REQUEST_RESULT result = audio_focus_interface.on_request_audio_focus(item->cookie, item->hint);
        if (AUDIOFOCUS_REQUEST_GRANTED == result)
        {
            OnAudioFocusChangeCallback(item->cookie, item->hint);
        }
        return result;
    }
    return audio_focus_manager_impl_->RequestAudioFocus(item->cookie, item->hint);
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::AbandonAudioFocus(SESSION id)
{
    CFocusItem *item = GetFocusItem(id);
    if (item != NULL)
    {
        return AbandonAudioFocus(item);
    }
    return AUDIOFOCUS_REQUEST_GRANTED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::AbandonAudioFocus(OnAudioFocusChangeListener *listener)
{
    if (listener == NULL)
    {
        return AUDIOFOCUS_REQUEST_GRANTED;
    }
    CFocusItem *item = GetFocusItem(listener);
    if (item != NULL)
    {
        return AbandonAudioFocus(item);
    }
    return AUDIOFOCUS_REQUEST_GRANTED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::AbandonAudioFocus(CFocusItem *item)
{
    AUDIOFOCUS_REQUEST_RESULT result = AUDIOFOCUS_REQUEST_GRANTED;
    if (item != NULL)
    {
        // 释放焦点
        if (audio_focus_interface.on_abandon_audio_focus)
        {
            result = audio_focus_interface.on_abandon_audio_focus(item->cookie);
        }
        else
        {
            result = audio_focus_manager_impl_->AbandonAudioFocus(item->cookie);
        }
        if (AUDIOFOCUS_REQUEST_GRANTED == result)
        {
            OnAudioFocusChangeCallback(item->cookie, AUDIOFOCUS_LOSS);
        }
    }
    return result;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::AbandonAllAudioFocus()
{
    TLOG_TRACE("TXCAudioFocusManager::AbandonAllAudioFocus");
    // 释放焦点
    if (audio_focus_interface.on_abandon_audio_focus)
    {
        audio_focus_interface.on_abandon_audio_focus(-1); // -1 表示释放所有
    }
    else
    {
        audio_focus_manager_impl_->AbandonAllAudioFocus();
    }
    for (std::map<OnAudioFocusChangeListener *, CFocusItem *>::iterator iter = m_lis_item.begin(); iter != m_lis_item.end(); iter++)
    {
        OnAudioFocusChangeCallback(iter->second->cookie, AUDIOFOCUS_LOSS);
    }

    TLOG_TRACE("TXCAudioFocusManager::AbandonAllAudioFocus m_id_item[%d] m_cookie_item[%d] m_lis_item[%d] ", m_id_item.size(), m_cookie_item.size(), m_lis_item.size());

    m_id_item.clear();
    m_cookie_item.clear();
    m_lis_item.clear();
    return AUDIOFOCUS_REQUEST_GRANTED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::RequestAudioFocusWithCookie(int cookie, OnAudioFocusChangeListener *listener, DURATION_HINT duration)
{
    if (duration < AUDIOFOCUS_GAIN || duration > AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE || listener == NULL)
    {
        return AUDIOFOCUS_REQUEST_FAILED;
    }
    CFocusItem *item = GetFocusItemWithCookie(cookie);
    if (item == NULL)
    {
        item = new CFocusItem;
        item->cookie = cookie;
        m_cookie_item[item->cookie] = item;
    }
    item->listener = listener;
    item->hint = duration;
    m_lis_item[item->listener] = item;

    // 请求焦点
    if (audio_focus_interface.on_request_audio_focus)
    {
        AUDIOFOCUS_REQUEST_RESULT result = audio_focus_interface.on_request_audio_focus(item->cookie, item->hint);
        if (AUDIOFOCUS_REQUEST_GRANTED == result)
        {
            OnAudioFocusChangeCallback(item->cookie, item->hint);
        }
        return result;
    }
    return audio_focus_manager_impl_->RequestAudioFocus(item->cookie, item->hint);
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManager::AbandonAudioFocusWithCookie(int cookie)
{
    CFocusItem *item = GetFocusItemWithCookie(cookie);
    if (item != NULL)
    {
        return AbandonAudioFocus(item);
    }
    return AUDIOFOCUS_REQUEST_GRANTED;
}

CFocusItem *TXCAudioFocusManager::GetFocusItem(SESSION id)
{
    std::map<int, CFocusItem *>::iterator itor = m_id_item.find(id);
    if (itor != m_id_item.end())
    {
        return itor->second;
    }
    return NULL;
}

CFocusItem *TXCAudioFocusManager::GetFocusItem(OnAudioFocusChangeListener *listener)
{
    std::map<OnAudioFocusChangeListener *, CFocusItem *>::iterator itor = m_lis_item.find(listener);
    if (itor != m_lis_item.end())
    {
        return itor->second;
    }
    return NULL;
}

CFocusItem *TXCAudioFocusManager::GetFocusItemWithCookie(int cookie)
{
    std::map<int, CFocusItem *>::iterator itor = m_cookie_item.find(cookie);
    if (itor != m_cookie_item.end())
    {
        return itor->second;
    }
    return NULL;
}

void TXCAudioFocusManager::SetAudioFocus(DURATION_HINT hint)
{
    audio_focus_manager_impl_->SetAudioFocus(hint);
}

void TXCAudioFocusManager::OnAudioFocusChangeCallback(int cookie, DURATION_HINT focus_change)
{
    CFocusItem *item = GetFocusItemWithCookie(cookie);
    if (item != NULL && item->listener != NULL)
    {

        TLOG_TRACE("TXCAudioFocusManager::OnAudioFocusChangeCallback cookie=%d hint=%s", cookie, Util::ToString(focus_change).c_str());

        item->listener->OnAudioFocusChange(focus_change);
    }
    if (focus_change == AUDIOFOCUS_LOSS)
    {
        Release(item);
    }
}

void TXCAudioFocusManager::Release(CFocusItem *item)
{
    if (item != NULL)
    {
        m_id_item.erase(item->id);
        m_cookie_item.erase(item->cookie);
        m_lis_item.erase(item->listener);
        // 如果是Out，需要delete
        COuterFocusListener *lis = dynamic_cast<COuterFocusListener *>(item->listener);
        if (lis != NULL)
        {
            delete lis;
        }
        delete item;
    }
}

int TXCAudioFocusManager::CreateCookie(int id)
{
    std::map<int, int>::iterator itor = m_id_cookie.find(id);
    if (itor != m_id_cookie.end())
    {
        return itor->second;
    }
    int cookie = ++s_cookie;
    m_id_cookie[id] = cookie;
    return cookie;
}

// 过滤掉焦点类型的消息
bool TXCAudioFocusManager::HandleAudioFocusMessage(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2)
{
    TLOG_TRACE("sessionId=%d TXCAudioFocusManager::HandleAudioFocusMessage event=%s, arg1=%ld, arg2=%ld.", id, Util::ToString(event).c_str(), arg1, arg2);
    switch (event)
    {
    case XWM_REQUEST_AUDIO_FOCUS:
    {
        if (id == -1)
        {
            int cookie = (int)reinterpret_cast<long>(arg1);
            DURATION_HINT hint = (DURATION_HINT) reinterpret_cast<long>(arg2);
            COuterFocusListener *listener = new COuterFocusListener;
            listener->cookie = cookie;
            TXCServices::instance()->GetAudioFocusManager()->RequestAudioFocusWithCookie(cookie, listener, hint);
        }
        else
        {
            TXCServices::instance()->GetAudioFocusManager()->RequestAudioFocus(id);
        }
        return true;
    }
    case XWM_ABANDON_AUDIO_FOCUS:
    {
        if ((bool)arg1)
        {
            TXCServices::instance()->GetAudioFocusManager()->AbandonAllAudioFocus();
        }
        else
        {
            int cookie = 0;
            if (arg2 != NULL)
            {
                cookie = (int)reinterpret_cast<long>(arg2);
            }
            if (cookie > 0)
            {
                TXCServices::instance()->GetAudioFocusManager()->AbandonAudioFocusWithCookie(cookie);
            }
            else
            {
                TXCServices::instance()->GetAudioFocusManager()->AbandonAudioFocus(id);
            }
        }
        return true;
    }
    case XWM_SET_AUDIO_FOCUS:
    {
        TXCServices::instance()->GetAudioFocusManager()->SetAudioFocus((DURATION_HINT)(reinterpret_cast<long>(arg1)));
        return true;
    }
    case XWM_SET_AUDIO_FOCUS_INTERFACE_CHANGE:
    {
        TXCServices::instance()->GetAudioFocusManager()->OnAudioFocusChangeCallback((int)(reinterpret_cast<long>(arg1)), (DURATION_HINT)(reinterpret_cast<long>(arg2)));
        return true;
    }

    default:
        break;
    }
    return false;
}

audio_focus_change_function g_focus_change;

void COuterFocusListener::OnAudioFocusChange(int focusChange)
{
    if (g_focus_change)
    {
        g_focus_change(cookie, focusChange);
    }
}

SDK_API void txc_set_audio_focus_change_callback(audio_focus_change_function func)
{
    g_focus_change = func;
}

SDK_API void txc_request_audio_focus(int &cookie, DURATION_HINT duration)
{
    if (cookie <= 0)
    {
        cookie = ++s_cookie;
    }
    post_message(-1, XWM_REQUEST_AUDIO_FOCUS, XWPARAM((long)cookie), XWPARAM(duration));
}

SDK_API void txc_abandon_audio_focus(int cookie)
{
    post_message(-1, XWM_ABANDON_AUDIO_FOCUS, XWPARAM(0), XWPARAM((long)cookie));
}

SDK_API void txc_abandon_all_audio_focus()
{
    post_message(-1, XWM_ABANDON_AUDIO_FOCUS, XWPARAM(1), NULL);
}

SDK_API void txc_set_audio_focus(DURATION_HINT focus)
{
    post_message(-1, XWM_SET_AUDIO_FOCUS, XWPARAM(focus), NULL);
}

SDK_API void txc_set_audio_focus_interface_change(int cookie, DURATION_HINT duration)
{
    post_message(-1, XWM_SET_AUDIO_FOCUS_INTERFACE_CHANGE, XWPARAM((long)cookie), XWPARAM(duration));
}

#endif /* AudioFocusManager_cpp */
