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
#include "txctypedef.h"
#include "TXCMessageQueue.hpp"
#include <vector>
#include <list>
#include "TXCServices.hpp"
#include "ResponseHolder.hpp"
#include "AudioApp.h"
#include "TXCLock.hpp"
#include "TXCSemaphore.hpp"
#include "AsyncTask.h"
#include "logger.h"
#include <string.h>
#include "Util.hpp"

#if defined(OS_LINUX)
#include <sys/syscall.h>
#elif defined(OS_ANDROID)
#include <sys/types.h>
#endif
#include <unistd.h>

namespace inner
{

class CMsgQueueImplAsyn
{
  public:
    void Start();
    void Stop();

    bool Register(txc_event_processor processor);
    void UnRegister(txc_event_processor processor);

    bool SendMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2);
    bool AsyncMessage(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2);
    bool PostMessage(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2, size_t delay = 0);

    void ProcessResponse2(CResponseHolder &cRsp);

    void WorkThread();
    void PushTask(TXCAutoPtr<TXCTask> &spTask);

  protected:
    struct Message
    {
        SESSION sessionId;
        XWM_EVENT event;
        XWPARAM arg1;
        XWPARAM arg2;
    };
    bool DispatchMessage(Message &msg);
    bool DefaultProcessor(Message &msg);

    bool ProcessSysMessage(Message &msg);

  private:
    pid_t thread_id_;
    bool stoped_;
    TXCLock lock_;
    TXCSemaphore sem_;
    std::list<TXCAutoPtr<TXCTask> > tasks_;

    std::vector<txc_event_processor> vprocessors_;
};

pid_t current_thread_id(void)
{
// Pthreads doesn't have the concept of a thread ID, so we have to reach down
// into the kernel.
#if defined(OS_LINUX)
    return syscall(__NR_gettid);
#elif defined(OS_ANDROID)
    return gettid();
#elif defined(OS_MAC)
    return (pid_t) reinterpret_cast<int64>(pthread_self());
#else
    return reinterpret_cast<int64>(pthread_self());
#endif
}

void *msg_queue_thread_routine(void *context)
{
#if defined(OS_MAC)
    pthread_setname_np("XWeiMessageQueue");
#endif
    CMsgQueueImplAsyn *asyner = reinterpret_cast<CMsgQueueImplAsyn *>(context);
    asyner->WorkThread();
    return NULL;
}

void CMsgQueueImplAsyn::Start()
{
    thread_id_ = -1;
    stoped_ = false;

    pthread_t pthread = {0};
    pthread_attr_t attr = {0};
    pthread_attr_init(&attr);
    void *context = reinterpret_cast<void *>(this);
    if (pthread_create(&pthread, &attr, inner::msg_queue_thread_routine, context) == 0)
    {
#if defined(OS_ANDROID)
        pthread_setname_np(pthread, "XWeiMessageQueue");
#elif defined(OS_LINUX)
        pthread_setname_np(pthread, "XWeiMessageQueue");
#endif
    }
    else
    {
    }

    pthread_attr_destroy(&attr);
}

void CMsgQueueImplAsyn::Stop()
{
    stoped_ = true;
    sem_.Post();
}

void CMsgQueueImplAsyn::WorkThread()
{
    thread_id_ = current_thread_id();

    while (!stoped_)
    {
        while (true)
        {
            TXCAutoPtr<TXCTask> runTask;
            {
                TXCAutoLock lock(lock_);
                if (!tasks_.empty())
                {
                    TXCAutoPtr<TXCTask> &spTask = tasks_.front();
                    if (spTask.get())
                    {
                        unsigned long long currentTime = Util::GetCurTimeMsLocal();

                        if (currentTime >= spTask->when)
                        {
                            runTask = spTask;
                            tasks_.pop_front();
                        }
                        else
                        {
                            unsigned long long waitTime = spTask->when - currentTime;
                            sem_.Wait(waitTime);// 到时间了一定会结束wait
                            continue;
                        }
                    }
                }
            }
            if (runTask.get())
            {
                runTask->Run();
            }
            else
            {
                break;
            }
        }
        sem_.Wait();
    }
}

