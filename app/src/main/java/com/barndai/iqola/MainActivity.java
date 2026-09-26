package com.barndai.iqola;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private IQOlaView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(IQOlaView.NAVY);
        getWindow().setNavigationBarColor(IQOlaView.NAVY);
        gameView = new IQOlaView(this);
        setContentView(gameView);
    }

    @Override
    public void onBackPressed() {
        if (!gameView.handleBack()) super.onBackPressed();
    }
}

/**
 * IQOla's premium Water Sort template. The first two moves are demonstrated in-context before
 * play begins, so players learn the rule by watching the board rather than reading an instruction.
 */
class IQOlaView extends View {
    static final int NAVY = Color.rgb(5, 8, 29);

    private static final int CAPACITY = 4;
    private static final int TUBE_COUNT = 6;
    private static final long LEVEL_TIME_MS = 120_000L;
    private static final int CYAN = Color.rgb(37, 221, 255);
    private static final int VIOLET = Color.rgb(125, 105, 255);
    private static final int PINK = Color.rgb(255, 81, 180);
    private static final int AMBER = Color.rgb(255, 182, 65);
    private static final int[] SPECTRUM = {CYAN, VIOLET, PINK, AMBER};
    private static final int[] CHAMBER_RESOURCES = {
            R.drawable.water_sort_chamber,
            R.drawable.water_sort_chamber_aurora,
            R.drawable.water_sort_chamber_eclipse
    };

    private enum TutorialStage { SOURCE, DESTINATION, POUR, PLAYER }

    private static final class GuideMove {
        final int from;
        final int to;
        final int amount;

