package com.example.botonpanicovertice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class AlertPieView extends View {

    public interface AlertPieListener {
        void onAlert(String alertType);
    }

    private Paint paint;
    private Paint textPaint;
    private List<AlertSection> sections = new ArrayList<>();
    private PointF center;
    private float radius;
    private float centerCircleRadius;
    private AlertPieListener listener;
    private static final String TAG = "AlertPieView";
    private int pressedSection = -1;

    public AlertPieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);

        center = new PointF();
    }

    public void setListener(AlertPieListener listener) {
        this.listener = listener;
    }

    public void addSection(String type, int color, Drawable icon) {
        sections.add(new AlertSection(type, color, icon));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float usableWidth = w - getPaddingLeft() - getPaddingRight();
        float usableHeight = h - getPaddingTop() - getPaddingBottom();
        radius = Math.min(usableWidth, usableHeight) / 2;
        center.x = getPaddingLeft() + usableWidth / 2;
        center.y = getPaddingTop() + usableHeight / 2;
        centerCircleRadius = radius / 3;
        updateSections(w, h);
    }

    private void updateSections(float width, float height) {
        if (sections.isEmpty()) return;

        float sweepAngle = 360f / sections.size();
        float startAngle = -90 - sweepAngle / 2;

        for (int i = 0; i < sections.size(); i++) {
            AlertSection section = sections.get(i);
            Path path = new Path();
            RectF rect = new RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
            path.moveTo(center.x, center.y);
            path.arcTo(rect, startAngle + i * sweepAngle, sweepAngle);
            path.close();
            section.setPath(path);

            // Posición del ícono
            float iconRadius = radius * 0.65f; // Mover los íconos más lejos del centro
            float angle = startAngle + i * sweepAngle + sweepAngle / 2;
            double rad = Math.toRadians(angle);
            float x = (float) (center.x + iconRadius * Math.cos(rad));
            float y = (float) (center.y + iconRadius * Math.sin(rad));
            section.setIconPosition(new PointF(x, y));
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sections.isEmpty() || center.x == 0) return;

        for (AlertSection section : sections) {
            paint.setColor(section.isPressed() ? Color.LTGRAY : section.getColor());
            canvas.drawPath(section.getPath(), paint);
            drawIconWithText(canvas, section);
        }

        paint.setColor(Color.WHITE);
        canvas.drawCircle(center.x, center.y, centerCircleRadius, paint);
    }

    private void drawIconWithText(Canvas canvas, AlertSection section) {
        Drawable icon = section.getIcon();
        PointF position = section.getIconPosition();
        if (icon == null || position == null) return;

        int iconSize = (int) (radius / 4); // Tamaño del ícono dinámico
        icon.setBounds((int) position.x - iconSize / 2, (int) position.y - iconSize / 2, (int) position.x + iconSize / 2, (int) position.y + iconSize / 2);
        icon.draw(canvas);

        // Dibujar el texto debajo del ícono
        float textY = position.y + iconSize / 2 + 40; // 40 es el offset para el texto
        canvas.drawText(section.getType(), position.x, textY, textPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int currentSection = getSectionAt(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedSection = currentSection;
                if (pressedSection != -1) {
                    sections.get(pressedSection).setPressed(true);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (currentSection != pressedSection) {
                    if (pressedSection != -1) {
                        sections.get(pressedSection).setPressed(false);
                    }
                    pressedSection = currentSection;
                    if (pressedSection != -1) {
                        sections.get(pressedSection).setPressed(true);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (pressedSection != -1) {
                    sections.get(pressedSection).setPressed(false);
                    invalidate();
                    if (listener != null) {
                        listener.onAlert(sections.get(pressedSection).getType());
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (pressedSection != -1) {
                    sections.get(pressedSection).setPressed(false);
                    invalidate();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private int getSectionAt(float x, float y) {
        float dx = x - center.x;
        float dy = y - center.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > radius || dist < centerCircleRadius) {
            return -1;
        }

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360;
        }

        float sweepAngle = 360f / sections.size();
        float startAngle = -90 - sweepAngle / 2;
        if (startAngle < 0) startAngle += 360;

        for (int i = 0; i < sections.size(); i++) {
            float sectionStartAngle = (startAngle + i * sweepAngle) % 360;
            float sectionEndAngle = (sectionStartAngle + sweepAngle) % 360;

            if (sectionEndAngle < sectionStartAngle) { // Cruza la línea de 0/360 grados
                if (angle >= sectionStartAngle || angle < sectionEndAngle) {
                    return i;
                }
            } else {
                if (angle >= sectionStartAngle && angle < sectionEndAngle) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static class AlertSection {
        private String type;
        private int color;
        private Drawable icon;
        private Path path;
        private PointF iconPosition;
        private boolean isPressed = false;

        AlertSection(String type, int color, Drawable icon) {
            this.type = type;
            this.color = color;
            this.icon = icon;
        }

        // Getters y Setters
        String getType() { return type; }
        int getColor() { return color; }
        Drawable getIcon() { return icon; }
        Path getPath() { return path; }
        void setPath(Path path) { this.path = path; }
        PointF getIconPosition() { return iconPosition; }
        void setIconPosition(PointF iconPosition) { this.iconPosition = iconPosition; }
        boolean isPressed() { return isPressed; }
        void setPressed(boolean pressed) { isPressed = pressed; }
    }
}
