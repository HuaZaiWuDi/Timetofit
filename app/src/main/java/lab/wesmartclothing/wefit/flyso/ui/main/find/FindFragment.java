package lab.wesmartclothing.wefit.flyso.ui.main.find;

import android.graphics.Bitmap;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.BridgeWebViewClient;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.MiddlewareWebClientBase;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.utils.SPUtils;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseAcFragment;
import lab.wesmartclothing.wefit.flyso.base.BaseShareActivity;
import lab.wesmartclothing.wefit.flyso.netutil.net.ServiceAPI;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.flyso.utils.AndroidInterface;

/**
 * Created icon_hide_password jk on 2018/5/7.
 */
public class FindFragment extends BaseAcFragment {

    @BindView(R.id.parent)
    RelativeLayout mParent;


    private BridgeWebView mBridgeWebView;
    private AgentWeb mAgentWeb;

    public static FindFragment getInstance() {
        return new FindFragment();
    }

    @Override
    public int layoutId() {
        return R.layout.fragment_find;
    }

    @Override
    protected void initViews() {
        super.initViews();
        mBridgeWebView = new BridgeWebView(mActivity);
        initWebView();
    }

    private void initWebView() {

        mAgentWeb = AgentWeb.with(this)//
                .setAgentWebParent(mParent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))//
                .useDefaultIndicator(-1, 2)//
                .setWebViewClient(new BridgeWebViewClient(mBridgeWebView))
                .setWebView(mBridgeWebView)
                .setMainFrameErrorView(R.layout.layout_web_error, -1) //参数1是错误显示的布局，参数2点击刷新控件ID -1表示点击整个布局都刷新， AgentWeb 3.0.0 加入。
                .useMiddlewareWebClient(getMiddleWareWebClient())
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
                .createAgentWeb()//
                .ready()//
                .go(ServiceAPI.FIND_Addr);
        RxLogUtils.e("加载的URL：" + ServiceAPI.FIND_Addr);

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
                    try {
                        JSONObject object = new JSONObject(data);
                        String img = object.getString("img");
                        final String title = object.getString("title");
                        final String desc = object.getString("desc");
                        final String url = object.getString("url");

                        if (getActivity() instanceof BaseShareActivity) {
                            BaseShareActivity activity = (BaseShareActivity) getActivity();
                            if (activity != null)
                                activity.shareURL(img, title, desc, url);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    protected MiddlewareWebClientBase getMiddleWareWebClient() {
        return new MiddlewareWebClientBase() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                RxLogUtils.i("加载网页地址：" + url);
                mAgentWeb.getJsAccessEntrace().quickCallJs("getUserId", SPUtils.getString(SPKey.SP_UserId));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return super.shouldOverrideUrlLoading(view, request);
            }
        };
    }


    @Override
    public void onDestroy() {
        if (mAgentWeb != null)
            mAgentWeb.destroy();
        super.onDestroy();
    }
}
