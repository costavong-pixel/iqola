// catalog
package com.barndai.iqola;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** The honest, playable IQOla catalog: every card opens a working game. */
final class GameCatalogView extends View {
    interface Listener {
        void openWaterSort();
        void openCoreGame(int gameIndex);
    }

    private static final int NAVY = Color.rgb(5, 8, 29);
    private static final int CYAN = Color.rgb(37, 221, 255);
    private static final int VIOLET = Color.rgb(125, 105, 255);
    private static final int PINK = Color.rgb(255, 81, 180);
    private static final int AMBER = Color.rgb(255, 182, 65);
    private static final int MINT = Color.rgb(71, 234, 180);

    private static final String[] CORE_TITLES = {
            "REVERSE STROOP", "MEMORY MATRIX", "STOP SIGNAL", "OBJECT TRACK"
    };
    private static final String[] CORE_SUBTITLES = {
            "Read the word, not its ink.", "Remember the glowing pattern.",
            "Act fast, then hold back.", "Follow one moving target."
    };
    private static final String[] CORE_LABELS = {"FOCUS", "MEMORY", "CONTROL", "TRACKING"};
    private static final int[] CORE_ACCENTS = {PINK, VIOLET, AMBER, MINT};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Typeface display = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface label = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mono = Typeface.create("sans-serif-smallcaps", Typeface.BOLD);
    private final Listener listener;
    private final RectF[] cards = new RectF[5];
    private final RectF adRect = new RectF();
    private float density;
    private float width;
    private float height;
    private final long openedAt = SystemClock.uptimeMillis();

