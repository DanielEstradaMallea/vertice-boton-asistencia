package com.example.botonpanicovertice;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;

public class AlertPieView extends View {

    public interface OnAlertListener {
        void onAlert(String alertType);
    }

    private enum Section {
        SEGURIDAD, SALUD, INCENDIO, ASISTENCIA, NONE
    }

    private static final int LONG_PRESS_DURATION = 3000;
    private static final float MARGIN_ANGLE = 2.5f;
    private static final float CORNER_RADIUS = 20f;

    private Paint paintSeguridad, paintSalud, paintIncendio, paintAsistencia, textPaint, progressPaint;
    private RectF pieBounds;
    private Drawable iconSeguridad, iconSalud, iconIncendio, iconAsistencia;

    private float centerX, centerY, radius;
    private Section touchedSection = Section.NONE;
    private ValueAnimator progressAnimator;
    private ValueAnimator scaleAnimator;
    private float animationProgress = 0f;
    private float currentScale = 1f;

    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;

    private OnAlertListener onAlertListener;

    public AlertPieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnAlertListener(OnAlertListener listener) {
        this.onAlertListener = listener;
    }

    private void init() {
        paintSeguridad = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSeguridad.setStyle(Paint.Style.FILL);
        paintSeguridad.setColor(ContextCompat.getColor(getContext(), R.color.seguridad)); //

        paintSalud = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSalud.setStyle(Paint.Style.FILL);
        paintSalud.setColor(ContextCompat.getColor(getContext(), R.color.salud)); //

        paintIncendio = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintIncendio.setStyle(Paint.Style.FILL);
        paintIncendio.setColor(ContextCompat.getColor(getContext(), R.color.incendio)); //

        paintAsistencia = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAsistencia.setStyle(Paint.Style.FILL);
        paintAsistencia.setColor(ContextCompat.getColor(getContext(), R.color.asistencia)); //

        // --- INICIO DE CAMBIO ---
        // Texto de las secciones (Seguridad, Salud, etc.) más pequeño y en negrita
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34); // Reducido de 40
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true); // Añadido
        // --- FIN DE CAMBIO ---

        // El titlePaint ya no es necesario aquí si se quita de onDraw
        // titlePaint = ...

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(Color.argb(128, 255, 255, 255));

        pieBounds = new RectF();

        // Carga de iconos desde los archivos del proyecto
        iconSeguridad = ContextCompat.getDrawable(getContext(), R.drawable.ic_security); //
        iconSalud = ContextCompat.getDrawable(getContext(), R.drawable.ic_health); //
        iconIncendio = ContextCompat.getDrawable(getContext(), R.drawable.ic_fire); //
        iconAsistencia = ContextCompat.getDrawable(getContext(), R.drawable.ic_assistance); //
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f * 0.9f;
        pieBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float sweepAngle = 90 - MARGIN_ANGLE;

        drawSection(canvas, 180 + MARGIN_ANGLE / 2, sweepAngle, paintSeguridad, Section.SEGURIDAD);
        drawSection(canvas, 270 + MARGIN_ANGLE / 2, sweepAngle, paintSalud, Section.SALUD);
        drawSection(canvas, 0 + MARGIN_ANGLE / 2, sweepAngle, paintIncendio, Section.INCENDIO);
        drawSection(canvas, 90 + MARGIN_ANGLE / 2, sweepAngle, paintAsistencia, Section.ASISTENCIA);

        drawSectionContent(canvas, "Seguridad", iconSeguridad, 225);
        drawSectionContent(canvas, "Salud", iconSalud, 315);
        drawSectionContent(canvas, "Incendio", iconIncendio, 45);
        drawSectionContent(canvas, "Asistencia", iconAsistencia, 135);

        if (touchedSection != Section.NONE && animationProgress > 0) {
            float startAngle = getStartAngleForSection(touchedSection);
            Path progressPath = createRoundedWedgePath(startAngle, sweepAngle * animationProgress, CORNER_RADIUS);
            canvas.drawPath(progressPath, progressPaint);
        }

        // --- INICIO DE CAMBIO ---
        // Título central eliminado
        // canvas.drawText("BOTÓN DE PÁNICO", centerX, centerY, titlePaint);
        // --- FIN DE CAMBIO ---
    }

    private void drawSection(Canvas canvas, float startAngle, float sweepAngle, Paint paint, Section section) {
        canvas.save();
        if (section == touchedSection) {
            canvas.scale(currentScale, currentScale, centerX, centerY);
        }
        Path path = createRoundedWedgePath(startAngle, sweepAngle, CORNER_RADIUS);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private Path createRoundedWedgePath(float startAngle, float sweepAngle, float cornerRadius) {
        Path path = new Path();
        float r = radius;

        double startAngleRad = Math.toRadians(startAngle);
        double endAngleRad = Math.toRadians(startAngle + sweepAngle);

        RectF outerArcRect = new RectF(centerX - r, centerY - r, centerX + r, centerY + r);
        path.arcTo(outerArcRect, startAngle, sweepAngle);

        float bottomRightCornerX = (float) (centerX + (r - cornerRadius) * Math.cos(endAngleRad));
        float bottomRightCornerY = (float) (centerY + (r - cornerRadius) * Math.sin(endAngleRad));
        RectF bottomRightCornerRect = new RectF(bottomRightCornerX - cornerRadius, bottomRightCornerY - cornerRadius, bottomRightCornerX + cornerRadius, bottomRightCornerY + cornerRadius);
        path.arcTo(bottomRightCornerRect, startAngle + sweepAngle, -90);

        path.lineTo(centerX + cornerRadius, centerY);

        float topLeftCornerX = (float) (centerX + cornerRadius * Math.cos(startAngleRad));
        float topLeftCornerY = (float) (centerY + cornerRadius * Math.sin(startAngleRad));
        RectF topLeftCornerRect = new RectF(topLeftCornerX - cornerRadius, topLeftCornerY - cornerRadius, topLeftCornerX + cornerRadius, topLeftCornerY + cornerRadius);
        path.arcTo(topLeftCornerRect, startAngle - 90, -90);

        path.close();
        return path;
    }

    private void drawSectionContent(Canvas canvas, String text, Drawable icon, float angle) {
        // --- INICIO DE CAMBIO ---
        // Invertidos los radios: ícono más lejos, texto más cerca.
        float textRadius = radius * 0.5f; // Texto más cerca del centro
        float iconRadius = radius * 0.7f; // Ícono más lejos del centro
        float iconSize = radius * 0.18f;
        // --- FIN DE CAMBIO ---

        double rad = Math.toRadians(angle);
        float textX = (float) (centerX + textRadius * Math.cos(rad));
        float textY = (float) (centerY + textRadius * Math.sin(rad));
        float iconX = (float) (centerX + iconRadius * Math.cos(rad));
        float iconY = (float) (centerY + iconRadius * Math.sin(rad));

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        textY -= (fm.ascent + fm.descent) / 2;

        canvas.drawText(text, textX, textY, textPaint);

        icon.setBounds((int) (iconX - iconSize), (int) (iconY - iconSize), (int) (iconX + iconSize), (int) (iconY + iconSize));
        icon.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchedSection = getSectionForPoint(x, y);
                if (touchedSection != Section.NONE) {
                    startAnimation();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelAnimation();
                touchedSection = Section.NONE;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private Section getSectionForPoint(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float distanceSq = dx * dx + dy * dy;

        // --- INICIO DE CAMBIO ---
        // Modificado para que no detecte el toque en el "agujero" central
        // Asumimos que el "agujero" es 1/3 del radio
        float innerRadius = radius / 3;
        if (distanceSq > radius * radius || distanceSq < innerRadius * innerRadius) {
            return Section.NONE;
        }
        // --- FIN DE CAMBIO ---

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360;
        }

        if (angle >= 180 && angle < 270) {
            return Section.SEGURIDAD;
        } else if (angle >= 270 && angle < 360) {
            return Section.SALUD;
        } else if (angle >= 0 && angle < 90) {
            return Section.INCENDIO;
        } else if (angle >= 90 && angle < 180) {
            return Section.ASISTENCIA;
        }
        return Section.NONE;
    }

    private void startAnimation() {
        cancelAnimation();

        progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(LONG_PRESS_DURATION);
        progressAnimator.addUpdateListener(animator -> {
            animationProgress = (float) animator.getAnimatedValue();
            invalidate();
        });

        scaleAnimator = ValueAnimator.ofFloat(1f, 1.05f);
        scaleAnimator.setDuration(200);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animator -> {
            currentScale = (float) animator.getAnimatedValue();
            invalidate();
        });

        longPressRunnable = () -> {
            if (onAlertListener != null) {
                onAlertListener.onAlert(touchedSection.name());
            }
            cancelAnimation();
        };

        progressAnimator.start();
        scaleAnimator.start();
        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
    }

    private void cancelAnimation() {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
        if (longPressRunnable != null) {
            longPressHandler.removeCallbacks(longPressRunnable);
        }

        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
        scaleAnimator = ValueAnimator.ofFloat(currentScale, 1f);
        scaleAnimator.setDuration(200);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animator -> {
            currentScale = (float) animator.getAnimatedValue();
            invalidate();
        });
        scaleAnimator.start();

        animationProgress = 0f;
        invalidate();
    }

    private float getStartAngleForSection(Section section) {
        switch (section) {
            case SEGURIDAD:
                return 180 + MARGIN_ANGLE / 2;
            case SALUD:
                return 270 + MARGIN_ANGLE / 2;
            case INCENDIO:
                return 0 + MARGIN_ANGLE / 2;
            case ASISTENCIA:
                return 90 + MARGIN_ANGLE / 2;
            default:
                return 0;
        }
    }
}