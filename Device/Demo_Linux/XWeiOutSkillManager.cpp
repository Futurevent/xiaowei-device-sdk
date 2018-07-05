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

#include "XWeiOutSkillManager.h"
#include "XWeiPlayer.h"
#include "XWeiDevice.h"
#include "TXCSkillsDefine.h"
#include "TXSDKCommonDef.h"
#include "TXCAudioCommon.h"
#include "TXCAudioType.h"
#include "document.h"
#include "writer.h"
#include "stringbuffer.h"
#include <cstdlib>
#include <sys/time.h>
#include <algorithm>
#include <sstream>

// 焦点改变回调接口
void on_audio_focus_change(int cookie, int focus_change)
{
    XWeiOutSkillManager::instance()->onAudioFocusChange(cookie, focus_change);
}

XWeiOutSkillManager::XWeiOutSkillManager()
{
    // 注册焦点回调接口
    txc_set_audio_focus_change_callback(on_audio_focus_change);
}

XWeiOutSkillManager::~XWeiOutSkillManager()
{
    for (auto iter = handlers.begin(); iter != handlers.end();)
    {
        OutSkillHandler *handler = iter->second;
        handlers.erase(iter++);
        delete handler;
    }
}

void XWeiOutSkillManager::registerSkillId(const std::string &skillId, OutSkillHandler *handler)
{
    handlers[skillId] = handler;
}

void XWeiOutSkillManager::registerSkillName(const std::string &skillName, OutSkillHandler *handler)
{
    handlers[skillName] = handler;
}

bool XWeiOutSkillManager::startOutSkill(int sessionId, const std::string &skillName, const std::string &skillId)
{
    // 优先使用 SkillId 去匹配
    return (handlers.find(skillId) != handlers.end() || handlers.find(skillName) != handlers.end());
}

bool XWeiOutSkillManager::handleSkillResponse(int sessionId, TXCA_PARAM_RESPONSE *cRsp)
{
    if (cRsp == NULL)
    {
        return false;
    }

    if (cRsp->skill_info.id == NULL && cRsp->skill_info.name == NULL)
    {
        return false;
    }

    std::map<std::string, OutSkillHandler *>::iterator iter = handlers.find(cRsp->skill_info.id);
    if (iter != handlers.end())
    {
        return iter->second->handleResponse(sessionId, cRsp);
    }

    iter = handlers.find(cRsp->skill_info.name);
    if (iter != handlers.end())
    {
        return iter->second->handleResponse(sessionId, cRsp);
    }

    return false;
}

bool XWeiOutSkillManager::onMessage(int session_id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2)
{
    bool handled = false;

    for (std::map<std::string, OutSkillHandler *>::iterator iter = handlers.begin();
         iter != handlers.end(); iter++)
    {
        if (iter->second->onMessage(session_id, event, arg1, arg2))
        {
            break;
        }
    }

    return handled;
}

void XWeiOutSkillManager::onAudioFocusChange(int cookie, int focus_change)
{
    std::map<int, OutSkillAudioFocusListener *>::const_iterator iter = listeners.find(cookie);
    if (iter != listeners.end())
    {
        iter->second->onAudioFocucChange(focus_change);
    }
}

void XWeiOutSkillManager::requestAudioFocus(OutSkillAudioFocusListener *listener, DURATION_HINT duration)
{
    int cookie = -1;
    txc_request_audio_focus(cookie, duration);
    if (cookie >= 0)
    {
        listeners[cookie] = listener;
    }
}

void XWeiOutSkillManager::abandonAudioFocus(OutSkillAudioFocusListener *listener)
{
    int cookie = -1;
    for (std::map<int, OutSkillAudioFocusListener *>::iterator iter = listeners.begin(); iter != listeners.end();)
    {
        if (iter->second == listener)
        {
            cookie = iter->first;
            listeners.erase(iter);
            break;
        }
    }

    if (cookie >= 0)
    {
        txc_abandon_audio_focus(cookie);
    }
}

// 收到消息的音效和消息发送成功的音效都是这个铃声
const std::string QQMsgSkillHandler::MSG_RING = "http://qzonestyle.gtimg.cn/qzone/vas/opensns/res/doc/msg.ring.mp3";

TipPlayer::TipPlayer()
{
}

TipPlayer::~TipPlayer()
{
}

void TipPlayer::playTipRes(std::string rhs)
{
    // 播放完毕后，需要释放声音焦点
    XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
}