// 按照when，找到比spTask大的元素，插入到它前面。找不到直接放末尾
void CMsgQueueImplAsyn::PushTask(TXCAutoPtr<TXCTask> &spTask)
{
    {
        TXCAutoLock lock(lock_);
        bool inserted = false;
        for (std::list<TXCAutoPtr<TXCTask> >::iterator iter = tasks_.begin(); iter != tasks_.end(); iter++)
        {
            TXCAutoPtr<TXCTask> &task = *iter;
            if (task.get() && task->when > spTask->when)
            {
                inserted = true;
                tasks_.insert(iter, spTask);
                break;
            }
        }
        if (!inserted)
        {
            tasks_.push_back(spTask);
        }
    }
    sem_.Post();
}

TXC_ASYN_TASK(CMsgQueueImplAsyn, ProcessResponse2, CResponseHolder, cRsp);
void CMsgQueueImplAsyn::ProcessResponse2(CResponseHolder &cRsp)
{
    TXC_ASYN_CALL_NR(CMsgQueueImplAsyn, ProcessResponse2, cRsp);

    TXCServices::instance()->GetAppManager()->OnAiAudioRsp(cRsp.response());
}

TXC_SYNC_TASK4(CMsgQueueImplAsyn, SendMessage, bool, SESSION, id, XWM_EVENT, p_event, XWPARAM, arg1, XWPARAM, arg2);
bool CMsgQueueImplAsyn::SendMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2)
{
    TXC_SYNC_CALL4(CMsgQueueImplAsyn, SendMessage, id, p_event, arg1, arg2);

    bool handled = false;

    Message msg = {0};
    msg.sessionId = id;
    msg.event = p_event;
    msg.arg1 = arg1;
    msg.arg2 = arg2;

    handled = DispatchMessage(msg);

    return handled;
}

TXC_SYNC_TASK4(CMsgQueueImplAsyn, AsyncMessage, bool,
               SESSION, id, XWM_EVENT, p_event,
               XWPARAM, arg1, XWPARAM, arg2);
bool CMsgQueueImplAsyn::AsyncMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2)
{
    bool handled = true;

    Message msg = {0};
    msg.sessionId = id;
    msg.event = p_event;
    msg.arg1 = arg1;
    msg.arg2 = arg2;

    handled = DispatchMessage(msg);

    return handled;
}

bool CMsgQueueImplAsyn::PostMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2, size_t delay)
{
    TXC_ASYN_PUSH_TASK(CMsgQueueImplAsyn, AsyncMessage, id, p_event, arg1, arg2, delay <= 0 ? 0 : Util::GetCurTimeMsLocal() + delay);
}

TXC_ASYN_TASK(CMsgQueueImplAsyn, Register, txc_event_processor, processor);
bool CMsgQueueImplAsyn::Register(txc_event_processor processor)
{
    TXC_ASYN_CALL(CMsgQueueImplAsyn, Register, processor);
#ifdef SEG_ERROR
    inner::CMsgQueueImplAsyn::Register(processor);
#endif

    bool pushed = false;
    if (processor)
    {
        std::vector<txc_event_processor>::iterator itr = std::find(vprocessors_.begin(), vprocessors_.end(), processor);
        if (vprocessors_.end() == itr)
        {
            vprocessors_.push_back(processor);
            pushed = true;
        }
        else
        {
            assert(!"duplicate register processor");
        }
    }

    return pushed;
}

TXC_ASYN_TASK(CMsgQueueImplAsyn, UnRegister, txc_event_processor, processor);
void CMsgQueueImplAsyn::UnRegister(txc_event_processor processor)
{
    TXC_ASYN_CALL_NR(CMsgQueueImplAsyn, UnRegister, processor);

    std::vector<txc_event_processor>::iterator itr = std::find(vprocessors_.begin(), vprocessors_.end(), processor);
    if (vprocessors_.end() == itr)
    {
        vprocessors_.erase(itr);
    }
}

bool CMsgQueueImplAsyn::DispatchMessage(Message &msg)
{
    bool handled = false;

    if (msg.event >= XWM_SYSTEM && msg.event < XWM_USER)
    {
        handled = ProcessSysMessage(msg);
    }

    //  user handler
    if (!handled)
    {
        std::vector<txc_event_processor>::iterator itrProc = vprocessors_.begin();
        while (!handled && vprocessors_.end() != itrProc)
        {
            txc_event_processor processor = *itrProc;
            if (processor)
            {
                handled = processor(msg.sessionId, msg.event, msg.arg1, msg.arg2);
            }
            ++itrProc;
        }
    }

    //  app skill handler
    if (!handled)
    {
        TXCServices::instance()->GetAppManager()->OnMessage(msg.sessionId, msg.event, msg.arg1, msg.arg2);
    }

    if (!handled)
    {
        handled = DefaultProcessor(msg);
    }

    return handled;
}

