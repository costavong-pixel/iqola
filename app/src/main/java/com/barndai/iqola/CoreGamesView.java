package com.barndai.iqola;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/** Four short, playable IQOla brain-training loops in the shared dark game frame. */
final class CoreGamesView extends View {
    private static final int NAVY = Color.rgb(5, 8, 29);
    private static final int INK = Color.rgb(246, 249, 255);
    private static final int MUTED = Color.rgb(173, 194, 221);
    private static final int CARD = Color.rgb(14, 25, 56);
    private static final int BLUE = Color.rgb(37, 221, 255);
    private static final int LILAC = Color.rgb(125, 105, 255);
    private static final int AQUA = Color.rgb(70, 231, 208);
    private static final int CORAL = Color.rgb(255, 81, 180);
    private static final int GOLD = Color.rgb(255, 182, 65);
    private static final int MINT = Color.rgb(73, 234, 179);
    private static final int PALE_BLUE = Color.rgb(18, 52, 83);
    private static final int PALE_LILAC = Color.rgb(42, 32, 86);
    private static final int PALE_AQUA = Color.rgb(15, 61, 70);
    private static final int PALE_CORAL = Color.rgb(83, 25, 68);
    private static final int PALE_MINT = Color.rgb(20, 70, 61);
    private static final int PALE_GOLD = Color.rgb(79, 56, 26);
    private static final int BORDER = Color.rgb(77, 108, 147);
    private static final int ROUNDS_PER_LEVEL = 2;
    private static final int TOTAL_ROUNDS = 8;
    private static final String[] COLOR_NAMES = {"PINK", "CYAN", "GOLD", "MINT"};
    private static final int[] COLOR_VALUES = {CORAL, BLUE, GOLD, MINT};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Path path = new Path();
    private final Typeface display = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface label = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mono = Typeface.create("sans-serif-smallcaps", Typeface.BOLD);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private final Runnable onExit;
    private final RectF[] stroopChoices = new RectF[4];
    private final RectF[] matrixTiles = new RectF[25];
    private final RectF backRect = new RectF();
    private final RectF resultReplayRect = new RectF();
    private final RectF resultCatalogRect = new RectF();
    private final RectF objectTrackRect = new RectF();

    private enum Screen { GAME, RESULT }

    private enum GameType {
        REVERSE_STROOP("REVERSE STROOP", "Read the word, not its ink.", CORAL, PALE_CORAL),
        MEMORY_MATRIX("MEMORY MATRIX", "Remember the glowing pattern.", BLUE, PALE_BLUE),
        STOP_SIGNAL("STOP SIGNAL", "Act fast, then hold back.", GOLD, PALE_GOLD),
        OBJECT_TRACK("OBJECT TRACK", "Follow one moving target.", AQUA, PALE_AQUA);

        final String title;
        final String subtitle;
        final int accent;
        final int pale;

        GameType(String title, String subtitle, int accent, int pale) {
            this.title = title;
            this.subtitle = subtitle;
            this.accent = accent;
            this.pale = pale;
        }

        static GameType fromIndex(int index) {
            GameType[] values = values();
            return values[Math.max(0, Math.min(values.length - 1, index))];
        }
    }

    private enum StopStage { READY, GO, STOP }
    private enum ObjectStage { STUDY, TRACKING, CHOOSE }

    private Screen screen = Screen.GAME;
    private GameType selectedGame;
    private int score;
    private int level = 1;
    private int roundsPlayed;
    private int correctRounds;
    private int resultBest;
    private String resultTitle = "Nice work";
    private String feedback = "";
    private int roundToken;
    private float density;
    private float width;
    private float height;

    // Reverse Stroop state.
    private int stroopWordIndex;
    private int stroopInkIndex;
    private final int[] stroopChoiceOrder = new int[4];
    private long stroopEndsAt;

    // Memory Matrix state.
    private final ArrayList<Integer> matrixPattern = new ArrayList<>();
    private final Set<Integer> matrixChosen = new LinkedHashSet<>();
    private int matrixGridSize = 3;
    private boolean matrixShowing;

    // Stop Signal state.
    private StopStage stopStage = StopStage.READY;
    private boolean stopTrial;
    private long stopGoStartedAt;

    // Object Track state.
    private final ArrayList<MovingDot> movingDots = new ArrayList<>();
    private ObjectStage objectStage = ObjectStage.STUDY;
    private int objectTarget = -1;
    private long objectLastFrameAt;

