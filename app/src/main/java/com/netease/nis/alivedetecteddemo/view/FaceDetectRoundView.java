package com.netease.nis.alivedetecteddemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 动作活体检测 overlay，参考百度 FaceDetectRoundView。
 *
 * 白色背景 + 圆形镂空 + 进度弧 + 双层错误/引导文字。
 *
 * API：
 *   setTipTopText(String)    — 顶部错误提示（红色）
 *   setTipSecondText(String) — 下方引导文字（黑色）
 *   setProcessCount(int total, int success) — 进度弧
 *   reset()
 */
public class FaceDetectRoundView extends RelativeLayout {

    private static final float WIDTH_SPACE_RATIO  = 0.33f;
    private static final float HEIGHT_OFFSET_RATIO = 0.10f;

    private float mX, mY, mR;

    private Paint mBgPaint;
    private Paint mFaceRoundPaint;
    private Paint mCircleBorderPaint1;
    private Paint mCircleBorderPaint2;
    private Paint mCirclePaint;
    private Paint mCircleSelectPaint;

    private int mTotalActiveCount   = 1;
    private int mSuccessActiveCount = 0;

    private InternalView mInternalView;
    private TextView     mTextViewTop;
    private TextView     mTextViewSecond;
    private LayoutParams mLayoutParamsTop;
    private LayoutParams mLayoutParamsSecond;

    public FaceDetectRoundView(Context context) {
        this(context, null);
    }

