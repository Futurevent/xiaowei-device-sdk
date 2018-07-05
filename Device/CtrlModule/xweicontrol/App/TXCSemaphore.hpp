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
#ifndef TXCSemaphore_hpp
#define TXCSemaphore_hpp

#include <semaphore.h>
#ifdef OS_MAC
#include <dispatch/dispatch.h>
#endif

class TXCSemaphore
{
  public:
    TXCSemaphore();
    ~TXCSemaphore();

    int Wait();
    int Wait(unsigned long long time_ms);
    int Try();
    int Post();

  private:
    //  disabled:
    TXCSemaphore(const TXCSemaphore &other);
    const TXCSemaphore &operator=(TXCSemaphore &other);

  private:

    
#ifdef OS_MAC
    dispatch_semaphore_t semaphore;

#else
    sem_t sem_;
    sem_t *psem_;
    char sem_name_[32];
#endif
};

#endif /* TXCSemaphore_hpp */
