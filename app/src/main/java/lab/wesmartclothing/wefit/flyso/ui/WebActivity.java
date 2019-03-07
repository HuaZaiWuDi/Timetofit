package lab.wesmartclothing.wefit.flyso.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.RelativeLayout;

import com.vondear.rxtools.activity.RxActivityUtils;

import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseWebActivity;
import lab.wesmartclothing.wefit.flyso.tools.Key;

//网页
public class WebActivity extends BaseWebActivity {
    RelativeLayout parent;
    private String url;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_software_protocol_activiy);
        parent = findViewById(R.id.parent);
        initWebView(parent);

    }



    @Nullable
    @Override
    protected String getUrl() {
        return getIntent().getStringExtra(Key.BUNDLE_WEB_URL);
//        return "http://www.baidu.com";
    }
}
