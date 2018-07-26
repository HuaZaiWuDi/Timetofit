package lab.wesmartclothing.wefit.flyso.ui.main.slimming.sports;

import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.qmuiteam.qmui.arch.QMUIFragment;
import com.qmuiteam.qmui.widget.QMUITopBar;
import com.smartclothing.blelibrary.BleKey;
import com.smartclothing.blelibrary.BleTools;
import com.vondear.rxtools.dateUtils.RxFormat;
import com.vondear.rxtools.model.timer.MyTimer;
import com.vondear.rxtools.model.timer.MyTimerListener;
import com.vondear.rxtools.utils.RxFormatValue;
import com.vondear.rxtools.utils.RxLogUtils;
import com.vondear.rxtools.utils.SPUtils;
import com.vondear.rxtools.view.SwitchView;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;

import java.util.Arrays;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import lab.wesmartclothing.wefit.flyso.R;
import lab.wesmartclothing.wefit.flyso.base.BaseAcFragment;
import lab.wesmartclothing.wefit.flyso.rxbus.SportsDataTab;
import lab.wesmartclothing.wefit.flyso.tools.Key;
import lab.wesmartclothing.wefit.flyso.tools.SPKey;
import lab.wesmartclothing.wefit.netlib.utils.RxBus;

/**
 * Created by jk on 2018/7/19.
 */
@EFragment
public class SportingFragment extends BaseAcFragment {

    @BindView(R.id.QMUIAppBarLayout)
    QMUITopBar mQMUIAppBarLayout;
    @BindView(R.id.sportsStatus)
    TextView mSportsStatus;
    @BindView(R.id.tv_sportsStatus)
    TextView mTvSportsStatus;
    @BindView(R.id.sw_music)
    SwitchView mSwMusic;
    @BindView(R.id.tv_kcal)
    TextView mTvKcal;
    @BindView(R.id.tv_avHeartRate)
    TextView mTvAvHeartRate;
    @BindView(R.id.tv_maxHeartRate)
    TextView mTvMaxHeartRate;
    @BindView(R.id.tv_sportsTime)
    TextView mTvSportsTime;
    @BindView(R.id.layout_sports)
    LinearLayout mLayoutSports;
    @BindView(R.id.lineCHart_HeartRate)
    LineChart mMLineChart;
    Unbinder unbinder;


    public static QMUIFragment getInstance() {
        return new SportingFragment_();
    }

    private Button btn_Connect;

    TextToSpeech textToSpeech;

    private int currentTime = 0;


    //监听瘦身衣连接情况
    @Receiver(actions = Key.ACTION_CLOTHING_CONNECT)
    void clothingConnectStatus(@Receiver.Extra(Key.EXTRA_CLOTHING_CONNECT) boolean state) {
        if (state) {
            btn_Connect.setText(R.string.connected);
        } else {
            btn_Connect.setText(R.string.disConnected);
        }
    }

