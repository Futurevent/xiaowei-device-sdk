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

#ifndef XWeiOutSkillManager_hpp
#define XWeiOutSkillManager_hpp

#include <map>
#include <string>
#include <queue>
#include <pthread.h>
#include "TXCAudioType.h"
#include "txctypedef.h"
#include "AudioApp.h"
#include "AudioFocus.h"
#include "Media.h"
#include "XWeiTransferMgr.h"
#include "XWeiMsgbox.h"
#include "XWeiAudioEngine.h"

// 外部声音焦点回调变化监听器
class OutSkillAudioFocusListener
{
  public:
    virtual ~OutSkillAudioFocusListener(){};

  public:
    virtual void onAudioFocucChange(int focusChange) = 0;
};

// 本地Skill处理器接口定义
class OutSkillHandler
{
  public:
    virtual ~OutSkillHandler(){};

  public:
    // 处理本地Skill响应数据
    virtual bool handleResponse(int sessionId, TXCA_PARAM_RESPONSE *cRsp) = 0;
    // 处理控制层分发出来的消息
    virtual bool onMessage(int session_id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2) = 0;
};

// 实现一个简单播放提示音的功能
class TipPlayer : public OutSkillAudioFocusListener
{
  public:
    TipPlayer();
    ~TipPlayer();

  public:
    virtual void onAudioFocucChange(int focusChange);
    void playTipRes(std::string rhs);

  private:
    std::string tipRes;
};

// QQ消息Skill中的媒体定义
class QQMsgMedia
{

  public:
    QQMsgMedia();
    QQMsgMedia(std::string content, MEDIA_TYPE type, bool isMsg, std::string id);
    ~QQMsgMedia();

  public:
    std::string content;
    MEDIA_TYPE type;
    bool isMsg; // 是否是消息本身
    std::string id;
};

// QQ消息Skill处理器
class QQMsgSkillHandler : public OutSkillHandler, public FileTransferListener, public OutSkillAudioFocusListener, public CXWeiRequestListener
{
    enum QQMsgSkillState
    {
        SKILL_STATE_IDLE = 0,
        SKILL_STATE_LOOP_PLAY = 1,
        SKILL_STATE_ONCE_PLAY = 2,
        SKILL_STATE_SEND_MSG = 3
    };

  public:
    QQMsgSkillHandler();
    ~QQMsgSkillHandler();

  public:
    // 实现 FileTransferListener
    // 收到语音消息，需要先下载语音文件到本地，本接口为下载结果回调接口
    virtual void onDownloadFileResult(int err_code, const txc_msg_info *msg_info);
    // 消息发送结果回调接口
    virtual void onSendMsgResult(int err_code);

  public:
    // 实现OutSkillAudioFocusListener
    // 声音焦点回调回调处理
    virtual void onAudioFocucChange(int focusChange);

  public:
    // 实现OutSkillHandler
    virtual bool handleResponse(int sessionId, TXCA_PARAM_RESPONSE *cRsp);
    virtual bool onMessage(int session_id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2);

  public:
    // 实现CXWeiRequestListener
    virtual bool OnRequest(TXCA_EVENT event);
    virtual void OnFeedAudioData(const char *data, int length);

  private:
    bool processRecvMsg(TXCA_PARAM_RESPONSE *cRsp);
    bool processTextMsg(const char *content, const char *extendBuf);
    bool processAudioMsg(const char *content, const char *extendBuf);
    bool processSendMsg(TXCA_PARAM_RESPONSE *cRsp);
    bool processPlayMsg(TXCA_PARAM_RESPONSE *cRsp);
    bool processException(TXCA_PARAM_RESPONSE *cRsp);

    void playTipRes(std::string url);
    void clearMsgPlayList();
    void genMediaWithMsg(XWeiCMsgBase *msg);
    void playNextMedia();

  private:
    static const std::string MSG_RING;
    int m_curState;
    int m_sessionId;
    unsigned long long m_targetId;
    QQMsgMedia m_curMedia;
    TipPlayer *m_tipPlayer;
    pthread_mutex_t m_msgPlayListMutex;
    std::queue<QQMsgMedia> m_msgPlayList;
};

// 用于分发本地Skill的响应数据和消息以及焦点管理
class XWeiOutSkillManager
{
  public:
    XWeiOutSkillManager();
    ~XWeiOutSkillManager();

  public:
    static XWeiOutSkillManager *instance()
    {
        static XWeiOutSkillManager m_Instance;
        return &m_Instance;
    }

    // 根据skillId注册本地能处理的Skill
    void registerSkillId(const std::string &skillId, OutSkillHandler *handler);

    // 根据skillName注册本地能处理的Skill
    void registerSkillName(const std::string &skillName, OutSkillHandler *handler);

    // 当控制抛出Skill响应时，判断本地是否能处理
    bool startOutSkill(int sessionId, const std::string &skillName, const std::string &skillId);

    // 处理Skill响应
    bool handleSkillResponse(int sessionId, TXCA_PARAM_RESPONSE *cRsp);

    // 处理控制层消息队列的事件
    bool onMessage(int session_id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2);

    // focus_change焦点类型
    void onAudioFocusChange(int cookie, int focus_change);

    // 申请焦点
    void requestAudioFocus(OutSkillAudioFocusListener *listener, DURATION_HINT duration);

    // 释放焦点
    void abandonAudioFocus(OutSkillAudioFocusListener *listener);

  private:
    std::map<int, OutSkillAudioFocusListener *> listeners;
    std::map<std::string, OutSkillHandler *> handlers;
};

#endif /* XWeiOutSkillManager_hpp */
