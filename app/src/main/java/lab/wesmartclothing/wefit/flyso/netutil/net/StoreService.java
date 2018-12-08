package lab.wesmartclothing.wefit.flyso.netutil.net;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by jk on 2018/5/24.
 */
public interface StoreService {
    //        String BASE_URL = "https://dev.wesmartclothing.com/system/";
//    String BASE_URL = "http://10.10.11.208:15320/";
    String BASE_URL = "http://39.108.152.50:15320/smart/";


    //系统配置
    @GET("fetchSystemConfigList")
    Observable<String> getSystemConfig();


}
