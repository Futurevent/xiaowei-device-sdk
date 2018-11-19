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
package com.tencent.aiaudio.activity;

import com.tencent.aiaudio.activity.base.BaseActivity;
import com.tencent.xiaowei.util.Singleton;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityManager {
    private static Singleton<ActivityManager> sSingleton = new Singleton<ActivityManager>() {
        @Override
        protected ActivityManager createInstance() {
            return new ActivityManager();
        }
    };

    public static ActivityManager getInstance() {
        if (sSingleton == null) {
            sSingleton = new Singleton<ActivityManager>() {
                @Override
                protected ActivityManager createInstance() {
                    return new ActivityManager();
                }
            };
        }
        return sSingleton.getInstance();
    }

    private HashMap<Integer, BaseActivity> map = new HashMap<>();
    private ArrayList<BaseActivity> list = new ArrayList<>();


    public void put(int sessionId, BaseActivity activity) {
        map.put(sessionId, activity);
        list.add(activity);
    }


    public void remove(int sessionId) {
        list.remove(map.remove(sessionId));
    }

    public void finish(int sessionId) {
        BaseActivity activity = map.remove(sessionId);
        if (activity != null) {
            activity.onSkillIdle();
        }
        list.remove(activity);
    }

    public void finishAll() {
        for (BaseActivity activity : list) {
            activity.finish();
        }
        list.clear();
        map.clear();
    }

}
