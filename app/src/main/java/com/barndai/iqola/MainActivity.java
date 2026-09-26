package com.barndai.iqola;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private IQOlaView iqOlaView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(IQOlaView.NAVY);
        getWindow().setNavigationBarColor(IQOlaView.NAVY);
        iqOlaView = new IQOlaView(this);
        setContentView(iqOlaView);
    }

    @Override
    public void onBackPressed() {
        if (!iqOlaView.handleBack()) super.onBackPressed();
    }
}

/**
 * The first IQOla core pack. Every game uses this one canvas surface so the product keeps the
 * same board, typography, scoring, result screen, and reserved ad position as it grows.
 */
class IQOlaView extends View {
    static final int NAVY = Color.rgb(14, 28, 66);
    private static final int INK = Color.rgb(28, 42, 79);
    private static final int MUTED = Color.rgb(101, 114, 143);
    private static final int SURFACE = Color.rgb(247, 249, 255);
    private static final int CARD = Color.WHITE;
    private static final int BLUE = Color.rgb(70, 105, 232);
    private static final int LILAC = Color.rgb(130, 105, 242);
    private static final int AQUA = Color.rgb(55, 192, 200);
    private static final int CORAL = Color.rgb(246, 111, 111);
    private static final int GOLD = Color.rgb(243, 177, 67);
    private static final int MINT = Color.rgb(79, 190, 139);
    private static final int PALE_BLUE = Color.rgb(232, 238, 255);
    private static final int PALE_LILAC = Color.rgb(239, 234, 255);
    private static final int PALE_AQUA = Color.rgb(225, 248, 249);
    private static final int PALE_CORAL = Color.rgb(255, 234, 234);
    private static final int PALE_MINT = Color.rgb(225, 247, 239);
    private static final int PALE_GOLD = Color.rgb(255, 244, 218);
    private static final int BORDER = Color.rgb(220, 226, 241);

    private static final int ROUNDS_PER_LEVEL = 2;
    private static final int TOTAL_ROUNDS = 8;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;

    private final RectF[] homeCards = new RectF[4];
    private final RectF[] stroopChoices = new RectF[4];
    private final RectF[] matrixTiles = new RectF[25];
    private final RectF backRect = new RectF();
    private final RectF resultReplayRect = new RectF();
    private final RectF resultHomeRect = new RectF();
    private final RectF objectTrackRect = new RectF();

    private enum Screen { HOME, GAME, RESULT }

    private enum GameType {
        REVERSE_STROOP("Reverse Stroop", "Read the word, not its ink", CORAL, PALE_CORAL),
        MEMORY_MATRIX("Memory Matrix", "Remember the glowing pattern", BLUE, PALE_BLUE),
        STOP_SIGNAL("Stop Signal", "Act fast, then hold back", LILAC, PALE_LILAC),
        OBJECT_TRACK("Object Track", "Follow one moving target", AQUA, PALE_AQUA);

        final String title;
        final String subtitle;
        final int color;
        final int pale;

