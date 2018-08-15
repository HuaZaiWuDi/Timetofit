package lab.wesmartclothing.wefit.flyso.ui.main.mine;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.imagepicker.view.CropImageView;
import com.qmuiteam.qmui.arch.QMUIFragment;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.vondear.rxtools.dateUtils.RxFormat;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.view.RxToast;
import com.vondear.rxtools.view.dialog.RxDialogSureCancel;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cn.qqtheme.framework.entity.City;
import cn.qqtheme.framework.entity.County;
import cn.qqtheme.framework.entity.Province;
import cn.qqtheme.framework.picker.DatePicker;
import cn.qqtheme.framework.picker.NumberPicker;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseAcFragment;
import lab.wesmartclothing.wefit.flyso.entity.UserInfo;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.flyso.ui.main.slimming.weight.TargetDetailsFragment;
import lab.wesmartclothing.wefit.flyso.utils.PicassoImageLoader;
import lab.wesmartclothing.wefit.flyso.utils.RxComposeUtils;
import lab.wesmartclothing.wefit.flyso.view.picker.AddressPickTask;
import lab.wesmartclothing.wefit.flyso.view.picker.CustomDatePicker;
import lab.wesmartclothing.wefit.flyso.view.picker.CustomNumberPicker;
import lab.wesmartclothing.wefit.netlib.net.RetrofitService;
import lab.wesmartclothing.wefit.netlib.rx.NetManager;
import lab.wesmartclothing.wefit.netlib.rx.RxManager;
import lab.wesmartclothing.wefit.netlib.rx.RxNetSubscriber;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by jk on 2018/8/9.
 */
