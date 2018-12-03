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
package com.tencent.aiaudio.login;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.tencent.xiaowei.info.XWLoginStatusInfo;
import com.tencent.xiaowei.util.JsonUtil;
import com.tencent.xiaowei.util.QLog;

import org.json.JSONObject;

public class LoginView extends LinearLayout implements AdvancedWebView.Listener {

    private static final String URL_QQ_LOGIN = "https://graph.qq.com/oauth2.0/authorize?response_type=code&style=smart&display=mobile&client_id=1106062274&autorefresh=1&redirect_uri=https://xiaowei.qcloud.com/xiaowei-video-login";

    private static final String TAG = "LoginView";
    private AdvancedWebView mWebView;
    private MyWebViewClient mWebClient;
    private LoginResultListener listener;

    public LoginView(Context context) {
        this(context, null);
    }

    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        mWebView = new AdvancedWebView(getContext());
        mWebView.setListener(this);
        mWebView.setMixedContentAllowed(true);
        mWebView.setCookiesEnabled(true);
        mWebView.setThirdPartyCookiesEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setUseWideViewPort(true);      //任意比例缩放
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

        });

        mWebView.addHttpHeader("X-Requested-With", "XMLHttpRequest");
        mWebClient = new MyWebViewClient(mWebView);
        mWebClient.enableLogging();
        mWebView.setWebViewClient(mWebClient);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mWebView, layoutParams);
    }

    /**
     * 显示二维码
     */
    public void loadQQUrl(LoginResultListener listener) {
        this.listener = listener;
        mWebView.loadUrl(URL_QQ_LOGIN);
    }

    // 需要和Activity生命周期一同调用
    public void onResume() {
        mWebView.onResume();
    }

    public void onPause() {
        mWebView.onPause();
    }

    public void onDestroy() {
        mWebView.onDestroy();
    }

    class MyWebViewClient extends WVJBWebViewClient {
        public MyWebViewClient(WebView webView) {

            super(webView);

            enableLogging();

            registerHandler("setLoginInfo", new WVJBWebViewClient.WVJBHandler() {

                @Override
                public void request(Object data, final WVJBResponseCallback callback) {
                    int code = -1;
                    if (data instanceof JSONObject) {
                        JSONObject jo = ((JSONObject) data);
                        code = jo.optInt("code", -1);
                        String msg = jo.optString("msg", "");
                        JSONObject accountObj = jo.optJSONObject("account");
                        if (accountObj != null) {
                            QLog.i("LoginView", "登录成功");
                            setVisibility(GONE);
                            if (listener != null) {
                                XWLoginStatusInfo info = new XWLoginStatusInfo();
                                info.type = XWLoginStatusInfo.QQ;
                                info.appID = "1106062274";
                                info.openID = JsonUtil.getValue(accountObj.toString(), "openid");
                                info.accessToken = JsonUtil.getValue(accountObj.toString(), "access_token");
                                info.refreshToken = JsonUtil.getValue(accountObj.toString(), "refresh_token");
                                listener.onResult(XWLoginStatusInfo.QQ, code, info);
                                listener = null;
                            }
                        } else {
                            code = -1;
                            QLog.i("LoginView", "登录失败");
                            if (listener != null) {
                                listener.onResult(XWLoginStatusInfo.QQ, code, null);
                            }
                        }
                    } else {
                        QLog.i("LoginView", "登录失败");
                        if (listener != null) {
                            listener.onResult(XWLoginStatusInfo.QQ, code, null);
                        }
                    }
                }
            });

        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("market")) {
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            try {
                webView.loadUrl("javascript:(function() { " +
                        "document.getElementById('download-area-pad').style.display='none'; " +
                        "})()");
            } catch (Throwable e) {
                QLog.e("LoginView", e.getMessage());
            }
        }

    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {

    }

    @Override
    public void onPageFinished(String url) {

    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {

    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

    }

    @Override
    public void onExternalPageRequest(String url) {

    }

    public interface LoginResultListener {
        void onResult(int type, int error, XWLoginStatusInfo info);
    }
}
