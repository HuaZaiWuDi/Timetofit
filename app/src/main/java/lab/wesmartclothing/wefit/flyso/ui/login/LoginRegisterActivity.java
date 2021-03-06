package lab.wesmartclothing.wefit.flyso.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;
import android.widget.ImageView;

import com.flyco.tablayout.CommonTabLayout;
import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButtonDrawable;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxAnimationUtils;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxEncryptUtils;
import com.vondear.rxtools.utils.RxKeyboardUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxRegUtils;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.utils.StatusBarUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSure;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.vondear.rxtools.view.viewpager.UnScrollableViewPager;
import com.wesmarclothing.mylibrary.net.RxBus;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.entity.BottomTabItem;
import lab.wesmartclothing.wefit.flyso.entity.LoginResult;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxSubscriber;
import lab.wesmartclothing.wefit.flyso.rxbus.PasswordLoginBus;
import lab.wesmartclothing.wefit.flyso.rxbus.VCodeBus;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.utils.LoginSuccessUtils;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;

public class LoginRegisterActivity extends BaseActivity {

    @BindView(R.id.mCommonTabLayout)
    CommonTabLayout mCommonTabLayout;
    @BindView(R.id.mViewPager)
    UnScrollableViewPager mViewPager;
    @BindView(R.id.btn_login)
    QMUIRoundButton btn_login;
    @BindView(R.id.img_wexin)
    ImageView mImgWexin;
    @BindView(R.id.img_qq)
    ImageView mImgQq;
    @BindView(R.id.img_weibo)
    ImageView mImgWeibo;
    @BindView(R.id.v_emptyLayout)
    View mVEmptyLayout;

