package lab.wesmartclothing.wefit.flyso.view;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.qmuiteam.qmui.layout.QMUIRelativeLayout;
import com.vondear.rxtools.utils.RxDataUtils;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.RxTextUtils;
import com.vondear.rxtools.view.dialog.RxDialog;
import com.vondear.rxtools.view.roundprogressbar.RxTextRoundProgressBar;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.MyAPP;
import lab.wesmartclothing.wefit.flyso.ble.MyBleManager;
import lab.wesmartclothing.wefit.flyso.ble.dfu.DfuService;
import lab.wesmartclothing.wefit.flyso.buryingpoint.BuryingPoint;
import lab.wesmartclothing.wefit.flyso.netutil.net.NetManager;
import lab.wesmartclothing.wefit.flyso.netutil.utils.FileDownLoadObserver;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * @author zzp
 */
public class AboutUpdateDialog extends RxDialog {
    @BindView(R.id.tv_progress)
    TextView mTvProgress;
    @BindView(R.id.mRxTextRoundProgressBar)
    RxTextRoundProgressBar mMRxTextRoundProgressBar;
    @BindView(R.id.tv_update_tip)
    TextView mTvUpdateTip;
    @BindView(R.id.fr_content)
    QMUIRelativeLayout mFrContent;
    @BindView(R.id.loadView)
    RxLoadingView mLoadView;


    private String filePath;
    private Context mContext;
    private boolean mustUpdate;
    private final String Dir = "/" + Key.COMPANY_KEY + "/";
    private String fileName = Key.COMPANY_KEY + ".zip";

    private BLEUpdateListener mBLEUpdateListener;

    public void setBLEUpdateListener(BLEUpdateListener BLEUpdateListener) {
        mBLEUpdateListener = BLEUpdateListener;
    }

    public interface BLEUpdateListener {
        void success();

        void fail();
    }

    public AboutUpdateDialog(Context context, String filePath, boolean mustUpdate) {
        super(context);
        View view = View.inflate(context, R.layout.dialog_recharge_tip, null);
        super.setContentView(view);
        ButterKnife.bind(this, view);

        mContext = context;
        this.mustUpdate = mustUpdate;


        this.setCanceledOnTouchOutside(false);
        setFullScreenWidth();


        mMRxTextRoundProgressBar.setMax(100);
        mMRxTextRoundProgressBar.setProgressText("");
        mMRxTextRoundProgressBar.setTextProgressSize(dp2px(10));
        mTvUpdateTip.setTypeface(MyAPP.typeface);

        //下载文件固件升级文件
        if (!RxDataUtils.isNullString(filePath))
            downLoadFile(filePath);

//        //演示使用本地DFU文件
//        startMyDFU(null);

        setOnDismissListener(dialog -> {
            DfuServiceListenerHelper.unregisterProgressListener(mContext, listenerAdapter);
            MyBleManager.Companion.setDFUStarting(false);
        });
    }

