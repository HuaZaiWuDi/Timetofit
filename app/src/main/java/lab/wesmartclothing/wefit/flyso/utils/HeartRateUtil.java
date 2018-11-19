package lab.wesmartclothing.wefit.flyso.utils;

import com.smartclothing.blelibrary.util.ByteUtil;
import com.vondear.rxtools.aboutByte.BitUtils;
import com.vondear.rxtools.dateUtils.RxFormat;
import com.vondear.rxtools.utils.RxFormatValue;
import com.vondear.rxtools.utils.RxLogUtils;
import com.zchu.rxcache.RxCache;
import com.zchu.rxcache.data.CacheResult;

import java.util.ArrayList;
import java.util.List;

import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.entity.HeartRateBean;
import lab.wesmartclothing.wefit.flyso.rxbus.RefreshSlimming;
import lab.wesmartclothing.wefit.flyso.rxbus.SportsDataTab;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.netlib.net.RetrofitService;
import lab.wesmartclothing.wefit.netlib.rx.NetManager;
import lab.wesmartclothing.wefit.netlib.rx.RxManager;
import lab.wesmartclothing.wefit.netlib.rx.RxNetSubscriber;
import lab.wesmartclothing.wefit.netlib.utils.RxBus;
import lab.wesmartclothing.wefit.netlib.utils.RxSubscriber;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * @Package lab.wesmartclothing.wefit.flyso.utils
 * @FileName HeartRateUtil
 * @Date 2018/11/6 16:48
 * @Author JACK
 * @Describe TODO心率保存工具类
 * @Project Android_WeFit_2.0
 */
public class HeartRateUtil {
    private List<Integer> heartRateLists = new ArrayList<>();
    private int maxHeart = 0;//最大心率
    private int minHeart = 200;//最小心率
    private double kcalTotal = 0;//总卡路里
    private int lastHeartRate = 0;//上一次的心率值
    private int realHeartRate = 0;//真实心率
    private static final int heartDeviation = 5;//心率误差值
    private static int heartSupplement = (int) (Math.random() * 3 + 2);//补差值：修改补差值为2-5的随机数
    private long lastTime = 0;//历史记录上一条时间
    private int packageCounts = 0;//历史数据包序号


    public HeartRateUtil() {
    }

    public void addRealTimeData(byte[] bytes) {
        int heartRate = realHeartRate = bytes[8] & 0xff;

        if (lastHeartRate == 0) {
            lastHeartRate = heartRate;
        }

        if (lastHeartRate - heartRate > heartDeviation) {
            heartRate = lastHeartRate - heartSupplement;
        } else if (lastHeartRate - heartRate < -heartDeviation) {
            heartRate = lastHeartRate + heartSupplement;
        }
        lastHeartRate = heartRate;

        long time = ByteUtil.bytesToLongD4(bytes, 3) * 1000;
        RxLogUtils.d("硬件时间：" + RxFormat.setFormatDate(time, RxFormat.Date_Date));

        maxHeart = heartRate > maxHeart ? heartRate : maxHeart;
        minHeart = heartRate < minHeart ? heartRate : minHeart;

        SportsDataTab mSportsDataTab = new SportsDataTab();
        mSportsDataTab.setAthlRecord_2(heartRateLists);
        mSportsDataTab.setCurHeart(heartRate);
        mSportsDataTab.setMaxHeart(maxHeart);
        mSportsDataTab.setMinHeart(minHeart);
        mSportsDataTab.setRealHeart(realHeartRate);
        mSportsDataTab.setVoltage(ByteUtil.bytesToIntD2(new byte[]{bytes[15], bytes[16]}));
        mSportsDataTab.setLightColor((BitUtils.setBitValue(bytes[17], 7, (byte) 0) & 0xff));
        mSportsDataTab.setTemp((bytes[10] & 0xff));
        mSportsDataTab.setSteps(ByteUtil.bytesToIntD2(new byte[]{bytes[12], bytes[13]}));
        mSportsDataTab.setData(bytes);
        mSportsDataTab.setDate(System.currentTimeMillis());
        mSportsDataTab.setPower((BitUtils.checkBitValue(bytes[17], 7)));
        //卡路里累加计算
        kcalTotal += HeartRateToKcal.getCalorie(heartRate, 2f / 3600);

        mSportsDataTab.setKcal(RxFormatValue.format4S5R(kcalTotal, 1));//统一使用卡为基本热量单位

        RxLogUtils.d("数据：" + mSportsDataTab.toString());
        RxBus.getInstance().post(mSportsDataTab);

    }


    /**
     * 未使用
     * 瘦身衣历史心率数据
     *
     * @param bytes
     */
    public void addHsitoryData(byte[] bytes) {
        long time = ByteUtil.bytesToLongD4(bytes, 3);

        //根据时间去重
        if (lastTime == time) {
            RxLogUtils.d("表示重复包");
        } else {
            packageCounts++;
            RxLogUtils.d("包序号：" + packageCounts);
            lastTime = time;
            int heartRate = bytes[8] & 0xff;

        }
    }


    public void uploadHeartRate() {
        RxCache.getDefault().<HeartRateBean>load(Key.CACHE_ATHL_RECORD_PLAN, HeartRateBean.class)
                .map(new CacheResult.MapFunc<HeartRateBean>())
                .subscribe(new RxSubscriber<HeartRateBean>() {
                    @Override
                    protected void _onNext(HeartRateBean mHeartRateBean) {
                        saveHeartRate(mHeartRateBean);
                    }
                });

        RxCache.getDefault().<HeartRateBean>load(Key.CACHE_ATHL_RECORD_FREE, HeartRateBean.class)
                .map(new CacheResult.MapFunc<HeartRateBean>())
                .subscribe(new RxSubscriber<HeartRateBean>() {
                    @Override
                    protected void _onNext(HeartRateBean mHeartRateBean) {
                        saveHeartRate(mHeartRateBean);
                    }
                });
    }


    public void saveHeartRate(final HeartRateBean heartRateBean) {
        String s = MyAPP.getGson().toJson(heartRateBean, HeartRateBean.class);
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), s);
        RetrofitService dxyService = NetManager.getInstance().createString(RetrofitService.class);
        RxManager.getInstance().doNetSubscribe(dxyService.addAthleticsInfo(body))
                .subscribe(new RxNetSubscriber<String>() {
                    @Override
                    protected void _onNext(String s) {
                        RxLogUtils.d("添加心率：保存成功删除本地缓存：");
                        clearData(heartRateBean);
                        //这里因为是后台上传数据，并不是跳转，使用RxBus方式
                        RxBus.getInstance().post(new RefreshSlimming());
                    }

                });
    }

    public void clearData(HeartRateBean heartRateBean) {
        boolean remove;
        if (heartRateBean.getPlanFlag() == 1) {
            remove = RxCache.getDefault().remove(Key.CACHE_ATHL_RECORD_PLAN);
        } else {
            remove = RxCache.getDefault().remove(Key.CACHE_ATHL_RECORD_FREE);
        }
        RxLogUtils.d("删除成功：" + heartRateBean.toString());
        RxLogUtils.d("删除成功remove：" + remove);
    }

}
