package com.lenovobuds.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Five draggable EQ points: 60 Hz, 230 Hz, 910 Hz, 3.6 kHz and 14 kHz. */
public final class EqualizerGraphView extends View {
    public interface OnGainsChanged { void onChanged(float[] gains); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] gains = new float[]{2.5f, 1.5f, 0f, 1f, 2f};
    private final String[] frequencyLabels = {"60", "230", "910", "3.6k", "14k"};
    private OnGainsChanged listener;
    private int activePoint = -1;
    private float density;
    private float left, right, top, bottom;

    public EqualizerGraphView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setOnGainsChanged(OnGainsChanged callback) { listener = callback; }

    public void setGains(float[] values) {
        if (values == null || values.length != 5) return;
        System.arraycopy(values, 0, gains, 0, 5);
        invalidate();
    }

    public float[] getGains() { return gains.clone(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = 20f * density;
        left = pad;
        right = getWidth() - pad;
        top = 12f * density;
        bottom = getHeight() - 34f * density;
        if (right <= left || bottom <= top) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFCFAFD);
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), 16f * density, 16f * density, paint);

        paint.setStrokeWidth(1f * density);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFFEDE6F1);
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            canvas.drawLine(left, y, right, y, paint);
        }
        for (int i = 0; i < 5; i++) {
            float x = pointX(i);
            canvas.drawLine(x, top, x, bottom, paint);
        }

        float zeroY = valueY(0f);
        Path fill = makeCurve();
        fill.lineTo(right, zeroY);
        fill.lineTo(left, zeroY);
        fill.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x223F51B5);
        canvas.drawPath(fill, paint);

        Path curve = makeCurve();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xFF9852C4);
        canvas.drawPath(curve, paint);

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 5; i++) {
            float x = pointX(i);
            float y = valueY(gains[i]);
            paint.setColor(0xFF9852C4);
            canvas.drawCircle(x, y, 8f * density, paint);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, 3.3f * density, paint);
            paint.setTextSize(9f * density);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(0xFF82788B);
            canvas.drawText(frequencyLabels[i], x, getHeight() - 8f * density, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(9f * density);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(0xFFAA9FB1);
        canvas.drawText("+8 dB", left, top - 2f * density, paint);
        canvas.drawText("0", left, zeroY - 4f * density, paint);
        canvas.drawText("−8 dB", left, bottom + 12f * density, paint);
    }

    private Path makeCurve() {
        Path path = new Path();
        path.moveTo(pointX(0), valueY(gains[0]));
        for (int i = 1; i < 5; i++) {
            float mid = (pointX(i - 1) + pointX(i)) / 2f;
            path.cubicTo(mid, valueY(gains[i - 1]), mid, valueY(gains[i]), pointX(i), valueY(gains[i]));
        }
        return path;
    }

    private float pointX(int index) { return left + (right - left) * index / 4f; }

    private float valueY(float value) {
        float clamped = Math.max(-8f, Math.min(8f, value));
        return top + (8f - clamped) / 16f * (bottom - top);
    }

    private float yValue(float y) {
        return Math.max(-8f, Math.min(8f, 8f - ((y - top) / (bottom - top)) * 16f));
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activePoint = Math.round(((x - left) / Math.max(1f, right - left)) * 4f);
                activePoint = Math.max(0, Math.min(4, activePoint));
                updatePoint(y);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activePoint >= 0) updatePoint(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activePoint >= 0) {
                    updatePoint(y);
                    activePoint = -1;
                    performClick();
                }
                return true;
        }
        return true;
    }

    private void updatePoint(float y) {
        if (activePoint < 0) return;
        gains[activePoint] = Math.round(yValue(y) * 2f) / 2f;
        invalidate();
        if (listener != null) listener.onChanged(gains.clone());
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
