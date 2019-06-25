package lab.wesmartclothing.wefit.flyso.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.alibaba.fastjson.JSON;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.vondear.rxtools.activity.RxActivityUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxNetUtils;
import com.vondear.rxtools.utils.RxSystemBroadcastUtil;
import com.vondear.rxtools.utils.RxUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.view.RxToast;
import com.wesmarclothing.mylibrary.net.RxBus;
import com.yolanda.health.qnblesdk.listener.QNBleDeviceDiscoveryListener;
import com.yolanda.health.qnblesdk.listener.QNDataListener;
import com.yolanda.health.qnblesdk.out.QNBleDevice;
import com.yolanda.health.qnblesdk.out.QNScaleData;
import com.yolanda.health.qnblesdk.out.QNScaleStoreData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lab.wesmartclothing.wefit.flyso.BuildConfig;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.ActivityLifecycleImpl;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.base.SportInterface;
import lab.wesmartclothing.wefit.flyso.ble.BleAPI;
import lab.wesmartclothing.wefit.flyso.ble.BleKey;
import lab.wesmartclothing.wefit.flyso.ble.BleTools;
import lab.wesmartclothing.wefit.flyso.ble.QNBleTools;
import lab.wesmartclothing.wefit.flyso.ble.listener.BleChartChangeCallBack;
import lab.wesmartclothing.wefit.flyso.ble.util.ByteUtil;
import lab.wesmartclothing.wefit.flyso.entity.BindDeviceBean;
import lab.wesmartclothing.wefit.flyso.entity.DeviceLink;
import lab.wesmartclothing.wefit.flyso.entity.DeviceVersionBean;
import lab.wesmartclothing.wefit.flyso.rxbus.BleStateChangedBus;
import lab.wesmartclothing.wefit.flyso.rxbus.ClothingConnectBus;
import lab.wesmartclothing.wefit.flyso.rxbus.DeviceVoltageBus;
import lab.wesmartclothing.wefit.flyso.rxbus.HeartRateChangeBus;
import lab.wesmartclothing.wefit.flyso.rxbus.NetWorkType;
import lab.wesmartclothing.wefit.flyso.rxbus.RefreshSlimming;
import lab.wesmartclothing.wefit.flyso.rxbus.ScaleConnectBus;
import lab.wesmartclothing.wefit.flyso.rxbus.ScaleHistoryData;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.flyso.ui.main.slimming.sports.PlanSportingActivity;
import lab.wesmartclothing.wefit.flyso.ui.main.slimming.sports.SportingActivity;
import lab.wesmartclothing.wefit.flyso.ui.main.slimming.weight.WeightAddFragment;
import lab.wesmartclothing.wefit.flyso.utils.HeartRateUtil;
import lab.wesmartclothing.wefit.flyso.utils.VoltageToPower;

import static no.nordicsemi.android.dfu.DfuBaseService.NOTIFICATION_ID;

public class BleService extends Service {
    static boolean isFirst = true;//固件升级检查弹窗提示

    //Channel ID 必须保证唯一
    public static final String CHANNEL_ID = "lab.wesmartclothing.wefit.flyso.channel";
    private boolean dfuStarting = false; //DFU升级时候需要断连重连，防止升级时做其他操作，导致升级失败

    private Map<String, Object> connectDevices = new HashMap<>();

    public static boolean clothingFinish = true;
    QNBleTools mQNBleTools = QNBleTools.getInstance();


    private static boolean isFirstJoin = true;

