package lab.wesmartclothing.wefit.flyso.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RelativeLayout;

import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseWebActivity;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.utils.StatusBarUtils;

//网页
public class WebActivity extends BaseWebActivity {
    RelativeLayout parent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_software_protocol_activiy);
        parent = findViewById(R.id.parent);
        //屏幕沉浸
        StatusBarUtils.from(this).setStatusBarColor(getResources().getColor(R.color.colorTheme)).process();

        initWebView(parent);
    }

    @Override
    public void initView() {

    }


    @Nullable
    @Override
    protected String getUrl() {
        return getIntent().getStringExtra(Key.BUNDLE_WEB_URL);
    }
}