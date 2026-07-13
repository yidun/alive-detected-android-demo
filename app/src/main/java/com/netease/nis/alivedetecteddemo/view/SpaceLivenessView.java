package com.netease.nis.alivedetecteddemo.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 空间活体检测 overlay，参考百度 FaceDistanceDetectRoundView。
 *
 * 白色背景 + 椭圆镂空 + 旋转渐变线圈 + 双层文字。
 *
 * distanceType:
 *   DISTANCE_FAR  = 0  人离远，椭圆小（mR/1.5），引导靠近
 *   DISTANCE_NEAR = 1  人离近，椭圆大（mR），引导远离
 *
 * ringType:
 *   RING_TYPE_COLOR = 0  彩色渐变（引导中）
 *   RING_TYPE_BLUE  = 1  蓝色（保持中）
 *   RING_TYPE_GREEN = 2  绿色（通过）
 *
 * API：
 *   setTipTopText(String)    — 顶部错误提示（红色）
 *   setTipSecondText(String) — 下方引导文字（黑色）
 *   setTipText(String)       — 同 setTipSecondText（compat）
 *   setDistanceType(int)
 *   setRingType(int)
 *   startFarAnimation()      — 椭圆收缩（引导远离）
 *   startNearAnimation()     — 椭圆膨胀（引导靠近）
 *   reset()
 */
public class SpaceLivenessView extends RelativeLayout {

    // ── 距离常量 ──────────────────────────────────────────────────────────────
    public static final int DISTANCE_FAR  = 0;
    public static final int DISTANCE_NEAR = 1;

    // ── 线圈颜色常量 ──────────────────────────────────────────────────────────
    public static final int RING_TYPE_COLOR = 0;
    public static final int RING_TYPE_BLUE  = 1;
    public static final int RING_TYPE_GREEN = 2;

    // ── 旧常量兼容 ────────────────────────────────────────────────────────────
    public static final int STATE_INITIAL   = DISTANCE_FAR;
    public static final int STATE_TOO_NEAR  = DISTANCE_NEAR;
    public static final int STATE_IN_TARGET = 10;

    private static final float WIDTH_SPACE_RATIO   = 0.33f;
    private static final float HEIGHT_OFFSET_RATIO = 0.10f;

    // ── 椭圆几何 ──────────────────────────────────────────────────────────────
    private float mX, mY, mR;

    // ── 状态 ──────────────────────────────────────────────────────────────────
    private int   mCurrentDistanceType = DISTANCE_FAR;
    private int   mRingType            = RING_TYPE_COLOR;
    private float mProgress            = 0f;
    /** 椭圆缩放因子：1.0=大圆(mR)，1.5=小圆(mR/1.5)，动画始终从当前值出发，不跳变 */
    private float mOvalScale           = 1.5f;

    // ── Paint ─────────────────────────────────────────────────────────────────
    private Paint         mBGPaint;
    private Paint         mFaceRoundPaint;
    private Paint         mCircleBorderPaint1;
    private Paint         mCircleBorderPaint2;
    private Paint         mCirclePaint;   // 主旋转线圈（type 0/1）
    private Paint         mCirclePaint2;  // 绿色线圈（type 2）
    private SweepGradient sweepGradient;

    // ── Animator ──────────────────────────────────────────────────────────────
    private ValueAnimator rotateAnimator;
    private ValueAnimator scaleAnimator;

    // ── Views ─────────────────────────────────────────────────────────────────
    private InternalView mInternalView;
    private TextView     mTextViewSecond;          // 下方引导文字
    private TextView     mTextViewTop;             // 顶部错误提示
    private LayoutParams mLayoutParamsSecond;
    private LayoutParams mLayoutParamsTop;

    // ── 构造 ──────────────────────────────────────────────────────────────────

    public SpaceLivenessView(Context context) {
        this(context, null);
    }

