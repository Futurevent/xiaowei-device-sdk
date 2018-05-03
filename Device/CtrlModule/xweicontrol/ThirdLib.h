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
#ifndef ThirdLib_h
#define ThirdLib_h

#include <cstddef>   // NULL
#include <algorithm> // std::swap

// can be replaced by other error mechanism
#include <cassert>

namespace tpl
{
//  add third part library namespace for avoid dumplicate define of shared_ptr
#include "../library/shared_ptr/include/shared_ptr.hpp"
}

#endif /* ThirdLib_h */
