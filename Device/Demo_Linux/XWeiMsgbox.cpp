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

#include "XWeiMsgbox.h"
#include <sstream>
#include <stdio.h>
#include <string.h>

static int g_seedMsgId = 1000;

XWeiCMsgBase::XWeiCMsgBase()
    : msgId(g_seedMsgId++), uin_(0), timestamp(0), isReaded(false), isRecv(true), msgType(qq_msg_type_invalid)
{
}

XWeiCMsgBase::~XWeiCMsgBase()
{
}

bool XWeiCMsgBase::isPlayable() const
{
    return false;
}

void XWeiCMsgBase::Clear()
{
}

std::string XWeiCMsgBase::toString()
{
    std::stringstream ss;

    ss << "{";
    ToString(ss);
    ss << "}";

    std::string strData = ss.str();
    return strData;
}

void XWeiCMsgBase::ToString(std::stringstream &ss) const
{
    ss << "msgId:";
    ss << msgId;
    ss << ", msgType:";
    ss << msgType;
    char szUin[10];
    snprintf(szUin, 10, "%llu", uin_);
    ss << ", uin:";
    ss << szUin;
    ss << ", isReaded:";
    ss << isReaded;
    ss << ", isRecv:";
    ss << isRecv;
    ss << ", timestamp:";
    ss << timestamp;
}

CXWeiMsgText::CXWeiMsgText()
{
    msgType = qq_msg_type_iot_text;
}

CXWeiMsgText::~CXWeiMsgText()
{
}

void CXWeiMsgText::ToString(std::stringstream &ss) const
{
    XWeiCMsgBase::ToString(ss);
    ss << ", text:";
    ss << text;
}

CXWeiMsgAudio::CXWeiMsgAudio()
    : duration(0)
{
    msgType = qq_msg_type_iot_audio;
}

CXWeiMsgAudio::~CXWeiMsgAudio()
{
}

bool CXWeiMsgAudio::isPlayable() const
{
    return true;
}

void CXWeiMsgAudio::Clear()
{
    if (!localUrl.empty())
    {
        remove(localUrl.c_str());
        localUrl = "";
    }
}

void CXWeiMsgAudio::ToString(std::stringstream &ss) const
{
    XWeiCMsgBase::ToString(ss);

    ss << ", amr:";
    ss << localUrl;
    ss << ", duration:";
    ss << duration;
}

CXWeiMsgWechat::CXWeiMsgWechat()
{
    msgType = wechat_msg_type_text;
}

CXWeiMsgWechat::~CXWeiMsgWechat()
{
}

void CXWeiMsgWechat::ToString(std::stringstream &ss) const
{
    XWeiCMsgBase::ToString(ss);

    ss << ", from:";
    ss << from;
    ss << ", content:";
    ss << content;
    ss << ", remark:";
    ss << remark;
    ss << ", headurl:";
    ss << headurl;
}

XWeiCMsgbox::XWeiCMsgbox()
    : m_nMaxMsgSize(50), m_nMsgIndex(-1), m_uin(0)
{
}

XWeiCMsgbox::~XWeiCMsgbox()
{
    //释放内存-消息队列
    for (MsgBoxList::iterator it = m_msgboxList.begin(); it != m_msgboxList.end(); ++it)
    {
        if (*it)
            delete (*it);
    }
    m_msgboxList.clear();
}

XWeiCMsgbox &XWeiCMsgbox::instance()
{
    static XWeiCMsgbox _instance;
    return _instance;
}

void XWeiCMsgbox::SetMaxMsgSize(unsigned int nMaxSize)
{
    m_nMaxMsgSize = nMaxSize;
}

bool XWeiCMsgbox::AddMsg(XWeiCMsgBase *pMsg)
{
    if (NULL == pMsg)
    {
        return false;
    }

    bool bAdd = AddMsgInner(pMsg);

    if (bAdd)
        NotifyMsgAdd(pMsg);

    return bAdd;
}

bool XWeiCMsgbox::AddMsgInner(XWeiCMsgBase *pMsg)
{
    bool bAdd = false;
    unsigned int nSize = (unsigned int)m_msgboxList.size();
    m_msgboxList.push_back(pMsg);
    bAdd = (m_msgboxList.size() > nSize) ? true : false;
    ResizeMsgboxSize(&m_msgboxList);
    return bAdd;
}

void XWeiCMsgbox::ResizeMsgboxSize(MsgBoxList *pMsgboxList)
{
    if (NULL == pMsgboxList)
        return;

    while (pMsgboxList->size() > m_nMaxMsgSize)
    {
        XWeiCMsgBase *pMsg = *(pMsgboxList->begin());
        if (pMsg)
        {
            pMsg->Clear();
            delete pMsg;
            pMsg = NULL;
        }
        pMsgboxList->erase(pMsgboxList->begin());
    }
}

void XWeiCMsgbox::NotifyMsgAdd(XWeiCMsgBase *pMsg)
{
}

XWeiCMsgBase *XWeiCMsgbox::GetNextUnReadMsg()
{
    int nOffset = (m_nMsgIndex >= 0 ? m_nMsgIndex : 0);
    MsgBoxList::iterator it = m_msgboxList.begin();
    if (m_nMsgIndex > 0)
    {
        std::advance(it, nOffset);
    }
    for (; it != m_msgboxList.end(); ++it)
    {
        if ((m_uin != 0 && (*it)->uin_ == m_uin) || m_uin == 0)
        {
            if (!(*it)->isReaded)
            {
                SetMsgReadIndex(nOffset);
                return (*it);
            }
        }
        nOffset++;
    }
    return NULL;
}

XWeiCMsgBase *XWeiCMsgbox::GetPrevMsg()
{
    int nOffset = (m_nMsgIndex >= 0 ? m_nMsgIndex : 0);
    if (nOffset == 0)
    {
        return NULL;
    }

    MsgBoxList::iterator it = m_msgboxList.begin();
    std::advance(it, nOffset - 1);
    SetMsgReadIndex(nOffset - 1);
    return (*it);
}

XWeiCMsgBase *XWeiCMsgbox::GetNextMsg()
{
    unsigned int nOffset = (m_nMsgIndex >= 0 ? m_nMsgIndex : 0);
    if (nOffset >= m_msgboxList.size())
    {
        return NULL;
    }

    MsgBoxList::iterator it = m_msgboxList.begin();
    std::advance(it, nOffset + 1);
    SetMsgReadIndex(nOffset + 1);
    return (*it);
}

XWeiCMsgBase *XWeiCMsgbox::GetMsgById(unsigned int msgId)
{
    MsgBoxList::iterator it = m_msgboxList.begin();
    for (; it != m_msgboxList.end(); ++it)
    {
        if ((*it)->msgId == msgId)
        {
            return (*it);
        }
    }
    return NULL;
}

void XWeiCMsgbox::SetMsgReadIndex(int nIndex)
{
    m_nMsgIndex = nIndex;
}

bool XWeiCMsgbox::SetMsgReaded(unsigned int msgId)
{
    bool bSuc = false;
    MsgBoxList::iterator it = m_msgboxList.begin();
    for (; it != m_msgboxList.end(); ++it)
    {
        if ((*it)->msgId == msgId)
        {
            (*it)->isReaded = true;
            bSuc = true;
            break;
        }
    }
    return bSuc;
}

void XWeiCMsgbox::SetSingleUinMode(unsigned long long uin)
{
    m_uin = uin;
}
