package lab.wesmartclothing.wefit.flyso.ui.main.slimming.heat;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.google.gson.Gson;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxTextUtils;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.view.RxToast;
import com.zchu.rxcache.data.CacheResult;
import com.zchu.rxcache.stategy.CacheStrategy;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseActivity;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.AddFoodItem;
import lab.wesmartclothing.wefit.flyso.entity.HotKeyItem;
import lab.wesmartclothing.wefit.flyso.entity.ListBean;
import lab.wesmartclothing.wefit.flyso.entity.SearchListItem;
import lab.wesmartclothing.wefit.flyso.entity.sql.SearchWordTab;
import lab.wesmartclothing.wefit.flyso.utils.StatusBarUtils;
import lab.wesmartclothing.wefit.flyso.view.AddOrUpdateFoodDialog;
import lab.wesmartclothing.wefit.flyso.view.DynamicTagFlowLayout;
import lab.wesmartclothing.wefit.netlib.net.RetrofitService;
import lab.wesmartclothing.wefit.netlib.rx.NetManager;
import lab.wesmartclothing.wefit.netlib.rx.RxManager;
import lab.wesmartclothing.wefit.netlib.rx.RxNetSubscriber;
import lab.wesmartclothing.wefit.netlib.utils.RxBus;
import okhttp3.RequestBody;


@EActivity(R.layout.activity_search_history)
public class SearchHistoryActivity extends BaseActivity {

    private List<String> latelyLists = new ArrayList<>();
    private List<String> hotLists = new ArrayList<>();
    private BaseQuickAdapter searchListAdapter;
    private boolean isStorage = false;
    private Disposable subscribe;

    @ViewById
    DynamicTagFlowLayout tagFlowLayout_hot;
    @ViewById
    DynamicTagFlowLayout tagFlowLayout_lately;
    @ViewById
    SearchView mSearchView;
    @ViewById
    LinearLayout layoutHistory;
    @ViewById
    RecyclerView mRecyclerView;

    @Bean
    AddOrUpdateFoodDialog mAddOrUpdateFoodDialog;

    @Extra
    String ADD_FOOD_DATE;
    @Extra
    int ADD_FOOD_TYPE;

    @Click
    void tv_cancel() {
        finish();
    }

    @Click
    void tv_delete() {
        SearchWordTab.deleteAll();
        latelyLists.clear();
        tagFlowLayout_lately.setTags(latelyLists);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.from(this).setTransparentStatusbar(true).process();
    }