    private void downLoadFile(String filePath) {
        int lastIndexOf = filePath.lastIndexOf("/");
        fileName = filePath.substring(lastIndexOf + 1);
        RxLogUtils.i("文件名：" + fileName);


        NetManager.getApiService().downLoadFile(filePath)
                .subscribeOn(Schedulers.io())
                .map(body -> mFileDownLoadObserver.saveFile(body, Dir, fileName))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFileDownLoadObserver);

    }

    private FileDownLoadObserver<File> mFileDownLoadObserver = new FileDownLoadObserver<File>() {
        @Override
        public void onDownLoadSuccess(File o) {

            RxLogUtils.e("当前线程：" + Thread.currentThread().getName());
            mTvUpdateTip.setText("下载文件成功");

            startMyDFU(o);
        }

        @Override
        public void onDownLoadFail(Throwable throwable) {
            mTvUpdateTip.setText("下载文件失败");
            RxLogUtils.e(throwable.toString());
            setCanceledOnTouchOutside(true);

            RxLogUtils.e("当前线程：" + Thread.currentThread().getName());
            BuryingPoint.INSTANCE.netErrorMessage("DFUDownLoad", "DFU下载文件失败");
        }

        @Override
        public void onProgress(int progress, long total) {
            RxLogUtils.e("当前线程：" + Thread.currentThread().getName());
            mTvUpdateTip.post(() -> {
                mTvUpdateTip.setText("正在下载文件：" + progress + "%");
            });
        }
    };


    private void startMyDFU(File o) {
        if (o == null || !o.exists() || o.getAbsolutePath().equals("") || !o.getAbsolutePath().endsWith(".zip")) {
            mTvUpdateTip.setText("升级文件有误");
            setCanceledOnTouchOutside(true);
            return;
        }

        BluetoothDevice bluetoothDevice = MyBleManager.Companion.getInstance().getBluetoothDevice();
        if (bluetoothDevice == null) {
            mTvUpdateTip.setText("设备未连接");
            setCanceledOnTouchOutside(true);
            return;
        }

        final DfuServiceInitiator starter = new DfuServiceInitiator(bluetoothDevice.getAddress())
                .setDeviceName(bluetoothDevice.getName());
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
//        starter.setZip(R.raw.nrf52832_xxaa_app_7);
        starter.setZip(o.getPath());
        starter.start(mContext, DfuService.class);

        MyBleManager.Companion.getInstance().disConnect();
        MyBleManager.Companion.setDFUStarting(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(mContext);
        }
    }

    //拦截返回按钮
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return mustUpdate;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void show() {
        DfuServiceListenerHelper.registerProgressListener(mContext, listenerAdapter);
        super.show();
    }


    private final DfuProgressListenerAdapter listenerAdapter = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnected(String deviceAddress) {
            super.onDeviceConnected(deviceAddress);
            RxLogUtils.d("onDeviceConnected：" + deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            super.onDeviceDisconnected(deviceAddress);
            RxLogUtils.d("onDeviceDisconnected：" + deviceAddress);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            super.onDfuAborted(deviceAddress);
            RxLogUtils.d("onDfuAborted：" + deviceAddress);
            MyBleManager.Companion.setDFUStarting(false);
            mTvUpdateTip.setText("升级中断,请重试");
            setCanceledOnTouchOutside(true);
            if (mBLEUpdateListener != null)
                mBLEUpdateListener.fail();
            BuryingPoint.INSTANCE.clothingState(deviceAddress, "DFU升级中断,请重试");
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            super.onDfuProcessStarted(deviceAddress);
            RxLogUtils.d("onDfuProcessStarted");
            BuryingPoint.INSTANCE.clothingState(deviceAddress, "DFU升级开始");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            super.onDfuProcessStarting(deviceAddress);
            RxLogUtils.d("onDfuProcessStarting");
            mTvUpdateTip.setText("正在升级，请稍后...");

        }

        @Override
        public void onDeviceConnecting(String deviceAddress) {
            super.onDeviceConnecting(deviceAddress);
            RxLogUtils.d("onDeviceConnecting");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            super.onDeviceDisconnecting(deviceAddress);
            RxLogUtils.d("onDeviceDisconnecting");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            super.onDfuCompleted(deviceAddress);
            MyBleManager.Companion.setDFUStarting(false);
            RxLogUtils.d("onDfuCompleted");
            mTvUpdateTip.setText("升级完成");
            BuryingPoint.INSTANCE.clothingState(deviceAddress, "DFU升级完成");
            setCanceledOnTouchOutside(true);
            if (mBLEUpdateListener != null)
                mBLEUpdateListener.success();
            mMRxTextRoundProgressBar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismiss();
                }
            }, 2000);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            super.onEnablingDfuMode(deviceAddress);
            RxLogUtils.d("onEnablingDfuMode");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            super.onError(deviceAddress, error, errorType, message);
            RxLogUtils.e("onError:" + message);
            MyBleManager.Companion.setDFUStarting(false);
            mTvUpdateTip.setText("升级失败,请重试");
            BuryingPoint.INSTANCE.clothingState(deviceAddress, "DFU升级失败");
            setCanceledOnTouchOutside(true);
            if (mBLEUpdateListener != null)
                mBLEUpdateListener.fail();
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            super.onFirmwareValidating(deviceAddress);
            RxLogUtils.d("onFirmwareValidating");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
            RxLogUtils.d("onProgressChanged:" + "percent:" + percent + "----" + speed + "----avgSpeed:" + avgSpeed + "-----currentPart:" + currentPart + "------prtsTotal:" + partsTotal);
            mMRxTextRoundProgressBar.setProgress(percent, false);
            mMRxTextRoundProgressBar.setProgressText("");
            mLoadView.setVisibility(View.GONE);
            RxTextUtils.getBuilder((int) mMRxTextRoundProgressBar.getProgress() + "")
                    .append("\t%").setForegroundColor(Color.parseColor("#574D5F"))
                    .setProportion(0.5f).into(mTvProgress);
        }
    };


    public static int dp2px(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
}