    GameCatalogView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < cards.length; i++) cards[i] = new RectF();
        setFocusable(true);
        setContentDescription("IQOla playable game catalog");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        layoutCards();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        drawBackground(canvas, now);
        drawHeader(canvas);
        drawCatalog(canvas, now);
        drawFooter(canvas);
        if (isAttachedToWindow()) postInvalidateDelayed(33L);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        performClick();
        float x = event.getX();
        float y = event.getY();
        for (int i = 0; i < cards.length; i++) {
            if (!cards[i].contains(x, y)) continue;
            if (i == 0) listener.openWaterSort();
            else listener.openCoreGame(i - 1);
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void layoutCards() {
        float inset = dp(20);
        float gap = dp(12);
        float featuredTop = dp(145);
        float featuredHeight = dp(126);
        cards[0].set(inset, featuredTop, width - inset, featuredTop + featuredHeight);

        float gridTop = cards[0].bottom + dp(14);
        float gridBottom = height - dp(88);
        float cellWidth = (width - inset * 2f - gap) / 2f;
        float cellHeight = Math.max(dp(108), (gridBottom - gridTop - gap) / 2f);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = inset + column * (cellWidth + gap);
            float top = gridTop + row * (cellHeight + gap);
            cards[i + 1].set(left, top, left + cellWidth, top + cellHeight);
        }
        adRect.set(inset, height - dp(62), width - inset, height - dp(16));
    }

    private void drawBackground(Canvas canvas, long now) {
        canvas.drawColor(NAVY);
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{Color.rgb(8, 16, 54), Color.rgb(11, 7, 38), Color.rgb(4, 9, 28)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        float seconds = (now - openedAt) / 1000f;
        for (int band = 0; band < 3; band++) {
            float cx = width * (.16f + band * .36f) + (float) Math.sin(seconds * (.28f + band * .04f)) * dp(18);
            float cy = height * (.26f + band * .19f);
            float radius = dp(105) + band * dp(27) + (float) Math.sin(seconds * .5f + band) * dp(8);
            int accent = band == 0 ? CYAN : band == 1 ? VIOLET : PINK;
            paint.setColor(withAlpha(accent, 15));
            canvas.drawCircle(cx, cy, radius, paint);
        }
        for (int i = 0; i < 20; i++) {
            float x = (float) ((i * 83 % 997) / 997.0 * width + Math.sin(seconds * .32 + i) * dp(10));
            float y = (float) ((i * 173 % 1201) / 1201.0 * height + Math.cos(seconds * .27 + i * 1.8) * dp(14));
            float pulse = .45f + .55f * (float) Math.sin(seconds * 1.35f + i);
            paint.setColor(Color.argb((int) (44 * pulse), 192, 236, 255));
            canvas.drawCircle(x, y, dp(i % 4 == 0 ? 1.5f : .8f), paint);
        }
    }

    private void drawHeader(Canvas canvas) {
        drawText(canvas, "IQOla", dp(22), dp(39), dp(24), Color.WHITE, display, Paint.Align.LEFT);
        drawText(canvas, "PLAYABLE BRAIN TRAINING", dp(22), dp(59), dp(9),
                Color.argb(185, 201, 221, 255), mono, Paint.Align.LEFT);

        RectF countPill = new RectF(width - dp(116), dp(21), width - dp(20), dp(52));
        drawGlassPill(canvas, countPill, Color.argb(48, 112, 173, 255), Color.argb(160, 211, 234, 255));
        drawText(canvas, "5 GAMES", countPill.centerX(), countPill.centerY() + dp(3), dp(10),
                Color.WHITE, mono, Paint.Align.CENTER);

        drawText(canvas, "Choose a game", dp(22), dp(102), dp(29), Color.WHITE, display, Paint.Align.LEFT);
        drawText(canvas, "Every card below opens a playable challenge.", dp(22), dp(124), dp(12),
                Color.argb(205, 215, 230, 255), label, Paint.Align.LEFT);
    }

    private void drawCatalog(Canvas canvas, long now) {
        RectF water = cards[0];
        paint.setShader(new LinearGradient(water.left, water.top, water.right, water.bottom,
                new int[]{Color.argb(180, 19, 69, 111), Color.argb(200, 33, 24, 93), Color.argb(190, 13, 65, 88)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(water, dp(24), dp(24), paint);
        paint.setShader(null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setColor(Color.argb(188, 201, 239, 255));
        canvas.drawRoundRect(water, dp(24), dp(24), strokePaint);

        float pulse = (float) ((Math.sin(now / 430.0) + 1d) / 2d);
        drawWaterIcon(canvas, water.right - dp(61), water.centerY(), dp(25), pulse);
        drawText(canvas, "FEATURED", water.left + dp(18), water.top + dp(23), dp(9),
                Color.argb(220, 205, 235, 255), mono, Paint.Align.LEFT);
        drawText(canvas, "WATER SORT", water.left + dp(18), water.top + dp(55), dp(21), Color.WHITE,
                display, Paint.Align.LEFT);
        drawText(canvas, "Guided puzzle • 2:00 focus run", water.left + dp(18), water.top + dp(78), dp(12),
                Color.argb(220, 224, 235, 255), label, Paint.Align.LEFT);
        drawPlayTag(canvas, new RectF(water.left + dp(18), water.bottom - dp(34), water.left + dp(93), water.bottom - dp(12)), CYAN);

        for (int i = 0; i < 4; i++) drawCoreCard(canvas, cards[i + 1], i);
    }

    private void drawCoreCard(Canvas canvas, RectF card, int index) {
        int accent = CORE_ACCENTS[index];
        drawGlassPill(canvas, card, Color.argb(164, 13, 22, 54), Color.argb(138, 173, 206, 255));
        RectF iconBox = new RectF(card.left + dp(13), card.top + dp(13), card.left + dp(52), card.top + dp(52));
        drawGlassPill(canvas, iconBox, withAlpha(accent, 46), withAlpha(accent, 200));
        drawCoreIcon(canvas, index, iconBox.centerX(), iconBox.centerY(), dp(12), accent);
        drawText(canvas, CORE_LABELS[index], card.left + dp(61), card.top + dp(29), dp(8),
                withAlpha(accent, 244), mono, Paint.Align.LEFT);
        drawText(canvas, CORE_TITLES[index], card.left + dp(13), card.top + dp(76), dp(14), Color.WHITE,
                display, Paint.Align.LEFT);
        drawTwoLineText(canvas, CORE_SUBTITLES[index], card.left + dp(13), card.top + dp(96), card.width() - dp(26),
                dp(10), Color.argb(196, 214, 228, 255), label);
        drawPlayTag(canvas, new RectF(card.left + dp(13), card.bottom - dp(29), card.left + dp(79), card.bottom - dp(11)), accent);
    }

    private void drawFooter(Canvas canvas) {
        drawGlassPill(canvas, adRect, Color.argb(33, 92, 114, 169), Color.argb(96, 184, 207, 255));
        drawText(canvas, "AD SPACE  ·  IQOLA PLUS — AD-FREE YEARLY", adRect.centerX(), adRect.centerY() + dp(3),
                dp(9), Color.argb(224, 229, 239, 255), mono, Paint.Align.CENTER);
    }

    private void drawWaterIcon(Canvas canvas, float cx, float cy, float radius, float pulse) {
        RectF tube = new RectF(cx - radius * .62f, cy - radius, cx + radius * .62f, cy + radius);
        paint.setColor(Color.argb(50, 232, 250, 255));
        canvas.drawRoundRect(tube, radius * .34f, radius * .34f, paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.4f));
        strokePaint.setColor(Color.argb(225, 235, 250, 255));
        canvas.drawRoundRect(tube, radius * .34f, radius * .34f, strokePaint);
        float unit = tube.height() / 4f;
        int[] colors = {CYAN, VIOLET, PINK, AMBER};
        for (int i = 0; i < colors.length; i++) {
            float top = tube.bottom - (i + 1) * unit + dp(1);
            RectF layer = new RectF(tube.left + dp(2), top, tube.right - dp(2), tube.bottom - i * unit - dp(1));
            paint.setColor(withAlpha(colors[i], 230));
            canvas.drawRoundRect(layer, dp(4), dp(4), paint);
        }
        paint.setColor(Color.argb((int) (90 + pulse * 70), 255, 255, 255));
        canvas.drawCircle(tube.centerX(), tube.top + dp(10), dp(2), paint);
    }

    private void drawCoreIcon(Canvas canvas, int game, float cx, float cy, float size, int accent) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        if (game == 0) {
            drawText(canvas, "Aa", cx, cy + size * .35f, size * 1.35f, accent, display, Paint.Align.CENTER);
        } else if (game == 1) {
            float cell = size * .52f;
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    float left = cx + (col - 1) * cell + size * .05f;
                    float top = cy + (row - 1) * cell + size * .05f;
                    paint.setColor((row == 0 && col == 1) ? accent : withAlpha(accent, 125));
                    canvas.drawRoundRect(new RectF(left, top, left + cell * .72f, top + cell * .72f), dp(2), dp(2), paint);
                }
            }
        } else if (game == 2) {
            canvas.drawCircle(cx, cy, size * .7f, paint);
            drawText(canvas, "!", cx, cy + size * .36f, size, Color.WHITE, display, Paint.Align.CENTER);
        } else {
            canvas.drawCircle(cx - size * .38f, cy, size * .25f, paint);
            paint.setColor(withAlpha(accent, 180));
            canvas.drawCircle(cx + size * .06f, cy - size * .25f, size * .25f, paint);
            paint.setColor(withAlpha(accent, 120));
            canvas.drawCircle(cx + size * .38f, cy + size * .25f, size * .25f, paint);
        }
    }

    private void drawPlayTag(Canvas canvas, RectF rect, int accent) {
        drawGlassPill(canvas, rect, withAlpha(accent, 48), withAlpha(accent, 185));
        drawText(canvas, "PLAY", rect.centerX(), rect.centerY() + dp(3), dp(8), Color.WHITE, mono, Paint.Align.CENTER);
    }

    private void drawTwoLineText(Canvas canvas, String value, float x, float baseline, float maxWidth,
                                 float size, int color, Typeface typeface) {
        paint.setTextSize(size);
        paint.setTypeface(typeface);
        String[] words = value.split(" ");
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        for (String word : words) {
            String candidate = first.length() == 0 ? word : first + " " + word;
            if (paint.measureText(candidate) <= maxWidth || first.length() == 0) {
                first.setLength(0);
                first.append(candidate);
            } else {
                if (second.length() > 0) second.append(' ');
                second.append(word);
            }
        }
        drawText(canvas, first.toString(), x, baseline, size, color, typeface, Paint.Align.LEFT);
        if (second.length() > 0) drawText(canvas, second.toString(), x, baseline + dp(13), size, color, typeface, Paint.Align.LEFT);
    }

    private void drawGlassPill(Canvas canvas, RectF rect, int fill, int border) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, Math.min(dp(22), rect.height() / 2f), Math.min(dp(22), rect.height() / 2f), paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(border);
        canvas.drawRoundRect(rect, Math.min(dp(22), rect.height() / 2f), Math.min(dp(22), rect.height() / 2f), strokePaint);
    }

    private void drawText(Canvas canvas, String text, float x, float baseline, float size, int color,
                          Typeface typeface, Paint.Align align) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(typeface);
        paint.setTextAlign(align);
        canvas.drawText(text, x, baseline, paint);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * density;
    }
}
// replacement check
