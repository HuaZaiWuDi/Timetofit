package lab.wesmartclothing.wefit.flyso.ui.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButtonDrawable;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxRegUtils;
import com.vondear.rxtools.utils.RxTextUtils;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.utils.StatusBarUtils;
import com.vondear.rxtools.view.RxToast;
import com.wesmarclothing.mylibrary.net.RxBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import lab.wesmartclothing.wefit.flyso.BuildConfig;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.entity.LoginResult;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.ServiceAPI;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.rxbus.VCodeBus;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.base.WebTitleActivity;
import lab.wesmartclothing.wefit.flyso.utils.LoginSuccessUtils;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;

public class BindPhoneActivity extends BaseActivity {
    private String phone;
    private String vCode;

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.edit_phone)
    EditText mEditPhone;
    @BindView(R.id.edit_Vcode)
    EditText mEditVcode;
    @BindView(R.id.btn_getVCode)
    QMUIRoundButton mBtnGetVCode;
    @BindView(R.id.btn_register)
    QMUIRoundButton mBtnBind;
    @BindView(R.id.tv_clause)
    TextView mTvClause;

    @OnClick(R.id.tv_clause)
    void tv_clause() {
        //服务协议
        WebTitleActivity.startWebActivity(mActivity, getString(R.string.ServiceAgreement), ServiceAPI.Term_Service);

    }

    @OnClick(R.id.btn_register)
    void btnBindPhone() {
        bindPhone();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_phone);
        ButterKnife.bind(this);
        StatusBarUtils.from(mActivity)
                .setStatusBarColor(getResources().getColor(R.color.white))
                .setLightStatusBar(true)
                .process();
        initView();
    }

    public void initView() {
        mQMUIAppBarLayout.addLeftImageButton(R.mipmap.icon_back, R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxActivityUtils.finishActivity();
            }
        });
        SpannableStringBuilder spannableStringBuilder = RxTextUtils.getBuilder(mTvClause.getText())
                .setForegroundColor(getResources().getColor(R.color.red))
                .setUnderline()
                .setLength(9);
        mTvClause.setText(spannableStringBuilder);

        switchEnable(false);

        mEditPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phone = s.toString();
                switchEnable(!RxDataUtils.isNullString(phone));
                RxBus.getInstance().post(new VCodeBus(phone, vCode));
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkBtn();
            }
        });
        mBtnGetVCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getVCode();
            }
        });
        mEditVcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vCode = s.toString();
                RxBus.getInstance().post(new VCodeBus(phone, vCode));
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkBtn();
            }
        });
    }

    private void switchEnable(boolean isEnable) {
        if (!mBtnGetVCode.getText().toString().equals(getString(R.string.getVCode))) return;
        mBtnGetVCode.setEnabled(isEnable);
        mBtnGetVCode.setAlpha(isEnable ? 1f : 0.5f);
    }

    private void getVCode() {
        String phone = mEditPhone.getText().toString();
        if (!RxRegUtils.isMobileSimple(phone) && !RxRegUtils.isEmail(phone)) {
            RxToast.warning(getString(R.string.phoneError));
            return;
        }
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().sendCode(phone, null))
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        RxUtils.countDown(mBtnGetVCode, 60, 1, getString(R.string.getVCode));
                    }
                })
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("验证码：" + s);
                        if (BuildConfig.DEBUG)
                            mEditVcode.setText(s);
                        RxToast.success(getString(R.string.SMSSended));
                    }


                    @Override
                    protected void _onError(String error, int code) {
                        super._onError(error, code);
                        RxToast.normal(error);
                    }

                });
    }

    private void bindPhone() {
        final String phone = mEditPhone.getText().toString();
        String vCode = mEditVcode.getText().toString();
        if (!RxRegUtils.isMobileSimple(phone) && !RxRegUtils.isEmail(phone)) {
            RxToast.warning(getString(R.string.phoneError));
            return;
        }
        if (!RxRegUtils.isVCode(vCode)) {
            RxToast.warning(getString(R.string.VCodeError));
            return;
        }
        LoginResult result = (LoginResult) getIntent().getSerializableExtra(Key.BUNDLE_OTHER_LOGIN_INFO);
        if (result == null) return;
        RxLogUtils.e("登录信息：" + result.toString());
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().phoneBind(phone, vCode, result.openId, result.nickname, result.imageUrl, result.userType))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        new LoginSuccessUtils(mContext, s);
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        super._onError(error, code);
                        RxToast.normal(error);
                    }

                });
    }

    private void checkBtn() {
        if (!RxDataUtils.isNullString(phone) && !RxDataUtils.isNullString(vCode)) {
            ((QMUIRoundButtonDrawable) mBtnBind.getBackground()).setColor(getResources().getColor(R.color.red));
            mBtnBind.setEnabled(true);
        } else {
            ((QMUIRoundButtonDrawable) mBtnBind.getBackground()).setColor(getResources().getColor(R.color.BrightGray));
            mBtnBind.setEnabled(false);
        }
    }

}