    public static List<QNScaleStoreData> historyWeightData;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    Bundle extras = intent.getExtras();
                    if (extras == null) break;
                    int state = extras.getInt(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        RxBus.getInstance().postSticky(new BleStateChangedBus(false));
                        stopScan();
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        initBle();
                        RxBus.getInstance().postSticky(new BleStateChangedBus(true));
                    } else if (state == BluetoothAdapter.STATE_TURNING_ON) {//正在开启蓝牙
                    }
                    break;
                case RxSystemBroadcastUtil.SCREEN_ON:
                    RxLogUtils.d("亮屏");
                    initBle();
                    break;
                case RxSystemBroadcastUtil.SCREEN_OFF:
                    RxLogUtils.d("息屏");
                    if (RxActivityUtils.currentActivity() instanceof SportInterface) {
                        RxActivityUtils.skipActivityTop(RxActivityUtils.currentActivity(), RxActivityUtils.currentActivity().getClass());
                    }
                    stopScan();
                    break;
                case Key.ACTION_CLOTHING_STOP:
                    clothingFinish = true;
                    BleAPI.clothingStop();
                    break;
                case Intent.ACTION_DATE_CHANGED://日期的变化
                    RxLogUtils.d("日期变化");
                    RxBus.getInstance().post(new RefreshSlimming());
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    int workType = RxNetUtils.getNetWorkType(context);
                    if (isFirstJoin) {
                        isFirstJoin = false;
                        if (workType == -1 || workType == 5) {
                            if (ActivityLifecycleImpl.APP_IS_FOREGROUND)
                                RxToast.normal(RxNetUtils.getNetType(workType));
                        }
                    } else {
                        if (ActivityLifecycleImpl.APP_IS_FOREGROUND)
                            RxToast.normal(RxNetUtils.getNetType(workType));
                        RxBus.getInstance().post(new NetWorkType(workType, workType != -1 && workType != 5));
                    }

                    if (workType != -1 && workType != 5) {
                        //一分钟之内只执行一次
                        if (!RxUtils.isFastClick(60 * 1000)) {
                            new HeartRateUtil().uploadHeartRate();
                        }
                    }