bool CMsgQueueImplAsyn::ProcessSysMessage(Message &msg)
{
    bool handled = false;
    switch (msg.event)
    {
    default:
        break;
    }

    return handled;
}

bool CMsgQueueImplAsyn::DefaultProcessor(Message &msg)
{
    bool handled = XWM_ERROR_RESPONSE != msg.event;
    return handled;
}

} //  namespace inner2

TXCMessageQueue::TXCMessageQueue()
    : msg_queue_impl_(NULL)
{
}

TXCMessageQueue::~TXCMessageQueue()
{
    Stop();
}

void TXCMessageQueue::Start()
{
    if (msg_queue_impl_ != NULL)
    {
        return;
    }

    msg_queue_impl_ = new inner::CMsgQueueImplAsyn();
    msg_queue_impl_->Start();
}

void TXCMessageQueue::Stop()
{
    if (msg_queue_impl_)
    {
        msg_queue_impl_->Stop();
        delete msg_queue_impl_;
        msg_queue_impl_ = NULL;
    }
}

bool TXCMessageQueue::AddProcessor(txc_event_processor processor)
{
    bool pushed = false;
    if (msg_queue_impl_)
    {
        pushed = msg_queue_impl_->Register(processor);
    }

    return pushed;
}

void TXCMessageQueue::RemoveProcessor(txc_event_processor processor)
{
    if (msg_queue_impl_)
    {
        msg_queue_impl_->UnRegister(processor);
    }
}

bool TXCMessageQueue::PostMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2, unsigned int delay)
{
    bool insert = true;

    if (msg_queue_impl_)
    {
        msg_queue_impl_->PostMessage(id, p_event, arg1, arg2, delay);
    }

    return insert;
}

bool TXCMessageQueue::SendMessage(SESSION id, XWM_EVENT p_event, XWPARAM arg1, XWPARAM arg2)
{
    bool handled = false;

    if (msg_queue_impl_)
    {
        handled = msg_queue_impl_->SendMessage(id, p_event, arg1, arg2);
    }

    return handled;
}

void TXCMessageQueue::ProcessResponse(TXCA_PARAM_RESPONSE *response)
{
    if (response && msg_queue_impl_)
    {
        CResponseHolder cresp;
        cresp.Copy(response);
        msg_queue_impl_->ProcessResponse2(cresp);
    }
}

SDK_API bool txc_process_response(const char *voice_id, TXCA_EVENT event, const char *state_info, const char *extend_info, unsigned int extend_info_len)
{
    switch (event)
    {
    case txca_event_on_recognize:
        break;
    case txca_event_on_response:
    {
        TXCA_PARAM_RESPONSE *response = reinterpret_cast<TXCA_PARAM_RESPONSE *>((char *)state_info);
        TXCServices::instance()->GetMessageQueue()->ProcessResponse(response);
    }
    break;
    case txca_event_on_speak:
        break;
    default:
        break;
    }

    const char *eveTxt[] = {
        "txca_event_on_idle               = 0,      // 空闲",
        "txca_event_on_request_start      = 1,      // 请求开始",
        "txca_event_on_speak              = 2,      // 检测到说话",
        "txca_event_on_silent             = 3,      // 检测到静音(only@device has not txca_device_local_vad)",
        "txca_event_on_recognize          = 4,      // 识别文本实时返回",
        "txca_event_on_response           = 5,      // 请求收到响应",
        "txca_event_on_tts                = 6,      // TTS Data"};
    const char *current_event = eveTxt[event];
    TLOG_DEBUG("#### %s, \n%", current_event);

    return true;
}

bool post_message(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2, unsigned int delay)
{
    return TXCServices::instance()->GetMessageQueue()->PostMessage(id, event, arg1, arg2, delay);
}

bool send_message(SESSION id, XWM_EVENT event, XWPARAM arg1, XWPARAM arg2)
{
    return TXCServices::instance()->GetMessageQueue()->SendMessage(id, event, arg1, arg2);
}