void TipPlayer::onAudioFocucChange(int focusChange)
{
    if (focusChange == AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    {
        // 实现提示音的播放
    }
    else if (focusChange == AUDIOFOCUS_LOSS)
    {
        // 停止提示音的播放
    }
}

QQMsgMedia::QQMsgMedia()
{
}

QQMsgMedia::QQMsgMedia(std::string content, MEDIA_TYPE type, bool isMsg, std::string id)
{
    this->content = content;
    this->type = type;
    this->isMsg = isMsg;
    this->id = id;
}

QQMsgMedia::~QQMsgMedia()
{
}

QQMsgSkillHandler::QQMsgSkillHandler() : m_sessionId(-1),
                                         m_curState(SKILL_STATE_IDLE),
                                         m_targetId(0)
{
    m_tipPlayer = new TipPlayer();
    pthread_mutex_init(&m_msgPlayListMutex, NULL);
}

QQMsgSkillHandler::~QQMsgSkillHandler()
{
    if (m_tipPlayer)
    {
        delete m_tipPlayer;
        m_tipPlayer = NULL;
    }
    pthread_mutex_destroy(&m_msgPlayListMutex);
}

void QQMsgSkillHandler::onDownloadFileResult(int err_code, const txc_msg_info *msg_info)
{
    if (err_code != err_null || msg_info == NULL)
    {
        printf("QQMsgSkillHandler::onDownloadFileResult err_code[%d] or msg_info is NULL", err_code);
        return;
    }

    CXWeiMsgAudio *audioMsg = new CXWeiMsgAudio();
    audioMsg->timestamp = msg_info->timestamp;
    audioMsg->duration = msg_info->duration;
    audioMsg->localUrl = msg_info->content;
    audioMsg->isRecv = msg_info->isRecv;
    audioMsg->uin_ = msg_info->tinyId;
    XWeiCMsgbox::instance().AddMsg(audioMsg);

    if (CXWeiApp::instance().AudioEngine().IsDeviceActive())
    {
        genMediaWithMsg(audioMsg);
        if (m_curState == SKILL_STATE_IDLE)
        {

            m_curState = SKILL_STATE_ONCE_PLAY;

            XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN);
        }
    }
    else
    {
        playTipRes(MSG_RING);
    }
}

void QQMsgSkillHandler::onSendMsgResult(int err_code)
{
    if (err_code != err_null)
    {
        printf("QQMsgSkillHandler::onSendMsgResult err_code[%d]", err_code);
        return;
    }

    playTipRes(MSG_RING);
}

bool QQMsgSkillHandler::OnRequest(TXCA_EVENT event)
{
    printf("QQMsgSkillHandler::OnRequest event[%d]", event);
    if (event == txca_event_on_request_start)
    {
        g_xwei_transfer_mgr.ProcessAudioMsgRecord(true);
    }
    else if (event == txca_event_on_silent)
    {
        g_xwei_transfer_mgr.ProcessAudioMsgRecord(false);
    }
    else if (event == txca_event_on_response)
    {
        g_xwei_transfer_mgr.ProcessAudioMsgSend(m_targetId, this);
    }

    return true;
}

void QQMsgSkillHandler::OnFeedAudioData(const char *data, int length)
{
    printf("QQMsgSkillHandler::OnFeedAudioData length[%d]", length);
    g_xwei_transfer_mgr.AddVoiceData(data, length);
}

bool QQMsgSkillHandler::handleResponse(int sessionId, TXCA_PARAM_RESPONSE *cRsp)
{
    if (m_sessionId == -1)
    {
        m_sessionId = sessionId;
    }
    bool handled = false;
    // 1. 接收消息
    handled = processRecvMsg(cRsp);

    // 2. 发送消息
    if (!handled)
    {
        handled = processSendMsg(cRsp);
    }
    // 3. 消息播放逻辑
    if (!handled)
    {
        handled = processPlayMsg(cRsp);
    }

    // 4. 异常场景处理
    if (!handled)
    {
        handled = processException(cRsp);
    }

    return handled;
}

