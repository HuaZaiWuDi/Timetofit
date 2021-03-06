package lab.wesmartclothing.wefit.flyso.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created icon_hide_password jk on 2018/5/19.
 */
public class HeartRateProgressView extends View {

    private int mWidth;
    private int mHeight;
    private Paint paint;
    private Paint paint_progress;
    private int round;//圆角

    public static final int LINE_TYPE_SLIMMING = 1;//热身
    public static final int LINE_TYPE_GREASE = 2;//燃脂
    public static final int LINE_TYPE_AEROBIC = 3;//有氧
    public static final int LINE_TYPE_ANAEROBIC = 4;//无氧
    public static final int LINE_TYPE_LIMIT = 5;//极限


    //点阵集合
    private float end = 0;

    private int[] colors = new int[]{Color.parseColor("#29EBF2"), Color.parseColor("#76FFCD")};


    public HeartRateProgressView(Context context) {
        this(context, null);
    }

    public HeartRateProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeartRateProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);//设置为线条圆头
        paint.setColor(Color.parseColor("#FAFAFA"));


        paint_progress = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_progress.setDither(true);
        paint_progress.setAntiAlias(true);
        paint_progress.setStyle(Paint.Style.FILL);
        paint_progress.setStrokeCap(Paint.Cap.ROUND);//设置为线条圆头

    }


    public void setData(float end, int type) {
        this.end = end;

        switch (type) {
            case LINE_TYPE_SLIMMING:
                colors = new int[]{Color.parseColor("#29EBF2"), Color.parseColor("#76FFCD")};
                break;
            case LINE_TYPE_GREASE:
                colors = new int[]{Color.parseColor("#73FFF6"), Color.parseColor("#8FCCFF")};
                break;
            case LINE_TYPE_AEROBIC:
                colors = new int[]{Color.parseColor("#9DAFFB"), Color.parseColor("#9DD2FF")};
                break;
            case LINE_TYPE_ANAEROBIC:
                colors = new int[]{Color.parseColor("#E4ABFF"), Color.parseColor("#BEBDFF")};
                break;
            case LINE_TYPE_LIMIT:
                colors = new int[]{Color.parseColor("#EAADF9"), Color.parseColor("#F9ADAD")};
                break;
        }

        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        round = mHeight / 2;

        drawBg(canvas);

        drawProgress(canvas);
    }


    //画进度
    private void drawProgress(Canvas canvas) {
        float progress = (end * mWidth);
        canvas.save();
        LinearGradient linearGradient = new LinearGradient(0, 0, progress, 0, colors, null, Shader.TileMode.CLAMP);
        paint_progress.setShader(linearGradient);
        RectF rf = new RectF(0, 0, progress, mHeight);
        /*绘制圆角矩形，背景色为画笔颜色*/
        canvas.drawRoundRect(rf, round, round, paint_progress);
        canvas.restore();
    }

    //画进度的背景
    private void drawBg(Canvas canvas) {
        canvas.save();

        RectF rf = new RectF(0, 0, mWidth, mHeight);
        /*绘制圆角矩形，背景色为画笔颜色*/
        canvas.drawRoundRect(rf, round, round, paint);

        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);

        if (wMode == MeasureSpec.EXACTLY) {
            mWidth = wSize;
        } else {
            mWidth = dp2px(800);
        }
        if (hMode == MeasureSpec.EXACTLY) {
            mHeight = hSize;
        } else {
            mHeight = dp2px(20);
        }
        setMeasuredDimension(mWidth, mHeight);
    }

    public static int dp2px(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

}