    public SpaceLivenessView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpaceLivenessView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBackgroundColor(Color.TRANSPARENT);
        initPaints();
        initViews(context);
        startAnimDrawLine();
    }

    // ── Paint 初始化 ───────────────────────────────────────────────────────────

    private void initPaints() {
        mBGPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBGPaint.setColor(Color.WHITE);
        mBGPaint.setStyle(Paint.Style.FILL);

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
        mCirclePaint.setStrokeWidth(20f);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeCap(Paint.Cap.ROUND);

        mCirclePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint2.setColor(Color.parseColor("#FF30BF13"));
        mCirclePaint2.setStrokeWidth(20f);
        mCirclePaint2.setStyle(Paint.Style.STROKE);
        mCirclePaint2.setStrokeCap(Paint.Cap.ROUND);
    }

    // ── View 初始化 ────────────────────────────────────────────────────────────

    private void initViews(Context context) {
        mInternalView = new InternalView(context);
        mInternalView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        addView(mInternalView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // 下方引导文字
        mTextViewSecond = new TextView(context);
        mLayoutParamsSecond = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mTextViewSecond.setMaxLines(2);
        mTextViewSecond.setTextColor(Color.parseColor("#1A1A1A"));
        mTextViewSecond.setTextSize(20f);
        mTextViewSecond.setGravity(android.view.Gravity.CENTER);
        mTextViewSecond.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        addView(mTextViewSecond, mLayoutParamsSecond);

        // 顶部错误提示（红色加粗）
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

    // ── 公共 API ───────────────────────────────────────────────────────────────

    /** 错误提示，红色，显示在上方 */
    public void setTipTopText(String text) {
        if (mTextViewTop != null) {
            mTextViewTop.post(() -> {
                mTextViewTop.setText(text != null ? text : "");
                if (!TextUtils.isEmpty(text)) mInternalView.invalidate();
            });
        }
    }

    /** 引导文字，黑色，显示在下方 */
    public void setTipSecondText(String text) {
        if (mTextViewSecond != null) {
            mTextViewSecond.post(() -> {
                mTextViewSecond.setText(text != null ? text : "");
                if (!TextUtils.isEmpty(text)) mInternalView.invalidate();
            });
        }
    }

    /** compat，等同 setTipSecondText */
    public void setTipText(String text) {
        setTipSecondText(text);
    }

    public void setDistanceType(int type) {
        mCurrentDistanceType = type;
        mInternalView.postInvalidate();
    }

    public void setRingType(int type) {
        mRingType = type;
        mInternalView.postInvalidate();
    }

    /** 兼容旧 setDistanceState */
    public void setDistanceState(int state) {
        if (state == STATE_IN_TARGET) {
            setRingType(RING_TYPE_GREEN);
        } else {
            setDistanceType(state);
            setRingType(RING_TYPE_COLOR);
        }
    }

    /** 椭圆缩小至 1.5（引导"请远离"），从当前 mOvalScale 出发，不跳变 */
    public void startFarAnimation() {
        animateOvalScaleTo(1.5f);
    }

    /** 椭圆放大至 1.0（引导"请靠近"），从当前 mOvalScale 出发，不跳变 */
    public void startNearAnimation() {
        animateOvalScaleTo(1.0f);
    }

    private void animateOvalScaleTo(float target) {
        if (scaleAnimator != null) scaleAnimator.cancel();
        scaleAnimator = ValueAnimator.ofFloat(mOvalScale, target);
        scaleAnimator.setDuration(800L);
        scaleAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        scaleAnimator.setRepeatCount(0);
        scaleAnimator.addUpdateListener(anim -> {
            mOvalScale = (float) anim.getAnimatedValue();
            mInternalView.invalidate();
        });
        scaleAnimator.start();
    }

    public void reset() {
        mCurrentDistanceType = DISTANCE_FAR;
        mRingType            = RING_TYPE_COLOR;
        // 不重置 mOvalScale，保留当前椭圆大小，让下一阶段动画从此处平滑过渡
        if (scaleAnimator != null) scaleAnimator.cancel();
        setTipTopText("");
        setTipSecondText("");
        mInternalView.postInvalidate();
    }

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (rotateAnimator != null) rotateAnimator.cancel();
        if (scaleAnimator  != null) scaleAnimator.cancel();
    }

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
            // 引导文字：椭圆上方 48dp 处
            float topSecond = mY - mR - 40f - 25f - dp(getContext(), 48);
            mLayoutParamsSecond.setMargins(0, Math.max(0, (int) topSecond), 0, 0);
            mLayoutParamsSecond.addRule(RelativeLayout.CENTER_HORIZONTAL);
            mTextViewSecond.setLayoutParams(mLayoutParamsSecond);

            if (mTextViewTop == null || mLayoutParamsTop == null) return;
            // 错误提示：椭圆上方 105dp 处
            float topTop = mY - mR - 40f - 25f - dp(getContext(), 105);
            mLayoutParamsTop.setMargins(0, Math.max(0, (int) topTop), 0, 0);
            mLayoutParamsTop.addRule(RelativeLayout.CENTER_HORIZONTAL);
            mTextViewTop.setLayoutParams(mLayoutParamsTop);
        });
    }

    // ── 旋转动画 ───────────────────────────────────────────────────────────────

    private void startAnimDrawLine() {
        rotateAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotateAnimator.setDuration(3600L);
        rotateAnimator.setInterpolator(new LinearInterpolator());
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        rotateAnimator.addUpdateListener(anim -> {
            mProgress = (float) anim.getAnimatedValue();
            mInternalView.invalidate();
        });
        rotateAnimator.start();
    }

    // ── 工具 ───────────────────────────────────────────────────────────────────

    private static int dp(Context ctx, float dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ── InternalView（椭圆绘制，software layer）───────────────────────────────

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
            canvas.drawPaint(mBGPaint);

            // 2. 椭圆镂空 + 双层边框（mOvalScale: 1.0=大圆, 1.5=小圆）
            float rx = mR / mOvalScale;
            float ry = mR / mOvalScale + 50f;

            // 镂空
            canvas.drawOval(mX - rx, mY - ry, mX + rx, mY + ry, mFaceRoundPaint);
            // 内边框 1
            canvas.drawOval(mX - (rx - 5f), mY - (ry - 5f), mX + (rx - 5f), mY + (ry - 5f), mCircleBorderPaint1);
            // 内边框 2
            canvas.drawOval(mX - (rx - 15f), mY - (ry - 15f), mX + (rx - 15f), mY + (ry - 5f), mCircleBorderPaint2);

            // 3. 旋转线圈
            canvas.save();
            canvas.translate(mX, mY);
            drawOvalLine(canvas, rx, ry);
            canvas.restore();
        }

        private void drawOvalLine(Canvas canvas, float rx, float ry) {
            RectF oval = new RectF(-rx - 20f, -ry - 20f, rx + 20f, ry + 20f);
            Matrix matrix = new Matrix();
            matrix.setRotate(mProgress, 0f, 0f);

            if (mRingType == RING_TYPE_COLOR) {
                int[] colors = {
                        Color.parseColor("#A0F2D224"),
                        Color.parseColor("#80FFD000"),
                        Color.parseColor("#CC21EA59"),
                        Color.parseColor("#CC2468F2"),
                        Color.parseColor("#FF2468F2")
                };
                sweepGradient = new SweepGradient(0f, 0f, colors, null);
                sweepGradient.setLocalMatrix(matrix);
                mCirclePaint.setShader(sweepGradient);
                canvas.drawArc(oval, mProgress, 360f, false, mCirclePaint);
            } else if (mRingType == RING_TYPE_BLUE) {
                int[] colors = {
                        Color.parseColor("#1A0080FF"),
                        Color.parseColor("#6E0080FF"),
                        Color.parseColor("#CC2468F2"),
                        Color.parseColor("#CC2468F2"),
                        Color.parseColor("#FF2468F2")
                };
                sweepGradient = new SweepGradient(0f, 0f, colors, null);
                sweepGradient.setLocalMatrix(matrix);
                mCirclePaint.setShader(sweepGradient);
                canvas.drawArc(oval, mProgress, 360f, false, mCirclePaint);
            } else {
                // RING_TYPE_GREEN：绿色静止线圈
                canvas.drawArc(oval, mProgress, 360f, false, mCirclePaint2);
            }
        }
    }
}
