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

#include "XWeiTransferMgr.h"
#include "TXCAudioMsg.h"

CXWeiTransferMgr g_xwei_transfer_mgr;

std::map<unsigned int, CXWeiTransferMgr *> g_mapCookieToObject;

void txc_on_transfer_progress(unsigned long long transfer_cookie, unsigned long long transfer_progress, unsigned long long max_transfer_progress)
{
    printf("transfer_progress cookie:[%llu] progress:[%llu/%llu]\n", transfer_cookie, transfer_progress, max_transfer_progress);
}

void txc_on_transfer_complete(unsigned long long transfer_cookie, int err_code, TXCA_FILE_TRANSFER_INFO *tran_info)
{
    printf("transfer_complete cookie:[%llu] err_code:[%d]\n", transfer_cookie, err_code);

    g_xwei_transfer_mgr.OnDownloadFileComplete(transfer_cookie, err_code, tran_info);
}

void txc_on_file_in_come(unsigned long long transfer_cookie, const TXCA_CCMSG_INST_INFO *inst_info, const TXCA_FILE_TRANSFER_INFO *tran_info)
{
    printf("file_in_come cookie:[%llu]\n", transfer_cookie);
}

int txca_on_auto_download_callback(unsigned long long file_size, unsigned int channel_type)
{
    printf("txca_on_auto_download_callback size:%llu channel:%u\n", file_size, channel_type);
    return 0;
}

void on_xwei_send_msg_ret(const unsigned int cookie, int err_code)
{
    printf("send msg cookie:%u err_code:%d", cookie, err_code);
    g_xwei_transfer_mgr.OnSendMsgResult(cookie, err_code);
}

CXWeiTransferMgr::CXWeiTransferMgr() : m_timeDuration(0),
                                       m_bStartRecord(false)
{
}

CXWeiTransferMgr::~CXWeiTransferMgr()
{
}

void CXWeiTransferMgr::Init()
{
    //初始化通道SDK的文件传输模块
    TXCA_FILE_TRANSFER_NOTIFY transfer_notify = {0};
    transfer_notify.on_transfer_progress = txc_on_transfer_progress;
    transfer_notify.on_transfer_complete = txc_on_transfer_complete;
    transfer_notify.on_file_in_come = txc_on_file_in_come;
    txca_init_file_transfer(transfer_notify, "./");

    txca_set_auto_download_callbak(txca_on_auto_download_callback);
}

void CXWeiTransferMgr::ProcessDownloadMsgFile(txc_download_msg_data_t *data, FileTransferListener *listener)
{
    printf("tinyId:%llu\n", data->tinyId);

    unsigned long long cookie = 0;
    int ret = txca_download_file((int)data->channel, (int)data->type, (char *)data->key, data->key_length, data->mini_token, &cookie);
    if (!ret)
    {
        MsgFileTransferInfo info = {0};
        info.targetId = data->tinyId;
        info.channelType = data->channel;
        info.fileType = data->type;
        info.strFileKey = data->key;
        info.strFileKey2 = data->mini_token;
        info.duration = data->duration;
        info.cookie = cookie;
        info.timestamp = data->timestamp;
        info.listener = listener;
        m_vecFileTransfer.push_back(info);
    }
}

void CXWeiTransferMgr::OnDownloadFileComplete(unsigned long long transfer_cookie, int err_code, TXCA_FILE_TRANSFER_INFO *tran_info)
{
    if (NULL == tran_info)
    {
        printf("OnDownloadFileComplete cookie:%llu tran_info empty.\n", transfer_cookie);
        return;
    }

    MsgFileTransferInfo info = {0};
    if (!GetFileTransferInfo(transfer_cookie, &info))
    {
        return;
    }

    txc_msg_info msginfo = {0};
    msginfo.tinyId = info.targetId;
    msginfo.type = info.fileType;
    msginfo.duration = info.duration;
    msginfo.content = tran_info->file_path;
    msginfo.timestamp = info.timestamp;
    msginfo.isRecv = true;

    if (info.listener != NULL)
    {
        info.listener->onDownloadFileResult(err_code, &msginfo);
    }
}