    private ArrayList<Fragment> mFragments = new ArrayList<>();
    private ArrayList<CustomTabEntity> mBottomTabItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);
        ButterKnife.bind(this);
        StatusBarUtils.from(mActivity)
                .setStatusBarColor(getResources().getColor(R.color.white))
                .setLightStatusBar(false)
                .process();
        initView();
    }


    @OnClick({R.id.img_wexin,
            R.id.img_qq,
            R.id.img_weibo
    })
    public void onViewClicked(View view) {

        SHARE_MEDIA media = SHARE_MEDIA.QQ;
        switch (view.getId()) {
            case R.id.img_wexin:
                media = SHARE_MEDIA.WEIXIN;
                break;
            case R.id.img_qq:
                media = SHARE_MEDIA.QQ;
                break;
            case R.id.img_weibo:
                media = SHARE_MEDIA.SINA;
                break;
        }

        UMShareAPI umShareAPI = UMShareAPI.get(this);
        umShareAPI.getPlatformInfo(this, media, mUMAuthListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UMShareAPI.get(this).release();
    }


    private UMAuthListener mUMAuthListener = new UMAuthListener() {
        @Override
        public void onStart(SHARE_MEDIA share_media) {
            RxLogUtils.d("开始登录");
            tipDialog.show(getString(R.string.logining), 3000);
        }

        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            RxLogUtils.d("login:onComplete: ");
            tipDialog.dismiss();
            Set<String> strings = map.keySet();
            for (String s : strings) {
                RxLogUtils.d("s: " + s + "--value" + map.get(s));
            }

            loginOther(new LoginResult(map, share_media));
        }

        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
            RxLogUtils.e("登录失败", throwable);
            RxToast.error(getString(R.string.loginFail));
            tipDialog.dismiss();
        }

        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {
            RxLogUtils.e("登录取消");
            RxToast.error(getString(R.string.loginCancel));
            tipDialog.dismiss();
        }
    };


    @Override
    protected void onStop() {
        tipDialog.dismiss();
        super.onStop();
    }


    public void initView() {
        initTab();
        initRxBus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RxKeyboardUtils.registerSoftInputChangedListener(mActivity, new RxKeyboardUtils.OnSoftInputChangedListener() {
            @Override
            public void onSoftInputChanged(int height) {
                RxLogUtils.e("软键盘高度：" + height);
                if (height < 300) {
                    RxAnimationUtils.animateHeight(0, RxUtils.dp2px(50), mVEmptyLayout);
                } else {
                    RxAnimationUtils.animateHeight(RxUtils.dp2px(50), 0, mVEmptyLayout);
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        RxKeyboardUtils.unregisterSoftInputChangedListener(mActivity);
    }

    private void initRxBus() {
        RxBus.getInstance().register2(PasswordLoginBus.class)
                .compose(RxComposeUtils.<PasswordLoginBus>bindLife(lifecycleSubject))
                .subscribe(new RxSubscriber<PasswordLoginBus>() {
                    @Override
                    protected void _onNext(PasswordLoginBus passwordLoginBus) {
                        checkRegister(passwordLoginBus.phone, passwordLoginBus.password);
                    }
                });
        RxBus.getInstance().register2(VCodeBus.class)
                .compose(RxComposeUtils.<VCodeBus>bindLife(lifecycleSubject))
                .subscribe(new RxSubscriber<VCodeBus>() {
                    @Override
                    protected void _onNext(VCodeBus vCodeBus) {
                        checkRegister(vCodeBus.phone, vCodeBus.vCode);
                    }
                });
    }


    private void checkRegister(final String phone, final String p2) {
        if ((RxRegUtils.isMobileSimple(phone) || RxRegUtils.isEmail(phone)) && !RxDataUtils.isNullString(p2)) {
            ((QMUIRoundButtonDrawable) btn_login.getBackground()).setColor(getResources().getColor(R.color.red));
            btn_login.setEnabled(true);
        } else {
            ((QMUIRoundButtonDrawable) btn_login.getBackground()).setColor(getResources().getColor(R.color.BrightGray));
            btn_login.setEnabled(false);
        }
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCommonTabLayout.getCurrentTab() == 0) {
                    loginPassword(phone, p2);
                } else {
                    loginVCode(phone, p2);
                }
            }
        });
    }

    private void initTab() {
        mFragments.clear();
        mFragments.add(PasswordLoginFragment.getInstance());
        mFragments.add(VCodeLoginFragment.getInstance());

        mViewPager.setAdapter(new FragmentPagerAdapter(this.getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        });

        mBottomTabItems.add(new BottomTabItem(getString(R.string.loginPassword)));
        mBottomTabItems.add(new BottomTabItem(getString(R.string.loginVCode)));
        mCommonTabLayout.setTabData(mBottomTabItems);
        mCommonTabLayout.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                mViewPager.setCurrentItem(position);
            }

            @Override
            public void onTabReselect(int position) {

            }
        });
    }


    private void loginVCode(String phone, String code) {
        if (!RxRegUtils.isMobileSimple(phone) && !RxRegUtils.isEmail(phone)) {
            RxToast.warning(getString(R.string.phoneError));
            return;
        }
        if (!RxRegUtils.isVCode(code)) {
            RxToast.warning(getString(R.string.VCodeError));
            return;
        }
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().login(phone, code))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxToast.success(getString(R.string.loginSuccess));
                        RxLogUtils.d("结束：" + s);
                        new LoginSuccessUtils(mContext, s);
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        super._onError(error, code);
                        RxToast.normal(error);
                    }
                });
    }


    private void loginPassword(String phone, String password) {
        if (!RxRegUtils.isMobileSimple(phone) && !RxRegUtils.isEmail(phone)) {
            RxToast.warning(getString(R.string.phoneError));
            return;
        }
        if (!RxRegUtils.isPassword(password)) {
            RxToast.warning(getString(R.string.passwordError));
            return;
        }
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().pwdLogin(phone, RxEncryptUtils.encryptMD5ToString(password)))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxToast.success(getString(R.string.loginSuccess));
                        new LoginSuccessUtils(mContext, s);
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        if (code == 10021) {//未设置登录密码
                            showDialog2settingPassword();
                        } else {
                            RxToast.error(error);
                        }
                    }

                });
    }

    private void showDialog2Register() {
        RxDialogSure dialog = new RxDialogSure(mActivity)
                .setTitle(getString(R.string.notResigter))
                .setSure(getString(R.string.register))
                .setSureListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //进入设置注册流程，跳转注册界面
                        RxActivityUtils.skipActivity(mActivity, RegisterActivity.class);
                    }
                });
        dialog.show();
    }

    private void showDialog2settingPassword() {
        RxDialogSureCancel rxDialog = new RxDialogSureCancel(mContext)
                .setContent(getString(R.string.notPassword))
                .setSure(getString(R.string.setPassword))
                .setSureListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //进入设置密码流程，跳转验证手机号
                        RxActivityUtils.skipActivity(mActivity, VerificationPhoneActivity.class);
                    }
                })
                .setCancel(getString(R.string.loginVCode))
                .setCancelListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCommonTabLayout.setCurrentTab(1);
                        mViewPager.setCurrentItem(1);
                    }
                });
        rxDialog.show();
    }

    private void loginOther(final LoginResult result) {
        if (result == null) return;
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService()
                .outerLogin(result.openId, result.nickname, result.imageUrl, result.userType))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        JsonParser parser = new JsonParser();
                        JsonObject object = (JsonObject) parser.parse(s);
                        if (object.has("success")) {
                            boolean success = object.get("success").getAsBoolean();
                            if (!success) {
                                //!success意味着没有绑定手机号码需要跳转到绑定手机号界面
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(Key.BUNDLE_OTHER_LOGIN_INFO, result);
                                RxActivityUtils.skipActivity(mContext, BindPhoneActivity.class, bundle);
                            } else
                                new LoginSuccessUtils(mContext, s);
                        }
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        super._onError(error, code);
                        RxToast.normal(error);
                    }
                });
    }


}
