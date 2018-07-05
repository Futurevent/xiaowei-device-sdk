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
package com.tencent.aiaudio.msg.data;

import android.os.AsyncTask;
import android.util.Log;

import com.tencent.aiaudio.alarm.SkillAlarmBean;

import org.xutils.DbManager;
import org.xutils.DbManager.DbOpenListener;
import org.xutils.DbManager.DbUpgradeListener;
import org.xutils.DbManager.TableCreateListener;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * QQ 消息的本地持久化存储
 */
public class QQMsgDbManager implements DbUpgradeListener, DbOpenListener, TableCreateListener {
    private static final String TAG = QQMsgDbManager.class.getSimpleName();

    private static final int DEF_DB_VERSION = 1;
    private static final String DB_NAME = "msg_db";

    public static final String MSG_ADD_ACTION = "ALARM_ADD_ACTION";
    public static final String MSG_DEL_ACTION = "ALARM_DEL_ACTION";
    public static final String MSG_UPDATE_ACTION = "ALARM_UPDATE_ACTION";

    private DbManager mDbManager;
    private volatile static QQMsgDbManager INSTANCE;

    public static QQMsgDbManager getInstance() {
        if (INSTANCE == null) {
            synchronized (QQMsgDbManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new QQMsgDbManager();
                }
            }
        }

