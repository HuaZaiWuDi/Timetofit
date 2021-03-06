package lab.wesmartclothing.wefit.flyso.ui.guide;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.alibaba.fastjson.JSON;
import com.google.gson.reflect.TypeToken;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxDeviceUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.utils.StatusBarUtils;
import com.vondear.rxtools.utils.dateUtils.RxFormat;
import com.zchu.rxcache.RxCache;
import com.zchu.rxcache.data.CacheResult;
import com.zchu.rxcache.stategy.CacheStrategy;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.SystemConfigBean;
import lab.wesmartclothing.wefit.flyso.entity.UpdateAppBean;
import lab.wesmartclothing.wefit.flyso.entity.UserInfo;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.ServiceAPI;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.service.BleService;
import lab.wesmartclothing.wefit.flyso.test.Test;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.flyso.ui.login.LoginRegisterActivity;
import lab.wesmartclothing.wefit.flyso.ui.main.MainActivity;
import lab.wesmartclothing.wefit.flyso.ui.main.mine.InvitationCodeActivity;
import lab.wesmartclothing.wefit.flyso.ui.userinfo.UserInfoActivity;
import lab.wesmartclothing.wefit.flyso.utils.HeartSectionUtil;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.flyso.utils.jpush.JPushUtils;

public class SplashActivity extends BaseActivity {


    BroadcastReceiver APPReplacedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //覆盖升级APP成功
            UpdateAppBean updateAppBean = new UpdateAppBean();
            updateAppBean.setVersionFlag(UpdateAppBean.VERSION_FLAG_APP);
            updateAppBean.setVersion(RxDeviceUtils.getAppVersionName());
            updateAppBean.setSystem("Android");
            updateAppBean.setPhoneType(RxDeviceUtils.getBuildMANUFACTURER());
            //AndroidID
            updateAppBean.setMacAddr(RxDeviceUtils.getAndroidId());
            updateAppBean.addDeviceVersion(updateAppBean);
        }
    };

    public static String weightTipList;


    @Override
    protected int layoutId() {
        return R.layout.activity_spalsh;
    }


    @Override
    protected void initStatusBar() {
        StatusBarUtils.from(mActivity).setHindStatusBar(true).process();
    }

    @Override
    protected void initViews() {
        super.initViews();
        HeartSectionUtil.initMaxHeart();
        startService(new Intent(this, BleService.class));
        JPushUtils.init(getApplication());
        registerReceiver(APPReplacedReceiver, new IntentFilter(Intent.ACTION_MY_PACKAGE_REPLACED));
    }

    @Override
    protected void initNetData() {
        super.initNetData();

        if (Test.testEnable) {
            Test.INSTANCE.main(this);
            return;
        }

        initData();
        initUserInfo();

    }

    private void initUserInfo() {
        RxLogUtils.i("启动时长：获取用户信息");
        if (!SPUtils.getBoolean(SPKey.SP_GUIDE)) {
            SPUtils.put(SPKey.SP_GUIDE, true);
            RxActivityUtils.skipActivityAndFinish(mActivity, GuideActivity.class);
            return;
        }
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().userInfo())
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        SPUtils.put(SPKey.SP_UserInfo, s);
                        gotoMain(s);
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        super._onError(error, code);
                        goError(code);
                    }
                });
    }

    private void gotoMain(String userInfoStr) {
        UserInfo userInfo = MyAPP.gUserInfo = JSON.parseObject(userInfoStr, UserInfo.class);
        boolean isSaveUserInfo = false, hasInviteCode = true;

        if (userInfo != null) {
            int sex = userInfo.getSex();
            isSaveUserInfo = sex == 0;
            hasInviteCode = userInfo.isHasInviteCode();

            RxLogUtils.d("跳转:" + userInfo.toString());
        }

        //通过验证是否保存userId来判断是否登录
        if (userInfo == null || userInfo.registerTime == 0) {
            RxActivityUtils.skipActivityAndFinish(mActivity, LoginRegisterActivity.class);
            return;
        }
        //默认不进入，只有请求到数据才进入
        if (!hasInviteCode) {
            RxActivityUtils.skipActivityAndFinish(mActivity, InvitationCodeActivity.class);
            return;
        }
        //默认不进入，只有请求到数据才进入
        if (isSaveUserInfo) {
            RxActivityUtils.skipActivityAndFinish(mActivity, UserInfoActivity.class);
            return;
        }

        RxActivityUtils.skipActivityAndFinish(mActivity, MainActivity.class);
        RxLogUtils.i("启动时长：引导页结束");
    }

    private void goError(int code) {
        if (code == -99 || code == 6 || code == 9001 || code == -1) {//token验证失败
            RxActivityUtils.skipActivityAndFinish(mActivity, LoginRegisterActivity.class);
        } else {
            gotoMain(SPUtils.getString(SPKey.SP_UserInfo));
        }
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(APPReplacedReceiver);
        super.onDestroy();
    }

    /**
     * 重传在无网络情况下未上传的数据
     */
    private void initData() {
        getSystemConfig();
        fetchSystemTime();
    }

    /**
     * 获取系统时间
     */
    private void fetchSystemTime() {
        RxManager.getInstance().doNetSubscribe(NetManager.getSystemService()
                .getServerTime())
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("服务器时间：" + RxFormat.setFormatDate(Long.parseLong(s)) +
                                "当前时间：" + RxFormat.setFormatDate(System.currentTimeMillis()));
                    }
                });
    }

    /**
     * 获取app配置信息
     * android 9.0不能访问未加密的Http网页，所以网页都需要部署秤Https
     */
    private void getSystemConfig() {
        RxManager.getInstance().doNetSubscribe(
                NetManager.getSystemService().getSystemConfig())
                .compose(RxCache.getDefault().transformObservable("getSystemConfig", String.class,
                        CacheStrategy.firstCacheTimeout(Key.DAY_1)))
                .map(new CacheResult.MapFunc<>())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        List<SystemConfigBean> configBeans = JSON.parseObject(s, new TypeToken<List<SystemConfigBean>>() {
                        }.getType());
                        for (SystemConfigBean bean : configBeans) {
                            RxLogUtils.d("系统配置：" + bean);
                            switch (bean.getConfName()) {
                                case "find.detail.url"://发现模块详情地址
                                    ServiceAPI.Detail = bean.getConfValue();
                                    break;
                                case "find.addr.url"://发现模块地址
                                    ServiceAPI.FIND_Addr = bean.getConfValue();
                                    break;
                                case "terms.agreement.url"://免责声明协议条款URL
                                    ServiceAPI.Term_Service = bean.getConfValue();
                                    break;
                                case "share.inform.url"://查看报告URL
                                    ServiceAPI.SHARE_INFORM_URL = bean.getConfValue();
                                    break;
                                case "app.download.url"://app下载URL
                                    ServiceAPI.APP_DOWN_LOAD_URL = bean.getConfValue();
                                    break;
                                case "share.root.url"://分享消息URL
//                                    ServiceAPI.Detail = bean.getConfValue();
                                    break;
                                case "order.address"://订单地址
                                    ServiceAPI.Order_Url = bean.getConfValue();
                                    break;
                                case "shpping.address"://商城地址
                                    ServiceAPI.Shopping_Address = bean.getConfValue();
                                    break;
                                case "mall.address"://购物车地址
                                    ServiceAPI.Store_Addr = bean.getConfValue();
                                    break;
                                case "recommend.html"://推荐食材图片
                                    ServiceAPI.RECIPES_URL = bean.getConfValue();
                                    break;
                                case "weight.ranking.tips"://推荐食材图片
                                    weightTipList = bean.getConfValue();
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
    }

}
