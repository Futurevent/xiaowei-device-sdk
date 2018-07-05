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
#ifndef AudioFocusManagerImpl_hpp
#define AudioFocusManagerImpl_hpp

#include <list>
#include <map>
#include <string>
#include "AudioFocus.h"
#include "AudioFocusManager.hpp"

#include "txctypedef.h"

class CFocusItem;
class OnAudioFocusCallback;

class CFocusImplItem
{
public:
  CFocusImplItem();
  ~CFocusImplItem();

  int cookie;         // cookie
  DURATION_HINT hint; // 申请的焦点类型
  unsigned int need;  // 需要的焦点数量。AUDIOFOCUS_GAIN(3)、AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK(2)、AUDIOFOCUS_GAIN_TRANSIENT(3)
  unsigned int old;   // 记录改变之前的焦点数量
  unsigned int cur;   // 记录改变后的焦点数量
  bool recoverable;   // 焦点是否可恢复 AUDIOFOCUS_GAIN(true)

  std::string ToString();
};

class TXCAudioFocusManagerImpl
{
public:
  TXCAudioFocusManagerImpl();
  virtual ~TXCAudioFocusManagerImpl();

  // 为item申请焦点
  AUDIOFOCUS_REQUEST_RESULT RequestAudioFocus(int cookie, DURATION_HINT hint);

  // 使用SESSIONID来释放焦点，如果该id不存在关联的OnAudioFocusChangeListener，会返回false
  AUDIOFOCUS_REQUEST_RESULT AbandonAudioFocus(int cookie);
  // 释放所有焦点，这个操作会导致所有注册的listener都收到AUDIOFOCUS_LOSS
  AUDIOFOCUS_REQUEST_RESULT AbandonAllAudioFocus();
  // 设置可以用的焦点，例如Android的音乐APP占用了焦点，那么XweiControl中分配焦点数量会相应调整
  void SetAudioFocus(DURATION_HINT hint);

  void SetAudioFocusChangeCallback(OnAudioFocusCallback *callback);

private:
  void AddFocusItem(int cookie, DURATION_HINT hint);
  bool RemoveFocusItem(int cookie);
  void DispatchAudioFocus(unsigned int focus);
  bool CallbackFocusChange(CFocusImplItem *item); // return: is Loss?
  int GetFocus(DURATION_HINT duration);

private:
  std::map<int, CFocusImplItem *> m_cookie_item;
  std::list<CFocusImplItem *> m_focus_items;

  unsigned int mFocus;     // 记录可分配的焦点数量
  bool mFocusTransitivity; // 记录焦点传递特性
  OnAudioFocusCallback *m_callback;
};

#endif /* AudioFocusManager_hpp */
