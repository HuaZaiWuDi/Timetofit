package lab.wesmartclothing.wefit.flyso.ui.main.mine;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxAnimationUtils;
import com.vondear.rxtools.utils.RxKeyboardUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxRegUtils;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.layout.RxTextView;

import butterknife.BindView;
import butterknife.OnClick;
import lab.wesmartclothing.wefit.flyso.BuildConfig;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.UserInfo;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.ui.main.MainActivity;
import lab.wesmartclothing.wefit.flyso.ui.userinfo.UserInfoActivity;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;

public class InvitationCodeActivity extends BaseActivity {

    public static final String REGEX_CODE = "^[A-Za-z0-9]{4,10}$";

    @BindView(R.id.tv_invitation)
    TextView mTvInvitation;
    @BindView(R.id.edit_invitation)
    EditText mEditInvitation;
    @BindView(R.id.tv_start)
    RxTextView mTvStart;
    @BindView(R.id.layout_edit)
    LinearLayout mLinearLayoutEdit;

    private UserInfo mUserInfo = MyAPP.getgUserInfo();


    @Override
    protected int layoutId() {
        return R.layout.activity_invitation_code;
    }

    @Override
    protected void initNetData() {
        super.initNetData();
    }

    @Override
    protected int statusBarColor() {
        return ContextCompat.getColor(mContext, R.color.white);
    }

    @Override
    protected void initViews() {
        super.initViews();

        mEditInvitation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                RxLogUtils.e("邀请码：" + s);
                if (RxRegUtils.isMatch(REGEX_CODE, s.toString())) {
                    mTvStart.getHelper().setBackgroundColorNormal(ContextCompat.getColor(mContext, R.color.red));
                    mTvStart.setEnabled(true);
                } else {
                    mTvStart.getHelper().setBackgroundColorNormal(ContextCompat.getColor(mContext, R.color.BrightGray));
                    mTvStart.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        RxKeyboardUtils.registerSoftInputChangedListener(mActivity, height -> {
            RxLogUtils.e("软键盘高度：" + height);
            if (height < 300) {
                RxAnimationUtils.animateMaginTop(RxUtils.dp2px(16), RxUtils.dp2px(100), mLinearLayoutEdit);
            } else {
                RxAnimationUtils.animateMaginTop(RxUtils.dp2px(100), RxUtils.dp2px(16), mLinearLayoutEdit);
            }
        });

        if (BuildConfig.DEBUG) {
            mEditInvitation.setText("a012");
        }
    }

    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
    }


    @Override
    protected void onDestroy() {
        RxKeyboardUtils.unregisterSoftInputChangedListener(mActivity);
        super.onDestroy();
    }

    @OnClick(R.id.tv_start)
    public void onViewClicked() {
        String invitation = mEditInvitation.getText().toString();
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService()
                .verifyInvitationCode(invitation))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d(s);
                        if (Boolean.parseBoolean(s)) {
                            if (mUserInfo != null && mUserInfo.getSex() == 0) {
                                RxActivityUtils.skipActivityAndFinish(mActivity, UserInfoActivity.class);
                            } else {
                                RxActivityUtils.skipActivity(mActivity, MainActivity.class);
                            }
                        } else {
                            RxToast.normal(getString(R.string.incitationInvalid));
//                            RxToast.showToast("邀请码无效，请正确输入有效邀请码");
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