void CXWeiTransferMgr::OnSendMsgResult(unsigned int cookie, int err_code)
{
    std::map<unsigned int, FileTransferListener *>::iterator iter = m_mapSendMsg.find(cookie);
    if (iter != m_mapSendMsg.end())
    {
        iter->second->onSendMsgResult(err_code);
        m_mapSendMsg.erase(iter);
    }
}

bool CXWeiTransferMgr::GetFileTransferInfo(unsigned long long transfer_cookie, MsgFileTransferInfo *info)
{
    bool bExist = false;
    std::vector<MsgFileTransferInfo>::iterator it = m_vecFileTransfer.begin();
    for (; it != m_vecFileTransfer.end(); ++it)
    {
        if (it->cookie == transfer_cookie)
        {
            bExist = true;
            *info = *it;
            m_vecFileTransfer.erase(it);
            break;
        }
    }
    return bExist;
}

// 录音开始或结束
void CXWeiTransferMgr::ProcessAudioMsgRecord(bool isStart)
{
    if (isStart)
    {
        ResetVoiceData();
        m_timeDuration = time(NULL);
        m_bStartRecord = true;
    }
    else
    {
        time_t t = time(NULL);
        m_timeDuration = t - m_timeDuration;
        m_bStartRecord = false;
    }
}

// 处理消息发送
void CXWeiTransferMgr::ProcessAudioMsgSend(unsigned long long tinyId, FileTransferListener *listener)
{
    time_t t = time(NULL);
    std::string strPath = "./";
    char szFileName[100] = {0};
    snprintf(szFileName, 100, "qqsend_%ld.amr", (long)t);
    std::string strFile = strPath;
    if (strFile.at(strFile.length() - 1) != '/')
    {
        strFile.append("/");
    }
    strFile.append(szFileName);

    std::string strVoiceData = GetVoiceData();
    if (EncodeVoiceDataToAmr(strVoiceData, strFile))
    {
        unsigned int cookie = 0;
        SendMsg(tinyId, strFile, (unsigned int)m_timeDuration, &cookie);
        m_mapSendMsg[cookie] = listener;
    }

    ResetVoiceData();
    m_timeDuration = 0;
}

bool CXWeiTransferMgr::EncodeVoiceDataToAmr(const std::string &strVoiceData, std::string &strFile)
{
    // 依赖开源库：https://sourceforge.net/projects/opencore-amr/，请自行实现编码，并保存到本地
    return true;
}

void CXWeiTransferMgr::SendMsg(unsigned long long tinyId, const std::string &strFile, unsigned int duration, unsigned int *cookie)
{
    TXCA_SEND_MSG_NOTIFY dp_notifyX = {0};
    dp_notifyX.on_send_structuring_msg_ret = on_xwei_send_msg_ret;

    STRUCTURING_MSG msg = {0};
    msg.msg_id = 3;
    msg.file_path = (char *)strFile.c_str();
    msg.duration = duration;
    msg.to_targetids = (unsigned long long *)malloc(sizeof(unsigned long long));
    msg.to_targetids[0] = tinyId;
    msg.to_targetids_count = 1;

    txca_send_structuring_msg(&msg, &dp_notifyX, cookie);

    free(msg.to_targetids);
    msg.to_targetids = NULL;
}

void CXWeiTransferMgr::AddVoiceData(const char *data, int length)
{
    if (!m_bStartRecord)
        return;

    if (data == NULL || length == 0)
    {
        return;
    }

    std::string strBuffer;
    strBuffer.assign(data, length);
    m_strVoiceData.append(strBuffer);
}

void CXWeiTransferMgr::ResetVoiceData()
{
    m_strVoiceData.clear();
}

std::string CXWeiTransferMgr::GetVoiceData()
{
    return m_strVoiceData;
}
