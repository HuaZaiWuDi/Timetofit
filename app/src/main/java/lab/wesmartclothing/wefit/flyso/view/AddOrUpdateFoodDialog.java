package lab.wesmartclothing.wefit.flyso.view;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundRelativeLayout;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.view.RxToast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.entity.AddedHeatInfo;
import lab.wesmartclothing.wefit.flyso.entity.FoodListBean;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.netlib.net.RetrofitService;
import lab.wesmartclothing.wefit.netlib.rx.NetManager;
import lab.wesmartclothing.wefit.netlib.rx.RxManager;
import lab.wesmartclothing.wefit.netlib.rx.RxNetSubscriber;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created icon_hide_password jk on 2018/5/14.
 */
public class AddOrUpdateFoodDialog {

    @BindView(R.id.tv_delete)
    ImageView mTvDelete;
    @BindView(R.id.img_food)
    QMUIRadiusImageView mImgFood;
    @BindView(R.id.tv_foodName)
    TextView mTvFoodName;
    @BindView(R.id.tv_heat)
    TextView mTvHeat;
    @BindView(R.id.tv_info)
    TextView mTvInfo;
    @BindView(R.id.et_food_g)
    EditText mEtFoodG;
    @BindView(R.id.layout_Text)
    QMUIRoundRelativeLayout mLayoutText;
    @BindView(R.id.tv_unit)
    TextView mTvUnit;
    @BindView(R.id.cancel)
    TextView mCancel;
    @BindView(R.id.ok)
    TextView mOk;

    private Context mContext;
    private boolean showDelete;//是否显示删除按钮
    private AddOrUpdateFoodListener addOrUpdateFoodListener;
    private DeleteFoodListener mDeleteFoodListener;
    private AlertDialog dialog;
    private FoodListBean listBean;
    private int foodType;
    private long currentTime;

    public void setFoodInfo(Context context, boolean showDelete, int foodType, long currentTime, FoodListBean listBean) {
        mContext = context;
        View view = View.inflate(mContext, R.layout.dialogfragment_add_food, null);
        ButterKnife.bind(this, view);
        dialog = new AlertDialog.Builder(mContext)
                .setView(view).create();

        this.showDelete = showDelete;
        this.listBean = listBean;
        this.foodType = foodType;
        this.currentTime = currentTime;
        getAddedHeatInfo();//icon_add_white：先请求是否添加过，未添加吧foodId赋值给Gid，添加过直接上传Gid
    }


    private void showAddFoodDialog() {
        loadCricle(listBean.getFoodImg(), mImgFood);
        if (!showDelete)
            mTvDelete.setVisibility(View.INVISIBLE);

        mTvFoodName.setText(listBean.getFoodName());
        mTvUnit.setText(listBean.getUnit());

        if (listBean.getFoodCount() != 0)
            mEtFoodG.setText(listBean.getFoodCount() + "");

        mTvHeat.setText(listBean.getUnitCalorie() + "kcal/" + listBean.getUnitCount() + listBean.getUnit());

        dialog.show();
    }


    private void getAddedHeatInfo() {
        AddedHeatInfo heatInfo = new AddedHeatInfo();
        listBean2Added(listBean, heatInfo);

        String s = new Gson().toJson(heatInfo, AddedHeatInfo.class);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), s);
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.getAddedHeatInfo(body))
                .compose(RxComposeUtils.<String>showDialog(new TipDialog(mContext)))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("结束：" + s);
                        FoodListBean addedHeatInfo = new Gson().fromJson(s, FoodListBean.class);
                        String foodImg = listBean.getFoodImg();
                        listBean = addedHeatInfo;
                        listBean.setFoodImg(foodImg);
                        showAddFoodDialog();
//                        if ("".equals(addedHeatInfo.getGid())) {
//                            showAddFoodDialog();
//                            listBean.setFoodId(listBean.getGid());
//                        } else {
//                            listBean.setFoodCount(addedHeatInfo.getFoodCount());//已经添加过，把后台的数量赋值给当前展示
//                            listBean.setGid(addedHeatInfo.getGid());
//                            showAddFoodDialog();
//                        }
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });
    }

    private void listBean2Added(FoodListBean item, AddedHeatInfo heatInfo) {
        heatInfo.setCalorie(item.getCalorie());
        heatInfo.setEatType(foodType);
        heatInfo.setFoodCount(item.getFoodCount());
        heatInfo.setFoodName(item.getFoodName());
        heatInfo.setUnit(item.getUnit());
        heatInfo.setHeatDate(currentTime);
        heatInfo.setRemark(item.getRemark());
        heatInfo.setFoodId(RxDataUtils.isNullString(item.getFoodId()) ? item.getGid() : item.getFoodId());
        heatInfo.setGid(RxDataUtils.isNullString(item.getGid()) ? item.getFoodId() : item.getGid());
    }


    @OnClick({R.id.tv_delete, R.id.cancel, R.id.ok})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_delete:
                deleteData(mContext, listBean);
                break;
            case R.id.cancel:
                dialog.dismiss();
                break;
            case R.id.ok:
                dialog.dismiss();
                String foodCount = mEtFoodG.getText().toString();
                if (!"0".equals(foodCount) && !RxDataUtils.isNullString(foodCount)) {
                    int count = Integer.parseInt(foodCount);
                    listBean.setCalorie(listBean.getUnitCalorie() / listBean.getUnitCount() * count);
                    listBean.setFoodCount(count);
                } else {
                    RxToast.warning(mContext.getString(R.string.weightNoZero));
                    return;
                }
                addOrUpdateFoodListener.complete(listBean);
                break;
        }
    }


    public int isExist(List<FoodListBean> addedLists, FoodListBean needFood) {
        for (int i = 0; i < addedLists.size(); i++) {
            if (addedLists.get(i).getFoodId().equals(needFood.getFoodId())) {
                return i;
            }
        }
        return -1;
    }


    public interface AddOrUpdateFoodListener {
        void complete(FoodListBean listBean);
    }

    public interface DeleteFoodListener {
        void deleteFood(FoodListBean listBean);
    }

    public void setDeleteFoodListener(DeleteFoodListener deleteFoodListener) {
        mDeleteFoodListener = deleteFoodListener;
    }


    public void setAddOrUpdateFoodListener(AddOrUpdateFoodListener addOrUpdateFoodListener) {
        this.addOrUpdateFoodListener = addOrUpdateFoodListener;
    }

    public void loadCricle(String img_url, @NonNull ImageView img) {
        Glide.with(mContext)
                .load(img_url)
                .error(R.mipmap.group15)
                .bitmapTransform(new CropCircleTransformation(mContext))//圆角图片
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(img);
    }

    public void deleteData(Context context, final FoodListBean listBean) {
        JsonObject object = new JsonObject();
        object.addProperty("gid", listBean.getGid());

        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), object.toString());
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.removeHeatInfo(body))
                .compose(RxComposeUtils.<String>showDialog(new TipDialog(context)))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("结束：" + s);
                        if (mDeleteFoodListener != null)
                            mDeleteFoodListener.deleteFood(listBean);
                        if (dialog != null)
                            dialog.dismiss();
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }

                });
    }

}
