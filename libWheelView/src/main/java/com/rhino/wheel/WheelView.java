package com.rhino.wheel;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.OverScroller;


/**
 * @author LuoLin
 * @since Created on 2018/1/27.
 **/
public class WheelView extends View {

    private static final int HORIZONTAL = 0;
    private static final int VERTICAL = 1;
    private int mOrientation;

    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 2;
    private static final int DEFAULT_ITEM_VERTICAL_HEIGHT = 40;
    private static final float DEFAULT_ITEM_MIN_ALPHA = 0.1f;
    private static final int DEFAULT_ITEM_TEXT_SIZE = 30;
    private static final int DEFAULT_ITEM_TEXT_COLOR = Color.BLACK;
    private static final float DEFAULT_ITEM_SELECT_LINE_LENGTH_SCALE = 0f;
    private static final int DEFAULT_ITEM_SELECT_LINE_WIDTH = 1;
    private static final int DEFAULT_ITEM_SELECT_LINE_COLOR = 0x66000000;
    private static final boolean DEFAULT_ITEM_CYCLIC_ENABLE = true;
    private static final boolean DEFAULT_ITEM_SELECT_LINE_ENABLE = true;
    private static final int DEFAULT_ITEM_VISIBLE_COUNT = 7;
    private static final int DEFAULT_MIN_VALUE = 1;
    private static final int DEFAULT_MAX_VALUE = 10;
    private static final int DEFAULT_VALUE = 1;
    private int mItemVerticalHeight;
    private float mItemMinAlpha;
    private int mItemTextSize;
    private int mItemTextColor;
    private float mItemSelectLineLengthScale;
    private int mItemSelectLineWidth;
    private int mItemSelectLineColor;
    private boolean mItemCyclicEnable;
    private boolean mItemSelectLineEnable;
    private int mItemVisibleCount;
    private int mMinValue;
    private int mMaxValue;
    private int mValue;
    private String mLabel;

    private int mViewWidth;
    private int mViewHeight;
    private int mHalfWidth;

    private TextPaint mPaint;
    private Rect mItemSelectLineRect;
    private OverScroller mFlingScroller;
    private OverScroller mAdjustScroller;
    private VelocityTracker mVelocityTracker;
    private int[] mSelectorIndices;
    private ItemRect[] mItemPostions;
    private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<String>();
    private String[] mItemsDrawContents;

    private float mLastDownEventX;
    private float mLastDownOrMoveEventX;
    private float mLastDownEventY;
    private float mLastDownOrMoveEventY;
    private int mLastScrollerX;
    private int mLastScrollerY;
    private int mScrollState;
    private OnValueChangeListener mOnValueChangeListener;
    private OnValueChangeFinishListener changeFinishListener;
    private OnScrollListener mOnScrollListener;

    private int mTouchSlop;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;
    private int mTotalHeight;
    private int mOffsetTotalHeight;
    private boolean mWheelEnableScrollOffset = true;
    private boolean mHandleScrollChange;
    private int mCurrentScrollOffset;
    private int mInitialScrollOffset;
    private int mSelectorElementSize;
    private int mElementTrigSize;


    public WheelView(Context context) {
        this(context, null);
    }