public class UserInfofragment extends BaseAcFragment {

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.iv_userImg)
    QMUIRadiusImageView mIvUserImg;
    @BindView(R.id.layout_userImg)
    RelativeLayout mLayoutUserImg;
    @BindView(R.id.groupListView)
    QMUIGroupListView mGroupListView;
    Unbinder unbinder;

    private QMUICommonListItemView userNameItem, sexItem, birthItem, heightItem, weightItem, cityItem, signItem;
    private UserInfo info;
    public static final int RESULT_CODE = 500;
    public static final int REQUEST_CODE = 501;
    public static final int IMAGE_PICKER = 503;

    public static QMUIFragment getInstance() {
        return new UserInfofragment();
    }

    @Override
    protected View onCreateView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fragment_user_info, null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        initTopBar();
        groupList();
        initImagePicker();
        String string = SPUtils.getString(SPKey.SP_UserInfo);
        info = new Gson().fromJson(string, UserInfo.class);
        notifyData(info);
    }


    private void initImagePicker() {
        ImagePicker imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new PicassoImageLoader());   //设置图片加载器
        imagePicker.setShowCamera(true);//显示拍照按钮
        imagePicker.setMultiMode(false);
        imagePicker.setCrop(true);        //允许裁剪（单选才有效）
        imagePicker.setSaveRectangle(true); //是否按矩形区域保存
        imagePicker.setSelectLimit(1);    //选中数量限制
        imagePicker.setStyle(CropImageView.Style.RECTANGLE);  //裁剪框的形状
        imagePicker.setFocusWidth(800);   //裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        imagePicker.setFocusHeight(800);  //裁剪框的高度。单位像素（圆形自动取宽高最小值）
        imagePicker.setOutPutX(1000);//保存文件的宽度。单位像素
        imagePicker.setOutPutY(1000);//保存文件的高度。单位像素
    }

    private void initTopBar() {
        mQMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mQMUIAppBarLayout.setTitle("个人资料");
        mQMUIAppBarLayout.addRightTextButton("保存", R.id.btn_save)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestSaveUserInfo();
                    }
                });
    }

    private void notifyData(UserInfo info) {
        Glide.with(mActivity).load(info.getImgUrl())
                .asBitmap()
                .placeholder(R.mipmap.userimg)
                .into(mIvUserImg);
        userNameItem.setDetailText(info.getUserName());
        sexItem.setDetailText(info.getSex() == 1 ? "男" : "女");
        birthItem.setDetailText(RxFormat.setFormatDate(info.getBirthday(), RxFormat.Date));
        heightItem.setDetailText(info.getHeight() + "\tcm");
        weightItem.setDetailText(info.getTargetWeight() + "\tkg");
        if (!RxDataUtils.isNullString(info.getProvince()) && !RxDataUtils.isNullString(info.getCity()))
            cityItem.setDetailText(info.getProvince() + "," + info.getCity());
        if (!RxDataUtils.isNullString(info.getSignature()))
            signItem.setDetailText(info.getSignature());
    }

    private void groupList() {
        userNameItem = mGroupListView.createItemView("昵称");
        userNameItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        setItemView(userNameItem);

        sexItem = mGroupListView.createItemView("性别");
        sexItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        setItemView(sexItem);

        birthItem = mGroupListView.createItemView("生日");
        birthItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        setItemView(birthItem);

        heightItem = mGroupListView.createItemView("身高");
        heightItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        setItemView(heightItem);

        weightItem = mGroupListView.createItemView("目标体重");
        weightItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        setItemView(weightItem);

        cityItem = mGroupListView.createItemView("所在城市");
        cityItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        cityItem.setDetailText("未知");
        setItemView(cityItem);

        signItem = mGroupListView.createItemView("签名");
        signItem.setOrientation(QMUICommonListItemView.HORIZONTAL);
        signItem.setDetailText("签名限制20字，这边显示不全…");
        TextView textView = signItem.getDetailTextView();
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        setItemView(signItem);

        QMUIGroupListView.newSection(mActivity)
                .setUseTitleViewForSectionSpace(false)
                .addItemView(userNameItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putString(Key.BUNDLE_TITLE, "昵称");
                        bundle.putString(Key.BUNDLE_DATA, info.getUserName());
                        QMUIFragment instance = EditFragment.getInstance();
                        instance.setArguments(bundle);
                        startFragmentForResult(instance, UserInfofragment.REQUEST_CODE);
                    }
                })
                .addItemView(sexItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSex();
                    }
                })
                .addTo(mGroupListView);

        QMUIGroupListView.newSection(mActivity)
                .setUseTitleViewForSectionSpace(false)
                .addItemView(birthItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDate();
                    }
                })
                .addItemView(heightItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showHeight();
                    }
                })
                .addItemView(weightItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (info.getTargetWeight() >= 0) {
                            RxToast.normal("您还未录入初始体重\n请上称！！", 3000);
                            return;
                        }
                        startFragment(TargetDetailsFragment.getInstance());
                    }
                })
                .addTo(mGroupListView);

        QMUIGroupListView.newSection(mActivity)
                .setUseTitleViewForSectionSpace(false)
                .addItemView(cityItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAddress();
                    }
                })
                .addItemView(signItem, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle bundle = new Bundle();
                        bundle.putString(Key.BUNDLE_TITLE, signItem.getText().toString());
                        bundle.putString(Key.BUNDLE_DATA, signItem.getDetailText().toString());
                        QMUIFragment instance = EditFragment.getInstance();
                        instance.setArguments(bundle);
                        startFragmentForResult(instance, UserInfofragment.REQUEST_CODE);
                    }
                })
                .addTo(mGroupListView);

    }

    private void setItemView(QMUICommonListItemView item) {
        item.getTextView().setTextColor(getResources().getColor(R.color.Gray));
        item.getTextView().setTextSize(15);
        item.getTextView().getPaint().setFakeBoldText(true);
        item.getDetailTextView().setTextColor(getResources().getColor(R.color.GrayWrite));
    }


    @OnClick(R.id.layout_userImg)
    public void onViewClicked() {
        Intent intent = new Intent(mContext, ImageGridActivity.class);
        startActivityForResult(intent, IMAGE_PICKER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        RxLogUtils.d("requestCode：" + requestCode);
        RxLogUtils.d("resultCode：" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (data != null && requestCode == IMAGE_PICKER) {
                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                Glide.with(mActivity).load(images.get(0).path)
                        .asBitmap()
                        .placeholder(R.mipmap.userimg)
                        .into(mIvUserImg);
                uploadImage(images.get(0).path);
            }
        }
    }


    private void showDate() {
        Calendar calendar = Calendar.getInstance();
        CustomDatePicker datePicker = new CustomDatePicker(mActivity);
        datePicker.setRangeStart(1940, 01, 01);
        datePicker.setRangeEnd(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        calendar.setTimeInMillis(info.getBirthday());
        datePicker.setTextColor(getResources().getColor(R.color.Gray));
        datePicker.setSelectedItem(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.setOnDatePickListener(new DatePicker.OnYearMonthDayPickListener() {
            @Override
            public void onDatePicked(String year, String month, String day) {
                RxLogUtils.d("年：" + year + "------月：" + month + "---------日：" + day);
                birthItem.setDetailText(year + "-" + month + "-" + day);
                Date date = RxFormat.setParseDate(year + "-" + month + "-" + day, RxFormat.Date);
                info.setBirthday(date.getTime());
            }
        });
        datePicker.show();

    }

    private void showAddress() {
        AddressPickTask task = new AddressPickTask(mActivity);
        task.setHideCounty(true);
        task.setCallback(new AddressPickTask.Callback() {
            @Override
            public void onAddressInitFailed() {
                RxLogUtils.e("数据初始化失败");
            }

            @Override
            public void onAddressPicked(Province province, City city, County county) {
                cityItem.setDetailText(province.getName() + "," + city.getName());
            }
        });
        task.execute(RxDataUtils.isNullString(info.getProvince()) ? "" : info.getProvince(),
                RxDataUtils.isNullString(info.getCity()) ? "" : info.getCity());
    }

    public void showHeight() {
        CustomNumberPicker picker = new CustomNumberPicker(mActivity);
        picker.setTextColor(getResources().getColor(R.color.Gray));
        picker.setRange(120, 200, 1);//数字范围
        picker.setSelectedItem(info.getHeight());
        picker.setLabel("cm");
        picker.setLabelTextColor(getResources().getColor(R.color.Gray));
        picker.setOnNumberPickListener(new NumberPicker.OnNumberPickListener() {
            @Override
            public void onNumberPicked(int index, Number item) {
                RxLogUtils.d("身高：" + item);
                heightItem.setDetailText(item + "cm");
                info.setHeight((int) item);
            }
        });
        picker.show();
    }


    private void showSex() {
        new QMUIBottomSheet.BottomListSheetBuilder(getActivity())
                .addItem("男")
                .addItem("女")
                .setCheckedIndex(info.getSex() - 1)
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();
                        sexItem.setDetailText(tag);
                        info.setSex(position + 1);
                    }
                })
                .build()
                .show();
    }

    private void uploadImage(String cropImagePath) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), new File(cropImagePath));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", cropImagePath, requestFile);
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.uploadUserImg(body))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("结束" + s);
                        RxToast.success("保存成功", 2000);
                        //成功后将本地图片设置到imageView中，并在退回到个人中心时，刷新贴图url
                        info.setImgUrl(s);
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });
    }

    /*保存按钮，提交用户数据*/
    private void requestSaveUserInfo() {
        final String gson = new Gson().toJson(info, UserInfo.class);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson);
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.saveUserInfo(body))
                .compose(RxComposeUtils.<String>showDialog(tipDialog))
                .compose(RxComposeUtils.<String>bindLife(lifecycleSubject))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("结束" + s);
                        RxToast.success("保存成功", 2000);
                        SPUtils.put(SPKey.SP_UserInfo, gson);
                        getBaseFragmentActivity().popBackStack();
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });
    }


    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Intent data) {
        super.onFragmentResult(requestCode, resultCode, data);
        RxLogUtils.d("返回:requestCode:" + requestCode + "---resultCode" + resultCode + "---Intent:" + data.toString());
        if (resultCode == UserInfofragment.RESULT_CODE && requestCode == UserInfofragment.REQUEST_CODE) {
            Bundle bundle = data.getExtras();
            String title = bundle.getString(Key.BUNDLE_TITLE);
            if (title.equals(userNameItem.getText().toString())) {
                userNameItem.setDetailText(bundle.getString(Key.BUNDLE_DATA));
                info.setUserName(bundle.getString(Key.BUNDLE_DATA));
            } else {
                signItem.setDetailText(bundle.getString(Key.BUNDLE_DATA));
                info.setSignature(bundle.getString(Key.BUNDLE_DATA));
            }
        }
    }

    @Override
    protected void popBackStack() {
        if (info.isChange()) {
            final RxDialogSureCancel dialog = new RxDialogSureCancel(mActivity);
            dialog.setCanceledOnTouchOutside(false);
            dialog.getTvTitle().setVisibility(View.GONE);
            dialog.setContent("您已经修改信息\n是否退出？");
            dialog.getTvCancel().setBackgroundColor(getResources().getColor(R.color.green_61D97F));
            dialog.setCancel("退出").setCancelListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    getBaseFragmentActivity().popBackStack();
                }
            })
                    .setSure("留下")
                    .setSureListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    }).show();
        } else
            super.popBackStack();
    }


}