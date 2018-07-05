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
#ifndef _AIAUDIO_PLAYER_HPP_
#define _AIAUDIO_PLAYER_HPP_

#include <string>
#include <vector>
#include <map>
#include "Medialist.hpp"
#include "Player.h"
#include "AudioApp.h"

//////////////////// Player.hpp ////////////////////

class TXCPlayerManager;

//  virtual player, call user specified callback to play media data
class TXCPlayer
{
public:
  //    action
  void Stop();
  void Pause();
  void Resume();
  void PlayMedia(const txc_media_t *media, bool withReleaseRes = false);
  int GetCurrentPosition();
  int GetDuration();
  void SeekTo(unsigned long long offset);
  bool IsPlaying();

  void SetStatus(PLAYER_STATUS st);
  PLAYER_STATUS GetStatus() const;

  void SetRepeatMode(REPEAT_MODE mode);
  REPEAT_MODE GetRepeatMode() const;

  void SetVolume(int volume);
  int GetVolume() const;

  txc_player_info_t *GetInfo();

private:
  friend class TXCPlayerManager;
  TXCPlayer(int id);
  TXCPlayer(int id, REPEAT_MODE repeatMode);

private:
  XWPARAM TriggerEvent(TXC_PLAYER_ACTION event, XWPARAM arg1, XWPARAM arg2);

private:
  txc_player_info_t info_;
  int app_id_;
  txc_xwei_control control_callback_;
};
typedef TXCAutoPtr<TXCPlayer> PtrPlayer;

#endif /* _AIAUDIO_PLAYER_HPP_ */
