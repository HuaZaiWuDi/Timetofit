package lab.wesmartclothing.wefit.flyso.ui.main.mine;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxFileUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.zchu.rxcache.RxCache;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.ui.login.LoginRegisterActivity;
import lab.wesmartclothing.wefit.flyso.ui.login.VerificationPhoneActivity;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.flyso.utils.UserLifecycleManager;

/**
 * Created by jk on 2018/8/9.
 */
public class Settingfragment extends BaseActivity {

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.groupListView)
    QMUIGroupListView mGroupListView;
    @BindView(R.id.tv_logout)
    TextView mTvLogout;
    Unbinder unbinder;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_setting);
        unbinder = ButterKnife.bind(this);
        initView();
    }


    private void initView() {
        initTopBar();
        groupList();

    }

    private void groupList() {
        QMUICommonListItemView changePasswordItem = mGroupListView.createItemView(getString(R.string.updatePassword));
        changePasswordItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        changePasswordItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        changePasswordItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        final QMUICommonListItemView clearCacheItem = mGroupListView.createItemView(getString(R.string.cleanCache));
        clearCacheItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        clearCacheItem.setDetailText(RxFileUtils.getTotalCacheSize(mContext.getApplicationContext()));
        clearCacheItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        clearCacheItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        QMUICommonListItemView accountItem = mGroupListView.createItemView(getString(R.string.accountManager));
        accountItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        accountItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        accountItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        QMUICommonListItemView clothing = mGroupListView.createItemView("瘦身衣");
        clothing.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        clothing.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        clothing.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        QMUICommonListItemView customerService = mGroupListView.createItemView("客服");
        customerService.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        customerService.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        customerService.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));


        QMUIGroupListView.newSection(mContext)
                .addItemView(changePasswordItem, v ->
                        RxActivityUtils.skipActivity(mActivity, VerificationPhoneActivity.class))
                .addItemView(clearCacheItem, v -> new RxDialogSureCancel(mActivity)
                        .setTitle(getString(R.string.cleanCache) + "?")
                        .setSure(getString(R.string.clean))
                        .setSureListener(v1 -> {
                            RxFileUtils.clearAllCache(mContext.getApplicationContext());
                            try {
                                RxCache.getDefault().clear2();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            clearCacheItem.setDetailText("0MB");
                        }).show())
                .addItemView(accountItem, v ->
                        RxActivityUtils.skipActivity(mContext, AccountFragment.class))
//                .addItemView(clothing, new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
////                        RxActivityUtils.skipActivity(mContext, TempActivity.class);
//                    }
//                })
//                .addItemView(customerService, v -> {
//                    ChatManager.INSTANCE.register();
//                })
                .setUseTitleViewForSectionSpace(false)
                .addTo(mGroupListView);
    }

    private void initTopBar() {
        mQMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mQMUIAppBarLayout.setTitle(R.string.setting);
    }

    @OnClick(R.id.tv_logout)
    public void onViewClicked() {
        RxDialogSureCancel rxDialog = new RxDialogSureCancel(mContext)
                .setCancelBgColor(ContextCompat.getColor(mContext, R.color.GrayWrite))
                .setSureBgColor(ContextCompat.getColor(mContext, R.color.green_61D97F))
                .setTitle(getString(R.string.logout) + "?")
                .setSure(getString(R.string.signOut))
                .setSureListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logout();
                    }
                });
        rxDialog.show();
    }


    private void logout() {
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().logout())
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        UserLifecycleManager.INSTANCE.logout();
                        RxActivityUtils.skipActivityAndFinishAll(mActivity, LoginRegisterActivity.class);

                    }

                    @Override
                    protected void _onError(String error, int code) {
                        RxToast.normal(error, code);
                    }
                });
    }

}