    public WheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, 0);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            mViewWidth = widthSize;
        } else {
            mViewWidth = getWidth();
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mViewHeight = heightSize;
        } else {
            mViewHeight = getHeight();
        }
        initViewSize(mViewWidth, mViewHeight);
        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            if (null != mVelocityTracker) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
            return true;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastDownEventX = event.getX();
                mLastDownEventY = event.getY();
                if (!mFlingScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                } else if (!mAdjustScroller.isFinished()) {
                    mFlingScroller.forceFinished(true);
                    mAdjustScroller.forceFinished(true);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                break;
            case MotionEvent.ACTION_MOVE: {
                float currentMoveX = event.getX();
                float currentMoveY = event.getY();
                if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    int deltaDown;
                    if (mOrientation == VERTICAL) {
                        deltaDown = (int) Math.abs(currentMoveY - mLastDownEventY);
                    } else {
                        deltaDown = (int) Math.abs(currentMoveX - mLastDownEventX);
                    }

                    if (deltaDown > mTouchSlop) {
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }
                } else {
                    int deltaMoveX = (int) (currentMoveX - mLastDownOrMoveEventX);
                    int deltaMoveY = (int) (currentMoveY - mLastDownOrMoveEventY);
                    scrollBy(deltaMoveX, deltaMoveY);
                    invalidate();
                }
                mLastDownOrMoveEventX = currentMoveX;
                mLastDownOrMoveEventY = currentMoveY;
            }
            break;
            case MotionEvent.ACTION_UP:
                VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                int initialVelocity = mOrientation == VERTICAL ? (int) velocityTracker.getYVelocity() : (int) velocityTracker.getXVelocity();
                if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                    fling(initialVelocity);
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                } else {
                    int eventX = (int) event.getX();
                    int deltaMoveX = (int) Math.abs(eventX - mLastDownEventX);
                    if (deltaMoveX <= mTouchSlop) {
                        ensureScrollWheelAdjusted();
                    } else {
                        ensureScrollWheelAdjusted();
                    }
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void scrollBy(int x, int y) {
        int space = mOrientation == VERTICAL ? y : x;
        int[] selectorIndices = mSelectorIndices;
        if (space > 0 && isDecrementToEnd()) {
            return;
        }
        if (space < 0 && isIncrementToEnd()) {
            return;
        }
        mCurrentScrollOffset += space;
        while (mCurrentScrollOffset - mInitialScrollOffset > mElementTrigSize) {
            mCurrentScrollOffset -= mSelectorElementSize;
            decrementSelectorIndices(selectorIndices);
            setValueInternal(selectorIndices[mItemVisibleCount / 2], true);
            if (isDecrementToEnd()) {
                mCurrentScrollOffset = mInitialScrollOffset;
            }
        }
        while (mCurrentScrollOffset - mInitialScrollOffset < -mElementTrigSize) {
            mCurrentScrollOffset += mSelectorElementSize;
            incrementSelectorIndices(selectorIndices);
            setValueInternal(selectorIndices[mItemVisibleCount / 2], true);
            if (isIncrementToEnd()) {
                mCurrentScrollOffset = mInitialScrollOffset;
            }
        }
    }

    @Override
    public void computeScroll() {
        OverScroller scroller = mFlingScroller;
        if (scroller.isFinished()) {
            scroller = mAdjustScroller;
            if (scroller.isFinished()) {
                return;
            }
        }
        scroller.computeScrollOffset();
        if (mOrientation == VERTICAL) {
            int currentScrollerY = scroller.getCurrY();
            if (mLastScrollerY == 0) {
                mLastScrollerY = scroller.getStartY();
            }
            scrollBy(0, currentScrollerY - mLastScrollerY);
            mLastScrollerY = currentScrollerY;
        } else {
            int currentScrollerX = scroller.getCurrX();
            if (mLastScrollerX == 0) {
                mLastScrollerX = scroller.getStartX();
            }
            scrollBy(currentScrollerX - mLastScrollerX, 0);
            mLastScrollerX = currentScrollerX;
        }
        if (scroller.isFinished()) {
            onScrollerFinished(scroller);
        } else {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOrientation == VERTICAL) {
            drawVertical(canvas);
        } else {
            drawHorizontal(canvas);
        }
    }

    private void init(Context context, AttributeSet attrs) {
        if (null != attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
            mOrientation = a.getInt(R.styleable.WheelView_orientation, HORIZONTAL);
            mItemVerticalHeight = a.getDimensionPixelSize(R.styleable.WheelView_item_vertical_height, DEFAULT_ITEM_VERTICAL_HEIGHT);
            mItemMinAlpha = a.getFloat(R.styleable.WheelView_item_min_alpha_value, DEFAULT_ITEM_MIN_ALPHA);
            mItemTextSize = a.getDimensionPixelSize(R.styleable.WheelView_item_text_size, DEFAULT_ITEM_TEXT_SIZE);
            mItemTextColor = a.getColor(R.styleable.WheelView_item_text_color, DEFAULT_ITEM_TEXT_COLOR);
            mItemSelectLineLengthScale = a.getFloat(R.styleable.WheelView_item_select_line_length_scale, DEFAULT_ITEM_SELECT_LINE_LENGTH_SCALE);
            mItemSelectLineWidth = a.getDimensionPixelSize(R.styleable.WheelView_item_select_line_width, DEFAULT_ITEM_SELECT_LINE_WIDTH);
            mItemSelectLineColor = a.getColor(R.styleable.WheelView_item_select_line_color, DEFAULT_ITEM_SELECT_LINE_COLOR);
            mItemCyclicEnable = a.getBoolean(R.styleable.WheelView_item_cyclic_enable, DEFAULT_ITEM_CYCLIC_ENABLE);
            mItemSelectLineEnable = a.getBoolean(R.styleable.WheelView_item_select_line_enable, DEFAULT_ITEM_SELECT_LINE_ENABLE);
            mItemVisibleCount = a.getInt(R.styleable.WheelView_item_visible_count, DEFAULT_ITEM_VISIBLE_COUNT);
            mMinValue = a.getInt(R.styleable.WheelView_min_value, DEFAULT_MIN_VALUE);
            mMaxValue = a.getInt(R.styleable.WheelView_max_value, DEFAULT_MAX_VALUE);
            mValue = a.getInt(R.styleable.WheelView_value, DEFAULT_VALUE);
            mLabel = a.getString(R.styleable.WheelView_label);
            a.recycle();
        }

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity()
                / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;

        mFlingScroller = new OverScroller(context, new LinearInterpolator());
        mAdjustScroller = new OverScroller(context, new DecelerateInterpolator(2.5f));

        mPaint = new TextPaint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(mItemSelectLineWidth);

        mSelectorIndices = new int[mItemVisibleCount];
        mItemPostions = new ItemRect[mItemVisibleCount];

        mItemSelectLineRect = new Rect();
    }

    private void initViewSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        mHalfWidth = mViewWidth / 2;

        initializeSelectorWheel();
        if (mOrientation == VERTICAL) {
            initVerticalItems();
            int itemLineWidth = mViewWidth;
            if (mItemSelectLineLengthScale >= 0f && mItemSelectLineLengthScale <= 1f) {
                itemLineWidth = (int) (mViewWidth * mItemSelectLineLengthScale);
            } else if (mItemSelectLineLengthScale > 1f) {
                itemLineWidth = (int) mItemSelectLineLengthScale;
            }
            mItemSelectLineRect.set((mViewWidth - itemLineWidth) / 2, (mViewHeight - mSelectorElementSize) / 2,
                    (mViewWidth + itemLineWidth) / 2, (mViewHeight + mSelectorElementSize) / 2);
        } else {
            initHorizontalItems();
            int itemLineHeight = mViewHeight / 2;
            if (mItemSelectLineLengthScale >= 0f && mItemSelectLineLengthScale <= 1f) {
                itemLineHeight = (int) (mViewHeight / 2 * mItemSelectLineLengthScale);
            } else if (mItemSelectLineLengthScale > 1f) {
                itemLineHeight = (int) mItemSelectLineLengthScale;
            }
            mItemSelectLineRect.set((mViewWidth - mSelectorElementSize) / 2, (mViewHeight / 2 - itemLineHeight) / 2,
                    (mViewWidth + mSelectorElementSize) / 2, mViewHeight - (mViewHeight / 2 - itemLineHeight) / 2);
        }
    }

    private void initVerticalItems() {
        int halfItemViewWidth = mViewWidth / 2;
        int centerYCoor = mViewHeight / 2;
        final int itemCount = mItemVisibleCount;
        mTotalHeight = mItemVerticalHeight * mItemVisibleCount;
        mOffsetTotalHeight = (mViewHeight - mTotalHeight) / 2;

        for (int i = 0; i < mItemPostions.length; i++) {
            ItemRect itemRect = new VerticalItemRect(i);
            itemRect.updateCenterCoorY(centerYCoor + (i - itemCount / 2) * mSelectorElementSize);
            itemRect.updateCenterCoorX(halfItemViewWidth);
            mItemPostions[i] = itemRect;
        }
        if (null != mItemsDrawContents) {
            String labelWidthString = TextUtils.isEmpty(mLabel) ? "" : mLabel + mLabel + " ";
            String maxWidthString = getMaxWidthString(mItemsDrawContents) + labelWidthString;
            float fac = mItemPostions[mItemVisibleCount / 2].getFac(0);
            int txtMeasureSize = measureTextSize((int) (fac * mViewWidth), maxWidthString);
//            int txtMeasureSize = measureTextSize(mViewWidth, maxWidthString);
            if (txtMeasureSize < mItemTextSize) {
                mItemTextSize = txtMeasureSize;
            }
            if (mItemSelectLineEnable) {
                int maxTextSize = measureTextSize((int) (mViewWidth * mItemSelectLineLengthScale), maxWidthString);
                if (maxTextSize < mItemTextSize) {
                    mItemTextSize = maxTextSize;
                }
            }
        }
    }

    private void initHorizontalItems() {
        int halfItemViewWidth = mSelectorElementSize / 2;
        for (int i = 0; i < mItemPostions.length; i++) {
            ItemRect itemRect = new HorizontalItemRect(mSelectorElementSize);
            itemRect.updateCenterCoorY(mViewHeight / 2);
            itemRect.updateCenterCoorX(halfItemViewWidth + mSelectorElementSize * i);
            mItemPostions[i] = itemRect;
        }
        if (null != mItemsDrawContents) {
            String maxWidthString = getMaxWidthString(mItemsDrawContents);
            float fac = mItemPostions[mItemVisibleCount / 2].getFac(0);
            int txtMeasureSize = measureTextSize((int) (fac * mSelectorElementSize), maxWidthString);
            if (txtMeasureSize < mItemTextSize) {
                mItemTextSize = txtMeasureSize;
            }
            if (mItemSelectLineEnable) {
                int maxTextSize = (int) (mViewHeight * (0.5 - mItemSelectLineLengthScale));
                if (maxTextSize < mItemTextSize) {
                    mItemTextSize = maxTextSize;
                }
            }
        }
    }

    private void drawHorizontal(Canvas canvas) {
        int color = mItemTextColor;
        int offset = mCurrentScrollOffset;
        for (int i = 0; i < mItemVisibleCount; i++) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(color);
            mPaint.setTextAlign(Align.CENTER);

            ItemRect ir = mItemPostions[i];
            float x = ir.getDrawX(offset);
            float y = ir.getRealY();
            float f = ir.getFac(mCurrentScrollOffset);
            String txt = mSelectorIndexToStringCache.get(mSelectorIndices[i]);
            int alpha = (int) (ir.getItemMinAlpha(mCurrentScrollOffset) * 255);

            mPaint.setTextSize(f * mItemTextSize);

            FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            int baseline = (int) (y - (fontMetrics.bottom + fontMetrics.top) / 2);

            if (null != txt) {
                mPaint.setAlpha(alpha);
                canvas.drawText(txt, x, baseline, mPaint);
            }
        }
        if (mItemSelectLineEnable) {
            mPaint.setColor(mItemSelectLineColor);
            canvas.drawLine(mItemSelectLineRect.centerX(), mItemSelectLineRect.centerY() - mItemSelectLineRect.top,
                    mItemSelectLineRect.centerX(), mItemSelectLineRect.top, mPaint);
            canvas.drawLine(mItemSelectLineRect.centerX(), mItemSelectLineRect.centerY() + mItemSelectLineRect.top,
                    mItemSelectLineRect.centerX(), mItemSelectLineRect.bottom, mPaint);
        }
    }

    private void drawVertical(Canvas canvas) {
        int color = mItemTextColor;
        int offset = mCurrentScrollOffset;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
        mPaint.setTextAlign(Align.CENTER);

        for (int i = 0; i < mItemVisibleCount; i++) {
            ItemRect ir = mItemPostions[i];
            float x = ir.getRealX();
            float y = mWheelEnableScrollOffset ? ir.getDrawY(offset) : (offset + ir.getRealY());
            float f = ir.getFac(offset);
            String txt = mSelectorIndexToStringCache.get(mSelectorIndices[i]);
            int alpha = (int) (ir.getItemMinAlpha(offset) * 255);

            mPaint.setTextSize(f * mItemTextSize);

            FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            int baseline = (int) (y - (fontMetrics.bottom + fontMetrics.top) / 2);

            if (null != txt) {
                mPaint.setAlpha(alpha);
                canvas.drawText(txt, x, baseline, mPaint);
            }
        }
        if (!TextUtils.isEmpty(mLabel)) {
            mPaint.setAlpha(255);
            mPaint.setTextSize(mItemTextSize);
            ItemRect ir = mItemPostions[mItemVisibleCount / 2];
            String maxWidthString = getMaxWidthString(mItemsDrawContents);
            float x = ir.getRealX() + mPaint.measureText(maxWidthString)/2 + mPaint.measureText(" " + mLabel)/2;
            float y = mViewHeight / 2;
            FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            int baseline = (int) (y - (fontMetrics.bottom + fontMetrics.top) / 2);
            canvas.drawText(mLabel, x, baseline, mPaint);
        }
        if (mItemSelectLineEnable) {
            mPaint.setColor(mItemSelectLineColor);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(mItemSelectLineRect.left, mItemSelectLineRect.top, mItemSelectLineRect.right, mItemSelectLineRect.top, mPaint);
            canvas.drawLine(mItemSelectLineRect.left, mItemSelectLineRect.bottom, mItemSelectLineRect.right, mItemSelectLineRect.bottom, mPaint);
        }
    }

    private void checkInitItemsDrawContents() {
        int size = mMaxValue - mMinValue + 1;
        if (mItemsDrawContents == null || mItemsDrawContents.length != size) {
            mItemsDrawContents = new String[size];
            for (int value = mMinValue; value <= mMaxValue; value++) {
                mItemsDrawContents[value - mMinValue] = String.valueOf(value);
            }
        }
    }

    @NonNull
    private String getMaxWidthString(String... arrs) {
        if (arrs == null) {
            return "";
        }
        String txt;
        Paint paint = new Paint();
        paint.setTextSize(10);
        int idx = 0;
        float curw = 0;
        for (int i = 0; i < arrs.length; i++) {
            txt = arrs[i];
            if (null != txt && txt.length() > 0) {
                float w = paint.measureText(txt);
                if (w >= curw) {
                    curw = w;
                    idx = i;
                }
            }
        }
        return arrs[idx];
    }

    private int measureTextSize(final int maxWidth, String text) {
        Paint paint = new Paint();
        int textSize = 1;
        if (maxWidth <= 0 || TextUtils.isEmpty(text)) {
            textSize = 0;
        } else {
            for (; ; ) {
                textSize++;
                paint.setTextSize(textSize);
                int mw = (int) paint.measureText(text);
                if (mw > maxWidth) {
                    textSize--;
                    for (; ; ) {
                        textSize--;
                        paint.setTextSize(textSize);
                        mw = (int) paint.measureText(text);
                        if (mw <= maxWidth) {
                            return textSize;
                        }
                    }
                }
            }
        }
        return textSize;
    }

    private void fling(int velocity) {
        mLastScrollerX = 0;
        mLastScrollerY = 0;

        if (mOrientation == VERTICAL) {
            if (velocity > 0) {
                mFlingScroller.fling(0, 0, 0, velocity, 0, 0, 0, Integer.MAX_VALUE);
            } else {
                mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocity, 0, 0, 0, Integer.MAX_VALUE);
            }

        } else {
            if (velocity > 0) {
                mFlingScroller.fling(0, 0, velocity, 0, 0, Integer.MAX_VALUE, 0, 0);
            } else {
                mFlingScroller.fling(Integer.MAX_VALUE, 0, velocity, 0, 0, Integer.MAX_VALUE, 0, 0);
            }
        }

        invalidate();
    }

    private boolean ensureScrollWheelAdjusted() {
        if (null != changeFinishListener && !mHandleScrollChange) {
            changeFinishListener.onValueChange(this, mValue);
        }
        mHandleScrollChange = false;

        int delta = mInitialScrollOffset - mCurrentScrollOffset;
        if (delta != 0) {
            mLastScrollerX = 0;
            mLastScrollerY = 0;
            if (Math.abs(delta) > mSelectorElementSize / 2) {
                delta += (delta > 0) ? -mSelectorElementSize : mSelectorElementSize;
            }
            if (mOrientation == VERTICAL) {
                mAdjustScroller.startScroll(0, 0, 0, delta, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            } else {
                mAdjustScroller.startScroll(0, 0, delta, 0, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            }

            postInvalidate();
            return true;
        }
        return false;
    }

    private void onScrollerFinished(OverScroller scroller) {
        if (scroller == mFlingScroller) {
            ensureScrollWheelAdjusted();
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        } else {
            invalidate();
        }
    }

    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(this, scrollState);
        }
    }

    private void notifyChange(int previous, int current) {
        if (mOnValueChangeListener != null && !mHandleScrollChange) {
            mOnValueChangeListener.onValueChange(this, previous, mValue);
        }
    }


    private void initializeSelectorWheel() {
        initializeSelectorWheelIndices();
        mInitialScrollOffset = 0;
        mCurrentScrollOffset = mInitialScrollOffset;

        mSelectorElementSize = getSelectorElementSize();
        mElementTrigSize = mSelectorElementSize / 2;
    }

    private int getSelectorElementSize() {
        if (mOrientation == VERTICAL) {
            return mItemVerticalHeight;
        } else {
            return mViewWidth / mItemVisibleCount;
        }
    }

    private void setValueInternal(int current, boolean notifyChange) {
        if (mValue == current) {
            return;
        }
        if (mItemCyclicEnable) {
            current = getWrappedSelectorIndex(current);
        } else {
            current = Math.max(current, mMinValue);
            current = Math.min(current, mMaxValue);
        }
        int previous = mValue;
        mValue = current;
        if (notifyChange) {
            notifyChange(previous, current);
        }
        initializeSelectorWheelIndices();
        invalidate();
    }

    private boolean isDecrementToEnd() {
        return !mItemCyclicEnable && mSelectorIndices[mItemVisibleCount / 2] <= mMinValue;
    }

    private boolean isIncrementToEnd() {
        return !mItemCyclicEnable && mSelectorIndices[mItemVisibleCount / 2] >= mMaxValue;
    }

    private void incrementSelectorIndices(int[] selectorIndices) {
        for (int i = 0; i < selectorIndices.length - 1; i++) {
            selectorIndices[i] = selectorIndices[i + 1];
        }
        int nextScrollSelectorIndex = selectorIndices[selectorIndices.length - 2] + 1;
        if (mItemCyclicEnable && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue;
        }
        selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
    }

    private void decrementSelectorIndices(int[] selectorIndices) {
        for (int i = selectorIndices.length - 1; i > 0; i--) {
            selectorIndices[i] = selectorIndices[i - 1];
        }
        int nextScrollSelectorIndex = selectorIndices[1] - 1;
        if (mItemCyclicEnable && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue;
        }
        selectorIndices[0] = nextScrollSelectorIndex;
    }

    private void initializeSelectorWheelIndices() {
        checkInitItemsDrawContents();
        mSelectorIndexToStringCache.clear();
        int[] selectorIndices = mSelectorIndices;
        int current = getValue();
        for (int i = 0; i < mSelectorIndices.length; i++) {
            int selectorIndex = current + (i - mItemVisibleCount / 2);
            if (mItemCyclicEnable) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex);
            }
            selectorIndices[i] = selectorIndex;
            ensureCachedScrollSelectorValue(selectorIndices[i]);
        }
    }

    private int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
        }
        return selectorIndex;
    }

    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = "";
        } else {
            if (mItemsDrawContents != null) {
                int displayedValueIndex = selectorIndex - mMinValue;
                scrollSelectorValue = mItemsDrawContents[displayedValueIndex];
            } else {
                scrollSelectorValue = "" + selectorIndex;
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    public interface ItemRect {
        void updateCenterCoorX(int x);

        void updateCenterCoorY(int y);

        int getRealX();

        int getDrawX(int offset);

        int getRealY();

        int getDrawY(int offset);

        int getDrawSize(int offset);

        float getFac(int offset);

        float getItemMinAlpha(int offset);
    }

    private class VerticalItemRect implements ItemRect {
        private int mItemCenterCoorX;
        private int mItemCenterCoorY;
        private int mPosition;

        public VerticalItemRect(int position) {
            this.mPosition = position;
        }

        @Override
        public void updateCenterCoorX(int x) {
            mItemCenterCoorX = x;
        }

        @Override
        public void updateCenterCoorY(int y) {
            mItemCenterCoorY = y;
        }

        @Override
        public int getRealX() {
            return mItemCenterCoorX;
        }

        @Override
        public int getDrawX(int offset) {
            return mItemCenterCoorX;
        }

        @Override
        public int getRealY() {
            return mItemCenterCoorY;
        }

        @Override
        public int getDrawY(int offset) {
            int cy = offset + mItemCenterCoorY;
            if (cy > (mViewHeight / 2)) {
                return (int) (mViewHeight - (mViewHeight - cy) * getFac(offset));
            } else {
                return (int) (getFac(offset) * cy);
            }
        }

        @Override
        public int getDrawSize(int offset) {
            return 0;
        }

        @Override
        public float getFac(int offset) {
            float fac = getFac1(offset);
            fac = 0.6f + 0.4f * fac * fac;
            return fac;
        }

        @Override
        public float getItemMinAlpha(int offset) {
            float fac = getFac1(offset);
            fac = mItemMinAlpha + (1 - mItemMinAlpha) * fac * fac;
            return fac;
        }

        private float getFac1(int offset) {
            float fac;
            int cy = mItemCenterCoorY + offset - mOffsetTotalHeight;
            int halfHeight = mTotalHeight / 2;
            if (cy > halfHeight) {
                fac = (mTotalHeight - cy) / (mTotalHeight * 0.5f);
            } else {
                fac = cy / (mTotalHeight * 0.5f);
            }
            return fac;
        }
    }

    private class HorizontalItemRect implements ItemRect {

        private int mItemCenterCoorX;
        private int mItemCenterCoorY;
        private int mItemSize;

        public HorizontalItemRect(int itemSize) {
            this.mItemSize = itemSize;
        }

        @Override
        public void updateCenterCoorX(int x) {
            this.mItemCenterCoorX = x;
        }

        @Override
        public void updateCenterCoorY(int y) {
            this.mItemCenterCoorY = y;
        }

        @Override
        public int getRealX() {
            return mItemCenterCoorX;
        }

        @Override
        public int getDrawX(int offset) {
            int curCoorX = mItemCenterCoorX + offset;
            if (curCoorX > mHalfWidth) {
                return (int) (mViewWidth - (mViewWidth - curCoorX) * getFac(offset));
            } else {
                return (int) (curCoorX * getFac(offset));
            }
        }

        @Override
        public int getRealY() {
            return mItemCenterCoorY;
        }

        @Override
        public int getDrawY(int offset) {
            return (int) (mItemCenterCoorY * getFac(offset));
        }

        @Override
        public int getDrawSize(int offset) {
            return (int) (mItemSize * getFac(offset));
        }

        @Override
        public float getFac(int offset) {
            int curCoorX = mItemCenterCoorX + offset;
            float fac;
            if (curCoorX > mHalfWidth) {
                fac = (mViewWidth - curCoorX) / (mHalfWidth * 1f);
            } else {
                fac = curCoorX / (mHalfWidth * 1f);
            }
            fac = 0.6f + 0.4f * fac * fac;
            return fac;
        }

        @Override
        public float getItemMinAlpha(int offset) {
            int curCoorX = mItemCenterCoorX + offset;
            float fac;
            if (curCoorX > mHalfWidth) {
                fac = (mViewWidth - curCoorX) / (mHalfWidth * 1f);
            } else {
                fac = curCoorX / (mHalfWidth * 1f);
            }
            fac = mItemMinAlpha + (1 - mItemMinAlpha) * fac * fac;
            return fac;
        }
    }

    public interface OnValueChangeListener {
        void onValueChange(WheelView picker, int oldVal, int newVal);
    }

    public interface OnValueChangeFinishListener {
        void onValueChange(WheelView picker, int value);
    }

    public interface OnScrollListener {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChange(WheelView view, int scrollState);
    }

    public void setMinValue(int minValue) {
        if (mMinValue == minValue) {
            return;
        }
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue must be >= 0");
        }
        mMinValue = minValue;
        if (mMinValue > mValue) {
            mValue = mMinValue;
        }
        initializeSelectorWheelIndices();
        invalidate();
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMaxValue(int maxValue) {
        if (mMaxValue == maxValue) {
            return;
        }
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        mMaxValue = maxValue;
        if (mMaxValue < mValue) {
            mValue = mMaxValue;
        }
        initializeSelectorWheelIndices();
        invalidate();
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setValue(int value) {
        setValueInternal(value, false);
    }

    public int getValue() {
        return mValue;
    }

    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public void setItemVerticalHeight(int mItemVerticalHeight) {
        this.mItemVerticalHeight = mItemVerticalHeight;
        initViewSize(mViewWidth, mViewHeight);
    }

    public void setItemMinAlpha(float mItemMinAlpha) {
        this.mItemMinAlpha = mItemMinAlpha;
    }

    public void setItemTextSize(int mItemTextSize) {
        this.mItemTextSize = mItemTextSize;
    }

    public void setItemTextColor(int mItemTextColor) {
        this.mItemTextColor = mItemTextColor;
    }

    public void setItemSelectLineLengthScale(float mItemSelectLineLengthScale) {
        this.mItemSelectLineLengthScale = mItemSelectLineLengthScale;
        initViewSize(mViewWidth, mViewHeight);
    }

    public void setItemSelectLineWidth(int mItemSelectLineWidth) {
        this.mItemSelectLineWidth = mItemSelectLineWidth;
        if (mPaint != null) {
            mPaint.setStrokeWidth(mItemSelectLineWidth);
        }
    }

    public void setItemSelectLineColor(int mItemSelectLineColor) {
        this.mItemSelectLineColor = mItemSelectLineColor;
    }

    public void setEnableItemOffset(boolean itemScale) {
        mWheelEnableScrollOffset = itemScale;
    }

    public void setItemVisibleCount(int count) {
        mItemVisibleCount = (count <= 2 ? DEFAULT_ITEM_VISIBLE_COUNT : count);
        mItemPostions = new ItemRect[mItemVisibleCount];
        mSelectorIndices = new int[mItemVisibleCount];
        initViewSize(mViewWidth, mViewHeight);
    }

    public int getItemVisibleCount() {
        return mItemVisibleCount;
    }

    public void setItemSelectLineEnable(boolean show) {
        this.mItemSelectLineEnable = show;
    }

    public void setItemCyclicEnable(boolean wrapSelectorWheel) {
        mItemCyclicEnable = wrapSelectorWheel;
        initializeSelectorWheelIndices();
    }

    public boolean isItemCyclicEnable() {
        return mItemCyclicEnable;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public void setDisplayedValues(String[] displayedValues) {
        mItemsDrawContents = displayedValues;
        if (displayedValues != null) {
            if (mItemVisibleCount > displayedValues.length) {
                setItemVisibleCount(displayedValues.length);
            }
            setMinValue(1);
            setMaxValue(displayedValues.length - 1);
            initViewSize(mViewWidth, mViewHeight);
        }
        initializeSelectorWheelIndices();
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public void setOnValueChangeFinishListener(OnValueChangeFinishListener listener) {
        this.changeFinishListener = listener;
    }

    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangeListener = onValueChangedListener;
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

}