        return INSTANCE;
    }

    public QQMsgDbManager() {
        DbManager.DaoConfig config = new DbManager.DaoConfig();
        config.setDbName(DB_NAME);
        config.setDbVersion(DEF_DB_VERSION);
        config.setAllowTransaction(true);
        config.setDbUpgradeListener(this);
        config.setDbOpenListener(this);
        config.setTableCreateListener(this);

        mDbManager = x.getDb(config);
    }

    public void release() {
        if (mDbManager == null) {
            Log.d(TAG, "release() mDbManager == null.");
            return;
        }

        DbManager.DaoConfig config = mDbManager.getDaoConfig();
        if (config != null) {
            config.setDbUpgradeListener(null);
            config.setDbOpenListener(null);
            config.setTableCreateListener(null);
        }

        try {
            mDbManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addMsgItem(QQMsgEntry entry, OnOperationFinishListener listener) {
        if (entry == null) {
            Log.e(TAG, "addMsgItem() entry == null.");
            return;
        }

        Log.d(TAG, String.format("addMsgItem() entry:%s", entry.toString()));

        AddMsgAsyncTask addAlarmAsyncTask = new AddMsgAsyncTask(listener);
        addAlarmAsyncTask.execute(entry);
    }

    public void queryAllMsgItem(OnQueryAllMsgListener listener) {
        QueryAllMsgAsyncTask task = new QueryAllMsgAsyncTask(listener);
        task.execute();
    }

    public void updateMsgItem(QQMsgEntry entry, OnOperationFinishListener listener) {
        if (entry == null) {
            Log.e(TAG, "updateMsgItem() entry == null.");
            return;
        }

        Log.d(TAG, String.format("updateMsgItem() entry:%s", entry.toString()));

        UpdateMsgAsyncTask task = new UpdateMsgAsyncTask(listener);
        task.execute(entry);
    }

    public void deleteMsgItems(List<QQMsgEntry> entries, OnOperationFinishListener listener) {
        if (entries == null) {
            Log.e(TAG, "deleteMsgItems() entries == null. ");
        }

        DeleteMsgAsyncTask task = new DeleteMsgAsyncTask(listener);
        task.execute(entries);
    }

    @Override
    public void onUpgrade(DbManager dbManager, int i, int i1) {
        Log.d(TAG, String.format("onUpgrade(i=%s,i1=%s).", i, i1));
    }

    @Override
    public void onDbOpened(DbManager dbManager) {
        Log.d(TAG, "onDbOpened().");
    }

    @Override
    public void onTableCreated(DbManager dbManager, TableEntity<?> tableEntity) {
        Log.d(TAG, "onTableCreated().");
    }

    private class QueryAllMsgAsyncTask extends AsyncTask<Void, Void, List<QQMsgEntry>> {
        private OnQueryAllMsgListener mOnQueryAllMsgListener;

        public QueryAllMsgAsyncTask(OnQueryAllMsgListener listener) {
            this.mOnQueryAllMsgListener = listener;
        }

        @Override
        protected List<QQMsgEntry> doInBackground(Void... params) {
            if (mDbManager == null) {
                Log.d(TAG, "QueryAllMsgAsyncTask mDbManager == null.");
                return null;
            }

            try {
                return mDbManager.findAll(QQMsgEntry.class);
            } catch (DbException e) {
                e.printStackTrace();
            }

            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<QQMsgEntry> entries) {
            if (mOnQueryAllMsgListener == null) {
                Log.d(TAG, "mOnQueryAllMsgListener == null.");
            } else {
                mOnQueryAllMsgListener.onQueryAllMsg(entries);
            }
        }
    }

    private class AddMsgAsyncTask extends AsyncTask<QQMsgEntry, Void, QQMsgEntry> {
        private OnOperationFinishListener mOnOperationFinishListener;

        public AddMsgAsyncTask(OnOperationFinishListener listener) {
            this.mOnOperationFinishListener = listener;
        }

        @Override
        protected QQMsgEntry doInBackground(QQMsgEntry... entries) {
            if (mDbManager == null) {
                Log.d(TAG, "AddMsgAsyncTask mDbManager == null.");
                return null;
            }

            if (entries == null || entries.length == 0) {
                Log.d(TAG, "AddMsgAsyncTask beans == null || beans.length == 0.");
                return null;
            }

            QQMsgEntry entry = entries[0];
            try {
                mDbManager.saveOrUpdate(entry);
            } catch (DbException e) {
                e.printStackTrace();
            }

            return entry;
        }

        @Override
        protected void onPostExecute(QQMsgEntry entry) {
            if (mOnOperationFinishListener == null) {
                Log.d(TAG, "mOnOperationFinishListener == null.");
            } else {
                mOnOperationFinishListener.onOperationFinish(entry, MSG_ADD_ACTION);
            }
        }
    }

    private class DeleteMsgAsyncTask extends AsyncTask<List<QQMsgEntry>, Void, QQMsgEntry> {
        private OnOperationFinishListener mOnOperationFinishListener;

        public DeleteMsgAsyncTask(OnOperationFinishListener listener) {
            this.mOnOperationFinishListener = listener;
        }


        @Override
        protected QQMsgEntry doInBackground(List<QQMsgEntry>... lists) {
            if (mDbManager == null) {
                Log.d(TAG, "DeleteMsgAsyncTask mDbManager == null.");
                return null;
            }

            if (lists == null || lists.length == 0) {
                Log.d(TAG, "DeleteMsgAsyncTask beans == null || beans.length == 0.");
                return null;
            }

            for (QQMsgEntry entry : lists[0]) {
                try {
                    mDbManager.delete(SkillAlarmBean.class, WhereBuilder.b("id", "=", entry.getId()));
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(QQMsgEntry entry) {
            if (mOnOperationFinishListener == null) {
                Log.d(TAG, "mOnOperationFinishListener == null.");
            } else {
                mOnOperationFinishListener.onOperationFinish(entry, MSG_DEL_ACTION);
            }
        }
    }

    private class UpdateMsgAsyncTask extends AsyncTask<QQMsgEntry, Void, QQMsgEntry> {
        private OnOperationFinishListener mOnOperationFinishListener;

        public UpdateMsgAsyncTask(OnOperationFinishListener listener) {
            this.mOnOperationFinishListener = listener;
        }

        @Override
        protected QQMsgEntry doInBackground(QQMsgEntry... entries) {
            if (mDbManager == null) {
                Log.e(TAG, "UpdateMsgAsyncTask mDbManager == null.");
                return null;
            }

            if (entries == null || entries.length == 0) {
                Log.e(TAG, "UpdateMsgAsyncTask entries == null || entries.length == 0.");
                return null;
            }

            QQMsgEntry entry = entries[0];

            try {
                mDbManager.update(entry);
            } catch (DbException e) {
                e.printStackTrace();
            }

            return entry;
        }

        @Override
        protected void onPostExecute(QQMsgEntry entry) {
            if (mOnOperationFinishListener == null) {
                Log.d(TAG, "mOnOperationFinishListener == null.");
            } else {
                mOnOperationFinishListener.onOperationFinish(entry, MSG_UPDATE_ACTION);
            }
        }
    }
}
