package lab.wesmartclothing.wefit.flyso.ui.main.slimming.heat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.dateUtils.RxFormat;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxTextUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.AddFoodItem;
import lab.wesmartclothing.wefit.flyso.entity.FetchHeatInfoBean;
import lab.wesmartclothing.wefit.flyso.entity.FoodListBean;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.flyso.view.AddOrUpdateFoodDialog;
import okhttp3.RequestBody;

/**
 * Created by jk on 2018/8/3.
 */
public class AddedFoodFragment extends BaseActivity {


    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.mRecyclerView)
    RecyclerView mMRecyclerView;
    @BindView(R.id.tv_addedNoData)
    TextView mTvAddedNoData;
    Unbinder unbinder;


    private int foodType = 0;
    private BaseQuickAdapter adapter;
    private Bundle bundle;
    private AddOrUpdateFoodDialog dialog = new AddOrUpdateFoodDialog();
    private long currentTime = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_added_food);
        ButterKnife.bind(this);
        initView();
    }


    public void initView() {
        initRecyclerView();
        initData();
        initTopBar();
        dialog.setLifecycleSubject(lifecycleSubject);
        dialog.setDeleteFoodListener(listBean -> {
            int exist = dialog.isExist(adapter.getData(), listBean);
            RxLogUtils.d("下标：" + exist);
            if (exist >= 0)
                adapter.remove(exist);
            mTvAddedNoData.setVisibility(adapter.getData().isEmpty() ? View.VISIBLE : View.GONE);
        });
        dialog.setAddOrUpdateFoodListener(listBean -> {
            int exist = dialog.isExist(adapter.getData(), listBean);
            if (exist >= 0) {
                adapter.setData(exist, listBean);
                updateFood(listBean);
            }
        });
    }

    private void initData() {
        bundle = getIntent().getExtras();
        if (bundle != null) {
            foodType = bundle.getInt(Key.ADD_FOOD_TYPE);
            currentTime = bundle.getLong(Key.ADD_FOOD_DATE);
            String heatData = bundle.getString(Key.ADD_FOOD_INFO);
            FetchHeatInfoBean bean = JSON.parseObject(heatData, FetchHeatInfoBean.class);
            RxLogUtils.d("加载食材：" + bean);
            if (bean != null) {
                List<FoodListBean> list = null;
                switch (foodType) {
                    case Key.TYPE_BREAKFAST:
                        list = bean.getBreakfast().getFoodList();
                        break;
                    case Key.TYPE_LUNCH:
                        list = bean.getLunch().getFoodList();
                        break;
                    case Key.TYPE_DINNER:
                        list = bean.getDinner().getFoodList();
                        break;
                    case Key.TYPED_MEAL:
                        list = bean.getSnacks().getFoodList();
                        break;
                }
                adapter.setNewData(list);
            }

            if (FoodDetailsFragment.addedLists.size() > 0) {
                if (adapter.getData().isEmpty()) {
                    adapter.setNewData(FoodDetailsFragment.addedLists);
                } else
                    adapter.addData(FoodDetailsFragment.addedLists);
            }

            mTvAddedNoData.setVisibility(adapter.getData().isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void initRecyclerView() {
        mMRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adapter = new BaseQuickAdapter<FoodListBean, BaseViewHolder>(R.layout.item_add_food) {
            @Override
            protected void convert(BaseViewHolder helper, FoodListBean item) {
                QMUIRadiusImageView foodImg = helper.getView(R.id.iv_foodImg);

                MyAPP.getImageLoader().displayImage(mActivity, item.getFoodImg(), foodImg);

                helper.setText(R.id.tv_foodName, item.getFoodName());
                TextView foodKcal = helper.getView(R.id.tv_foodKcal);
                RxTextUtils.getBuilder(item.getCalorie() + "")
                        .append("kcal/")
                        .setProportion(0.6f)
                        .setForegroundColor(getResources().getColor(R.color.orange_FF7200))
                        .append(RxFormat.setFormatNum(item.getFoodCount(), "0.0") + item.getUnit())
                        .setProportion(0.6f)
                        .setForegroundColor(getResources().getColor(R.color.GrayWrite))
                        .into(foodKcal);
            }
        };
        View footerView = View.inflate(mContext, R.layout.footer_added_food, null);
        footerView.findViewById(R.id.layout_addFood).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转添加食物
                RxActivityUtils.skipActivity(mActivity, FoodDetailsFragment.class, bundle);
            }
        });
        adapter.addFooterView(footerView);
        mMRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                dialog.setFoodInfo(mContext, true, foodType, currentTime, (FoodListBean) adapter.getData().get(position));
            }
        });
        adapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final BaseQuickAdapter adapter, View view, final int position) {
                RxDialogSureCancel rxDialog = new RxDialogSureCancel(mContext)
                        .setCancelBgColor(ContextCompat.getColor(mContext, R.color.GrayWrite))
                        .setSureBgColor(ContextCompat.getColor(mContext, R.color.green_61D97F))
                        .setContent(getString(R.string.deleteRecord))
                        .setSureListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.deleteData(mContext, ((FoodListBean) adapter.getItem(position)));
                            }
                        });
                rxDialog.show();
                return true;
            }
        });
    }


    private void initTopBar() {
        final String[] add_food = getResources().getStringArray(R.array.add_food);

        mQMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxActivityUtils.finishActivity();
            }
        });
        mQMUIAppBarLayout.setTitle(add_food[FoodDetailsFragment.FOOD_TYPE(foodType)].substring(2, 4));
        mQMUIAppBarLayout.addRightImageButton(R.mipmap.icon_search, R.id.id_search)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RxActivityUtils.skipActivity(mActivity, SearchHistoryFragment.class, bundle);
                    }
                });
    }

    private void updateFood(FoodListBean foodListBean) {
        AddFoodItem.intakeList intakeList = new AddFoodItem.intakeList();
        intakeList.setFoodId(foodListBean.getFoodId());
        intakeList.setFoodName(foodListBean.getFoodName());
        intakeList.setFoodCount(foodListBean.getFoodCount());
        intakeList.setUnit(foodListBean.getUnit());
        intakeList.setGid(foodListBean.getGid());
        intakeList.setUnitCount(foodListBean.getUnitCount());
        intakeList.setFoodImg(foodListBean.getFoodImg());
        intakeList.setRemark(foodListBean.getRemark());
        intakeList.setCalorie(foodListBean.getCalorie());
        intakeList.setHeatDate(currentTime);
        intakeList.setUnitCalorie(foodListBean.getUnitCalorie());

        AddFoodItem foodItem = new AddFoodItem();
        foodItem.setAddDate(currentTime);
        foodItem.setEatType(foodType);
        ArrayList<AddFoodItem.intakeList> lists = new ArrayList<>();
        lists.add(intakeList);
        foodItem.setIntakeLists(lists);
        String s = JSON.toJSONString(foodItem);


        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), s);
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().addHeatInfo(body))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("修改食物成功");
                    }

                    @Override
                    protected void _onError(String error, int code) {
                        RxToast.error(error, code);
                    }
                });
    }

}