bool QQMsgSkillHandler::onMessage(int session_id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2)
{
    // 判断session_id是不是给当前App处理的
    if (m_sessionId != session_id)
    {
        return false;
    }

    if (event == XWM_PLAYER_STATUS_CHANGED)
    {
        TXC_PLAYER_STATE state = TXC_PLAYER_STATE(reinterpret_cast<long>(arg1));
        printf("QQMsgSkillHandler::onMessage playstate[%d]", state);
        if (state == TXC_PLAYER_STATE_COMPLETE)
        {
            playNextMedia();
            if (m_curMedia.isMsg)
            {
                // 需要更新消息为已读
                int msgId = atoi(m_curMedia.id.c_str());
                printf("QQMsgSkillHandler::onMessage msgId[%d]", msgId);
                XWeiCMsgbox::instance().SetMsgReaded((unsigned int)msgId);
            }
        }
        else if (state == TXC_PLAYER_STATE_STOP)
        {
            clearMsgPlayList();
        }
    }

    return true;
}

void QQMsgSkillHandler::onAudioFocucChange(int focusChange)
{
    printf("QQMsgSkillHandler::onAudioFocucChange [%d]", focusChange);
    if (focusChange == AUDIOFOCUS_GAIN)
    {
        playNextMedia();
    }
    else if (focusChange == AUDIOFOCUS_LOSS || focusChange == AUDIOFOCUS_LOSS_TRANSIENT)
    {
        g_xwei_player_mgr.OnCallback(m_sessionId, ACT_PLAYER_STOP, 0, 0);
    }
}

bool QQMsgSkillHandler::processRecvMsg(TXCA_PARAM_RESPONSE *cRsp)
{
    bool handled = false;
    if (cRsp->resource_groups_size >= 1 && cRsp->resource_groups[0].resources_size >= 1)
    {
        const TXCA_PARAM_RESOURCE *resource = cRsp->resource_groups[0].resources;
        if (resource->format == txca_resource_command && resource->id && resource->id[0])
        {
            int cmdId = std::atoi(resource->id);
            if (cmdId == PROPERTY_ID_IOT_TEXT)
            { // 文本消息
                handled = processTextMsg(resource->content, resource->extend_buffer);
            }
            else if (cmdId == PROPERTY_ID_IOT_AUDIO)
            { //语音消息
                handled = processAudioMsg(resource->content, resource->extend_buffer);
            }
        }
    }

    return handled;
}

bool QQMsgSkillHandler::processTextMsg(const char *content, const char *extendBuf)
{
    if (content == NULL || extendBuf == NULL)
    {
        return false;
    }

    // 解析发送者的id信息
    unsigned long long sender = 0;
    rapidjson::Document json_ext;
    json_ext.Parse(extendBuf);
    if (!json_ext.HasParseError())
    {
        assert(json_ext.IsObject());
        if (json_ext.HasMember("sender"))
        {
            sender = json_ext["sender"].GetUint64();
        }
    }

    // 解析消息内容
    int timestamp = (int)time(NULL);
    ;
    std::string strText;
    rapidjson::Document json_content;
    json_content.Parse(content);
    if (!json_content.HasParseError())
    {
        assert(json_content.IsObject());
        if (json_content.HasMember("text"))
        {
            strText = json_content["text"].GetString();
        }
    }

    if (sender == 0 || strText.empty())
    {
        return false;
    }

    CXWeiMsgText *pMsgText = new CXWeiMsgText;
    pMsgText->uin_ = sender;
    pMsgText->timestamp = timestamp;
    pMsgText->text = strText;
    XWeiCMsgbox::instance().AddMsg(pMsgText);

    if (CXWeiApp::instance().AudioEngine().IsDeviceActive())
    {
        genMediaWithMsg(pMsgText);
        if (m_curState == SKILL_STATE_IDLE)
        {

            m_curState = SKILL_STATE_ONCE_PLAY;

            XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN);
        }
    }
    else
    {
        playTipRes(MSG_RING);
    }

    return true;
}

