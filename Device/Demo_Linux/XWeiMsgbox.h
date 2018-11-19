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

//  功能:存储、维护、获取消息内容
//  解绑清理消息盒子
#ifndef XWeiCMsgbox_hpp
#define XWeiCMsgbox_hpp

#include <string>
#include <map>
#include <vector>
#include <sstream>

#define WECHAT_MSG_TYPE_VOICE "voice"
#define WECHAT_MSG_TYPE_TEXT "text"

enum msg_type
{
    qq_msg_type_invalid = 0,
    qq_msg_type_iot_audio = 1,
    qq_msg_type_iot_text = 2,
    wechat_msg_type_text = 3,
    wechat_msg_type_audio_url = 4,
    wechat_msg_type_audio_file = 5,
};

//消息结构的基础信息
class XWeiCMsgBase
{
  public:
    unsigned int msgId;      //消息的序列号
    unsigned long long uin_; //消息发送者的tinyid
    int timestamp;           //接收到消息的时间(从sdk获取的服务器时间)
    bool isReaded;           //消息是否已读
    bool isRecv;             //消息方向 true: recv  false: send
    int msgType;             //消息类型

    XWeiCMsgBase();
    virtual ~XWeiCMsgBase();

    virtual bool isPlayable() const; //消息是否可播
    virtual void Clear();            //清理

    std::string toString();
    virtual void ToString(std::stringstream &ss) const;
};

class CXWeiMsgText : public XWeiCMsgBase
{
  public:
    CXWeiMsgText();
    virtual ~CXWeiMsgText();

    std::string text;

    virtual void ToString(std::stringstream &ss) const;
};

class CXWeiMsgAudio : public XWeiCMsgBase
{
  public:
    CXWeiMsgAudio();
    virtual ~CXWeiMsgAudio();

    std::string localUrl;
    unsigned int duration;

    bool isPlayable() const;
    void Clear();

    virtual void ToString(std::stringstream &ss) const;
};

class CXWeiMsgWechat : public XWeiCMsgBase
{
  public:
    CXWeiMsgWechat();
    virtual ~CXWeiMsgWechat();

    std::string from;
    std::string content;
    std::string remark;
    std::string headurl;

    virtual void ToString(std::stringstream &ss) const;
};

struct compTXCMsg
{
    bool operator()(XWeiCMsgBase *left, XWeiCMsgBase *right) //重载（）运算符
    {
        if (left == right || NULL == left || NULL == right)
        {
            return true;
        }
        else
        {
            return left->timestamp < right->timestamp;
        }
    }
};

class XWeiCMsgbox
{
  public:
    XWeiCMsgbox();
    ~XWeiCMsgbox();

    typedef std::vector<XWeiCMsgBase *> MsgBoxList;

    static XWeiCMsgbox &instance();

    //设置消息盒子单个账号下保存的最大消息数量
    void SetMaxMsgSize(unsigned int nMaxSize);
    //清理消息盒子
    void Clear();
    //新增一条消息
    bool AddMsg(XWeiCMsgBase *pMsg);
    // 第一条未读消息
    XWeiCMsgBase *GetNextUnReadMsg();
    // 获取上一条消息
    XWeiCMsgBase *GetPrevMsg();
    // 获取下一条消息
    XWeiCMsgBase *GetNextMsg();
    // 消息id获取消息信息
    XWeiCMsgBase *GetMsgById(unsigned int msgId);
    // 设置消息读取的索引id
    void SetMsgReadIndex(int nIndex); //用于“播放消息”重置索引id，从头开始播放
    // 设置消息已读状态
    bool SetMsgReaded(unsigned int msgId);
    // 设置单用户模式
    void SetSingleUinMode(unsigned long long uin);

  private:
    bool AddMsgInner(XWeiCMsgBase *pMsg);
    void ResizeMsgboxSize(MsgBoxList *pMsgboxList);
    void NotifyMsgAdd(XWeiCMsgBase *pMsg);

  private:
    MsgBoxList m_msgboxList;    //消息盒子队列
    unsigned int m_nMaxMsgSize; //MsgBoxList的最大存储量
    int m_nMsgIndex;            //消息读取的索引id(上一次播放的消息，由播放消息、上一条、下一条等控制)
    unsigned long long m_uin;   //消息盒子单用户模式的用户id
};

#endif /* XWeiCMsgbox_hpp */