    @Override
    @AfterViews
    public void initView() {

        subscribe = new RxPermissions(mActivity)
                .requestEach(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        isStorage = permission.granted;
                    }
                });

        init();
        initRecyclerView();
    }


    private void initRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        searchListAdapter = new BaseQuickAdapter<ListBean, BaseViewHolder>(R.layout.item_search) {
            @Override
            protected void convert(BaseViewHolder helper, ListBean item) {
                helper.setText(R.id.tv_foodName, item.getFoodName());
                TextView view = helper.getView(R.id.tv_foodInfo);
                RxTextUtils.getBuilder("")
                        .append(item.getCalorie() + getString(R.string.unit_kcal)).setForegroundColor(getResources().getColor(R.color.colorTheme))
                        .append(getString(R.string.unit_heat))
                        .into(view);
            }
        };
        searchListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (RxUtils.isFastClick(1000)) return;
                ListBean listBean = (ListBean) adapter.getData().get(position);
                String foodName = listBean.getFoodName();
                if (!SearchWordTab.isExist(foodName)) {
                    if (isStorage) {
                        SearchWordTab keyTab = new SearchWordTab(System.currentTimeMillis(), foodName);
                        keyTab.save();
                    }
                    latelyLists.add(foodName);
                    tagFlowLayout_lately.setTags(latelyLists);
                }
                showAddFoodDialog(listBean);
            }
        });
        searchListAdapter.bindToRecyclerView(mRecyclerView);
        searchListAdapter.setEmptyView(R.layout.layout_not_data);
    }

    private void init() {
        initLateLyData();
        initHotData();

        tagFlowLayout_hot.setOnTagItemClickListener(new DynamicTagFlowLayout.OnTagItemClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.setQuery(((TextView) v).getText().toString(), true);
            }
        });

        tagFlowLayout_lately.setOnTagItemClickListener(new DynamicTagFlowLayout.OnTagItemClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.setQuery(((TextView) v).getText().toString(), true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (subscribe != null) subscribe.dispose();
        subscribe = null;
        super.onDestroy();
    }

    private void showAddFoodDialog(final ListBean item) {
        item.setEatType(ADD_FOOD_TYPE);
        item.setHeatDate(ADD_FOOD_DATE);

        mAddOrUpdateFoodDialog.setFoodInfo(mContext, true, item, new AddOrUpdateFoodDialog.AddOrUpdateFoodListener() {
            @Override
            public void complete(AddFoodItem.intakeList item) {
                mRecyclerView.setVisibility(View.GONE);
                RxBus.getInstance().post(item);
                onBackPressed();
            }
        });

    }


    private void initLateLyData() {
        /*------------------ SearchView有三种默认展开搜索框的设置方式，区别如下： ------------------*/
//        //设置搜索框直接展开显示。左侧有放大镜(在搜索框中) 右侧有叉叉 可以关闭搜索框
//        mSearchView.setIconified(false);
//        //设置搜索框直接展开显示。左侧有放大镜(在搜索框外) 右侧无叉叉 有输入内容后有叉叉 不能关闭搜索框
        mSearchView.setIconifiedByDefault(false);
//        //设置搜索框直接展开显示。左侧有无放大镜(在搜索框中) 右侧无叉叉 有输入内容后有叉叉 不能关闭搜索框
//        mSearchView.onActionViewExpanded();

        //修改搜索框的字体颜色及大小
        EditText textView = mSearchView
                .findViewById(
                        android.support.v7.appcompat.R.id.search_src_text
                );
        textView.setTextColor(getResources().getColor(R.color.white));//字体颜色
        textView.setTextSize(15);//字体、提示字体大小
        textView.setHintTextColor(Color.WHITE);//提示字体颜色**

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //TODO 处理Text的。防止输入特殊文字
                if (!RxDataUtils.isNullString(query) && query.length() <= 20)
                    initSearchData(query);
                else RxToast.warning(getString(R.string.inputRightFoodName));

                if (!SearchWordTab.isExist(query)) {
                    if (isStorage) {
                        SearchWordTab keyTab = new SearchWordTab(System.currentTimeMillis(), query);
                        keyTab.save();
                    }
                    latelyLists.add(query);
                    tagFlowLayout_lately.setTags(latelyLists);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //TODO 处理Text的。防止输入特殊文字
                if (!RxDataUtils.isNullString(newText) && newText.length() <= 20)
                    initSearchData(newText);
                else mRecyclerView.setVisibility(View.GONE);
                return false;
            }
        });

        if (!isStorage) return;

        List<SearchWordTab> all = SearchWordTab.soft(SearchWordTab.getAll());

        latelyLists.clear();
        for (int i = 0; i < all.size(); i++) {
            latelyLists.add(all.get(i).searchKey);
        }
        tagFlowLayout_lately.setTags(latelyLists);

    }

    private void initSearchData(String key) {
        mRecyclerView.setVisibility(View.VISIBLE);
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.searchFoodInfo(key))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        SearchListItem item = new Gson().fromJson(s, SearchListItem.class);
                        List<ListBean> beans = item.getList();
                        searchListAdapter.setNewData(beans);
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });
    }

    private void initHotData() {
        JSONObject jsonObject = new JSONObject();
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.getKeyWord(body))
                .compose(MyAPP.getRxCache().<String>transformObservable("getKeyWord", String.class, CacheStrategy.cacheAndRemote()))
                .map(new CacheResult.MapFunc<String>())
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        HotKeyItem item = new Gson().fromJson(s, HotKeyItem.class);
                        hotLists.clear();
                        List<HotKeyItem.ListBean> list = item.getList();
                        for (HotKeyItem.ListBean bean : list) {
                            hotLists.add(bean.getKeyWord());
                        }
                        tagFlowLayout_hot.setTags(hotLists);
                    }

                    @Override
                    protected void _onError(String error) {
                        RxToast.error(error);
                    }
                });

    }


}