bool QQMsgSkillHandler::processAudioMsg(const char *content, const char *extendBuf)
{
    if (content == NULL || extendBuf == NULL)
    {
        return false;
    }

    // 解析一下消息的发送者id信息
    unsigned long long sender = 0;
    rapidjson::Document json_ext;
    json_ext.Parse(extendBuf);
    if (!json_ext.HasParseError())
    {
        assert(json_ext.IsObject());
        if (json_ext.HasMember("sender"))
        {
            sender = json_ext["sender"].GetUint64();
        }
    }

    // 解析消息内容信息
    std::string strMediaKey;
    std::string strFileKey;
    std::string strFileKey2;

    int timestamp = (int)time(NULL);
    int nDuration = 0;
    rapidjson::Document json_doc;
    json_doc.Parse(content);

    if (!json_doc.HasParseError())
    {
        assert(json_doc.IsObject());
        if (json_doc.HasMember("media_key"))
        {
            strMediaKey = json_doc["media_key"].GetString();
        }
        if (json_doc.HasMember("file_key"))
        {
            strFileKey = json_doc["file_key"].GetString();
        }
        if (json_doc.HasMember("fkey2"))
        {
            strFileKey2 = json_doc["fkey2"].GetString();
        }
        if (json_doc.HasMember("duration"))
        {
            nDuration = json_doc["duration"].GetInt();
        }
    }

    char szFileKey[200] = {0};
    if (!strMediaKey.empty())
    {
        memcpy(szFileKey, strMediaKey.c_str(), strMediaKey.size());
    }
    if (!strFileKey.empty())
    {
        memcpy(szFileKey, strFileKey.c_str(), strFileKey.size());
    }
    if (0 == strlen(szFileKey))
    {
        return false;
    }

    txc_download_msg_data_t download_data;
    download_data.tinyId = sender;
    download_data.channel = transfer_channeltype_MINI;
    download_data.type = transfer_filetype_audio;
    download_data.key = szFileKey;
    download_data.key_length = (unsigned int)strlen(szFileKey);
    download_data.mini_token = strFileKey2.c_str();
    download_data.min_token_length = (unsigned int)strFileKey2.length();
    download_data.duration = nDuration;
    download_data.timestamp = timestamp;
    g_xwei_transfer_mgr.ProcessDownloadMsgFile(&download_data, this);

    return true;
}

bool QQMsgSkillHandler::processSendMsg(TXCA_PARAM_RESPONSE *cRsp)
{
    bool handled = false;
    if (cRsp->resource_groups_size >= 1 && cRsp->resource_groups[0].resources_size >= 1)
    {
        const TXCA_PARAM_RESOURCE *resource = cRsp->resource_groups[0].resources;
        if (resource->format == txca_resource_command && resource->id && resource->id[0])
        {
            int cmdId = std::atoi(resource->id);
            if (cmdId == PROPERTY_ID_SEND_IOT_AUDIO_MSG)
            { // 发送语音消息
                m_curState = SKILL_STATE_SEND_MSG;
                if (resource->content)
                {
                    m_targetId = strtoull(resource->content, NULL, 0);
                }

                handled = true;
            }
        }
    }

    if (handled)
    {
        clearMsgPlayList();
        for (int i = 0; i < cRsp->resource_groups_size; i++)
        {
            for (int j = 0; j < cRsp->resource_groups[i].resources_size; j++)
            {
                const TXCA_PARAM_RESOURCE *resource = cRsp->resource_groups[i].resources + j;
                if (resource->format == txca_resource_tts || resource->format == txca_resource_url)
                {
                    QQMsgMedia media;
                    media.type = resource->format == txca_resource_tts ? TYPE_TTS_OPUS : TYPE_MUSIC_URL;
                    media.id = resource->id;
                    media.content = resource->content;

                    pthread_mutex_lock(&m_msgPlayListMutex);
                    m_msgPlayList.push(media);
                    pthread_mutex_unlock(&m_msgPlayListMutex);
                }
            }
        }

        XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN);
    }

    return handled;
}

bool QQMsgSkillHandler::processPlayMsg(TXCA_PARAM_RESPONSE *cRsp)
{
    bool handled = false;
    if (cRsp->resource_groups_size >= 1 && cRsp->resource_groups[0].resources_size >= 1)
    {
        const TXCA_PARAM_RESOURCE *resource = cRsp->resource_groups[0].resources;
        if (resource->format == txca_resource_command && resource->id && resource->id[0])
        {
            int cmdId = std::atoi(resource->id);
            if (cmdId == PROPERTY_ID_START)
            { // 播放未读消息
                m_curState = SKILL_STATE_LOOP_PLAY;
                handled = true;
                XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN);
            }
        }
    }

    return handled;
}

bool QQMsgSkillHandler::processException(TXCA_PARAM_RESPONSE *cRsp)
{
    if (cRsp->resource_groups_size >= 1 && cRsp->resource_groups[0].resources_size >= 1)
    {
        const TXCA_PARAM_RESOURCE *resource = cRsp->resource_groups[0].resources;
        if (resource->format == txca_resource_tts && resource->id && resource->id[0])
        {
            QQMsgMedia media;
            media.type = TYPE_TTS_OPUS;
            media.id = resource->id;
            media.content = resource->content;

            pthread_mutex_lock(&m_msgPlayListMutex);
            m_msgPlayList.push(media);
            pthread_mutex_unlock(&m_msgPlayListMutex);

            XWeiOutSkillManager::instance()->requestAudioFocus(this, AUDIOFOCUS_GAIN);
        }
    }

    return false;
}