    @Override
    protected View onCreateView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fragment_sportsing, null);
        unbinder = ButterKnife.bind(this, view);
        initView();
        return view;
    }

    private void initView() {
        initRxBus();
        initChart(mMLineChart);
        Typeface typeface = Typeface.createFromAsset(mActivity.getAssets(), "fonts/DIN-Regular.ttf");
        mTvKcal.setTypeface(typeface);
        mTvAvHeartRate.setTypeface(typeface);
        mTvMaxHeartRate.setTypeface(typeface);
        mTvSportsTime.setTypeface(typeface);

        mQMUIAppBarLayout.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mQMUIAppBarLayout.setTitle("运动进行中");
        btn_Connect = mQMUIAppBarLayout.addRightTextButton(getString(BleTools.getInstance().isConnect() ? R.string.connected : R.string.disConnected), R.id.tv_connect);
        btn_Connect.setTextColor(Color.WHITE);
        btn_Connect.setTextSize(13);

        mSwMusic.setOnStateChangedListener(new SwitchView.OnStateChangedListener() {
            @Override
            public void toggleToOn(final SwitchView view) {
                openVoice();
                mSwMusic.setOpened(true);
                SPUtils.put(SPKey.SP_VoiceTip, true);
            }

            @Override
            public void toggleToOff(SwitchView view) {
                view.setOpened(false);
                SPUtils.put(SPKey.SP_VoiceTip, false);
                if (textToSpeech != null) {
                    textToSpeech.stop();
                }
            }
        });
        mSwMusic.setOpened(SPUtils.getBoolean(SPKey.SP_VoiceTip, false));
        if (mSwMusic.isOpened()) {
            openVoice();
        }
    }

    @Background
    public void openVoice() {
        if (textToSpeech == null)
            textToSpeech = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    RxLogUtils.e("语音合成：" + status);
                    // 判断是否转化成功
                    if (status == TextToSpeech.SUCCESS) {
                        //默认设定语言为中文，原生的android貌似不支持中文。
                        int result = textToSpeech.setLanguage(Locale.CHINESE);
                        RxLogUtils.e("支持中文:" + result);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            RxLogUtils.e("支持中文");
                        } else {
                            //不支持中文就将语言设置为英文
                            textToSpeech.setLanguage(Locale.US);
                        }
                    }
                }
            });
    }

    private void initRxBus() {
        Disposable register = RxBus.getInstance().register(SportsDataTab.class, new Consumer<SportsDataTab>() {
            @Override
            public void accept(SportsDataTab sportsDataTab) throws Exception {
                if (currentTime == 0) {
                    currentTime = sportsDataTab.getDuration();
                    timer.startTimer();
                    timer2.startTimer();
                    for (int heartRate : sportsDataTab.getAthlRecord_2()) {
                        setHeartRateData(heartRate);
                    }
                } else {
                    setHeartRateData(sportsDataTab.getCurHeart());
                }
                if (mSwMusic.isOpened())
                    heartRate(sportsDataTab.getCurHeart());
                mTvKcal.setText(RxFormatValue.fromat4S5R(sportsDataTab.getKcal(), 1));
                mTvAvHeartRate.setText(sportsDataTab.getCurHeart() + "");
                mTvMaxHeartRate.setText(sportsDataTab.getMaxHeart() + "");
            }
        });
        RxBus.getInstance().addSubscription(this, register);
    }

    private void initChart(LineChart lineChartBase) {
//        lineChartBase.setEnabled(false);
        lineChartBase.setMaxVisibleValueCount(60);
        lineChartBase.getLegend().setEnabled(false);
        lineChartBase.getDescription().setEnabled(false);
        lineChartBase.setTouchEnabled(true);//可以点击
        lineChartBase.setDragEnabled(true);
        lineChartBase.setScaleEnabled(false);
        lineChartBase.setPinchZoom(false);//X，Y轴缩放

        lineChartBase.setViewPortOffsets(0, 0, 160, 0);
//        lineChartBase.setBackgroundColor(getResources().getColor(R.color.gray_FBFBFC));
        XAxis xAxis = lineChartBase.getXAxis();
        xAxis.setEnabled(false);

        YAxis leftAxis = lineChartBase.getAxisLeft();
        YAxis rightAxis = lineChartBase.getAxisRight();
        rightAxis.setEnabled(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false);

        YAxis yAxis = lineChartBase.getAxisLeft();
        yAxis.setAxisMaximum(200);
        yAxis.setAxisMinimum(100);
        XAxis x = lineChartBase.getXAxis();
        x.setSpaceMin(14f);
        lineChartBase.setData(new LineData());
        addLimitLine2Y(lineChartBase, new float[]{120f, 140f, 160f, 180f}, new String[]{"", "", "", ""});
        lineChartBase.invalidate();

        mMLineChart.setVisibleXRangeMaximum(15);
    }


    private void setHeartRateData(int heartRate) {
        LineData data = mMLineChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        data.addEntry(new Entry(data.getDataSetByIndex(0).getEntryCount(), heartRate), 0);
        data.notifyDataChanged();

        mMLineChart.notifyDataSetChanged();

        mMLineChart.moveViewTo(data.getEntryCount() - 15, 50f, YAxis.AxisDependency.LEFT);
    }

    int type = 0;

    private void heartRate(int heart) {
        if (textToSpeech == null) return;

        byte[] heartRates = BleKey.heartRates;
        int heart_1 = heartRates[1] & 0xff;
        int heart_2 = heartRates[2] & 0xff;
        int heart_3 = heartRates[3] & 0xff;
        int heart_4 = heartRates[4] & 0xff;
        RxLogUtils.d("心率区间：" + Arrays.toString(heartRates));

        if ((heartRates[0] & 0xff) <= heart && heart <= heart_1) {
            if (type != 0) {
                mTvSportsStatus.setText("热身");
                textToSpeech.speak(getString(R.string.speech_warm), TextToSpeech.QUEUE_FLUSH, null);
                type = 0;
            }
        } else if (heart >= heart_1 && heart < heart_2) {
            if (type != 1) {
                mTvSportsStatus.setText("燃脂");
                textToSpeech.speak(getString(R.string.speech_grease), TextToSpeech.QUEUE_FLUSH, null);
                type = 1;
            }
        } else if (heart >= heart_2 && heart < heart_3) {
            if (type != 2) {
                mTvSportsStatus.setText("耐力");
                type = 2;
                textToSpeech.speak(getString(R.string.speech_aerobic), TextToSpeech.QUEUE_FLUSH, null);
            }
        } else if (heart >= heart_3 && heart < heart_4) {
            if (type != 3) {
                mTvSportsStatus.setText("无氧");
                type = 3;
                textToSpeech.speak(getString(R.string.speech_anaerobic), TextToSpeech.QUEUE_FLUSH, null);
            }
        } else if (heart >= heart_4) {
            if (type != 4) {
                mTvSportsStatus.setText("极限");
                type = 4;
                textToSpeech.speak(getString(R.string.speech_limit), TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    //添加提示线
    public void addLimitLine2Y(BarLineChartBase lineChartBase, float[] value, String[] label) {
        YAxis y = lineChartBase.getAxisLeft();
        y.removeAllLimitLines();
        if (value == null) return;
        for (int i = 0; i < value.length; i++) {
            //提示线，
            LimitLine ll = new LimitLine(value[i], label[i]);//线条颜色宽度等
            ll.setLineColor(getResources().getColor(R.color.GrayWrite));
            ll.setLineWidth(0.1f);
            //加入到 mXAxis 或 mYAxis
            y.addLimitLine(ll);
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "heartRate");
        set.setDrawIcons(false);
        set.setDrawValues(false);
        set.setColor(getResources().getColor(R.color.red));
        set.setDrawCircleHole(false);
        set.setDrawCircles(false);
        set.setLineWidth(3f);
        set.setHighlightEnabled(false);
//        set.setCircleColors();
//        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
//        set.setCubicIntensity(0.2f);
        return set;
    }

    MyTimer timer = new MyTimer(1000, new MyTimerListener() {
        @Override
        public void enterTimer() {
            currentTime++;
            mTvSportsTime.setText(RxFormat.setFormatDate(currentTime * 1000, "mm'ss''"));
        }
    });

    MyTimer timer2 = new MyTimer(2 * 60 * 1000, 2 * 60 * 1000, new MyTimerListener() {
        @Override
        public void enterTimer() {
            if (mSwMusic.isOpened())
                textToSpeech.speak(getString(R.string.speech_currentKcal, mTvKcal.getText()), TextToSpeech.QUEUE_FLUSH, null);
        }
    });


    @Override
    public void onDestroy() {
        timer.stopTimer();
        timer2.stopTimer();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        RxBus.getInstance().unSubscribe(this);
        super.onDestroy();
    }

    @OnClick(R.id.tv_kcal)
    public void onViewClicked() {


    }
}