    CoreGamesView(Context context, int gameIndex, Runnable onExit) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        this.onExit = onExit;
        selectedGame = GameType.fromIndex(gameIndex);
        preferences = context.getSharedPreferences("iqola_core_progress", Context.MODE_PRIVATE);
        setFocusable(true);
        setContentDescription(selectedGame.title + " IQOla game");
        for (int i = 0; i < stroopChoices.length; i++) stroopChoices[i] = new RectF();
        for (int i = 0; i < matrixTiles.length; i++) matrixTiles[i] = new RectF();
        post(new Runnable() {
            @Override
            public void run() {
                openGame(selectedGame);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas, SystemClock.uptimeMillis());
        if (screen == Screen.GAME) drawGame(canvas);
        else drawResult(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        performClick();
        float x = event.getX();
        float y = event.getY();

        if (screen == Screen.RESULT) {
            if (resultReplayRect.contains(x, y)) openGame(selectedGame);
            else if (resultCatalogRect.contains(x, y)) exitToCatalog();
            return true;
        }
        if (backRect.contains(x, y)) {
            exitToCatalog();
            return true;
        }
        if (selectedGame == GameType.REVERSE_STROOP) handleStroopTap(x, y);
        else if (selectedGame == GameType.MEMORY_MATRIX) handleMatrixTap(x, y);
        else if (selectedGame == GameType.STOP_SIGNAL) handleStopSignalTap(x, y);
        else handleObjectTrackTap(x, y);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        clearScheduledWork();
        super.onDetachedFromWindow();
    }

    boolean handleBack() {
        return false;
    }

    private void exitToCatalog() {
        clearScheduledWork();
        onExit.run();
    }

    private void openGame(GameType game) {
        clearScheduledWork();
        selectedGame = game;
        score = 0;
        level = 1;
        roundsPlayed = 0;
        correctRounds = 0;
        feedback = "";
        screen = Screen.GAME;
        startCurrentRound();
    }

    private void clearScheduledWork() {
        handler.removeCallbacksAndMessages(null);
    }

    private void startCurrentRound() {
        clearScheduledWork();
        roundToken++;
        feedback = "";
        if (selectedGame == GameType.REVERSE_STROOP) startReverseStroopRound();
        else if (selectedGame == GameType.MEMORY_MATRIX) startMemoryMatrixRound();
        else if (selectedGame == GameType.STOP_SIGNAL) startStopSignalRound();
        else startObjectTrackRound();
        invalidate();
    }

    private void completeRound(boolean correct, String message) {
        if (screen != Screen.GAME) return;
        clearScheduledWork();
        final int completionToken = ++roundToken;
        feedback = message;
        if (correct) {
            correctRounds++;
            score += 90 + level * 35;
        }
        roundsPlayed++;
        invalidate();
        if (roundsPlayed >= TOTAL_ROUNDS) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (screen == Screen.GAME && roundToken == completionToken) finishGame();
                }
            }, 700L);
            return;
        }
        level = 1 + roundsPlayed / ROUNDS_PER_LEVEL;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (screen == Screen.GAME && roundToken == completionToken) startCurrentRound();
            }
        }, correct ? 520L : 850L);
    }

    private void finishGame() {
        clearScheduledWork();
        roundToken++;
        resultBest = recordBest();
        if (correctRounds >= 7) resultTitle = "Excellent focus";
        else if (correctRounds >= 4) resultTitle = "Strong training";
        else resultTitle = "Keep building it";
        screen = Screen.RESULT;
        invalidate();
    }

    private int recordBest() {
        String key = "best_" + selectedGame.name();
        int best = preferences.getInt(key, 0);
        if (score > best) {
            preferences.edit().putInt(key, score).apply();
            return score;
        }
        return best;
    }

    // Reverse Stroop ---------------------------------------------------------------------------

    private void startReverseStroopRound() {
        stroopWordIndex = random.nextInt(COLOR_NAMES.length);
        do stroopInkIndex = random.nextInt(COLOR_NAMES.length);
        while (stroopInkIndex == stroopWordIndex);
        for (int i = 0; i < stroopChoiceOrder.length; i++) stroopChoiceOrder[i] = i;
        shuffle(stroopChoiceOrder);
        stroopEndsAt = SystemClock.elapsedRealtime() + Math.max(2700L, 5300L - level * 450L);
        final int token = roundToken;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tickStroopTimer(token);
            }
        }, 80L);
    }

    private void tickStroopTimer(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.REVERSE_STROOP || token != roundToken) return;
        if (SystemClock.elapsedRealtime() >= stroopEndsAt) {
            completeRound(false, "Time ran out");
            return;
        }
        invalidate();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tickStroopTimer(token);
            }
        }, 80L);
    }

    private void handleStroopTap(float x, float y) {
        layoutStroopChoices();
        for (int i = 0; i < stroopChoices.length; i++) {
            if (stroopChoices[i].contains(x, y)) {
                int chosen = stroopChoiceOrder[i];
                completeRound(chosen == stroopWordIndex,
                        chosen == stroopWordIndex ? "Correct" : "Read the word, not the ink");
                return;
            }
        }
    }

    // Memory Matrix ---------------------------------------------------------------------------

    private void startMemoryMatrixRound() {
        matrixGridSize = level < 3 ? 3 : level < 5 ? 4 : 5;
        int patternCount = Math.min(matrixGridSize * matrixGridSize - 2, level + 1);
        matrixPattern.clear();
        matrixChosen.clear();
        while (matrixPattern.size() < patternCount) {
            int candidate = random.nextInt(matrixGridSize * matrixGridSize);
            if (!matrixPattern.contains(candidate)) matrixPattern.add(candidate);
        }
        matrixShowing = true;
        final int token = roundToken;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (screen == Screen.GAME && selectedGame == GameType.MEMORY_MATRIX && token == roundToken) {
                    matrixShowing = false;
                    feedback = "Now repeat the pattern";
                    invalidate();
                }
            }
        }, 900L + patternCount * 350L);
    }

    private void handleMatrixTap(float x, float y) {
        if (matrixShowing) {
            feedback = "Watch the glowing tiles first";
            invalidate();
            return;
        }
        layoutMatrixTiles();
        int tile = -1;
        for (int i = 0; i < matrixGridSize * matrixGridSize; i++) {
            if (matrixTiles[i].contains(x, y)) {
                tile = i;
                break;
            }
        }
        if (tile < 0 || matrixChosen.contains(tile)) return;
        if (!matrixPattern.contains(tile)) {
            completeRound(false, "That tile was not in the pattern");
            return;
        }
        matrixChosen.add(tile);
        if (matrixChosen.size() == matrixPattern.size()) completeRound(true, "Pattern complete");
        else {
            feedback = "Good — keep going";
            invalidate();
        }
    }

    // Stop Signal -----------------------------------------------------------------------------

    private void startStopSignalRound() {
        stopStage = StopStage.READY;
        stopTrial = random.nextInt(100) < Math.min(70, 30 + level * 10);
        final int token = roundToken;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showGoSignal(token);
            }
        }, 750L + random.nextInt(700));
    }

    private void showGoSignal(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.STOP_SIGNAL || token != roundToken) return;
        stopStage = StopStage.GO;
        stopGoStartedAt = SystemClock.elapsedRealtime();
        invalidate();
        if (stopTrial) {
            long stopDelay = Math.max(340L, 980L - level * 115L) + random.nextInt(220);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showStopSignal(token);
                }
            }, stopDelay);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (screen == Screen.GAME && selectedGame == GameType.STOP_SIGNAL
                            && token == roundToken && stopStage == StopStage.GO) {
                        completeRound(false, "You missed GO");
                    }
                }
            }, 1600L);
        }
    }

    private void showStopSignal(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.STOP_SIGNAL
                || token != roundToken || stopStage != StopStage.GO) return;
        stopStage = StopStage.STOP;
        feedback = "Hold still";
        invalidate();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (screen == Screen.GAME && selectedGame == GameType.STOP_SIGNAL
                        && token == roundToken && stopStage == StopStage.STOP) completeRound(true, "Good stop");
            }
        }, 650L);
    }

    private void handleStopSignalTap(float x, float y) {
        if (!getPlayBoard().contains(x, y)) return;
        if (stopStage == StopStage.READY) completeRound(false, "Too early");
        else if (stopStage == StopStage.STOP) completeRound(false, "Stopped too late");
        else if (stopTrial) completeRound(false, "The signal was about to stop");
        else {
            long reaction = Math.max(1L, SystemClock.elapsedRealtime() - stopGoStartedAt);
            completeRound(true, reaction + " ms reaction");
        }
    }

    // Object Track ---------------------------------------------------------------------------

    private void startObjectTrackRound() {
        movingDots.clear();
        int count = Math.min(7, 2 + level);
        for (int i = 0; i < count; i++) {
            MovingDot dot = new MovingDot();
            placeNewDot(dot);
            movingDots.add(dot);
        }
        objectTarget = random.nextInt(movingDots.size());
        objectStage = ObjectStage.STUDY;
        final int token = roundToken;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startObjectMotion(token);
            }
        }, 1100L);
    }

    private void placeNewDot(MovingDot dot) {
        int attempts = 0;
        do {
            dot.x = 0.14f + random.nextFloat() * 0.72f;
            dot.y = 0.16f + random.nextFloat() * 0.68f;
            attempts++;
        } while (attempts < 30 && overlapsExisting(dot));
        float speed = 0.075f + level * 0.012f;
        double angle = random.nextDouble() * Math.PI * 2.0;
        dot.vx = (float) (Math.cos(angle) * speed);
        dot.vy = (float) (Math.sin(angle) * speed);
    }

    private boolean overlapsExisting(MovingDot candidate) {
        for (MovingDot existing : movingDots) {
            float dx = candidate.x - existing.x;
            float dy = candidate.y - existing.y;
            if (dx * dx + dy * dy < 0.035f) return true;
        }
        return false;
    }

    private void startObjectMotion(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.OBJECT_TRACK || token != roundToken) return;
        objectStage = ObjectStage.TRACKING;
        objectLastFrameAt = SystemClock.elapsedRealtime();
        runObjectFrame(token);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finishObjectMotion(token);
            }
        }, 1700L + level * 300L);
    }

    private void runObjectFrame(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.OBJECT_TRACK
                || token != roundToken || objectStage != ObjectStage.TRACKING) return;
        long now = SystemClock.elapsedRealtime();
        float elapsed = Math.min(.05f, (now - objectLastFrameAt) / 1000f);
        objectLastFrameAt = now;
        for (MovingDot dot : movingDots) {
            dot.x += dot.vx * elapsed;
            dot.y += dot.vy * elapsed;
            if (dot.x < .09f || dot.x > .91f) {
                dot.vx = -dot.vx;
                dot.x = clamp(dot.x, .09f, .91f);
            }
            if (dot.y < .12f || dot.y > .88f) {
                dot.vy = -dot.vy;
                dot.y = clamp(dot.y, .12f, .88f);
            }
        }
        invalidate();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runObjectFrame(token);
            }
        }, 16L);
    }

    private void finishObjectMotion(int token) {
        if (screen != Screen.GAME || selectedGame != GameType.OBJECT_TRACK || token != roundToken) return;
        objectStage = ObjectStage.CHOOSE;
        feedback = "Tap the dot you tracked";
        invalidate();
    }

    private void handleObjectTrackTap(float x, float y) {
        if (objectStage != ObjectStage.CHOOSE) {
            feedback = objectStage == ObjectStage.STUDY ? "Find the outlined target first" : "Keep your eyes on the target";
            invalidate();
            return;
        }
        layoutObjectTrackRect();
        float radius = objectDotRadius();
        for (int i = 0; i < movingDots.size(); i++) {
            MovingDot dot = movingDots.get(i);
            float cx = objectTrackRect.left + dot.x * objectTrackRect.width();
            float cy = objectTrackRect.top + dot.y * objectTrackRect.height();
            float dx = x - cx;
            float dy = y - cy;
            if (dx * dx + dy * dy <= radius * radius * 1.45f) {
                completeRound(i == objectTarget, i == objectTarget ? "Target found" : "That was a different dot");
                return;
            }
        }
    }

    // Shared game frame -----------------------------------------------------------------------

    private void drawBackground(Canvas canvas, long now) {
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{Color.rgb(7, 15, 49), Color.rgb(12, 8, 37), NAVY}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
        float seconds = now / 1000f;
        for (int i = 0; i < 14; i++) {
            float x = (float) ((i * 89 % 997) / 997.0 * width + Math.sin(seconds * .32 + i) * dp(7));
            float y = (float) ((i * 157 % 1201) / 1201.0 * height + Math.cos(seconds * .24 + i) * dp(10));
            paint.setColor(Color.argb(30 + (i % 4) * 7, 196, 236, 255));
            canvas.drawCircle(x, y, dp(i % 3 == 0 ? 1.25f : .7f), paint);
        }
    }

    private void drawGame(Canvas canvas) {
        drawGameHeader(canvas);
        if (selectedGame == GameType.REVERSE_STROOP) drawReverseStroopGame(canvas);
        else if (selectedGame == GameType.MEMORY_MATRIX) drawMemoryMatrixGame(canvas);
        else if (selectedGame == GameType.STOP_SIGNAL) drawStopSignalGame(canvas);
        else drawObjectTrackGame(canvas);
        drawAdSlot(canvas);
    }

    private void drawGameHeader(Canvas canvas) {
        backRect.set(dp(18), dp(20), dp(60), dp(62));
        drawGlassPill(canvas, backRect, Color.argb(42, 124, 170, 230), Color.argb(150, 201, 232, 255));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(Color.WHITE);
        canvas.drawLine(dp(45), dp(31), dp(34), dp(41), strokePaint);
        canvas.drawLine(dp(34), dp(41), dp(45), dp(51), strokePaint);
        strokePaint.setStrokeCap(Paint.Cap.BUTT);

        drawText(canvas, selectedGame.title, dp(72), dp(39), dp(18), Color.WHITE, display, Paint.Align.LEFT);
        drawText(canvas, "LEVEL " + level + "  ·  ROUND " + (roundsPlayed + 1) + "/" + TOTAL_ROUNDS,
                dp(72), dp(57), dp(9), MUTED, mono, Paint.Align.LEFT);
        RectF scorePill = new RectF(width - dp(102), dp(20), width - dp(18), dp(62));
        drawGlassPill(canvas, scorePill, withAlpha(selectedGame.accent, 48), withAlpha(selectedGame.accent, 205));
        drawText(canvas, String.valueOf(score), scorePill.centerX(), scorePill.centerY() + dp(5), dp(16),
                Color.WHITE, display, Paint.Align.CENTER);
    }

    private void drawReverseStroopGame(Canvas canvas) {
        RectF board = getPlayBoard();
        drawBoard(canvas, board, "Tap the word you read — never the ink colour.");
        float duration = Math.max(2700L, 5300L - level * 450L);
        float timerProgress = clamp((stroopEndsAt - SystemClock.elapsedRealtime()) / duration, 0f, 1f);
        RectF timer = new RectF(board.left + dp(20), board.top + dp(58), board.right - dp(20), board.top + dp(65));
        fillRound(canvas, timer, dp(4), PALE_CORAL);
        fillRound(canvas, new RectF(timer.left, timer.top, timer.left + timer.width() * timerProgress, timer.bottom), dp(4), CORAL);
        drawText(canvas, "READ THE WORD", board.centerX(), board.top + dp(104), dp(10), MUTED, mono, Paint.Align.CENTER);
        drawText(canvas, COLOR_NAMES[stroopWordIndex], board.centerX(), board.top + dp(157), dp(35),
                COLOR_VALUES[stroopInkIndex], display, Paint.Align.CENTER);
        drawText(canvas, "Choose its meaning", board.centerX(), board.top + dp(183), dp(12), MUTED, label, Paint.Align.CENTER);
        layoutStroopChoices();
        for (int i = 0; i < stroopChoices.length; i++) {
            RectF choice = stroopChoices[i];
            drawGlassPill(canvas, choice, Color.argb(120, 18, 31, 66), Color.argb(112, 164, 200, 240));
            int colorIndex = stroopChoiceOrder[i];
            fillRound(canvas, new RectF(choice.left + dp(12), choice.centerY() - dp(8), choice.left + dp(28), choice.centerY() + dp(8)),
                    dp(8), COLOR_VALUES[colorIndex]);
            drawText(canvas, COLOR_NAMES[colorIndex], choice.left + dp(38), choice.centerY() + dp(5), dp(14),
                    INK, display, Paint.Align.LEFT);
        }
        drawFeedback(canvas, board);
    }

    private void layoutStroopChoices() {
        RectF board = getPlayBoard();
        float margin = dp(20);
        float gap = dp(10);
        float cellWidth = (board.width() - margin * 2 - gap) / 2f;
        float cellHeight = Math.min(dp(58), (board.bottom - board.top - dp(230)) / 2f - gap);
        float top = board.bottom - cellHeight * 2 - gap - dp(22);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = board.left + margin + column * (cellWidth + gap);
            float y = top + row * (cellHeight + gap);
            stroopChoices[i].set(left, y, left + cellWidth, y + cellHeight);
        }
    }

    private void drawMemoryMatrixGame(Canvas canvas) {
        RectF board = getPlayBoard();
        drawBoard(canvas, board, matrixShowing ? "Remember every glowing tile." : "Tap the tiles in any order.");
        drawText(canvas, matrixShowing ? "WATCH" : "REPEAT", board.centerX(), board.top + dp(92), dp(10),
                matrixShowing ? BLUE : MUTED, mono, Paint.Align.CENTER);
        layoutMatrixTiles();
        for (int i = 0; i < matrixGridSize * matrixGridSize; i++) {
            boolean highlighted = matrixShowing && matrixPattern.contains(i);
            boolean chosen = !matrixShowing && matrixChosen.contains(i);
            int fill = highlighted ? BLUE : chosen ? MINT : CARD;
            fillRound(canvas, matrixTiles[i], dp(12), fill);
            strokeRound(canvas, matrixTiles[i], dp(12), highlighted ? BLUE : chosen ? MINT : BORDER, 1);
            if (highlighted) {
                paint.setColor(Color.argb(75, 255, 255, 255));
                canvas.drawCircle(matrixTiles[i].centerX(), matrixTiles[i].centerY(), matrixTiles[i].width() * .16f, paint);
            }
        }
        String remaining = matrixShowing ? matrixPattern.size() + " tiles" : (matrixPattern.size() - matrixChosen.size()) + " left";
        drawText(canvas, remaining, board.centerX(), board.bottom - dp(22), dp(11), matrixShowing ? BLUE : MUTED,
                mono, Paint.Align.CENTER);
        drawFeedback(canvas, board);
    }

    private void layoutMatrixTiles() {
        RectF board = getPlayBoard();
        float maxWidth = board.width() - dp(52);
        float maxHeight = board.height() - dp(168);
        float gap = dp(9);
        float size = Math.min((maxWidth - gap * (matrixGridSize - 1)) / matrixGridSize,
                (maxHeight - gap * (matrixGridSize - 1)) / matrixGridSize);
        float gridWidth = size * matrixGridSize + gap * (matrixGridSize - 1);
        float left = board.centerX() - gridWidth / 2f;
        float top = board.centerY() - gridWidth / 2f + dp(18);
        for (int i = 0; i < matrixGridSize * matrixGridSize; i++) {
            int row = i / matrixGridSize;
            int column = i % matrixGridSize;
            float x = left + column * (size + gap);
            float y = top + row * (size + gap);
            matrixTiles[i].set(x, y, x + size, y + size);
        }
    }

    private void drawStopSignalGame(Canvas canvas) {
        RectF board = getPlayBoard();
        drawBoard(canvas, board, "Tap GO. If it turns STOP, do not tap.");
        float cx = board.centerX();
        float cy = board.centerY() + dp(22);
        float radius = Math.min(board.width(), board.height()) * .23f;
        int color = stopStage == StopStage.GO ? MINT : stopStage == StopStage.STOP ? CORAL : GOLD;
        int pale = stopStage == StopStage.GO ? PALE_MINT : stopStage == StopStage.STOP ? PALE_CORAL : PALE_GOLD;
        fillRound(canvas, new RectF(cx - radius - dp(12), cy - radius - dp(12), cx + radius + dp(12), cy + radius + dp(12)),
                radius + dp(18), pale);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
        String signalLabel = stopStage == StopStage.READY ? "READY" : stopStage == StopStage.GO ? "GO" : "STOP";
        drawText(canvas, signalLabel, cx, cy + dp(12), stopStage == StopStage.READY ? dp(21) : dp(31), Color.WHITE,
                display, Paint.Align.CENTER);
        String sub = stopStage == StopStage.READY ? "Wait for a signal" : stopStage == StopStage.GO ? "Tap now" : "Hold still";
        drawText(canvas, sub, cx, board.bottom - dp(34), dp(13), MUTED, label, Paint.Align.CENTER);
        drawFeedback(canvas, board);
    }

    private void drawObjectTrackGame(Canvas canvas) {
        RectF board = getPlayBoard();
        String instruction;
        if (objectStage == ObjectStage.STUDY) instruction = "Find the outlined dot. Keep it in sight.";
        else if (objectStage == ObjectStage.TRACKING) instruction = "Follow the target while every dot moves.";
        else instruction = "Tap the dot that started with the outline.";
        drawBoard(canvas, board, instruction);
        layoutObjectTrackRect();
        fillRound(canvas, objectTrackRect, dp(18), PALE_AQUA);
        strokeRound(canvas, objectTrackRect, dp(18), withAlpha(AQUA, 142), 1);
        drawTrackGrid(canvas, objectTrackRect);
        float radius = objectDotRadius();
        for (int i = 0; i < movingDots.size(); i++) {
            MovingDot dot = movingDots.get(i);
            float cx = objectTrackRect.left + dot.x * objectTrackRect.width();
            float cy = objectTrackRect.top + dot.y * objectTrackRect.height();
            paint.setColor(AQUA);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(Color.argb(190, 255, 255, 255));
            canvas.drawCircle(cx - radius * .18f, cy - radius * .18f, radius * .22f, paint);
            if (objectStage == ObjectStage.STUDY && i == objectTarget) {
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(dp(3));
                strokePaint.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, radius + dp(6), strokePaint);
            }
        }
        String state = objectStage == ObjectStage.STUDY ? "TRACK THIS DOT" :
                objectStage == ObjectStage.TRACKING ? "KEEP WATCHING" : "MAKE YOUR PICK";
        drawText(canvas, state, board.centerX(), board.bottom - dp(22), dp(10),
                objectStage == ObjectStage.CHOOSE ? AQUA : MUTED, mono, Paint.Align.CENTER);
        drawFeedback(canvas, board);
    }

    private void layoutObjectTrackRect() {
        RectF board = getPlayBoard();
        objectTrackRect.set(board.left + dp(20), board.top + dp(74), board.right - dp(20), board.bottom - dp(52));
    }

    private float objectDotRadius() {
        return Math.max(dp(12), Math.min(dp(21), objectTrackRect.width() / 15f));
    }

    private void drawTrackGrid(Canvas canvas, RectF rect) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1));
        strokePaint.setColor(withAlpha(AQUA, 45));
        for (int i = 1; i < 5; i++) {
            float x = rect.left + rect.width() * i / 5f;
            float y = rect.top + rect.height() * i / 5f;
            canvas.drawLine(x, rect.top + dp(10), x, rect.bottom - dp(10), strokePaint);
            canvas.drawLine(rect.left + dp(10), y, rect.right - dp(10), y, strokePaint);
        }
    }

    private void drawBoard(Canvas canvas, RectF board, String instruction) {
        paint.setShader(new LinearGradient(board.left, board.top, board.right, board.bottom,
                new int[]{Color.argb(212, 19, 34, 73), Color.argb(219, 12, 20, 54)}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(board, dp(25), dp(25), paint);
        paint.setShader(null);
        strokeRound(canvas, board, dp(25), BORDER, 1);
        drawText(canvas, instruction, board.centerX(), board.top + dp(30), dp(12), MUTED, label, Paint.Align.CENTER);
    }

    private void drawFeedback(Canvas canvas, RectF board) {
        if (feedback == null || feedback.length() == 0) return;
        boolean good = feedback.equals("Correct") || feedback.equals("Pattern complete") || feedback.equals("Good stop")
                || feedback.equals("Target found") || feedback.contains("reaction");
        fillRound(canvas, new RectF(board.left + dp(18), board.bottom - dp(48), board.right - dp(18), board.bottom - dp(16)),
                dp(14), good ? PALE_MINT : PALE_GOLD);
        drawText(canvas, feedback, board.centerX(), board.bottom - dp(27), dp(11), good ? MINT : INK,
                display, Paint.Align.CENTER);
    }

    private RectF getPlayBoard() {
        float top = dp(82);
        float footerTop = height - dp(90);
        return new RectF(dp(16), top, width - dp(16), Math.max(top + dp(360), footerTop - dp(10)));
    }

    private void drawAdSlot(Canvas canvas) {
        float top = height - dp(78);
        RectF ad = new RectF(dp(16), top, width - dp(16), height - dp(14));
        drawGlassPill(canvas, ad, Color.argb(33, 92, 114, 169), Color.argb(96, 184, 207, 255));
        drawText(canvas, "AD SPACE  ·  IQOLA PLUS — AD-FREE YEARLY", ad.centerX(), ad.centerY() + dp(3), dp(9),
                Color.argb(224, 229, 239, 255), mono, Paint.Align.CENTER);
    }

    private void drawResult(Canvas canvas) {
        float centerX = width / 2f;
        float centerY = height / 2f - dp(30);
        paint.setColor(Color.argb(40, selectedGame.accent == GOLD ? 255 : Color.red(selectedGame.accent),
                selectedGame.accent == GOLD ? 206 : Color.green(selectedGame.accent), 255));
        canvas.drawCircle(centerX, centerY - dp(84), dp(66), paint);
        paint.setColor(selectedGame.accent);
        canvas.drawCircle(centerX, centerY - dp(84), dp(38), paint);
        drawStar(canvas, centerX, centerY - dp(84), dp(17), Color.WHITE);

        RectF card = new RectF(dp(18), centerY - dp(46), width - dp(18), centerY + dp(278));
        paint.setShader(new LinearGradient(card.left, card.top, card.right, card.bottom,
                new int[]{Color.argb(230, 24, 40, 82), Color.argb(234, 12, 20, 55)}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(card, dp(27), dp(27), paint);
        paint.setShader(null);
        strokeRound(canvas, card, dp(27), BORDER, 1);
        drawText(canvas, selectedGame.title, card.centerX(), card.top + dp(33), dp(9),
                withAlpha(selectedGame.accent, 242), mono, Paint.Align.CENTER);
        drawText(canvas, resultTitle, card.centerX(), card.top + dp(68), dp(24), Color.WHITE, display, Paint.Align.CENTER);
        drawText(canvas, correctRounds + " OF " + TOTAL_ROUNDS + " ROUNDS", card.centerX(), card.top + dp(91), dp(10),
                MUTED, mono, Paint.Align.CENTER);

        RectF scoreBox = new RectF(card.left + dp(22), card.top + dp(113), card.centerX() - dp(7), card.top + dp(181));
        RectF bestBox = new RectF(card.centerX() + dp(7), card.top + dp(113), card.right - dp(22), card.top + dp(181));
        fillRound(canvas, scoreBox, dp(17), PALE_BLUE);
        fillRound(canvas, bestBox, dp(17), PALE_MINT);
        drawText(canvas, "SCORE", scoreBox.centerX(), scoreBox.top + dp(22), dp(9), MUTED, mono, Paint.Align.CENTER);
        drawText(canvas, String.valueOf(score), scoreBox.centerX(), scoreBox.top + dp(51), dp(23), BLUE, display, Paint.Align.CENTER);
        drawText(canvas, "BEST", bestBox.centerX(), bestBox.top + dp(22), dp(9), MUTED, mono, Paint.Align.CENTER);
        drawText(canvas, String.valueOf(resultBest), bestBox.centerX(), bestBox.top + dp(51), dp(23), MINT, display, Paint.Align.CENTER);

        resultReplayRect.set(card.left + dp(22), card.top + dp(201), card.right - dp(22), card.top + dp(250));
        resultCatalogRect.set(card.left + dp(22), card.top + dp(260), card.right - dp(22), card.top + dp(304));
        fillRound(canvas, resultReplayRect, dp(16), selectedGame.accent);
        drawText(canvas, "PLAY AGAIN", resultReplayRect.centerX(), resultReplayRect.centerY() + dp(5), dp(12),
                Color.WHITE, display, Paint.Align.CENTER);
        drawGlassPill(canvas, resultCatalogRect, Color.argb(43, 121, 164, 218), Color.argb(142, 188, 219, 255));
        drawText(canvas, "BACK TO ALL GAMES", resultCatalogRect.centerX(), resultCatalogRect.centerY() + dp(4), dp(10),
                Color.WHITE, mono, Paint.Align.CENTER);
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, int color) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2d + i * Math.PI / 5d;
            float r = i % 2 == 0 ? radius : radius * .44f;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private void fillRound(Canvas canvas, RectF rect, float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private void strokeRound(Canvas canvas, RectF rect, float radius, int color, float strokeWidth) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(strokeWidth));
        strokePaint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    private void drawGlassPill(Canvas canvas, RectF rect, int fill, int border) {
        float radius = Math.min(dp(20), rect.height() / 2f);
        fillRound(canvas, rect, radius, fill);
        strokeRound(canvas, rect, radius, border, 1);
    }

    private void drawText(Canvas canvas, String value, float x, float baseline, float size, int color,
                          Typeface typeface, Paint.Align align) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setTypeface(typeface);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTextAlign(align);
        canvas.drawText(value, x, baseline, paint);
    }

    private void shuffle(int[] values) {
        for (int i = values.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temporary = values[i];
            values[i] = values[index];
            values[index] = temporary;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * density;
    }

    private static final class MovingDot {
        float x;
        float y;
        float vx;
        float vy;
    }
}