void QQMsgSkillHandler::playTipRes(std::string url)
{
    if (m_tipPlayer)
    {
        m_tipPlayer->playTipRes(url);
    }
}

void QQMsgSkillHandler::clearMsgPlayList()
{
    pthread_mutex_lock(&m_msgPlayListMutex);
    std::queue<QQMsgMedia> empty;
    std::swap(m_msgPlayList, empty);
    pthread_mutex_unlock(&m_msgPlayListMutex);
}

void QQMsgSkillHandler::genMediaWithMsg(XWeiCMsgBase *msg)
{
    char voiceId[33] = {0};
    txca_request_protocol_tts(voiceId, msg->uin_, msg->timestamp, txca_protocol_cmd_msg);

    QQMsgMedia tipMedia;
    tipMedia.id = voiceId;
    tipMedia.isMsg = false;
    tipMedia.type = TYPE_TTS_OPUS;

    std::stringstream msgIdStr;
    msgIdStr << msg->msgId;

    QQMsgMedia msgMedia;
    msgMedia.isMsg = true;
    msgMedia.id = msgIdStr.str();
    if (msg->msgType == qq_msg_type_iot_audio)
    {
        CXWeiMsgAudio *tmp = dynamic_cast<CXWeiMsgAudio *>(msg);
        msgMedia.type = TYPE_MUSIC_URL;
        msgMedia.content = tmp->localUrl;
    }
    else if (msg->msgType == qq_msg_type_iot_text)
    {
        CXWeiMsgText *tmp = dynamic_cast<CXWeiMsgText *>(msg);
        msgMedia.type = TYPE_TTS_TEXT;
        msgMedia.content = tmp->text;
    }

    pthread_mutex_lock(&m_msgPlayListMutex);
    m_msgPlayList.push(tipMedia);
    m_msgPlayList.push(msgMedia);
    pthread_mutex_unlock(&m_msgPlayListMutex);
}

void QQMsgSkillHandler::playNextMedia()
{
    bool isNeedContinue = false;
    if (m_msgPlayList.empty())
    {
        if (m_curState == SKILL_STATE_LOOP_PLAY)
        {
            XWeiCMsgBase *msg = XWeiCMsgbox::instance().GetNextUnReadMsg();
            if (msg != NULL)
            {
                genMediaWithMsg(msg);
                isNeedContinue = true;
            }
            else
            {
                // end tip for user
                m_curState = SKILL_STATE_IDLE;
                QQMsgMedia tipMedia;
                tipMedia.id = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                tipMedia.isMsg = false;
                tipMedia.content = "没有更多消息了";
                tipMedia.type = TYPE_TTS_TEXT;
                pthread_mutex_lock(&m_msgPlayListMutex);
                m_msgPlayList.push(tipMedia);
                pthread_mutex_unlock(&m_msgPlayListMutex);
            }
        }
        else if (m_curState == SKILL_STATE_ONCE_PLAY)
        {
            m_curState = SKILL_STATE_IDLE;
        }
        else if (m_curState == SKILL_STATE_SEND_MSG)
        {
            m_curState = SKILL_STATE_IDLE;
            // 发起VAD请求
            TXCA_PARAM_CONTEXT context = {0};
            context.request_param |= txca_param_only_vad;
            context.speak_timeout = 5000;
            context.silent_timeout = 2000;
            CXWeiApp::instance().AudioEngine().OnWakeUp(&context, this);
        }
    }
    else
    {
        isNeedContinue = true;
    }

    if (!isNeedContinue)
    {
        XWeiOutSkillManager::instance()->abandonAudioFocus(this);
        return;
    }

    pthread_mutex_lock(&m_msgPlayListMutex);
    m_curMedia = m_msgPlayList.front();
    m_msgPlayList.pop();
    pthread_mutex_unlock(&m_msgPlayListMutex);

    txc_media_t media = {0};
    media.content = m_curMedia.content.c_str();
    media.res_id = m_curMedia.id.c_str();
    media.type = m_curMedia.type;
    g_xwei_player_mgr.OnCallback(m_sessionId, ACT_MUSIC_PUSH_MEDIA, XWPARAM(&media), XWPARAM(false));
}
