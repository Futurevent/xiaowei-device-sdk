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
#ifndef PlayList_hpp
#define PlayList_hpp

#include <vector>
#include <string>
#include "AudioApp.h"
#include "Player.h"
#include "Playlist.h"

struct txc_media_t;

// 显示在UI的播放列表，记录索引
class CPlaylist
{
public:
  typedef int UI_INDEX;   //  index for UI display
  typedef long SRC_INDEX; //  index in media source
  /**
        UI_INDEX表示的是需要显示在UI上的Index，SRC_INDEX表示的是内部存储所有真实资源列表的Index。
        CPlaylist中存储的只是索引，没有实际资源。外面可以根据ui_index找到真正的index，去取到资源。
     */

  CPlaylist();
  CPlaylist(REPEAT_MODE repeatMode, SESSION id);

  size_t Count(int list_type = 0);

  long PushBack(int list_type, const std::vector<txc_play_item_t> &list);
  long PushFront(int list_type, const std::vector<txc_play_item_t> &list);
  bool Remove(std::string resId);
  void print();
  void ResetRandomList();
  void Clear(int list_type = 0);

  void SetRepeat(REPEAT_MODE mode);

  REPEAT_MODE GetRepeat();

  const txc_play_item_t *GetItem(int list_type, UI_INDEX index);

  SRC_INDEX Seek(UI_INDEX index, int list_type = 0, bool cannot_be_restart = false); //  seek item in the ui list;

  SRC_INDEX NextX(long offset, bool isAuto); //  next item in the order/repeat list;
  SRC_INDEX NextY();                         //  next index in current item/group;

  UI_INDEX GetCurIndex();
  int GetCurPlayListType();
  void SetCurPlayListType(TXCA_PLAYLIST_TYPE type);
  void GetMorePlayListIfNeed();

private:
  int RandomIndexOf(UI_INDEX ui_index);
  int CreateRandomNum(int num);
  void AddRandomList(UI_INDEX index);

private:
  std::vector<txc_play_item_t> list_;
  std::vector<txc_play_item_t> list_history_; // 部分skill有历史列表，例如内置音乐SKill

  // 由于列表总是慢慢加载出来的，不适合使用先随机列表再顺序播放的逻辑，使用两个list来记录随机模式的状态。
  std::vector<UI_INDEX> random_list_;    // 随机的都记到这个list
  std::vector<UI_INDEX> un_random_list_; // 还未随机的都记到这个list

  TXCA_PLAYLIST_TYPE current_list_type_;
  REPEAT_MODE repeat_;
  SESSION id_;
  UI_INDEX x_;       //  current_group_index, in play order，内部列表的索引值，随机播放可能和x_in_ui_不同，其他模式是一样的
  UI_INDEX x_in_ui_; //  current_group_index, in ui order，UI界面的播放列表中的索引值
  UI_INDEX y_;       //  current_group_item_index，播放
};

#endif /* PlayList_hpp */
