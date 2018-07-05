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
#ifndef AudioFocusManager_hpp
#define AudioFocusManager_hpp

#include <vector>
#include <map>
#include <string>
#include "AudioFocus.h"
#include "AudioFocusManagerImpl.hpp"

#include "txctypedef.h"

class TXCAudioFocusManagerImpl;

// 音频焦点改变监听
class OnAudioFocusChangeListener
{
  public:
    virtual ~OnAudioFocusChangeListener(){

    };
    virtual void OnAudioFocusChange(int focus_change) = 0;
};

class CFocusItem
{
  public:
    CFocusItem();
    ~CFocusItem();

    SESSION id;
    int cookie;         // cookie
    DURATION_HINT hint; // 申请的焦点类型
    OnAudioFocusChangeListener *listener;

    std::string ToString();
};

class COuterFocusListener : public OnAudioFocusChangeListener
{
  public:
    COuterFocusListener()
        : cookie(-1){};

    int cookie;
    virtual void OnAudioFocusChange(int focusChange);
};

class OnAudioFocusCallback
{
  public:
    virtual ~OnAudioFocusCallback(){

    };
    virtual void OnAudioFocusChangeCallback(int cookie, DURATION_HINT focus_change) = 0;
};

class TXCAudioFocusManager : public OnAudioFocusCallback
{
  public:
    TXCAudioFocusManager();
    virtual ~TXCAudioFocusManager();

    // 使用SESSIONID来请求焦点，如果该id不存在关联的OnAudioFocusChangeListener，会返回false，是否申请到以关联的listener的回调为准
    AUDIOFOCUS_REQUEST_RESULT RequestAudioFocus(SESSION id);
    // 为listener申请duration类型的焦点，并关联id和listener，是否申请到以listener的回调为准
    AUDIOFOCUS_REQUEST_RESULT RequestAudioFocus(SESSION id, OnAudioFocusChangeListener *listener, DURATION_HINT duration);

    // 使用SESSIONID来释放焦点，如果该id不存在关联的OnAudioFocusChangeListener，会返回false
    AUDIOFOCUS_REQUEST_RESULT AbandonAudioFocus(SESSION id);
    // 释放listener的焦点
    AUDIOFOCUS_REQUEST_RESULT AbandonAudioFocus(OnAudioFocusChangeListener *listener);

    // 释放所有焦点，这个操作会导致所有注册的listener都收到AUDIOFOCUS_LOSS
    AUDIOFOCUS_REQUEST_RESULT AbandonAllAudioFocus();
    // 设置可以用的焦点，例如Android的音乐APP占用了焦点，那么XweiControl中分配焦点数量会相应调整
    void SetAudioFocus(DURATION_HINT hint);

    virtual void OnAudioFocusChangeCallback(int cookie, DURATION_HINT focus_change);
    bool HandleAudioFocusMessage(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2);

  private:
    AUDIOFOCUS_REQUEST_RESULT AbandonAudioFocus(CFocusItem *item);
    AUDIOFOCUS_REQUEST_RESULT RequestAudioFocusWithCookie(int cookie, OnAudioFocusChangeListener *listener, DURATION_HINT duration);
    AUDIOFOCUS_REQUEST_RESULT AbandonAudioFocusWithCookie(int cookie);
    CFocusItem *GetFocusItem(SESSION id);
    CFocusItem *GetFocusItemWithCookie(int cookie);
    CFocusItem *GetFocusItem(OnAudioFocusChangeListener *listener);
    void Release(CFocusItem *item);
    int CreateCookie(int id);

  private:
    std::map<int, CFocusItem *> m_id_item;
    std::map<int, CFocusItem *> m_cookie_item;
    std::map<OnAudioFocusChangeListener *, CFocusItem *> m_lis_item;
    std::map<int, int> m_id_cookie;

    TXCAudioFocusManagerImpl *audio_focus_manager_impl_;
};

#endif /* AudioFocusManager_hpp */
