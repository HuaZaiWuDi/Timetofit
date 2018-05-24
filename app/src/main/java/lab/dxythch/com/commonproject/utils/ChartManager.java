package lab.dxythch.com.commonproject.utils;

import android.content.Context;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import lab.dxythch.com.commonproject.R;

/**
 * Created by jk on 2018/5/23.
 */
public class ChartManager {
    private XAxis x;
    private YAxis y;
    private Context mContext;
    private LineChart mLineChart;

    //初始化图表
    public ChartManager(Context context, LineChart mLineChart) {
        mContext = context;
        this.mLineChart = mLineChart;
        // no description text
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setTouchEnabled(true);//可以点击
        // enable scaling and dragging
        mLineChart.setDragEnabled(true);
        mLineChart.setScaleEnabled(false);
        // if disabled, scaling can be done on x- and y-axis separately
        mLineChart.setPinchZoom(false);//X，Y轴缩放

        mLineChart.getAxisRight().setEnabled(false);
        mLineChart.getLegend().setEnabled(false);//关闭图例
        mLineChart.setAutoScaleMinMaxEnabled(false);
        mLineChart.setNoDataText("");//没有数据时显示
        mLineChart.setViewPortOffsets(10, 80, 10, 50);


        x = mLineChart.getXAxis();
        x.setTextColor(Color.WHITE);
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGridColor(context.getResources().getColor(R.color.lineColor));
        x.setAxisLineColor(Color.WHITE);
        x.setDrawAxisLine(true);
        x.setDrawLabels(true);
        x.setXOffset(10f);


        y = mLineChart.getAxisLeft();
        y.setDrawLimitLinesBehindData(true);
        y.setLabelCount(13, false);
        y.setTextColor(Color.WHITE);
        y.setDrawGridLines(true);
        y.setGridColor(context.getResources().getColor(R.color.lineColor));
        y.setAxisLineColor(Color.WHITE);
//        y.setGranularity(2f);// //设置最小间隔，防止当放大时出现重复标签
        y.setDrawAxisLine(false);
        y.setDrawLabels(false);
//        y.setAxisMaximum(150f);
//        y.setAxisMinimum(20f);


        mLineChart.invalidate();

    }


    //添加提示线
    public void addLimitLine2Y(float value, String label) {
        //提示线，
        LimitLine ll = new LimitLine(value, label);//线条颜色宽度等
        ll.setLineColor(mContext.getResources().getColor(R.color.colorTheme));
        ll.setLineWidth(2f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);//文字颜色、大小
        ll.setTextColor(mContext.getResources().getColor(R.color.white));
        ll.setTextSize(12f);

        YAxis y = mLineChart.getAxisLeft();
        y.removeAllLimitLines();
        //加入到 mXAxis 或 mYAxis
        y.addLimitLine(ll);
    }


    //添加Tip
    public void addMarker(MarkerView mv) {
        mv.setChartView(mLineChart); // For bounds control
        mLineChart.setMarker(mv); // Set the marker to the chart
        mLineChart.invalidate();
    }

    //添加数据
    public void setData(ArrayList<Entry> yVals, List<String> xVals) {
        addXLabel(xVals);

        // create a dataset and give it a type
        LineDataSet set1 = null;
        set1 = addLine(yVals, mContext.getString(R.string.heat));

        // create a data object with the datasets
        LineData data = new LineData(set1);
        data.setDrawValues(false);

        // set data
        mLineChart.setData(data);

        mLineChart.invalidate();
        mLineChart.setVisibleXRangeMaximum(7);
        mLineChart.animateX(1000);

    }

    //添加X轴标签
    private void addXLabel(List<String> days) {
        XAxis x = mLineChart.getXAxis();
        x.setValueFormatter(new MyXFormatter(days));
        mLineChart.invalidate();
    }

    //添加实线
    private LineDataSet addLine(ArrayList<Entry> YAxis, String label) {
        //热量
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(YAxis, label);
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set1.setCubicIntensity(0.2f);
        set1.setLineWidth(3f);
        set1.setDrawCircles(false);//是否显示节点圆心
        set1.setColor(Color.WHITE);
        set1.setDrawVerticalHighlightIndicator(false);
        set1.setDrawHorizontalHighlightIndicator(false);
        return set1;
    }


    //添加虚线
    private LineDataSet addDashedLine(ArrayList<Entry> YAxis, String label) {
        //心率
        LineDataSet set2 = new LineDataSet(YAxis, label);
        set2.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set2.enableDashedLine(20f, 10f, 20f); //设置线条为虚线 1.线条宽度2.间隔宽度3.角度
        set2.setLineWidth(3f);
        set2.setColor(Color.WHITE);
        set2.setDrawCircles(false);//是否显示节点圆心
        set2.setDrawVerticalHighlightIndicator(false);
        set2.setDrawHorizontalHighlightIndicator(false);
        set2.setHighlightEnabled(true);
        return set2;
    }

    //添加两条线
    public void setData(ArrayList<Entry> line1, ArrayList<Entry> line2, List<String> days) {
        addXLabel(days);

        LineDataSet set1, set2;

        set1 = addLine(line1, mContext.getString(R.string.heat));

        set2 = addDashedLine(line2, mContext.getString(R.string.sportsTime));

        // create a data object with the datasets
        LineData data = new LineData(set1, set2);
        data.setDrawValues(false);

        // set data
        mLineChart.setData(data);


        data.notifyDataChanged();
        mLineChart.notifyDataSetChanged();
        mLineChart.invalidate();

        // do not forget to refresh the chart
        mLineChart.setVisibleXRangeMaximum(7);
        mLineChart.animateX(1000);
    }


}