    public FaceDetectRoundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceDetectRoundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackgroundColor(Color.TRANSPARENT);
        initPaints();
        initViews(context);
    }

    private void initPaints() {
        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setColor(Color.WHITE);
        mBgPaint.setStyle(Paint.Style.FILL);

        mFaceRoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFaceRoundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mFaceRoundPaint.setStyle(Paint.Style.FILL);

        mCircleBorderPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircleBorderPaint1.setColor(Color.parseColor("#26171D24"));
        mCircleBorderPaint1.setStyle(Paint.Style.STROKE);
        mCircleBorderPaint1.setStrokeWidth(10f);

        mCircleBorderPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircleBorderPaint2.setColor(Color.parseColor("#0D171D24"));
        mCircleBorderPaint2.setStyle(Paint.Style.STROKE);
        mCircleBorderPaint2.setStrokeWidth(10f);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(Color.parseColor("#337C49F2"));
        mCirclePaint.setStrokeWidth(20f);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeCap(Paint.Cap.ROUND);

        mCircleSelectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircleSelectPaint.setColor(Color.parseColor("#CC21EA59"));
        mCircleSelectPaint.setStrokeWidth(20f);
        mCircleSelectPaint.setStyle(Paint.Style.STROKE);
        mCircleSelectPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    private void initViews(Context context) {
        mInternalView = new InternalView(context);
        mInternalView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        addView(mInternalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 引导文字（椭圆上方）
        mTextViewSecond = new TextView(context);
        mLayoutParamsSecond = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTextViewSecond.setMaxLines(2);
        mTextViewSecond.setTextColor(Color.parseColor("#1A1A1A"));
        mTextViewSecond.setTextSize(20f);
        mTextViewSecond.setGravity(android.view.Gravity.CENTER);
        mTextViewSecond.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        addView(mTextViewSecond, mLayoutParamsSecond);

        // 错误提示（引导文字上方）
        mTextViewTop = new TextView(context);
        mLayoutParamsTop = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTextViewTop.setMaxLines(2);
        mTextViewTop.setTextColor(Color.parseColor("#FF3B30"));
        mTextViewTop.setTextSize(24f);
        mTextViewTop.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mTextViewTop.setGravity(android.view.Gravity.CENTER);
        mTextViewTop.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        addView(mTextViewTop, mLayoutParamsTop);
    }

    // ── 公共 API ────────────────────────────────────────────────────────────

    public void setTipTopText(String text) {
        if (mTextViewTop != null) {
            mTextViewTop.post(() -> mTextViewTop.setText(text != null ? text : ""));
        }
    }

    public void setTipSecondText(String text) {
        if (mTextViewSecond != null) {
            mTextViewSecond.post(() -> {
                mTextViewSecond.setText(text != null ? text : "");
                if (!TextUtils.isEmpty(text)) mInternalView.invalidate();
            });
        }
    }

    public void setProcessCount(int total, int success) {
        mTotalActiveCount   = Math.max(1, total);
        mSuccessActiveCount = success;
        mInternalView.postInvalidate();
    }

    public void reset() {
        mTotalActiveCount   = 1;
        mSuccessActiveCount = 0;
        setTipTopText("");
        setTipSecondText("");
        mInternalView.postInvalidate();
    }

    // ── 生命周期 ─────────────────────────────────────────────────────────────

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!changed) return;

        float canvasWidth  = r - l;
        float canvasHeight = b - t;
        float x = canvasWidth  / 2f;
        float y = canvasHeight / 2f - canvasHeight / 2f * HEIGHT_OFFSET_RATIO;
        float radius = canvasWidth / 2f - canvasWidth / 2f * WIDTH_SPACE_RATIO;

        if ((canvasWidth / canvasHeight) > 0.7f) {
            canvasWidth = (r - l) / 1.5f;
            x = (r - l) / 2f;
            radius = canvasWidth / 2f - canvasWidth / 2f * WIDTH_SPACE_RATIO;
        }

        mX = x;
        mY = y;
        mR = radius;

        post(() -> {
            if (mTextViewSecond == null || mLayoutParamsSecond == null) return;
            float topSecond = mY - mR - 40f - 25f - dp(getContext(), 36);
            mLayoutParamsSecond.setMargins(0, (int) topSecond, 0, 0);
            mLayoutParamsSecond.addRule(RelativeLayout.CENTER_HORIZONTAL);
            mTextViewSecond.setLayoutParams(mLayoutParamsSecond);

            if (mTextViewTop == null || mLayoutParamsTop == null) return;
            float topTop = mY - mR - 40f - 25f - dp(getContext(), 98);
            mLayoutParamsTop.setMargins(0, (int) topTop, 0, 0);
            mLayoutParamsTop.addRule(RelativeLayout.CENTER_HORIZONTAL);
            mTextViewTop.setLayoutParams(mLayoutParamsTop);
        });
    }

    // ── 工具 ─────────────────────────────────────────────────────────────────

    private static int dp(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ── InternalView ─────────────────────────────────────────────────────────

    private class InternalView extends View {
        InternalView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mX == 0 || mR == 0) return;

            // 1. 白色背景
            canvas.drawColor(0);
            canvas.drawPaint(mBgPaint);

            // 2. CLEAR 圆形镂空
            canvas.drawCircle(mX, mY, mR, mFaceRoundPaint);

            // 3. 双层边框
            canvas.drawCircle(mX, mY, mR - 5f,  mCircleBorderPaint1);
            canvas.drawCircle(mX, mY, mR - 15f, mCircleBorderPaint2);

            // 4. 底圈（灰紫色）
            canvas.save();
            canvas.translate(mX, mY);
            canvas.rotate(-90f);
            RectF ringOval = new RectF(-mR - 20f, -mR - 20f, mR + 20f, mR + 20f);
            canvas.drawArc(ringOval, 0f, 360f, false, mCirclePaint);
            canvas.restore();

            // 5. 进度弧（绿色）
            int degree = (int) ((float) mSuccessActiveCount / mTotalActiveCount * 330f);
            if (degree > 0) {
                canvas.save();
                canvas.translate(mX, mY);
                canvas.rotate(-60f);
                canvas.drawArc(new RectF(-mR - 20f, -mR - 20f, mR + 20f, mR + 20f),
                        0f, degree, false, mCircleSelectPaint);
                canvas.restore();
            }
        }
    }
}
