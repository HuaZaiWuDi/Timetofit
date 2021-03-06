package lab.wesmartclothing.wefit.flyso.utils;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.view.RxToast;

import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.UserInfo;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.net.RxManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.RxNetSubscriber;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.flyso.ui.main.MainActivity;
import lab.wesmartclothing.wefit.flyso.ui.main.mine.InvitationCodeActivity;
import lab.wesmartclothing.wefit.flyso.ui.userinfo.UserInfoActivity;
import lab.wesmartclothing.wefit.flyso.utils.jpush.JPushUtils;


/**
 * Created by jk on 2018/7/5.
 */

public class LoginSuccessUtils {

    private Context mContext;

    public LoginSuccessUtils(Context context, String s) {
        mContext = context;

        JsonParser parser = new JsonParser();
        JsonObject object = (JsonObject) parser.parse(s);
        String userId = object.get("userId").getAsString();
        String token = object.get("token").getAsString();
        SPUtils.put(SPKey.SP_UserId, userId);
        SPUtils.put(SPKey.SP_token, token);

        initUserInfo();
    }


    //获取用户信息
    private void initUserInfo() {
        RxManager.getInstance().doNetSubscribe(NetManager.getApiService().userInfo())
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("获取用户信息：" + s);
                        SPUtils.put(SPKey.SP_UserInfo, s);

                        UserInfo userInfo = MyAPP.gUserInfo = JSON.parseObject(s, UserInfo.class);

                        int sex = userInfo.getSex();


                        JPushUtils.setAliasOrTags("");

                        if (!userInfo.isHasInviteCode()) {
                            RxActivityUtils.skipActivityAndFinish(mContext, InvitationCodeActivity.class);
                        } else if (sex == 0) {
                            RxActivityUtils.skipActivityAndFinish(mContext, UserInfoActivity.class);
                        } else {
                            RxActivityUtils.skipActivity(mContext, MainActivity.class);
                        }

                    }

                    @Override
                    protected void _onError(String error, int code) {
                        RxToast.error(error);
                    }
                });
    }


}
