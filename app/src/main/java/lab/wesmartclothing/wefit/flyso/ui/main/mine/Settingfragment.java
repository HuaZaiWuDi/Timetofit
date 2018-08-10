package lab.wesmartclothing.wefit.flyso.ui.main.mine;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.qmuiteam.qmui.arch.QMUIFragment;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxFileUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseALocationActivity;
import lab.wesmartclothing.wefit.flyso.base.BaseAcFragment;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.ui.login.LoginRegisterActivity;
import lab.wesmartclothing.wefit.flyso.ui.login.VerificationPhoneActivity;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.netlib.net.RetrofitService;
import lab.wesmartclothing.wefit.netlib.rx.NetManager;
import lab.wesmartclothing.wefit.netlib.rx.RxManager;
import lab.wesmartclothing.wefit.netlib.rx.RxNetSubscriber;

/**
 * Created by jk on 2018/8/9.
 */
public class Settingfragment extends BaseAcFragment {

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.groupListView)
    QMUIGroupListView mGroupListView;
    @BindView(R.id.tv_logout)
    TextView mTvLogout;
    Unbinder unbinder;


    public static QMUIFragment getInstance() {
        return new Settingfragment();
    }

    @Override
    protected View onCreateView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fragment_setting, null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        initTopBar();
        groupList();

    }

    private void groupList() {
        QMUICommonListItemView changePasswordItem = mGroupListView.createItemView("修改密码");
        changePasswordItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        changePasswordItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        changePasswordItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        final QMUICommonListItemView clearCacheItem = mGroupListView.createItemView("清除缓存");
        clearCacheItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        clearCacheItem.setDetailText(RxFileUtils.getTotalCacheSize(mContext.getApplicationContext()));
        clearCacheItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        clearCacheItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        QMUICommonListItemView accountItem = mGroupListView.createItemView("账号管理");
        accountItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        accountItem.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        accountItem.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));

        QMUIGroupListView.newSection(mContext)
                .addItemView(changePasswordItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RxActivityUtils.skipActivityAndFinishAll(mActivity, VerificationPhoneActivity.class);
                    }
                })
                .addItemView(clearCacheItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final RxDialogSureCancel dialog = new RxDialogSureCancel(mActivity);
                        dialog.getTvTitle().setVisibility(View.GONE);
                        dialog.getTvContent().setText("清除缓存？");
                        dialog.setCancel("清除");
                        dialog.setCancelListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                RxFileUtils.clearAllCache(mContext.getApplicationContext());
                                clearCacheItem.setDetailText("0MB");
                            }
                        });
                        dialog.setSure("取消");
                        dialog.setSureListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        dialog.show();
                    }
                })
                .addItemView(accountItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startFragment(AccountFragment.getInstance());
                    }
                })
                .setUseTitleViewForSectionSpace(false)
                .addTo(mGroupListView);

    }

    private void initTopBar() {
        mQMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mQMUIAppBarLayout.setTitle("设置");
    }

    @OnClick(R.id.tv_logout)
    public void onViewClicked() {
        final RxDialogSureCancel dialog = new RxDialogSureCancel(mActivity);
        dialog.getTvTitle().setVisibility(View.GONE);
        dialog.getTvContent().setText("退出登录？");
        dialog.setCancel("退出");
        dialog.setCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                logout();
            }
        });
        dialog.setSure("取消");
        dialog.setSureListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private void logout() {
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.logout())
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {

                        BaseALocationActivity.aMapLocation = null;
                        SPUtils.clear();
                        MyAPP.getACache().clear();
                        MyAPP.getRxCache().clear();
                        RxActivityUtils.skipActivityAndFinishAll(mActivity, LoginRegisterActivity.class);
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });
    }

}