                    RxLogUtils.d("网络状态：" + workType);
                    break;
            }
        }
    };


    public BleService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        initHeartRate();
        connectScaleCallBack();
        initBroadcast();
    }


    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(RxSystemBroadcastUtil.SCREEN_ON);
        filter.addAction(RxSystemBroadcastUtil.SCREEN_OFF);
        filter.addAction(Key.ACTION_CLOTHING_STOP);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, filter);

    }


    @Override
    public void onDestroy() {
        stopScan();
        unregisterReceiver(mBroadcastReceiver);
        if (BuildConfig.LeakCanary) {
            RefWatcher refWatcher = LeakCanary.installedRefWatcher();
            // We expect schrodingerCat to be gone soon (or not), let's watch it.
            refWatcher.watch(this);
        }
        stopForeground(true);
        RxLogUtils.d("【BleService】：onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("APP_BACKGROUND", false)) {
            stopScan();
            System.gc();
        } else {
            if (BleTools.getBleManager().isBlueEnable())
                initBle();
        }
        setForegroundService();
        return START_STICKY;
    }

    /**
     * 通过通知启动服务
     */
    public void setForegroundService() {
        //向系统注册通知渠道，注册后不能改变重要性以及其他通知行为
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //设定的通知渠道名称
        String channelName = getString(R.string.appName);
        //设置通知的重要程度
        //构建通知渠道
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("描述");
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
        //在创建的通知渠道上发送通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.icon_app_lightness) //设置通知图标
                .setContentTitle("提示")//设置通知标题
                .setContentText("智享瘦正在后台运行")//设置通知内容
                .setAutoCancel(true) //用户触摸时，自动关闭
                .setOngoing(true);//设置处于运行状态
        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(NOTIFICATION_ID, builder.build());

    }


    private void initBle() {
        stopScan();

        if (!BleTools.getInstance().isConnect())
            scanClothing();
        mQNBleTools.scanBle();
    }


    private void scanClothing() {
        BleScanRuleConfig bleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(new UUID[]{UUID.fromString(BleKey.UUID_Servie)})
                .setDeviceMac(SPUtils.getString(SPKey.SP_clothingMAC))
                .setScanTimeOut(0)
                .build();
        BleTools.getBleManager().initScanRule(bleConfig);

        BleTools.getBleManager().scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                RxLogUtils.d("扫描结束：" + scanResultList.size());
            }

            @Override
            public void onScanStarted(boolean success) {
                RxLogUtils.d("扫描开始：" + success);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                RxLogUtils.d("扫描结果：" + bleDevice.toString());

                if (bleDevice.getMac().equals(SPUtils.getString(SPKey.SP_clothingMAC)) &&
                        !BleTools.getBleManager().isConnected(bleDevice.getMac()) &&
                        !connectDevices.containsKey(bleDevice.getMac())) {//判断是否正在连接，或者已经连接则不在连接
                    connectClothing(bleDevice);
                    connectDevices.put(bleDevice.getMac(), bleDevice);
                    //扫描到设备停止扫描
                    BleTools.getInstance().stopScan();
                }

                BindDeviceBean bindDeviceBean = new BindDeviceBean(BleKey.TYPE_CLOTHING,
                        getString(R.string.clothing), bleDevice.getMac(), bleDevice.getRssi());
                RxBus.getInstance().post(bindDeviceBean);
            }
        });
    }


    private void stopScan() {
        BleTools.getInstance().stopScan();
        mQNBleTools.stopScan();
    }


    private void connectScaleCallBack() {
        //扫描体脂称
        MyAPP.QNapi.setBleDeviceDiscoveryListener(new QNBleDeviceDiscoveryListener() {
            @Override
            public void onDeviceDiscover(QNBleDevice bleDevice) {
                RxLogUtils.d("扫描体脂称：" + bleDevice.getMac() + ":" + bleDevice.getRssi() + ":" + bleDevice.getModeId());
                BindDeviceBean bindDeviceBean = new BindDeviceBean(BleKey.TYPE_SCALE, bleDevice.getName(), bleDevice.getMac(), bleDevice.getRssi());
                RxBus.getInstance().post(bindDeviceBean);

                if (bleDevice.getMac().equals(SPUtils.getString(SPKey.SP_scaleMAC)) &&
                        mQNBleTools.getConnectState() == QNBleTools.QN_DISCONNECED &&
                        !connectDevices.containsKey(bleDevice.getMac())) {//判断是否正在连接，或者已经连接则不在连接
                    mQNBleTools.connectDevice(bleDevice);
                    connectDevices.put(bleDevice.getMac(), bleDevice);

                    mQNBleTools.stopScan();
                }
            }

            @Override
            public void onStartScan() {
                RxLogUtils.d("轻牛SDK ：开始扫描");
            }

            @Override
            public void onStopScan() {
                RxLogUtils.d("轻牛SDK ：停止扫描");
            }

            @Override
            public void onScanFail(int i) {
                RxLogUtils.d("轻牛SDK ：扫描失败");
            }
        });

        /**
         * 2018-10-16
         * 修改逻辑判断有实时数据或者稳定数据时在跳转
         *
         * */
        MyAPP.QNapi.setDataListener(new QNDataListener() {
            @Override
            public void onGetUnsteadyWeight(QNBleDevice qnBleDevice, double v) {
//                RxBus.getInstance().post(new ScaleUnsteadyWeight(v));
                Bundle bundle = new Bundle();
                bundle.putDouble(Key.BUNDLE_WEIGHT_UNSTEADY, v);
                if (RxActivityUtils.currentActivity() != null)
                    if (!RxActivityUtils.currentActivity().getClass().equals(PlanSportingActivity.class)
                            && !RxActivityUtils.currentActivity().getClass().equals(SportingActivity.class))
                        RxActivityUtils.skipActivity(RxActivityUtils.currentActivity(), WeightAddFragment.class, bundle);
            }

            @Override
            public void onGetScaleData(QNBleDevice qnBleDevice, final QNScaleData qnScaleData) {
                RxLogUtils.d("实时的稳定测量数据是否有效：" + Arrays.toString(qnScaleData.getAllItem().toArray()));
                Bundle bundle = new Bundle();

//                qnScaleData.setFatThreshold();

                bundle.putString(Key.BUNDLE_WEIGHT_QNDATA, JSON.toJSONString(qnScaleData));
                if (RxActivityUtils.currentActivity() != null) {
                    if (!RxActivityUtils.currentActivity().getClass().equals(PlanSportingActivity.class)
                            && !RxActivityUtils.currentActivity().getClass().equals(SportingActivity.class))
                        RxActivityUtils.skipActivity(RxActivityUtils.currentActivity(), WeightAddFragment.class, bundle);
                }
//                RxBus.getInstance().post(qnScaleData);
            }

            @Override
            public void onGetStoredScale(QNBleDevice qnBleDevice, final List<QNScaleStoreData> list) {
                RxLogUtils.d("历史数据：" + list.size());
                historyWeightData = list;
                RxBus.getInstance().post(new ScaleHistoryData(list));
            }

            @Override
            public void onGetElectric(QNBleDevice qnBleDevice, int i) {
                RxLogUtils.d("onGetElectric:" + i);
            }
        });

        MyAPP.QNapi.setBleConnectionChangeListener(new com.yolanda.health.qnblesdk.listener.QNBleConnectionChangeListener() {
            @Override
            public void onConnecting(QNBleDevice qnBleDevice) {
                RxLogUtils.e("正在连接:");
                mQNBleTools.setConnectState(QNBleTools.QN_CONNECTING);
            }

            @Override
            public void onConnected(QNBleDevice qnBleDevice) {
                RxLogUtils.d("连接成功:");
            }

            @Override
            public void onServiceSearchComplete(QNBleDevice qnBleDevice) {

                mQNBleTools.setConnectState(QNBleTools.QN_CONNECED);
                RxBus.getInstance().postSticky(new ScaleConnectBus(true));

                DeviceLink deviceLink = new DeviceLink();
                deviceLink.setMacAddr(SPUtils.getString(SPKey.SP_scaleMAC));
                deviceLink.setDeviceNo(BleKey.TYPE_SCALE);
                deviceLink.deviceLink(deviceLink);

            }

            @Override
            public void onDisconnecting(QNBleDevice qnBleDevice) {
                RxLogUtils.e("正在断开连接:");
                mQNBleTools.setConnectState(QNBleTools.QN_DISCONNECTING);
            }

            @Override
            public void onDisconnected(QNBleDevice qnBleDevice) {
                mQNBleTools.setConnectState(QNBleTools.QN_DISCONNECED);
                RxBus.getInstance().postSticky(new ScaleConnectBus(false));
                RxLogUtils.e("断开连接:");
                connectDevices.remove(qnBleDevice.getMac());

                mQNBleTools.scanBle();
            }

            @Override
            public void onConnectError(QNBleDevice qnBleDevice, int i) {
                mQNBleTools.setConnectState(QNBleTools.QN_DISCONNECED);
                RxLogUtils.d("连接异常：" + i);
                mQNBleTools.disConnectDevice();
                RxBus.getInstance().postSticky(new ScaleConnectBus(false));
                connectDevices.remove(qnBleDevice.getMac());

                mQNBleTools.scanBle();
            }

            @Override
            public void onScaleStateChange(QNBleDevice qnBleDevice, int i) {
                RxLogUtils.d("体重秤状态变化:" + i);
                switch (i) {
                    case 5://正在测量
                        break;
                    case 6://正在测量试试体重
                        break;
                    case 7://正在测试生物阻抗
                        break;
                    case 8://正在测试心率
                        break;
                    case 9://测量完成
                        break;
                }
            }
        });
    }


    //连接瘦身衣
    private void connectClothing(BleDevice device) {
        BleTools.getBleManager().connect(device, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                RxLogUtils.e("开始连接瘦身衣：");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                RxLogUtils.d("连接失败：" + exception.toString());
//                RxToast.info(getString(R.string.connectError));
                BleTools.getBleManager().disconnect(bleDevice);
                RxBus.getInstance().postSticky(new ClothingConnectBus(false));
                connectDevices.remove(bleDevice.getMac());

                scanClothing();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                RxLogUtils.d("瘦身衣连接成功");

                RxBus.getInstance().postSticky(new ClothingConnectBus(true));
                if (dfuStarting) return;

                BleTools.getInstance().setBleDevice(bleDevice);
                BleTools.getInstance().openNotify(isSuccess ->
                        BleTools.getBleManager().setMtu(bleDevice, 200, new BleMtuChangedCallback() {
                            @Override
                            public void onSetMTUFailure(BleException exception) {
                                RxLogUtils.e("更改Mtu值失败：", exception);
                            }

                            @Override
                            public void onMtuChanged(int mtu) {
                                syncBleSetting();
                            }
                        }));
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                RxLogUtils.d("断开连接");
                RxBus.getInstance().postSticky(new ClothingConnectBus(false));
                connectDevices.remove(device.getMac());

                scanClothing();
            }
        });
    }


    /**
     * 流程：同步时间->同步配置->同步历史数据->检查固件版本（仅一次）
     */
    private void syncBleSetting() {
        BleAPI.syncDeviceTime(new BleChartChangeCallBack() {
            @Override
            public void callBack(byte[] data) {
                RxLogUtils.d("同步时间成功");
                syncSetting();
            }
        });
    }

    private void syncSetting() {
        BleAPI.syncSetting(!BuildConfig.DEBUG, data -> {
            RxLogUtils.d("配置参数");
            syncHistoryData();
        });
    }

    private void syncHistoryData() {
        checkVersion();
//        BleAPI.syncDataCount(data -> {
//            long packageCount = ByteUtil.bytesToLongLittle(data, 3);
//            RxLogUtils.d("包总数：" + packageCount);
//            if (packageCount > 0) {
//                RxLogUtils.d("开始同步包数据");
////                    synData();
////                    RxToast.info("开始同步本地数据");
//            } else RxLogUtils.d("没有数据同步");
//
//        });
    }

    private void checkVersion() {
        BleAPI.readDeviceInfo(data -> {
            getVoltage();
            //021309 010203000400050607090a0b0c10111213
            String firmwareVersion = data[9] + "." + data[10] + "." + data[11];
            DeviceVersionBean versionBean = new DeviceVersionBean();
            versionBean.setCategory(data[3] & 0xFF);
            versionBean.setModelNo(data[4] & 0xFF);
            versionBean.setManufacture(ByteUtil.bytesToIntLittle2(new byte[]{data[5], data[6]}));
            versionBean.setHwVersion(ByteUtil.bytesToIntLittle2(new byte[]{data[7], data[8]}));
            versionBean.setFirmwareVersion(firmwareVersion);//当前固件版本

            //设备统计
            DeviceLink deviceLink = new DeviceLink();
            deviceLink.setMacAddr(SPUtils.getString(SPKey.SP_clothingMAC));
            deviceLink.setFirmwareVersion(firmwareVersion);
            deviceLink.setDeviceNo(BleKey.TYPE_CLOTHING);
            deviceLink.deviceLink(deviceLink);

            RxBus.getInstance().postSticky(versionBean);

            RxLogUtils.d("当前版本：" + versionBean.toString());
        });
    }


    private void getVoltage() {
        BleAPI.getVoltage(data -> {
            int voltage = ByteUtil.bytesToIntLittle2(new byte[]{data[3], data[4]});
            RxLogUtils.d("电压：" + voltage);
            VoltageToPower toPower = new VoltageToPower();
            int capacity = toPower.getBatteryCapacity(voltage / 1000f);
            double time = toPower.canUsedTime(voltage / 1000f, false);
            RxLogUtils.d("capacity:" + capacity + "time：" + time);
            RxBus.getInstance().postSticky(new DeviceVoltageBus(voltage, capacity, time));
        });
    }


    private void initHeartRate() {
        BleTools.getInstance().setBleCallBack(data -> {
            if (data.length < 20) return;
            RxBus.getInstance().post(new HeartRateChangeBus(data));
        });
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
