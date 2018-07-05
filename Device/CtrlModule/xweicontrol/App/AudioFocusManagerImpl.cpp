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
#ifndef AudioFocusManagerImpl_cpp
#define AudioFocusManagerImpl_cpp

#include "logger.h"
#include "AudioFocusManagerImpl.hpp"
#include "Util.hpp"

#include "TXCServices.hpp"
#include <sstream>

#define MAX_FOCUS 3

CFocusImplItem::CFocusImplItem()
    : cookie(0), need(0), old(0), cur(0), recoverable(false)
{
}

CFocusImplItem::~CFocusImplItem()
{
}

std::string CFocusImplItem::ToString()
{
    std::stringstream str;
    str << "cookie=";
    str << cookie;
    str << ",hint=";
    str << Util::ToString(hint);
    str << ",need=";
    str << need;
    str << ",cur=";
    str << cur;
    str << ",old=";
    str << old;
    return str.str();
}

TXCAudioFocusManagerImpl::TXCAudioFocusManagerImpl()
    : mFocus(MAX_FOCUS), mFocusTransitivity(true), m_callback(NULL)
{
}

TXCAudioFocusManagerImpl::~TXCAudioFocusManagerImpl()
{
    m_cookie_item.clear();
    for (std::list<CFocusImplItem *>::iterator iter = m_focus_items.begin(); iter != m_focus_items.end();)
    {
        delete (*iter);
        iter = m_focus_items.erase(iter);
    }
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManagerImpl::RequestAudioFocus(int cookie, DURATION_HINT hint)
{
    if (hint < AUDIOFOCUS_GAIN || hint > AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
    {
        return AUDIOFOCUS_REQUEST_FAILED;
    }

    // 将cookie和hint添加到list的头部
    AddFocusItem(cookie, hint);

    // 重新按照顺序分配焦点
    DispatchAudioFocus(mFocus);
    return AUDIOFOCUS_REQUEST_GRANTED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManagerImpl::AbandonAudioFocus(int cookie)
{
    CFocusImplItem *item = NULL;

    std::map<int, CFocusImplItem *>::iterator itor = m_cookie_item.find(cookie);
    if (itor != m_cookie_item.end())
    {
        item = itor->second;
    }
    if (item != NULL)
    {
        RemoveFocusItem(cookie);
        if (mFocusTransitivity)
        {
            // 重新按照顺序分配焦点
            DispatchAudioFocus(mFocus);
        }
    }
    return AUDIOFOCUS_REQUEST_GRANTED;
}

AUDIOFOCUS_REQUEST_RESULT TXCAudioFocusManagerImpl::AbandonAllAudioFocus()
{
    TLOG_TRACE("TXCAudioFocusManagerImpl::AbandonAllAudioFocus");
    for (std::list<CFocusImplItem *>::iterator iter = m_focus_items.begin(); iter != m_focus_items.end();)
    {
        delete (*iter);
        iter = m_focus_items.erase(iter);
    }
    m_cookie_item.clear();
    return AUDIOFOCUS_REQUEST_GRANTED;
}

void TXCAudioFocusManagerImpl::AddFocusItem(int cookie, DURATION_HINT hint)
{
    CFocusImplItem *item = NULL;

    std::map<int, CFocusImplItem *>::iterator itor = m_cookie_item.find(cookie);
    if (itor != m_cookie_item.end())
    {
        item = itor->second;
    }
    if (item == NULL)
    {
        item = new CFocusImplItem;
        m_cookie_item[cookie] = item;
    }
    else
    {
        std::list<CFocusImplItem *>::iterator iter = std::find(m_focus_items.begin(), m_focus_items.end(), item);
        m_focus_items.erase(iter);
    }
    item->recoverable = hint == AUDIOFOCUS_GAIN;
    item->cookie = cookie;
    item->hint = hint;
    item->need = GetFocus(hint);
    item->old = 0;
    item->cur = 0;

    m_focus_items.push_front(item);
}

bool TXCAudioFocusManagerImpl::RemoveFocusItem(int cookie)
{
    m_cookie_item.erase(cookie);
    for (std::list<CFocusImplItem *>::iterator iter = m_focus_items.begin(); iter != m_focus_items.end(); iter++)
    {
        CFocusImplItem *item = *iter;
        if (item->cookie == cookie)
        {
            m_focus_items.erase(iter);
            delete item;
            return true;
        }
    }
    return false;
}

int TXCAudioFocusManagerImpl::GetFocus(DURATION_HINT duration)
{
    if (duration == AUDIOFOCUS_GAIN)
    {
        return MAX_FOCUS;
    }
    if (duration == AUDIOFOCUS_GAIN_TRANSIENT || duration == AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
    {
        return MAX_FOCUS; // 其他会暂停
    }
    if (duration == AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    {
        return 2; // 其他会降低音量
    }
    return 0;
}

void TXCAudioFocusManagerImpl::SetAudioFocusChangeCallback(OnAudioFocusCallback *callback)
{
    m_callback = callback;
}

void TXCAudioFocusManagerImpl::DispatchAudioFocus(unsigned int duration)
{
    if (m_focus_items.size() == 0)
    {
        return;
    }
    unsigned int focus = duration; // 可以分配的焦点
    unsigned int found = 0;        // 之前已经分配的焦点

    // 依次取出CFocusItem，分配focus给它，如果分配给它后还有剩余focus，就分给下一个。同时记录之前分配的焦点数。直到分配完毕并且之前的焦点都找到了，就结束循环。
    for (std::list<CFocusImplItem *>::iterator iter = m_focus_items.begin(); iter != m_focus_items.end();)
    {
        (*iter)->old = (*iter)->cur;
        (*iter)->cur = std::min((*iter)->need, focus);
        found += (*iter)->old;
        focus -= (*iter)->cur;

        bool released = false;

        if ((*iter)->cur != (*iter)->old)
        { // 旧的和新的不一致，就回调
            TLOG_TRACE("TXCAudioFocusManagerImpl::DispatchAudioFocus cookie=%d cur=%u old=%u focus=%u found=%u", (*iter)->cookie, (*iter)->cur, (*iter)->old, focus, found);
            released = CallbackFocusChange(*iter);
            if (released)
            {
                // 移走
                m_cookie_item.erase((*iter)->cookie);
                delete (*iter);
                iter = m_focus_items.erase(iter);
            }
        }
        if (!released)
        {
            iter++;
        }
        if (found == mFocus && focus <= 0)
        {
            // 找到了所有的焦点了，没更多了
            break;
        }
    }
}

void TXCAudioFocusManagerImpl::SetAudioFocus(DURATION_HINT hint)
{
    TLOG_TRACE("TXCAudioFocusManagerImpl::SetAudioFocus hint=%s mFocus[%d] mFocusTransitivity[%d]", Util::ToString(hint).c_str(), mFocus, mFocusTransitivity);
    int focus = 0;
    bool focusTransitivity = true;
    switch (hint)
    {
    case AUDIOFOCUS_GAIN:
    case AUDIOFOCUS_GAIN_TRANSIENT:
        focus = 3;
        break;
    case AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
        focus = 3;
        // 不可传递
        if (mFocus == 0)
        {
            focusTransitivity = false;
        }
        break;
    case AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
        focus = 2;
        break;
    case AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
        focus = 1;
        break;
    default:
        focus = 0;
        break;
    }
    if ((int)mFocus != focus || mFocusTransitivity != focusTransitivity)
    {
        DispatchAudioFocus(focus);
        mFocus = focus;
        mFocusTransitivity = focusTransitivity;
    }
}

bool TXCAudioFocusManagerImpl::CallbackFocusChange(CFocusImplItem *item)
{
    if (item->cur == item->need)
    {
        TLOG_TRACE("TXCAudioFocusManagerImpl::CallbackFocusChange @1 cookie=%d hint=%s", item->cookie, Util::ToString(item->hint).c_str());
        if (m_callback)
        {
            m_callback->OnAudioFocusChangeCallback(item->cookie, item->hint);
        }
    }
    else if (item->cur == 0)
    {
        TLOG_TRACE("TXCAudioFocusManagerImpl::CallbackFocusChange @2 cookie=%d hint=%s", item->cookie, Util::ToString(item->recoverable ? AUDIOFOCUS_LOSS_TRANSIENT : AUDIOFOCUS_LOSS).c_str());
        if (m_callback)
        {
            m_callback->OnAudioFocusChangeCallback(item->cookie, item->recoverable ? AUDIOFOCUS_LOSS_TRANSIENT : AUDIOFOCUS_LOSS);
        }
        return !item->recoverable;
    }
    else if (item->cur < item->need)
    {
        TLOG_TRACE("TXCAudioFocusManagerImpl::CallbackFocusChange @3 cookie=%d hint=AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK", item->cookie);
        if (m_callback)
        {
            m_callback->OnAudioFocusChangeCallback(item->cookie, AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
        }
    }
    return false;
}

#endif /* AudioFocusManagerImpl_cpp */
