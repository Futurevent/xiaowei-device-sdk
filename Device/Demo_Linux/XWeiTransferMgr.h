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

#ifndef XWeiTransferMgr_hpp
#define XWeiTransferMgr_hpp

#include <stdio.h>
#include <map>
#include <string>
#include <vector>
#include "AudioApp.h"
#include "Player.h"
#include "Media.h"
#include "TXCAudioFileTransfer.h"

// 下载结构定义
struct txc_download_msg_data_t
{
    unsigned long long tinyId;
    unsigned int channel;
    unsigned int type;
    const char *key;
    unsigned int key_length;
    const char *mini_token;
    unsigned int min_token_length;
    unsigned int duration;
    int timestamp;
};

// 消息结构体定义
struct txc_msg_info
{
    unsigned long long tinyId; // 消息来源id
    int type;                  // 类型
    const char *content;       // 内容
    int duration;              // 时长
    int timestamp;             // 时间戳
    bool isRecv;               // 是否接收
};

// 文件/消息发送回调接口
class FileTransferListener
{
  public:
    virtual ~FileTransferListener(){};

  public:
    virtual void onDownloadFileResult(int err_code, const txc_msg_info *msg_info) = 0;
    virtual void onSendMsgResult(int err_code) = 0;
};

// 文件下载信息结构体定义
struct MsgFileTransferInfo
{
    unsigned long long cookie;
    unsigned long long targetId;
    int channelType;
    int fileType;
    unsigned int propId;
    std::string strFileKey;
    std::string strFileKey2;
    int duration;
    int timestamp;

    FileTransferListener *listener;
};

// 文件下载、消息发送管理
class CXWeiTransferMgr
{
  public:
    CXWeiTransferMgr();
    ~CXWeiTransferMgr();

    void Init();

    void ProcessDownloadMsgFile(txc_download_msg_data_t *data, FileTransferListener *listener);

    void OnDownloadFileComplete(unsigned long long transfer_cookie, int err_code, TXCA_FILE_TRANSFER_INFO *tran_info);
    void OnSendMsgResult(unsigned int cookie, int err_code);

    void AddVoiceData(const char *data, int length);

    void ProcessAudioMsgRecord(bool isStart);
    void ProcessAudioMsgSend(unsigned long long tinyId, FileTransferListener *listener);

    void ProcessWechatAudioMsgSend(std::string toUser);
  private:
    void ResetVoiceData();
    std::string GetVoiceData();
    bool GetFileTransferInfo(unsigned long long transfer_cookie, MsgFileTransferInfo *info);
    bool EncodeVoiceDataToAmr(const std::string &strVoiceData, std::string &strFile);
    void SendMsg(unsigned long long tinyId, const std::string &strFile, unsigned int duration, unsigned int *cookie);
    void UploadFile(const std::string &strFile, unsigned long long *cookie);
  private:
    std::vector<MsgFileTransferInfo> m_vecFileTransfer;
    std::map<unsigned int, FileTransferListener *> m_mapSendMsg;
    std::map<unsigned long long, std::string> m_mapSendWechatMsg;

    time_t m_timeDuration;      // 录音时长
    std::string m_strVoiceData; // 语音数据
    bool m_bStartRecord;        // 开始录音
};

extern CXWeiTransferMgr g_xwei_transfer_mgr;

#endif /* XWeiTransferMgr_hpp */