        GuideMove(int from, int to, int amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path path = new Path();
    private final Typeface display = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface label = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mono = Typeface.create("sans-serif-smallcaps", Typeface.BOLD);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private final ArrayList<ArrayList<Integer>> tubes = new ArrayList<>();
    private final ArrayList<ArrayList<ArrayList<Integer>>> history = new ArrayList<>();
    private final ArrayList<GuideMove> solutionMoves = new ArrayList<>();
    private final RectF[] tubeRects = new RectF[TUBE_COUNT];
    private final RectF undoRect = new RectF();
    private final RectF restartRect = new RectF();
    private final RectF nextRect = new RectF();
    private final RectF adRect = new RectF();
    private final Bitmap[] chamberBackgrounds;

    private float density;
    private float width;
    private float height;
    private int selectedTube = -1;
    private int level = 1;
    private int moves;
    private int highestLevel;
    private boolean animating;
    private boolean demoPour;
    private boolean levelComplete;
    private boolean timeExpired;
    private int pourFrom = -1;
    private int pourTo = -1;
    private int pourAmount;
    private int pourColor;
    private long pourStartedAt;
    private boolean tutorialActive;
    private TutorialStage tutorialStage = TutorialStage.PLAYER;
    private int tutorialStep;
    private boolean countdownRunning;
    private long countdownRemainingMs = LEVEL_TIME_MS;
    private long countdownResumedAt;
    private int backgroundIndex;
    private int previousBackgroundIndex;
    private long backgroundChangedAt;
    private long nextBackgroundChangeAt;
    private String message = "Tap a tube, then its matching colour.";
    private long messageUntil;

    IQOlaView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        preferences = context.getSharedPreferences("iqola_water_sort", Context.MODE_PRIVATE);
        highestLevel = preferences.getInt("highest_level", 1);
        chamberBackgrounds = new Bitmap[CHAMBER_RESOURCES.length];
        for (int i = 0; i < CHAMBER_RESOURCES.length; i++) {
            chamberBackgrounds[i] = BitmapFactory.decodeResource(getResources(), CHAMBER_RESOURCES[i]);
        }
        for (int i = 0; i < TUBE_COUNT; i++) {
            tubes.add(new ArrayList<Integer>());
            tubeRects[i] = new RectF();
        }
        setFocusable(true);
        setContentDescription("IQOla Water Sort puzzle");
        startLevel(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        layoutGame();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        if (countdownRunning && remainingTime(now) == 0L) expireTimer();
        if (!tutorialActive && !levelComplete && !timeExpired && now >= nextBackgroundChangeAt) rotateChamber(now);

        drawChamber(canvas, now);
        drawHeader(canvas, now);
        drawBoardAura(canvas, now);
        if (animating) drawPourStream(canvas, now);
        for (int i = 0; i < TUBE_COUNT; i++) drawTube(canvas, tubeRects[i], i, now);
        if (tutorialActive) drawTutorialGuide(canvas, now);
        drawFooter(canvas);
        if (levelComplete) drawCompletion(canvas, now);
        else if (timeExpired) drawTimeExpired(canvas, now);

        // Ambient light, particles, guide glow, and the countdown all stay alive at 30fps.
        if (isAttachedToWindow()) postInvalidateDelayed(33L);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        performClick();
        float x = event.getX();
        float y = event.getY();

        if (levelComplete) {
            if (nextRect.contains(x, y)) startLevel(level + 1);
            return true;
        }
        if (timeExpired) {
            if (nextRect.contains(x, y)) startLevel(level);
            return true;
        }
        if (tutorialActive || animating) return true;
        if (undoRect.contains(x, y)) {
            undoMove();
            return true;
        }
        if (restartRect.contains(x, y)) {
            startLevel(level);
            return true;
        }
        for (int i = 0; i < TUBE_COUNT; i++) {
            if (tubeRects[i].contains(x, y)) {
                tapTube(i);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    boolean handleBack() {
        if (levelComplete || timeExpired) {
            startLevel(level);
            return true;
        }
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    private void layoutGame() {
        float inset = dp(22);
        float gap = dp(16);
        float boardTop = dp(184);
        float footerTop = height - dp(154);
        float tubeWidth = Math.min(dp(82), (width - inset * 2f - gap * 2f) / 3f);
        float rowGap = dp(34);
        float tubeHeight = Math.min(dp(218), Math.max(dp(106), (footerTop - boardTop - rowGap) / 2f));
        float rowWidth = tubeWidth * 3f + gap * 2f;
        float startX = (width - rowWidth) / 2f;
        float extraY = Math.max(0f, (footerTop - boardTop - tubeHeight * 2f - rowGap) / 2f);
        float firstY = boardTop + extraY;

        for (int i = 0; i < TUBE_COUNT; i++) {
            int row = i / 3;
            int column = i % 3;
            float left = startX + column * (tubeWidth + gap);
            float top = firstY + row * (tubeHeight + rowGap);
            tubeRects[i].set(left, top, left + tubeWidth, top + tubeHeight);
        }

        float controlY = height - dp(104);
        float controlSize = dp(48);
        undoRect.set(width / 2f - dp(68) - controlSize / 2f, controlY - controlSize / 2f,
                width / 2f - dp(68) + controlSize / 2f, controlY + controlSize / 2f);
        restartRect.set(width / 2f + dp(68) - controlSize / 2f, controlY - controlSize / 2f,
                width / 2f + dp(68) + controlSize / 2f, controlY + controlSize / 2f);
        adRect.set(dp(20), height - dp(58), width - dp(20), height - dp(14));
        nextRect.set(width / 2f - dp(112), height / 2f + dp(106), width / 2f + dp(112), height / 2f + dp(160));
    }

    private void startLevel(int requestedLevel) {
        handler.removeCallbacks(animationFrame);
        handler.removeCallbacks(tutorialFrame);
        handler.removeCallbacks(startCountdownFrame);
        long now = SystemClock.uptimeMillis();
        level = Math.max(1, requestedLevel);
        previousBackgroundIndex = backgroundIndex;
        backgroundIndex = (level - 1) % chamberBackgrounds.length;
        backgroundChangedAt = now;
        nextBackgroundChangeAt = now + 28_000L;
        selectedTube = -1;
        moves = 0;
        animating = false;
        demoPour = false;
        levelComplete = false;
        timeExpired = false;
        pourFrom = -1;
        history.clear();
        solutionMoves.clear();
        countdownRunning = false;
        countdownRemainingMs = LEVEL_TIME_MS;
        countdownResumedAt = 0L;
        for (ArrayList<Integer> tube : tubes) tube.clear();
        for (int colour : SPECTRUM) {
            ArrayList<Integer> tube = findFirstEmptyTube();
            for (int slot = 0; slot < CAPACITY; slot++) tube.add(colour);
        }
        scrambleFromSolvedState();
        beginGuidedStart();
        invalidate();
    }

    private ArrayList<Integer> findFirstEmptyTube() {
        for (ArrayList<Integer> tube : tubes) if (tube.isEmpty()) return tube;
        return tubes.get(0);
    }

    /**
     * Builds a legal reverse path back to a sorted board. The opening guide replays the first two
     * moves from that path, so it is both a real lesson and a valid start to the level.
     */
    private void scrambleFromSolvedState() {
        Random random = new Random(7331L + level * 997L);
        ArrayList<GuideMove> scrambleMoves = new ArrayList<>();
        int created = 0;
        int attempts = 0;
        int previousFrom = -1;
        int previousTo = -1;
        int desiredMoves = 28 + Math.min(level, 8) * 3;

        while (created < desiredMoves && attempts++ < desiredMoves * 25) {
            ArrayList<Integer> sources = new ArrayList<>();
            for (int i = 0; i < TUBE_COUNT; i++) if (!tubes.get(i).isEmpty()) sources.add(i);
            if (sources.isEmpty()) break;
            int from = sources.get(random.nextInt(sources.size()));
            ArrayList<Integer> source = tubes.get(from);
            int colour = topColour(source);
            int run = topRun(source);
            ArrayList<Integer> destinations = new ArrayList<>();
            for (int to = 0; to < TUBE_COUNT; to++) {
                if (to == from || (to == previousFrom && from == previousTo)) continue;
                ArrayList<Integer> destination = tubes.get(to);
                if (destination.size() >= CAPACITY) continue;
                if (destination.isEmpty() || topColour(destination) != colour) destinations.add(to);
            }
            if (destinations.isEmpty()) continue;
            int to = destinations.get(random.nextInt(destinations.size()));
            ArrayList<Integer> destination = tubes.get(to);
            int max = Math.min(run, CAPACITY - destination.size());
            ArrayList<Integer> legalAmounts = new ArrayList<>();
            for (int amount = 1; amount <= max; amount++) {
                if (amount < run || amount == source.size()) legalAmounts.add(amount);
            }
            if (legalAmounts.isEmpty()) continue;
            int amount = legalAmounts.get(random.nextInt(legalAmounts.size()));
            forcedMove(from, to, amount);
            scrambleMoves.add(new GuideMove(from, to, amount));
            previousFrom = from;
            previousTo = to;
            created++;
        }

        for (int i = scrambleMoves.size() - 1; i >= 0; i--) {
            GuideMove move = scrambleMoves.get(i);
            solutionMoves.add(new GuideMove(move.to, move.from, move.amount));
        }
    }

    private void beginGuidedStart() {
        tutorialStep = 0;
        tutorialActive = solutionMoves.size() >= 2;
        tutorialStage = tutorialActive ? TutorialStage.SOURCE : TutorialStage.PLAYER;
        if (tutorialActive) {
            message = "WATCH THE FIRST TWO MOVES • TIMER PAUSED";
            messageUntil = 0L;
            handler.postDelayed(tutorialFrame, 850L);
        } else {
            releasePlayerToCountdown();
        }
    }

    private final Runnable tutorialFrame = new Runnable() {
        @Override
        public void run() {
            if (!tutorialActive || animating || timeExpired || levelComplete) return;
            if (tutorialStage == TutorialStage.SOURCE) {
                tutorialStage = TutorialStage.DESTINATION;
                message = "NOW POUR INTO THE GOLD GLOW";
                messageUntil = 0L;
                handler.postDelayed(this, 1050L);
            } else if (tutorialStage == TutorialStage.DESTINATION) {
                GuideMove move = activeGuideMove();
                if (move == null || !canPour(move.from, move.to)) {
                    releasePlayerToCountdown();
                    return;
                }
                tutorialStage = TutorialStage.POUR;
                beginPour(move.from, move.to, true);
            }
        }
    };

    private void completeDemoPour() {
        tutorialStep++;
        if (tutorialStep >= 2 || tutorialStep >= solutionMoves.size()) {
            releasePlayerToCountdown();
            return;
        }
        tutorialStage = TutorialStage.SOURCE;
        message = "WATCH MOVE " + (tutorialStep + 1) + " OF 2";
        messageUntil = 0L;
        handler.postDelayed(tutorialFrame, 700L);
    }

    private void releasePlayerToCountdown() {
        tutorialActive = false;
        tutorialStage = TutorialStage.PLAYER;
        selectedTube = -1;
        message = "YOUR TURN • BEAT THE CLOCK.";
        messageUntil = 0L;
        handler.postDelayed(startCountdownFrame, 520L);
    }

    private final Runnable startCountdownFrame = new Runnable() {
        @Override
        public void run() {
            if (!tutorialActive && !levelComplete && !timeExpired && !countdownRunning) {
                countdownResumedAt = SystemClock.uptimeMillis();
                countdownRunning = true;
                invalidate();
            }
        }
    };

    private void forcedMove(int from, int to, int amount) {
        ArrayList<Integer> source = tubes.get(from);
        ArrayList<Integer> destination = tubes.get(to);
        for (int step = 0; step < amount; step++) {
            destination.add(source.remove(source.size() - 1));
        }
    }

    private void tapTube(int tapped) {
        if (selectedTube == -1) {
            if (tubes.get(tapped).isEmpty()) {
                showMessage("Choose a coloured tube first.");
            } else {
                selectedTube = tapped;
                showMessage("Now pour it into a matching colour.");
            }
            invalidate();
            return;
        }

        if (tapped == selectedTube) {
            selectedTube = -1;
            showMessage("Selection cleared.");
            invalidate();
            return;
        }

        if (canPour(selectedTube, tapped)) {
            beginPour(selectedTube, tapped, false);
            return;
        }

        if (!tubes.get(tapped).isEmpty()) {
            selectedTube = tapped;
            showMessage("New tube selected.");
        } else {
            showMessage("That tube needs the same top colour.");
        }
        invalidate();
    }

    private boolean canPour(int from, int to) {
        ArrayList<Integer> source = tubes.get(from);
        ArrayList<Integer> destination = tubes.get(to);
        if (source.isEmpty() || destination.size() >= CAPACITY) return false;
        return destination.isEmpty() || topColour(source) == topColour(destination);
    }

    private void beginPour(int from, int to, boolean isDemo) {
        ArrayList<Integer> source = tubes.get(from);
        ArrayList<Integer> destination = tubes.get(to);
        pourFrom = from;
        pourTo = to;
        pourColor = topColour(source);
        pourAmount = Math.min(topRun(source), CAPACITY - destination.size());
        demoPour = isDemo;
        animating = true;
        pourStartedAt = SystemClock.uptimeMillis();
        if (isDemo) {
            message = "WATCH THE COLOUR FLOW";
            messageUntil = 0L;
        } else {
            showMessage("Perfect blend.");
        }
        handler.post(animationFrame);
        invalidate();
    }

    private final Runnable animationFrame = new Runnable() {
        @Override
        public void run() {
            if (!animating) return;
            long elapsed = SystemClock.uptimeMillis() - pourStartedAt;
            if (elapsed >= 360L) {
                boolean wasDemo = demoPour;
                if (!wasDemo) saveHistory();
                forcedMove(pourFrom, pourTo, pourAmount);
                if (!wasDemo) moves++;
                selectedTube = -1;
                animating = false;
                demoPour = false;
                if (wasDemo) {
                    completeDemoPour();
                } else if (isSolved()) {
                    celebrate();
                }
                invalidate();
                return;
            }
            invalidate();
            handler.postDelayed(this, 16L);
        }
    };

    private void saveHistory() {
        ArrayList<ArrayList<Integer>> snapshot = new ArrayList<>();
        for (ArrayList<Integer> tube : tubes) snapshot.add(new ArrayList<>(tube));
        history.add(snapshot);
        if (history.size() > 36) history.remove(0);
    }

    private void undoMove() {
        if (history.isEmpty()) {
            showMessage("No pours to undo yet.");
            invalidate();
            return;
        }
        ArrayList<ArrayList<Integer>> snapshot = history.remove(history.size() - 1);
        for (int i = 0; i < TUBE_COUNT; i++) {
            tubes.get(i).clear();
            tubes.get(i).addAll(snapshot.get(i));
        }
        moves = Math.max(0, moves - 1);
        selectedTube = -1;
        showMessage("Last pour reversed.");
        invalidate();
    }

    private boolean isSolved() {
        for (ArrayList<Integer> tube : tubes) {
            if (tube.isEmpty()) continue;
            if (tube.size() != CAPACITY) return false;
            int colour = tube.get(0);
            for (int item : tube) if (item != colour) return false;
        }
        return true;
    }

    private void celebrate() {
        pauseCountdown(SystemClock.uptimeMillis());
        levelComplete = true;
        highestLevel = Math.max(highestLevel, level + 1);
        preferences.edit().putInt("highest_level", highestLevel).apply();
        message = "Spectrum complete.";
        messageUntil = 0L;
        invalidate();
    }

    private long remainingTime(long now) {
        if (!countdownRunning) return Math.max(0L, countdownRemainingMs);
        return Math.max(0L, countdownRemainingMs - (now - countdownResumedAt));
    }

    private void pauseCountdown(long now) {
        if (countdownRunning) {
            countdownRemainingMs = remainingTime(now);
            countdownRunning = false;
        }
    }

    private void expireTimer() {
        if (timeExpired) return;
        countdownRemainingMs = 0L;
        countdownRunning = false;
        handler.removeCallbacks(animationFrame);
        animating = false;
        selectedTube = -1;
        timeExpired = true;
        message = "Time is up.";
        messageUntil = 0L;
    }

    private int topColour(ArrayList<Integer> tube) {
        return tube.get(tube.size() - 1);
    }

    private int topRun(ArrayList<Integer> tube) {
        if (tube.isEmpty()) return 0;
        int colour = topColour(tube);
        int count = 0;
        for (int i = tube.size() - 1; i >= 0 && tube.get(i) == colour; i--) count++;
        return count;
    }

    private GuideMove activeGuideMove() {
        if (!tutorialActive || tutorialStep >= solutionMoves.size()) return null;
        return solutionMoves.get(tutorialStep);
    }

    private void showMessage(String value) {
        message = value;
        messageUntil = SystemClock.uptimeMillis() + 1700L;
    }

    private void drawChamber(Canvas canvas, long now) {
        canvas.drawColor(NAVY);
        float transition = Math.min(1f, (now - backgroundChangedAt) / 800f);
        if (previousBackgroundIndex != backgroundIndex && transition < 1f) {
            drawBackgroundBitmap(canvas, chamberBackgrounds[previousBackgroundIndex], (int) (255 * (1f - transition)), now, .2f);
            drawBackgroundBitmap(canvas, chamberBackgrounds[backgroundIndex], (int) (255 * transition), now, .8f);
        } else {
            drawBackgroundBitmap(canvas, chamberBackgrounds[backgroundIndex], 255, now, .6f);
        }

        drawAmbientMotion(canvas, now);
        paint.setShader(new LinearGradient(0, 0, 0, height,
                new int[]{Color.argb(72, 3, 5, 24), Color.argb(132, 5, 7, 27), Color.argb(214, 3, 5, 21)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void rotateChamber(long now) {
        previousBackgroundIndex = backgroundIndex;
        backgroundIndex = (backgroundIndex + 1) % chamberBackgrounds.length;
        backgroundChangedAt = now;
        nextBackgroundChangeAt = now + 28_000L;
    }

    private void drawBackgroundBitmap(Canvas canvas, Bitmap bitmap, int alpha, long now, float phase) {
        if (bitmap == null || width <= 0 || height <= 0) return;
        float breathe = 1.035f + .014f * (float) Math.sin(now / 6800.0 + phase * 6.0);
        float scale = Math.max(width / bitmap.getWidth(), height / bitmap.getHeight()) * breathe;
        float bitmapWidth = bitmap.getWidth() * scale;
        float bitmapHeight = bitmap.getHeight() * scale;
        float left = (width - bitmapWidth) / 2f + dp(3) * (float) Math.sin(now / 9400.0 + phase);
        float top = (height - bitmapHeight) / 2f + dp(5) * (float) Math.cos(now / 8100.0 + phase);
        bitmapPaint.setAlpha(alpha);
        canvas.drawBitmap(bitmap, null, new RectF(left, top, left + bitmapWidth, top + bitmapHeight), bitmapPaint);
    }

    private void drawAmbientMotion(Canvas canvas, long now) {
        float seconds = now / 1000f;
        for (int band = 0; band < 3; band++) {
            float baseX = width * (.1f + band * .32f);
            float sway = dp(18) * (float) Math.sin(seconds * (.28f + band * .05f) + band);
            path.reset();
            path.moveTo(baseX - dp(62), -dp(20));
            path.cubicTo(baseX + sway, height * .22f, baseX - sway, height * .52f, baseX + dp(34), height + dp(20));
            path.lineTo(baseX + dp(78), height + dp(20));
            path.cubicTo(baseX + dp(6) + sway, height * .53f, baseX + dp(36) - sway, height * .2f, baseX - dp(18), -dp(20));
            path.close();
            paint.setColor(Color.argb(10 + band * 3, band == 1 ? 75 : 93, 213, 255));
            canvas.drawPath(path, paint);
        }
        for (int i = 0; i < 22; i++) {
            float x = (float) ((i * 97 % 1000) / 1000.0 * width + Math.sin(seconds * .42 + i) * dp(9));
            float y = (float) ((i * 149 % 1300) / 1300.0 * height + Math.cos(seconds * .31 + i * 2) * dp(13));
            float flicker = .55f + .45f * (float) Math.sin(seconds * 1.4f + i);
            paint.setColor(Color.argb((int) (42 * flicker), 186, 237, 255));
            canvas.drawCircle(x, y, dp(i % 4 == 0 ? 1.5f : .8f), paint);
        }
    }

    private void drawHeader(Canvas canvas, long now) {
        float left = dp(22);
        drawText(canvas, "IQOla", left, dp(36), dp(23), Color.WHITE, display, Paint.Align.LEFT);
        drawText(canvas, "FOCUS MODE", left, dp(56), dp(9), Color.argb(178, 198, 217, 255), mono, Paint.Align.LEFT);

        float levelWidth = dp(92);
        RectF levelPill = new RectF(width - dp(22) - levelWidth, dp(18), width - dp(22), dp(48));
        drawGlassPill(canvas, levelPill, Color.argb(40, 143, 180, 255), Color.argb(112, 211, 232, 255));
        drawText(canvas, String.format(Locale.US, "LVL %02d", level), levelPill.centerX(), levelPill.centerY() + dp(3),
                dp(10), Color.WHITE, mono, Paint.Align.CENTER);

        drawText(canvas, "WATER SORT", left, dp(93), dp(28), Color.WHITE, display, Paint.Align.LEFT);
        drawText(canvas, "Sort every frequency into its own vessel.", left, dp(116), dp(12),
                Color.argb(205, 215, 230, 255), label, Paint.Align.LEFT);

        drawCountdown(canvas, now);
        int messageColor = messageUntil > 0 && now > messageUntil
                ? Color.argb(150, 200, 220, 255) : Color.argb(235, 222, 236, 255);
        drawText(canvas, message, width / 2f, dp(143), dp(11), messageColor, label, Paint.Align.CENTER);
    }

    private void drawCountdown(Canvas canvas, long now) {
        long remaining = remainingTime(now);
        int timerColour = remaining <= 10_000L ? PINK : remaining <= 30_000L ? AMBER : CYAN;
        RectF timer = new RectF(width - dp(132), dp(59), width - dp(20), dp(120));
        drawGlassPill(canvas, timer, withAlpha(timerColour, 43), withAlpha(timerColour, 190));
        String caption = tutorialActive ? "TIME • PAUSED" : "TIME LEFT";
        drawText(canvas, caption, timer.centerX(), timer.top + dp(15), dp(8),
                withAlpha(timerColour, 225), mono, Paint.Align.CENTER);
        drawText(canvas, formatTime(remaining), timer.centerX(), timer.bottom - dp(13), dp(26),
                Color.WHITE, display, Paint.Align.CENTER);
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1000L);
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private void drawBoardAura(Canvas canvas, long now) {
        float centerX = width / 2f;
        float centerY = (tubeRects[0].top + tubeRects[5].bottom) / 2f;
        float pulse = (float) Math.sin(now / 800.0) * dp(5);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(24, 101, 205, 255));
        canvas.drawOval(new RectF(centerX - dp(156) - pulse, centerY - dp(205),
                centerX + dp(156) + pulse, centerY + dp(205)), paint);
        paint.setColor(Color.argb(18, 255, 91, 192));
        canvas.drawOval(new RectF(centerX - dp(120), centerY - dp(168) - pulse,
                centerX + dp(120), centerY + dp(168) + pulse), paint);
    }

    private void drawTube(Canvas canvas, RectF sourceRect, int index, long now) {
        GuideMove guide = activeGuideMove();
        boolean guideSource = guide != null && (tutorialStage == TutorialStage.SOURCE) && index == guide.from;
        boolean guideTarget = guide != null && (tutorialStage == TutorialStage.DESTINATION || tutorialStage == TutorialStage.POUR) && index == guide.to;
        boolean selected = selectedTube == index || guideSource || guideTarget;
        int accent = guideTarget ? AMBER : (guideSource ? CYAN : Color.rgb(119, 239, 255));

        RectF rect = new RectF(sourceRect);
        float lift = 0f;
        if (animating && index == pourFrom) {
            float progress = Math.min(1f, (now - pourStartedAt) / 360f);
            lift = -dp(20) * (float) Math.sin(progress * Math.PI);
        } else if (selectedTube == index || guideSource) {
            lift = -dp(6) - dp(2) * (float) Math.sin(now / 145.0);
        }
        rect.offset(0, lift);

        float radius = Math.min(dp(18), rect.width() * .26f);
        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(accent, 34));
            canvas.drawRoundRect(new RectF(rect.left - dp(8), rect.top - dp(8), rect.right + dp(8), rect.bottom + dp(8)),
                    radius + dp(6), radius + dp(6), paint);
        }

        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                new int[]{Color.argb(56, 207, 239, 255), Color.argb(18, 66, 84, 141), Color.argb(48, 211, 237, 255)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        RectF inner = new RectF(rect.left + dp(7), rect.top + dp(13), rect.right - dp(7), rect.bottom - dp(8));
        paint.setColor(Color.argb(78, 4, 9, 31));
        canvas.drawRoundRect(inner, Math.max(dp(9), radius - dp(6)), Math.max(dp(9), radius - dp(6)), paint);
        drawLiquid(canvas, inner, tubes.get(index), now);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.35f));
        strokePaint.setColor(selected ? withAlpha(accent, 245) : Color.argb(178, 201, 234, 255));
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
        strokePaint.setColor(Color.argb(148, 244, 254, 255));
        strokePaint.setStrokeWidth(dp(1.1f));
        canvas.drawLine(rect.left + dp(5), rect.top + dp(21), rect.left + dp(5), rect.bottom - dp(24), strokePaint);

        RectF rim = new RectF(rect.left - dp(1), rect.top - dp(3), rect.right + dp(1), rect.top + dp(15));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(92, 224, 246, 255));
        canvas.drawRoundRect(rim, dp(8), dp(8), paint);
        strokePaint.setColor(selected ? withAlpha(accent, 230) : Color.argb(220, 238, 252, 255));
        strokePaint.setStrokeWidth(dp(1.2f));
        canvas.drawRoundRect(rim, dp(8), dp(8), strokePaint);

        drawTubeIndex(canvas, rect, index);
    }

    private void drawLiquid(Canvas canvas, RectF inner, ArrayList<Integer> tube, long now) {
        if (tube.isEmpty()) return;
        float unit = inner.height() / CAPACITY;
        for (int slot = 0; slot < tube.size(); slot++) {
            int colour = tube.get(slot);
            float top = inner.bottom - (slot + 1) * unit + dp(.8f);
            RectF layer = new RectF(inner.left + dp(1), top, inner.right - dp(1), inner.bottom - slot * unit - dp(.6f));
            int highlight = brighten(colour, .42f);
            paint.setShader(new LinearGradient(layer.left, layer.top, layer.right, layer.bottom,
                    new int[]{highlight, colour, darken(colour, .2f)}, new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(layer, slot == tube.size() - 1 ? dp(7) : dp(3), slot == tube.size() - 1 ? dp(7) : dp(3), paint);
            paint.setShader(null);
            paint.setColor(Color.argb(95, 255, 255, 255));
            canvas.drawRoundRect(new RectF(layer.left + dp(3), layer.top + dp(3), layer.left + dp(5), layer.bottom - dp(4)),
                    dp(2), dp(2), paint);

            if (slot == tube.size() - 1) {
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(dp(1));
                strokePaint.setColor(Color.argb(175, 255, 255, 255));
                canvas.drawRoundRect(layer, dp(7), dp(7), strokePaint);
            }

            float bubblePhase = (float) ((now / 570.0 + slot * 1.7 + Color.red(colour)) % 1.0);
            paint.setColor(Color.argb(85, 255, 255, 255));
            canvas.drawCircle(layer.right - dp(7) - bubblePhase * dp(5), layer.bottom - dp(7) - bubblePhase * dp(8),
                    dp(1.55f), paint);
        }
    }

    private void drawTutorialGuide(Canvas canvas, long now) {
        GuideMove move = activeGuideMove();
        if (move == null || tutorialStage == TutorialStage.PLAYER) return;
        boolean toTarget = tutorialStage == TutorialStage.DESTINATION || tutorialStage == TutorialStage.POUR;
        int focusIndex = toTarget ? move.to : move.from;
        int colour = toTarget ? AMBER : CYAN;
        RectF focus = tubeRects[focusIndex];
        float pulse = dp(4) + dp(3) * (float) ((Math.sin(now / 160.0) + 1f) / 2f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(colour, 28));
        canvas.drawRoundRect(new RectF(focus.left - pulse, focus.top - pulse, focus.right + pulse, focus.bottom + pulse),
                dp(22), dp(22), paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.5f));
        strokePaint.setColor(withAlpha(colour, 238));
        canvas.drawRoundRect(new RectF(focus.left - pulse, focus.top - pulse, focus.right + pulse, focus.bottom + pulse),
                dp(22), dp(22), strokePaint);

        RectF banner = new RectF(width / 2f - dp(136), dp(155), width / 2f + dp(136), dp(181));
        drawGlassPill(canvas, banner, Color.argb(57, 13, 25, 54), withAlpha(colour, 190));
        String instruction = tutorialStage == TutorialStage.SOURCE ? "1. START HERE" :
                tutorialStage == TutorialStage.DESTINATION ? "2. POUR HERE" : "WATCH THE POUR";
        drawText(canvas, "WATCH MOVE " + (tutorialStep + 1) + " OF 2  •  " + instruction,
                banner.centerX(), banner.centerY() + dp(3), dp(9), Color.WHITE, mono, Paint.Align.CENTER);

        float arrowX = focus.centerX();
        float arrowEndY = focus.top + dp(9);
        float arrowStartY = Math.max(banner.bottom + dp(2), arrowEndY - dp(36));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(withAlpha(colour, 238));
        canvas.drawLine(arrowX, arrowStartY, arrowX, arrowEndY, strokePaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(colour, 245));
        path.reset();
        path.moveTo(arrowX, arrowEndY + dp(4));
        path.lineTo(arrowX - dp(5), arrowEndY - dp(5));
        path.lineTo(arrowX + dp(5), arrowEndY - dp(5));
        path.close();
        canvas.drawPath(path, paint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);

        if (tutorialStage == TutorialStage.DESTINATION) {
            RectF source = tubeRects[move.from];
            path.reset();
            path.moveTo(source.centerX(), source.top + dp(11));
            path.cubicTo(source.centerX(), source.top - dp(34), focus.centerX(), focus.top - dp(34), focus.centerX(), focus.top + dp(11));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(1.2f));
            strokePaint.setColor(withAlpha(AMBER, 150));
            canvas.drawPath(path, strokePaint);
        }
    }

    private void drawTubeIndex(Canvas canvas, RectF rect, int index) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(118, 3, 8, 29));
        canvas.drawCircle(rect.centerX(), rect.bottom + dp(12), dp(9), paint);
        drawText(canvas, String.valueOf(index + 1), rect.centerX(), rect.bottom + dp(15), dp(8),
                Color.argb(218, 225, 241, 255), mono, Paint.Align.CENTER);
    }

    private void drawPourStream(Canvas canvas, long now) {
        if (pourFrom < 0 || pourTo < 0) return;
        RectF from = new RectF(tubeRects[pourFrom]);
        RectF to = tubeRects[pourTo];
        float progress = Math.min(1f, (now - pourStartedAt) / 360f);
        float lift = -dp(20) * (float) Math.sin(progress * Math.PI);
        from.offset(0, lift);
        float startX = from.centerX() + (to.centerX() > from.centerX() ? from.width() * .22f : -from.width() * .22f);
        float startY = from.top + dp(10);
        float endX = to.centerX();
        float endY = to.top + dp(18);

        path.reset();
        path.moveTo(startX, startY);
        float bend = (endX - startX) * .45f;
        path.cubicTo(startX + bend, startY - dp(15), endX - bend, endY - dp(34), endX, endY);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(dp(6) + dp(3) * (float) Math.sin(progress * Math.PI));
        strokePaint.setColor(withAlpha(pourColor, 215));
        canvas.drawPath(path, strokePaint);
        strokePaint.setStrokeWidth(dp(1.4f));
        strokePaint.setColor(Color.argb(165, 255, 255, 255));
        canvas.drawPath(path, strokePaint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawFooter(Canvas canvas) {
        float controlY = undoRect.centerY();
        boolean available = !tutorialActive && !timeExpired && !levelComplete;
        drawCircleControl(canvas, undoRect, available);
        drawCircleControl(canvas, restartRect, available);
        drawUndoIcon(canvas, undoRect.centerX(), controlY);
        drawRestartIcon(canvas, restartRect.centerX(), controlY);
        drawText(canvas, "UNDO", undoRect.centerX(), undoRect.bottom + dp(15), dp(8),
                Color.argb(210, 215, 230, 255), mono, Paint.Align.CENTER);
        drawText(canvas, "RESTART", restartRect.centerX(), restartRect.bottom + dp(15), dp(8),
                Color.argb(210, 215, 230, 255), mono, Paint.Align.CENTER);

        if (tutorialActive) {
            drawText(canvas, "GUIDED", width / 2f, controlY - dp(1), dp(18), Color.WHITE, display, Paint.Align.CENTER);
            drawText(canvas, "START", width / 2f, controlY + dp(15), dp(8), Color.argb(187, 211, 227, 255), mono, Paint.Align.CENTER);
        } else {
            drawText(canvas, String.format(Locale.US, "%02d", moves), width / 2f, controlY - dp(1), dp(25), Color.WHITE, display, Paint.Align.CENTER);
            drawText(canvas, "POURS", width / 2f, controlY + dp(15), dp(8), Color.argb(187, 211, 227, 255), mono, Paint.Align.CENTER);
        }

        drawGlassPill(canvas, adRect, Color.argb(33, 92, 114, 169), Color.argb(96, 184, 207, 255));
        drawText(canvas, "AD SPACE  ·  REMOVE ADS FOREVER — $1.49", adRect.centerX(), adRect.centerY() + dp(3),
                dp(9), Color.argb(224, 229, 239, 255), mono, Paint.Align.CENTER);
    }

    private void drawCompletion(Canvas canvas, long now) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 2, 4, 18));
        canvas.drawRect(0, 0, width, height, paint);

        float centerX = width / 2f;
        float centerY = height / 2f - dp(38);
        for (int i = 0; i < 18; i++) {
            float angle = (float) (i * Math.PI * 2 / 18.0 + now / 880.0);
            float radius = dp(70) + (i % 3) * dp(18);
            paint.setColor(withAlpha(SPECTRUM[i % SPECTRUM.length], 155));
            canvas.drawCircle(centerX + (float) Math.cos(angle) * radius, centerY + (float) Math.sin(angle) * radius,
                    dp(2.2f), paint);
        }

        RectF halo = new RectF(centerX - dp(112), centerY - dp(104), centerX + dp(112), centerY + dp(92));
        paint.setShader(new LinearGradient(halo.left, halo.top, halo.right, halo.bottom,
                new int[]{Color.argb(185, 27, 53, 112), Color.argb(202, 44, 25, 104), Color.argb(185, 19, 74, 120)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(halo, dp(27), dp(27), paint);
        paint.setShader(null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(Color.argb(170, 212, 238, 255));
        canvas.drawRoundRect(halo, dp(27), dp(27), strokePaint);

        drawText(canvas, "SPECTRUM", centerX, centerY - dp(34), dp(10), Color.argb(185, 205, 225, 255), mono, Paint.Align.CENTER);
        drawText(canvas, "COMPLETE", centerX, centerY + dp(3), dp(28), Color.WHITE, display, Paint.Align.CENTER);
        drawText(canvas, "A clear mind moves in colour.", centerX, centerY + dp(30), dp(12),
                Color.argb(220, 219, 231, 255), label, Paint.Align.CENTER);
        drawEndButton(canvas, "NEXT LEVEL");
    }

    private void drawTimeExpired(Canvas canvas, long now) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(202, 4, 2, 18));
        canvas.drawRect(0, 0, width, height, paint);
        float centerX = width / 2f;
        float centerY = height / 2f - dp(38);
        RectF halo = new RectF(centerX - dp(112), centerY - dp(104), centerX + dp(112), centerY + dp(92));
        paint.setShader(new LinearGradient(halo.left, halo.top, halo.right, halo.bottom,
                new int[]{Color.argb(185, 112, 51, 40), Color.argb(202, 112, 22, 76), Color.argb(185, 75, 24, 87)},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(halo, dp(27), dp(27), paint);
        paint.setShader(null);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(Color.argb(170, 255, 203, 180));
        canvas.drawRoundRect(halo, dp(27), dp(27), strokePaint);
        drawText(canvas, "TIME", centerX, centerY - dp(34), dp(10), Color.argb(225, 255, 209, 177), mono, Paint.Align.CENTER);
        drawText(canvas, "UP", centerX, centerY + dp(3), dp(34), Color.WHITE, display, Paint.Align.CENTER);
        drawText(canvas, "Reset and beat the next run.", centerX, centerY + dp(30), dp(12),
                Color.argb(220, 239, 219, 230), label, Paint.Align.CENTER);
        drawEndButton(canvas, "TRY AGAIN");
    }

    private void drawEndButton(Canvas canvas, String title) {
        paint.setShader(new LinearGradient(nextRect.left, nextRect.top, nextRect.right, nextRect.bottom,
                new int[]{CYAN, VIOLET, PINK}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(nextRect, dp(17), dp(17), paint);
        paint.setShader(null);
        drawText(canvas, title, nextRect.centerX(), nextRect.centerY() + dp(4), dp(12),
                Color.WHITE, display, Paint.Align.CENTER);
    }

    private void drawCircleControl(Canvas canvas, RectF rect, boolean active) {
        float radius = rect.width() / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? Color.argb(74, 91, 214, 255) : Color.argb(37, 132, 152, 190));
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(active ? Color.argb(196, 220, 241, 255) : Color.argb(105, 189, 212, 235));
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, strokePaint);
    }

    private void drawUndoIcon(Canvas canvas, float cx, float cy) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.8f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx + dp(10), cy - dp(7));
        path.cubicTo(cx + dp(1), cy - dp(12), cx - dp(12), cy - dp(6), cx - dp(10), cy + dp(4));
        path.cubicTo(cx - dp(8), cy + dp(10), cx + dp(2), cy + dp(11), cx + dp(9), cy + dp(6));
        canvas.drawPath(path, strokePaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx - dp(12), cy + dp(1));
        path.lineTo(cx - dp(6), cy - dp(4));
        path.lineTo(cx - dp(5), cy + dp(5));
        path.close();
        canvas.drawPath(path, paint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawRestartIcon(Canvas canvas, float cx, float cy) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.8f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(Color.WHITE);
        RectF arc = new RectF(cx - dp(10), cy - dp(10), cx + dp(10), cy + dp(10));
        canvas.drawArc(arc, -60, 282, false, strokePaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx + dp(7), cy - dp(12));
        path.lineTo(cx + dp(13), cy - dp(11));
        path.lineTo(cx + dp(10), cy - dp(5));
        path.close();
        canvas.drawPath(path, paint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawGlassPill(Canvas canvas, RectF rect, int fill, int border) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(border);
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, strokePaint);
    }

    private void drawText(Canvas canvas, String text, float x, float baseline, float size, int colour,
                          Typeface typeface, Paint.Align align) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(colour);
        paint.setTextSize(size);
        paint.setTypeface(typeface);
        paint.setTextAlign(align);
        canvas.drawText(text, x, baseline, paint);
    }

    private float dp(float value) {
        return value * density;
    }

    private int withAlpha(int colour, int alpha) {
        return Color.argb(alpha, Color.red(colour), Color.green(colour), Color.blue(colour));
    }

    private int brighten(int colour, float amount) {
        return Color.rgb(
                (int) (Color.red(colour) + (255 - Color.red(colour)) * amount),
                (int) (Color.green(colour) + (255 - Color.green(colour)) * amount),
                (int) (Color.blue(colour) + (255 - Color.blue(colour)) * amount));
    }

    private int darken(int colour, float amount) {
        return Color.rgb(
                (int) (Color.red(colour) * (1f - amount)),
                (int) (Color.green(colour) * (1f - amount)),
                (int) (Color.blue(colour) * (1f - amount)));
    }
}
