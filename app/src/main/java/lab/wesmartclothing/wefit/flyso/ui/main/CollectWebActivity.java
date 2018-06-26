package lab.wesmartclothing.wefit.flyso.ui.main;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebConfig;
import com.vondear.rxtools.aboutCarmera.RxImageUtils;
import com.vondear.rxtools.model.wechat.share.WechatShareModel;
import com.vondear.rxtools.model.wechat.share.WechatShareTools;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxUtils;

import org.json.JSONException;
import org.json.JSONObject;

import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseWebActivity;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.utils.AndroidInterface;
import lab.wesmartclothing.wefit.flyso.utils.StatusBarUtils;
import lab.wesmartclothing.wefit.flyso.view.SharePop;

public class CollectWebActivity extends BaseWebActivity {

    private BridgeWebView mBridgeWebView;
    private RelativeLayout parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_interactive);
        //屏幕沉浸
        StatusBarUtils.from(this).setStatusBarColor(getResources().getColor(R.color.colorTheme)).process();
        initView();
    }

    @Override
    public void initView() {
        parent = findViewById(R.id.parent);
        mBridgeWebView = new BridgeWebView(mActivity);

        mAgentWeb = AgentWeb.with(this)//
                .setAgentWebParent(parent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))//
                .useDefaultIndicator(-1, 2)//
                .setWebViewClient(new BridgeWebViewClient(mBridgeWebView))
                .setWebView(mBridgeWebView)
                .setMainFrameErrorView(R.layout.layout_web_error, -1) //参数1是错误显示的布局，参数2点击刷新控件ID -1表示点击整个布局都刷新， AgentWeb 3.0.0 加入。
                .useMiddlewareWebClient(getMiddleWareWebClient())
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
                .createAgentWeb()//
                .ready()//
                .go(getUrl());

        AgentWebConfig.debug();

        // AgentWeb 没有把WebView的功能全面覆盖 ，所以某些设置 AgentWeb 没有提供 ， 请从WebView方面入手设置。
        mAgentWeb.getWebCreator().getWebView().setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        WebSettings webSettings = mAgentWeb.getWebCreator().getWebView().getSettings();
//        设置默认加载的可视范围是大视野范围
        webSettings.setLoadWithOverviewMode(true);
//                自适应屏幕(导致活动页面显示出错了)
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setUseWideViewPort(true);

        if (mAgentWeb != null)
            mAgentWeb.getJsInterfaceHolder().addJavaObject("android", new AndroidInterface(mAgentWeb, mActivity));

        mBridgeWebView.registerHandler("shareEvent", new BridgeHandler() {
            @Override
            public void handler(final String data, CallBackFunction function) {
                RxLogUtils.i("传递参数：" + data);

                if (!RxUtils.isFastClick(1000)) {
                    initShare(data);
                }

            }
        });

        mBridgeWebView.registerHandler("back", new BridgeHandler() {

            @Override
            public void handler(final String data, CallBackFunction function) {
                RxLogUtils.i("传递参数：" + data);
                onBackPressed();
            }
        });
    }

    //    @Background
    public void initShare(String data) {
        try {
            JSONObject object = new JSONObject(data);
            String img = object.getString("img");
            final String title = object.getString("title");
            final String desc = object.getString("desc");
            final String url = object.getString("url");

            Glide.with(mActivity)
                    .load(img)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>(100, 100) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation) {
                            Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
                            byte[] thumbData = RxImageUtils.bitmap2Bytes(thumbBmp, Bitmap.CompressFormat.PNG);

                            final WechatShareModel wechatShareModel = new WechatShareModel(url, title, desc, thumbData);

                            showShareDialog(wechatShareModel);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void showShareDialog(final WechatShareModel wechatShareModel) {
        SharePop sharePop = new SharePop(mActivity);
        sharePop.initPop();
        sharePop.showAtLocation(parent, Gravity.BOTTOM, 0, 0);
        sharePop.setShareLinstener(new SharePop.ShareLinstener() {
            @Override
            public void shareType(int type) {
                switch (type) {
                    case SharePop.SHARE_PENGYOUYUAN:
                        WechatShareTools.shareURL(wechatShareModel, WechatShareTools.SharePlace.Zone);

                        break;
                    case SharePop.SHARE_WX:
                        WechatShareTools.shareURL(wechatShareModel, WechatShareTools.SharePlace.Friend);
                        break;
                }
            }
        });
    }


    @Nullable
    @Override
    protected WebView getWebView() {
        return mBridgeWebView;
    }

    @Nullable
    @Override
    protected String getUrl() {
        return getIntent().getStringExtra(Key.BUNDLE_WEB_URL);
    }

}