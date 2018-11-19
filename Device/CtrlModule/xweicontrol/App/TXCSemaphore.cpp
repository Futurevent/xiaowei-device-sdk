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
#include "TXCSemaphore.hpp"

#include <sys/time.h>
#include <stdio.h>
#include <string.h>
#include <cassert>
#include <sys/cdefs.h>

TXCSemaphore::TXCSemaphore()
{
#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    semaphore = dispatch_semaphore_create(0);
#else
    psem_ = &sem_;
    int err = sem_init(psem_, 0, 0);
    assert(err == 0);
#endif
}
TXCSemaphore::~TXCSemaphore()
{
#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    dispatch_release(semaphore);
#else
    sem_destroy(psem_);
#endif
}
int TXCSemaphore::Wait()
{
    int err = -1;
    
#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    err = (int) dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
#else
    if (SEM_FAILED != psem_)
    {
        err = sem_wait(psem_);
    }
#endif
    return err;
}

int TXCSemaphore::Wait(unsigned long long time_ms)
{
    int err = -1;

#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    dispatch_time_t  time = dispatch_time(DISPATCH_TIME_NOW, time_ms*1000*1000);
    err = (int) dispatch_semaphore_wait(semaphore, time);
#else
    if (SEM_FAILED != psem_)
    {
        struct timespec abs_time;
        struct timeval time;
        gettimeofday(&time, NULL);
        time.tv_usec += time_ms * 1000; // 单位是us
        if(time.tv_usec >= 1000000) // 进位
        {
            time.tv_sec += time.tv_usec / 1000000;
            time.tv_usec %= 1000000;
        }
        abs_time.tv_sec = time.tv_sec;
        abs_time.tv_nsec = time.tv_usec * 1000;
        err = sem_timedwait(psem_, &abs_time);
    }
#endif
    return err;

}

int TXCSemaphore::Try()
{
    int err = -1;
#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    err = (int) dispatch_semaphore_wait(semaphore, DISPATCH_TIME_NOW);
#else
    if (SEM_FAILED != psem_)
    {
        err = sem_trywait(psem_);
    }
#endif
    return err;
}

int TXCSemaphore::Post()
{
    int err = -1;
#if defined(OS_MAC) || defined(TARGET_OS_IPHONE)
    err = (int) dispatch_semaphore_signal(semaphore);
#else
    if (SEM_FAILED != psem_)
    {
        err = sem_post(psem_);
    }
#endif
    return err;
}
