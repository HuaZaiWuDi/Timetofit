package lab.wesmartclothing.wefit.flyso.ui.main.slimming.weight;

import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxFormatValue;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.dateUtils.RxFormat;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;
import com.wesmarclothing.mylibrary.net.RxBus;
import com.yolanda.health.qnblesdk.out.QNScaleData;
import com.yolanda.health.qnblesdk.out.QNScaleItemData;
import com.yolanda.health.qnblesdk.out.QNScaleStoreData;

import java.util.List;

import butterknife.BindView;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.HealthyInfoBean;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.rxbus.RefreshSlimming;
import lab.wesmartclothing.wefit.flyso.service.BleService;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.flyso.utils.WeightTools;


public class WeightDataActivity extends BaseActivity {

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar QMUIAppBarLayout;
    @BindView(R.id.tv_receive)
    TextView tv_receive;
    @BindView(R.id.mRecycler_Receive)
    RecyclerView mRecycler_Receive;

    private BaseQuickAdapter adapter_Receive;


    @Override
    protected int layoutId() {
        return R.layout.activity_weight_data;
    }

    @Override
    protected void initViews() {
        super.initViews();
        initTopBar();
        initRecyclerView();
        initData();
    }


    @Override
    protected int statusBarColor() {
        return ContextCompat.getColor(mContext, R.color.white);
    }

    @Override
    protected void initNetData() {
        super.initNetData();

    }

    private void initTopBar() {
        QMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter_Receive.getItemCount() > 0)
                    showDialog();
                else RxActivityUtils.finishActivity();
            }
        });
        QMUIAppBarLayout.setTitle(R.string.weightData);
    }


    private void initRecyclerView() {
        final Typeface typeface = MyAPP.typeface;
        mRecycler_Receive.setLayoutManager(new LinearLayoutManager(mContext));

        adapter_Receive = new BaseQuickAdapter<QNScaleStoreData, BaseViewHolder>(R.layout.item_weight_data) {
            @Override
            protected void convert(BaseViewHolder helper, QNScaleStoreData item) {

                helper.setText(R.id.tv_weight, RxFormatValue.fromat4S5R(item.getWeight(), 1))
                        .setTypeface(R.id.tv_weight, typeface)
                        .setText(R.id.tv_date, getString(R.string.measureTime) + RxFormat.setFormatDateG8(item.getMeasureTime(), "yyyy年MM月dd日 HH:mm"))
                        .setVisible(R.id.btn_new, helper.getAdapterPosition() == 0)
                        .addOnClickListener(R.id.btn_receive);
            }
        };

        adapter_Receive.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                RxLogUtils.d("点击了领取");
                addWeightData(position);
            }
        });

        mRecycler_Receive.setAdapter(adapter_Receive);
    }


    private void initData() {
        List<QNScaleStoreData> weightData = BleService.historyWeightData;
        if (RxDataUtils.isEmpty(weightData)) return;
        adapter_Receive.setNewData(weightData);

        tv_receive.setText(getString(R.string.receivedCount, "待", weightData.size()));
    }


    private void addWeightData(final int position) {
        final QNScaleStoreData qnScaleData = (QNScaleStoreData) adapter_Receive.getItem(position);
        if (RxDataUtils.isEmpty(qnScaleData)) return;
        HealthyInfoBean bean = new HealthyInfoBean();

        bean.setMeasureTime(qnScaleData.getMeasureTime().getTime());
        QNScaleData scaleData = qnScaleData.generateScaleData();
        if (scaleData != null)
            for (QNScaleItemData item : scaleData.getAllItem()) {
                WeightTools.ble2Backstage(item, bean);
            }
        bean.setWeight(qnScaleData.getWeight());
        String s = JSON.toJSONString(bean);

        RxManager.getInstance().doNetSubscribe(NetManager.getApiService()
                .addWeightInfo(NetManager.fetchRequest(s)))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("添加体重：" + s);
                        adapter_Receive.remove(position);
                        tv_receive.setText(getString(R.string.receivedCount, "待", adapter_Receive.getItemCount()));

                        //刷新数据
                        RxBus.getInstance().post(new RefreshSlimming());

                    }

                    @Override
                    protected void _onError(String error, int code) {
                        RxToast.normal(error, code);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        BleService.historyWeightData = null;
        super.onDestroy();
    }

    //监听返回按钮，提示如果返回，体重数据将清除
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            if (adapter_Receive.getItemCount() > 0)
                showDialog();
            else RxActivityUtils.finishActivity();
        return true;
    }


    private void showDialog() {
        new RxDialogSureCancel(mContext)
                .setCancelBgColor(ContextCompat.getColor(mContext, R.color.GrayWrite))
                .setSureBgColor(ContextCompat.getColor(mContext, R.color.green_61D97F))
                .setContent(getString(R.string.weightNotReceiver))
                .setCancel(getString(R.string.btn_leave))
                .setCancelListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RxActivityUtils.finishActivity();
                    }
                })
                .setSure(getString(R.string.btn_continue))
                .show();
    }

}
