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
#ifndef SkillControl_hpp
#define SkillControl_hpp

#include "AudioApp.hpp"
#include "ResponseHolder.hpp"
#include "Medialist.hpp"

class CPlaylist;

// 控制Skill 的播放、音量、循环模式、列表、播放完毕自动唤醒、UI展示等
class CSkillControl
{
public:
  CSkillControl(SESSION id, REPEAT_MODE repeatMode);
  ~CSkillControl();

  bool OnMessage(XWM_EVENT event, XWPARAM arg1, XWPARAM arg2);

  // interface of ui data
  long PlayListPushBack(const std::vector<txc_play_item_t> &list, int list_type = 0);
  long PlayListPushFront(const std::vector<txc_play_item_t> &list, int list_type = 0);
  size_t PlayListCount(int list_type = 0);

  // interface of media data
  int AddNotifyItem(_In_ PtrMedia &media);
  int AddMediaItem(_In_ PtrMedia &media);
  int UpdateMediaItem(const TXCA_PARAM_RESOURCE *item, bool only_quality);
  int AddResponseData(int response_type, const char *response_data, _Out_ PtrMedia &media);
  void SetMedialist(PtrMediaList &playList);
  PtrMediaList &GetMediaList();
  size_t MediaListCount();
  void ClearMediaList(int list_type = -1);

  PtrMedia Remove(std::string resId);
  void Release();
  bool ClearList(int list_type = -1); // 有屏设备移除播放列表元素，传入TXCA_PLAYLIST_TYPE可以指定列表

  // interface of Player
  PLAYER_STATUS GetStatus();
  bool Play(bool isAuto = false); // 开始播放，如果停止状态，播放第0个；如果暂停状态，恢复播放。
  void ReportPlayState(TXCA_PLAYSTATE play_state = txca_playstate_idle);
  int GetCurPlayListType();
  void SetCurPlayListType(TXCA_PLAYLIST_TYPE type);
private:
  bool Play(const char *res_id);
  bool Play(long index, int list_type = 0, bool cannot_be_restart = true);
  bool Stop();
  bool Next(long skip, bool isAuto = false);
  bool Pause(bool pause, bool isAuto);
  bool SetRepeat(REPEAT_MODE repeatMode);

  bool AddAlbum(long begin_index);                                                   // 有屏设备显示UI
  bool AddList(TXCA_PLAYLIST_TYPE resource_list_type, long begin_index, long count); // 有屏设备添加播放列表元素
  bool OnMediaUpdated(const char *res_id);                                           // 媒体资源更新了，未实现，暂时保留
  bool OnPlayerStatusChanged(TXC_PLAYER_STATE state_code);                           // 外部播放器播放状态改变
  bool OnSupplementRequest(XWPARAM arg1, XWPARAM arg2);                              // 需要自动唤醒

  bool SetStatus(PLAYER_STATUS status);

  bool PlayMediaIndex(long src_index, bool isAuto = false);
  bool PlayMedia(const txc_media_t *media);

  std::string GetFirstPlayId(TXCA_PLAYLIST_TYPE list_type);
  std::string GetLastPlayId(TXCA_PLAYLIST_TYPE list_type);

private:
  SESSION id_;

  std::string play_res_id_;

  struct DelayEvent
  {
    XWM_EVENT event;
    XWPARAM arg1;
    XWPARAM arg2;
    CResponseHolder resp;
  };
  std::vector<DelayEvent> delay_events_;

  txc_xwei_control control_callback_;
  bool auto_resume_able;

  TXCAutoPtr<TXCPlayer> player_;    // 虚拟的播放器
  TXCAutoPtr<CPlaylist> play_list_; // UI上显示的列表
  PtrMediaList media_list_;         // 媒体资源列表
};

//  players manager
class TXCPlayerManager
{
public:
  PtrPlayer NewPlayer(int app_id);
  PtrPlayer NewPlayer(int id, REPEAT_MODE repeatMode);
  PtrPlayer GetPlayer(int app_id);

private:
  friend class TXCServices;
  TXCPlayerManager();

private:
  //    ID => player
  std::map<int, PtrPlayer> vPlayers_;
};

#endif /* SkillControl_hpp */
