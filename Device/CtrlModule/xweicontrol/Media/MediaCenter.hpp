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
#ifndef _MEDIA_CENTER_HPP_
#define _MEDIA_CENTER_HPP_

#include <string>
#include <vector>
#include <map>
#include "Medialist.hpp"
#include "AudioApp.h"

//  media manager, store media data, not media info
class TXCMediaCenter
{
public:
  int AddMediaList(SESSION id, PtrMediaList &playList);
  PtrMediaList GetMediaList(SESSION id);
  size_t AddMedia(PtrMedia &media);
  bool RemoveMedia(const std::string &res_id);
  bool DecMediaTipCnt(const std::string &res_id);
  bool IsMediaNeedPlay(const std::string &res_id);
  std::string GenResourceId();

  template <class T>
  PtrMedia NewMedia(const std::string &res_id);

  PtrMedia GetMedia(const std::string &res_id);
  int ReadMedia(_In_ const char *res_id, _Out_ const void **data, _Out_ size_t *data_size, _In_ size_t offset);

  int TriggerMediaUpdated(const PtrMedia &media);

  void SetLastActiveTime();
  long GetLastActiveTime();
  void AddVoiceData(const char *data, int length);
  void ResetVoiceData();
  std::string GetVoiceData();

private:
  friend class TXCServices;
  TXCMediaCenter();

private:
  std::map<std::string, PtrMedia> map_media_;
  std::map<SESSION, PtrMediaList> map_playlist_;

  std::string m_strVoiceData;
  time_t m_lastActiveTime;
};

template <class T>
PtrMedia TXCMediaCenter::NewMedia(const std::string &res_id)
{
  PtrMedia media;
  T *ptr_media = new T(res_id);
  if (ptr_media)
  {
    media.reset(dynamic_cast<CMedia *>(ptr_media));

    map_media_[res_id] = media;
  }

  return media;
}

#endif /* _MEDIA_CENTER_HPP_ */