        GameType(String title, String subtitle, int color, int pale) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.pale = pale;
        }
    }

    private enum StopStage { READY, GO, STOP }
    private enum ObjectStage { STUDY, TRACKING, CHOOSE }

    private Screen screen = Screen.HOME;
    private GameType selectedGame = GameType.REVERSE_STROOP;
    private int score;
    private int level = 1;
    private int roundsPlayed;
    private int correctRounds;
    private int resultBest;
    private String resultTitle = "Nice work";
    private String feedback = "";
    private int roundToken;

    // Reverse Stroop state.
    private static final String[] COLOR_NAMES = {"RED", "BLUE", "GOLD", "MINT"};
    private static final int[] COLOR_VALUES = {CORAL, BLUE, GOLD, MINT};
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

    IQOlaView(Context context) {
        super(context);
        preferences = context.getSharedPreferences("iqola_progress", Context.MODE_PRIVATE);
        setBackgroundColor(SURFACE);
        setFocusable(true);
        setContentDescription("IQOla brain training games");
        for (int i = 0; i < homeCards.length; i++) homeCards[i] = new RectF();
        for (int i = 0; i < stroopChoices.length; i++) stroopChoices[i] = new RectF();
        for (int i = 0; i < matrixTiles.length; i++) matrixTiles[i] = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(SURFACE);
        if (screen == Screen.HOME) drawHome(canvas);
        else if (screen == Screen.GAME) drawGame(canvas);
        else drawResult(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        float x = event.getX();
        float y = event.getY();
        performClick();

        if (screen == Screen.HOME) {
            for (int i = 0; i < homeCards.length; i++) {
                if (homeCards[i].contains(x, y)) {
                    openGame(GameType.values()[i]);
                    return true;
                }
            }
            return true;
        }
        if (screen == Screen.RESULT) {
            if (resultReplayRect.contains(x, y)) openGame(selectedGame);
            else if (resultHomeRect.contains(x, y)) goHome();
            return true;
        }
        if (backRect.contains(x, y)) {
            goHome();
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

    boolean handleBack() {
        if (screen == Screen.HOME) return false;
        goHome();
        return true;
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

    private void goHome() {
        clearScheduledWork();
        roundToken++;
        screen = Screen.HOME;
        feedback = "";
        invalidate();
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
                @Override public void run() {
                    if (screen == Screen.GAME && roundToken == completionToken) finishGame();
                }
            }, 700L);
            return;
        }
        level = 1 + (roundsPlayed / ROUNDS_PER_LEVEL);
        handler.postDelayed(new Runnable() {
            @Override public void run() {
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

    private int bestFor(GameType game) {
        return preferences.getInt("best_" + game.name(), 0);
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
            @Override public void run() { tickStroopTimer(token); }
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
            @Override public void run() { tickStroopTimer(token); }
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
        matrixGridSize = level < 3 ? 3 : (level < 5 ? 4 : 5);
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
            @Override public void run() {
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
            @Override public void run() { showGoSignal(token); }
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
                @Override public void run() { showStopSignal(token); }
            }, stopDelay);
        } else {
            handler.postDelayed(new Runnable() {
                @Override public void run() {
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
            @Override public void run() {
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
            @Override public void run() { startObjectMotion(token); }
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
            @Override public void run() { finishObjectMotion(token); }
        }, 1700L + level * 300L);
    }

    private void runObjectFrame(final int token) {
        if (screen != Screen.GAME || selectedGame != GameType.OBJECT_TRACK
                || token != roundToken || objectStage != ObjectStage.TRACKING) return;
        long now = SystemClock.elapsedRealtime();
        float elapsed = Math.min(0.05f, (now - objectLastFrameAt) / 1000f);
        objectLastFrameAt = now;
        for (MovingDot dot : movingDots) {
            dot.x += dot.vx * elapsed;
            dot.y += dot.vy * elapsed;
            if (dot.x < 0.09f || dot.x > 0.91f) {
                dot.vx = -dot.vx;
                dot.x = clamp(dot.x, 0.09f, 0.91f);
            }
            if (dot.y < 0.12f || dot.y > 0.88f) {
                dot.vy = -dot.vy;
                dot.y = clamp(dot.y, 0.12f, 0.88f);
            }
        }
        invalidate();
        handler.postDelayed(new Runnable() {
            @Override public void run() { runObjectFrame(token); }
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

    private void drawHome(Canvas canvas) {
        float width = getWidth();
        paint.setColor(NAVY);
        canvas.drawRect(0, 0, width, dp(188), paint);
        text(canvas, "IQOla", dp(22), dp(56), 30, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "Train a little. Think sharper.", dp(22), dp(84), 15, withAlpha(Color.WHITE, 210), Paint.Align.LEFT, false);
        fillRound(canvas, new RectF(dp(22), dp(112), width - dp(22), dp(158)), dp(18), withAlpha(Color.WHITE, 22));
        text(canvas, "CORE PACK", dp(38), dp(141), 12, withAlpha(Color.WHITE, 210), Paint.Align.LEFT, true);
        text(canvas, "4 polished games", width - dp(38), dp(141), 15, Color.WHITE, Paint.Align.RIGHT, true);
        text(canvas, "Pick a game", dp(20), dp(222), 21, INK, Paint.Align.LEFT, true);
        text(canvas, "One shared IQOla style. New games will join this frame.", dp(20), dp(247), 13, MUTED, Paint.Align.LEFT, false);

        float margin = dp(16);
        float gap = dp(12);
        float cardWidth = (width - margin * 2 - gap) / 2f;
        float cardHeight = Math.min(dp(174), (getHeight() - dp(280) - margin) / 2f);
        float firstTop = dp(270);
        GameType[] games = GameType.values();
        for (int i = 0; i < games.length; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = margin + column * (cardWidth + gap);
            float top = firstTop + row * (cardHeight + gap);
            homeCards[i].set(left, top, left + cardWidth, top + cardHeight);
            drawHomeCard(canvas, homeCards[i], games[i]);
        }
    }

    private void drawHomeCard(Canvas canvas, RectF rect, GameType game) {
        fillRound(canvas, rect, dp(22), CARD);
        strokeRound(canvas, rect, dp(22), BORDER, 1);
        float iconSize = Math.min(dp(48), rect.width() * 0.28f);
        fillRound(canvas, new RectF(rect.left + dp(14), rect.top + dp(14), rect.left + dp(14) + iconSize,
                rect.top + dp(14) + iconSize), dp(16), game.pale);
        drawGameIcon(canvas, game, rect.left + dp(14) + iconSize / 2f, rect.top + dp(14) + iconSize / 2f,
                iconSize * 0.52f, game.color);
        text(canvas, game.title, rect.left + dp(14), rect.top + iconSize + dp(38), 16, INK, Paint.Align.LEFT, true);
        drawTwoLineText(canvas, game.subtitle, rect.left + dp(14), rect.top + iconSize + dp(61), rect.width() - dp(28),
                12, MUTED, 16);
        int best = bestFor(game);
        text(canvas, best > 0 ? "Best " + best : "Start training", rect.left + dp(14), rect.bottom - dp(16),
                12, game.color, Paint.Align.LEFT, true);
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
        float width = getWidth();
        backRect.set(dp(14), dp(18), dp(54), dp(58));
        fillRound(canvas, backRect, dp(14), CARD);
        strokeRound(canvas, backRect, dp(14), BORDER, 1);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setColor(INK);
        canvas.drawLine(dp(38), dp(29), dp(28), dp(38), strokePaint);
        canvas.drawLine(dp(28), dp(38), dp(38), dp(47), strokePaint);
        text(canvas, selectedGame.title, dp(66), dp(37), 18, INK, Paint.Align.LEFT, true);
        text(canvas, "Level " + level + "  ·  Round " + (roundsPlayed + 1) + "/" + TOTAL_ROUNDS,
                dp(66), dp(56), 12, MUTED, Paint.Align.LEFT, false);
        fillRound(canvas, new RectF(width - dp(98), dp(20), width - dp(16), dp(56)), dp(15), PALE_BLUE);
        text(canvas, String.valueOf(score), width - dp(57), dp(44), 15, BLUE, Paint.Align.CENTER, true);
    }

    private void drawReverseStroopGame(Canvas canvas) {
        RectF board = getPlayBoard();
        drawBoard(canvas, board, "Tap the word you read — never the ink colour.");
        float duration = Math.max(2700L, 5300L - level * 450L);
        float timerProgress = clamp((stroopEndsAt - SystemClock.elapsedRealtime()) / duration, 0f, 1f);
        RectF timer = new RectF(board.left + dp(20), board.top + dp(58), board.right - dp(20), board.top + dp(65));
        fillRound(canvas, timer, dp(4), PALE_CORAL);
        fillRound(canvas, new RectF(timer.left, timer.top, timer.left + timer.width() * timerProgress, timer.bottom), dp(4), CORAL);
        text(canvas, "READ THE WORD", board.centerX(), board.top + dp(106), 12, MUTED, Paint.Align.CENTER, true);
        text(canvas, COLOR_NAMES[stroopWordIndex], board.centerX(), board.top + dp(158), 35, COLOR_VALUES[stroopInkIndex], Paint.Align.CENTER, true);
        text(canvas, "Choose its meaning", board.centerX(), board.top + dp(185), 13, MUTED, Paint.Align.CENTER, false);
        layoutStroopChoices();
        for (int i = 0; i < stroopChoices.length; i++) {
            RectF choice = stroopChoices[i];
            fillRound(canvas, choice, dp(15), CARD);
            strokeRound(canvas, choice, dp(15), BORDER, 1);
            int colorIndex = stroopChoiceOrder[i];
            fillRound(canvas, new RectF(choice.left + dp(12), choice.centerY() - dp(8), choice.left + dp(28), choice.centerY() + dp(8)),
                    dp(8), COLOR_VALUES[colorIndex]);
            text(canvas, COLOR_NAMES[colorIndex], choice.left + dp(38), choice.centerY() + dp(5), 15, INK, Paint.Align.LEFT, true);
        }
        drawFeedback(canvas, board);
    }

    private void layoutStroopChoices() {
        RectF board = getPlayBoard();
        float margin = dp(20);
        float gap = dp(10);
        float width = (board.width() - margin * 2 - gap) / 2f;
        float height = Math.min(dp(58), (board.bottom - board.top - dp(230)) / 2f - gap);
        float top = board.bottom - height * 2 - gap - dp(22);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = board.left + margin + column * (width + gap);
            float y = top + row * (height + gap);
            stroopChoices[i].set(left, y, left + width, y + height);
        }
    }

    private void drawMemoryMatrixGame(Canvas canvas) {
        RectF board = getPlayBoard();
        drawBoard(canvas, board, matrixShowing ? "Remember every glowing tile." : "Tap the tiles in any order.");
        text(canvas, matrixShowing ? "WATCH" : "REPEAT", board.centerX(), board.top + dp(92), 12,
                matrixShowing ? BLUE : MUTED, Paint.Align.CENTER, true);
        layoutMatrixTiles();
        for (int i = 0; i < matrixGridSize * matrixGridSize; i++) {
            boolean highlighted = matrixShowing && matrixPattern.contains(i);
            boolean chosen = !matrixShowing && matrixChosen.contains(i);
            int fill = highlighted ? BLUE : (chosen ? MINT : CARD);
            fillRound(canvas, matrixTiles[i], dp(12), fill);
            strokeRound(canvas, matrixTiles[i], dp(12), highlighted ? BLUE : BORDER, 1);
        }
        String remaining = matrixShowing ? matrixPattern.size() + " tiles" : (matrixPattern.size() - matrixChosen.size()) + " left";
        text(canvas, remaining, board.centerX(), board.bottom - dp(22), 13, matrixShowing ? BLUE : MUTED, Paint.Align.CENTER, true);
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
        float gridHeight = gridWidth;
        float left = board.centerX() - gridWidth / 2f;
        float top = board.centerY() - gridHeight / 2f + dp(18);
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
        float radius = Math.min(board.width(), board.height()) * 0.23f;
        int color = stopStage == StopStage.GO ? MINT : (stopStage == StopStage.STOP ? CORAL : GOLD);
        int pale = stopStage == StopStage.GO ? PALE_MINT : (stopStage == StopStage.STOP ? PALE_CORAL : PALE_GOLD);
        fillRound(canvas, new RectF(cx - radius - dp(12), cy - radius - dp(12), cx + radius + dp(12), cy + radius + dp(12)),
                radius + dp(18), pale);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
        String label = stopStage == StopStage.READY ? "READY" : (stopStage == StopStage.GO ? "GO" : "STOP");
        text(canvas, label, cx, cy + dp(12), stopStage == StopStage.READY ? 21 : 31, Color.WHITE, Paint.Align.CENTER, true);
        String sub = stopStage == StopStage.READY ? "Wait for a signal" : (stopStage == StopStage.GO ? "Tap now" : "Hold still");
        text(canvas, sub, cx, board.bottom - dp(34), 14, MUTED, Paint.Align.CENTER, false);
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
        strokeRound(canvas, objectTrackRect, dp(18), withAlpha(AQUA, 120), 1);
        drawTrackGrid(canvas, objectTrackRect);
        float radius = objectDotRadius();
        for (int i = 0; i < movingDots.size(); i++) {
            MovingDot dot = movingDots.get(i);
            float cx = objectTrackRect.left + dot.x * objectTrackRect.width();
            float cy = objectTrackRect.top + dot.y * objectTrackRect.height();
            paint.setColor(AQUA);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(withAlpha(Color.WHITE, 200));
            canvas.drawCircle(cx - radius * 0.18f, cy - radius * 0.18f, radius * 0.22f, paint);
            if (objectStage == ObjectStage.STUDY && i == objectTarget) {
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(dp(3));
                strokePaint.setColor(NAVY);
                canvas.drawCircle(cx, cy, radius + dp(6), strokePaint);
            }
        }
        String state = objectStage == ObjectStage.STUDY ? "TRACK THIS DOT" :
                (objectStage == ObjectStage.TRACKING ? "KEEP WATCHING" : "MAKE YOUR PICK");
        text(canvas, state, board.centerX(), board.bottom - dp(22), 12,
                objectStage == ObjectStage.CHOOSE ? AQUA : MUTED, Paint.Align.CENTER, true);
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
        strokePaint.setColor(withAlpha(AQUA, 38));
        for (int i = 1; i < 5; i++) {
            float x = rect.left + rect.width() * i / 5f;
            float y = rect.top + rect.height() * i / 5f;
            canvas.drawLine(x, rect.top + dp(10), x, rect.bottom - dp(10), strokePaint);
            canvas.drawLine(rect.left + dp(10), y, rect.right - dp(10), y, strokePaint);
        }
    }

    private void drawBoard(Canvas canvas, RectF board, String instruction) {
        fillRound(canvas, board, dp(25), CARD);
        strokeRound(canvas, board, dp(25), BORDER, 1);
        text(canvas, instruction, board.centerX(), board.top + dp(30), 13, MUTED, Paint.Align.CENTER, false);
    }

    private void drawFeedback(Canvas canvas, RectF board) {
        if (feedback == null || feedback.length() == 0) return;
        int color = feedback.equals("Correct") || feedback.equals("Pattern complete") || feedback.equals("Good stop")
                || feedback.equals("Target found") ? MINT : INK;
        fillRound(canvas, new RectF(board.left + dp(18), board.bottom - dp(48), board.right - dp(18), board.bottom - dp(16)),
                dp(14), color == MINT ? PALE_MINT : PALE_GOLD);
        text(canvas, feedback, board.centerX(), board.bottom - dp(27), 12, color, Paint.Align.CENTER, true);
    }

    private RectF getPlayBoard() {
        float top = dp(78);
        float footerTop = getHeight() - dp(90);
        return new RectF(dp(16), top, getWidth() - dp(16), Math.max(top + dp(360), footerTop - dp(10)));
    }

    private void drawAdSlot(Canvas canvas) {
        float top = getHeight() - dp(78);
        RectF ad = new RectF(dp(16), top, getWidth() - dp(16), getHeight() - dp(14));
        fillRound(canvas, ad, dp(16), Color.rgb(239, 243, 252));
        strokeRound(canvas, ad, dp(16), BORDER, 1);
        fillRound(canvas, new RectF(ad.left + dp(14), ad.top + dp(13), ad.left + dp(44), ad.bottom - dp(13)), dp(9), PALE_LILAC);
        text(canvas, "AD", ad.left + dp(29), ad.centerY() + dp(4), 10, LILAC, Paint.Align.CENTER, true);
        text(canvas, "Reserved ad space", ad.left + dp(56), ad.centerY() + dp(5), 13, MUTED, Paint.Align.LEFT, false);
        text(canvas, "IQOla", ad.right - dp(16), ad.centerY() + dp(5), 12, BLUE, Paint.Align.RIGHT, true);
    }

    private void drawResult(Canvas canvas) {
        float width = getWidth();
        paint.setColor(NAVY);
        canvas.drawRect(0, 0, width, dp(214), paint);
        text(canvas, "IQOla", dp(22), dp(52), 27, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, selectedGame.title, dp(22), dp(79), 14, withAlpha(Color.WHITE, 200), Paint.Align.LEFT, false);
        float iconCx = width / 2f;
        paint.setColor(GOLD);
        canvas.drawCircle(iconCx, dp(157), dp(37), paint);
        drawStar(canvas, iconCx, dp(157), dp(17), Color.WHITE);
        RectF card = new RectF(dp(16), dp(184), width - dp(16), dp(508));
        fillRound(canvas, card, dp(26), CARD);
        strokeRound(canvas, card, dp(26), BORDER, 1);
        text(canvas, resultTitle, card.centerX(), card.top + dp(58), 25, INK, Paint.Align.CENTER, true);
        text(canvas, correctRounds + " of " + TOTAL_ROUNDS + " rounds completed", card.centerX(), card.top + dp(85), 13, MUTED, Paint.Align.CENTER, false);
        RectF scoreBox = new RectF(card.left + dp(22), card.top + dp(116), card.centerX() - dp(7), card.top + dp(192));
        RectF bestBox = new RectF(card.centerX() + dp(7), card.top + dp(116), card.right - dp(22), card.top + dp(192));
        fillRound(canvas, scoreBox, dp(17), PALE_BLUE);
        fillRound(canvas, bestBox, dp(17), PALE_MINT);
        text(canvas, "SCORE", scoreBox.centerX(), scoreBox.top + dp(24), 11, MUTED, Paint.Align.CENTER, true);
        text(canvas, String.valueOf(score), scoreBox.centerX(), scoreBox.top + dp(55), 24, BLUE, Paint.Align.CENTER, true);
        text(canvas, "BEST", bestBox.centerX(), bestBox.top + dp(24), 11, MUTED, Paint.Align.CENTER, true);
        text(canvas, String.valueOf(resultBest), bestBox.centerX(), bestBox.top + dp(55), 24, MINT, Paint.Align.CENTER, true);
        resultReplayRect.set(card.left + dp(22), card.top + dp(218), card.right - dp(22), card.top + dp(268));
        resultHomeRect.set(card.left + dp(22), card.top + dp(278), card.right - dp(22), card.top + dp(322));
        fillRound(canvas, resultReplayRect, dp(16), BLUE);
        text(canvas, "Play again", resultReplayRect.centerX(), resultReplayRect.centerY() + dp(5), 16, Color.WHITE, Paint.Align.CENTER, true);
        fillRound(canvas, resultHomeRect, dp(16), PALE_BLUE);
        text(canvas, "Back to games", resultHomeRect.centerX(), resultHomeRect.centerY() + dp(5), 15, BLUE, Paint.Align.CENTER, true);
    }

    private void drawGameIcon(Canvas canvas, GameType game, float cx, float cy, float size, int color) {
        paint.setColor(color);
        if (game == GameType.REVERSE_STROOP) {
            text(canvas, "Aa", cx, cy + size * 0.34f, size * 1.25f, color, Paint.Align.CENTER, true);
        } else if (game == GameType.MEMORY_MATRIX) {
            float cell = size * 0.48f;
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    float left = cx + (col - 1) * cell + size * 0.04f;
                    float top = cy + (row - 1) * cell + size * 0.04f;
                    fillRound(canvas, new RectF(left, top, left + cell * 0.74f, top + cell * 0.74f), size * 0.10f,
                            row == 0 && col == 1 ? color : withAlpha(color, 130));
                }
            }
        } else if (game == GameType.STOP_SIGNAL) {
            canvas.drawCircle(cx, cy, size * 0.58f, paint);
            text(canvas, "!", cx, cy + size * 0.34f, size, Color.WHITE, Paint.Align.CENTER, true);
        } else {
            canvas.drawCircle(cx - size * 0.30f, cy, size * 0.20f, paint);
            paint.setColor(withAlpha(color, 180));
            canvas.drawCircle(cx + size * 0.08f, cy - size * 0.20f, size * 0.20f, paint);
            paint.setColor(withAlpha(color, 120));
            canvas.drawCircle(cx + size * 0.30f, cy + size * 0.22f, size * 0.20f, paint);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(2));
            strokePaint.setColor(NAVY);
            canvas.drawCircle(cx - size * 0.30f, cy, size * 0.29f, strokePaint);
        }
    }

    private void drawStar(Canvas canvas, float cx, float cy, float radius, int color) {
        android.graphics.Path path = new android.graphics.Path();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2d + i * Math.PI / 5d;
            float r = i % 2 == 0 ? radius : radius * 0.44f;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private void drawTwoLineText(Canvas canvas, String value, float x, float baseline, float maxWidth,
                                 float sp, int color, float lineHeight) {
        paint.setTypeface(regular);
        paint.setTextSize(dp(sp));
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
        text(canvas, first.toString(), x, baseline, sp, color, Paint.Align.LEFT, false);
        if (second.length() > 0) text(canvas, second.toString(), x, baseline + dp(lineHeight), sp, color, Paint.Align.LEFT, false);
    }

    private void fillRound(Canvas canvas, RectF rect, float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private void strokeRound(Canvas canvas, RectF rect, float radius, int color, float width) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(width));
        strokePaint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    private void text(Canvas canvas, String value, float x, float baseline, float sp, int color,
                      Paint.Align align, boolean isBold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(isBold ? bold : regular);
        paint.setTextSize(dp(sp));
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
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class MovingDot {
        float x;
        float y;
        float vx;
        float vy;
    }
}
