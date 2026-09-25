package com.barndai.iqola;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
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
        if (!iqOlaView.handleBack()) {
            super.onBackPressed();
        }
    }
}

/**
 * A dependency-free, canvas-drawn game surface. Keeping the first release in one view makes the
 * game logic easy to test and lets the AdMob/Billing integrations stay isolated from the games.
 */
class IQOlaView extends View {
    static final int NAVY = Color.rgb(14, 28, 66);
    private static final int INK = Color.rgb(28, 42, 79);
    private static final int MUTED = Color.rgb(104, 116, 145);
    private static final int SURFACE = Color.rgb(248, 249, 255);
    private static final int CARD = Color.WHITE;
    private static final int BLUE = Color.rgb(70, 105, 232);
    private static final int LILAC = Color.rgb(130, 105, 242);
    private static final int AQUA = Color.rgb(55, 192, 200);
    private static final int CORAL = Color.rgb(246, 111, 111);
    private static final int GOLD = Color.rgb(243, 177, 67);
    private static final int MINT = Color.rgb(79, 190, 139);
    private static final int PALE_BLUE = Color.rgb(231, 237, 255);
    private static final int PALE_LILAC = Color.rgb(239, 234, 255);
    private static final int PALE_MINT = Color.rgb(225, 247, 239);
    private static final int PALE_GOLD = Color.rgb(255, 244, 218);
    private static final int PALE_CORAL = Color.rgb(255, 232, 232);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;

    private final RectF[] homeCards = new RectF[12];
    private final RectF[] memoryTiles = new RectF[9];
    private final RectF[] mathChoiceRects = new RectF[4];
    private final RectF[] digitKeyRects = new RectF[12];
    private final RectF[] arrowPadRects = new RectF[4];
    private final RectF[] orderTiles = new RectF[4];
    private final RectF[] oddTiles = new RectF[16];
    private final RectF[] shapeOptions = new RectF[4];
    private final RectF backRect = new RectF();
    private final RectF restartRect = new RectF();
    private final RectF removeAdsRect = new RectF();
    private final RectF resultReplayRect = new RectF();
    private final RectF resultHomeRect = new RectF();
    private final RectF connectArea = new RectF();

    private enum Screen { HOME, GAME, RESULT }

    private enum GameType {
        MEMORY("Pattern Memory", "Remember the glowing tiles", BLUE, PALE_BLUE),
        MATH("Quick Math", "Solve before the timer ends", LILAC, PALE_LILAC),
        DIGITS("Digit Recall", "Hold a number in your mind", GOLD, PALE_GOLD),
        ARROWS("Arrow Path", "Repeat the hidden route", AQUA, PALE_MINT),
        CONNECT("Connect Lines", "Match colors without crossing", CORAL, PALE_CORAL),
        WORD_MATCH("Word Match", "Find the closest meaning", LILAC, PALE_LILAC),
        COLOR_FOCUS("Color Focus", "Ignore the written word", CORAL, PALE_CORAL),
        REACTION("Reaction Tap", "Wait, then tap fast", AQUA, PALE_MINT),
        NUMBER_ORDER("Number Order", "Tap numbers in sequence", BLUE, PALE_BLUE),
        ODD_ONE_OUT("Odd One Out", "Spot the different tile", GOLD, PALE_GOLD),
        SEQUENCE_LOGIC("Sequence Logic", "Find what comes next", MINT, PALE_MINT),
        SHAPE_ROTATE("Shape Rotate", "Think in a different direction", LILAC, PALE_LILAC);

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

    private Screen screen = Screen.HOME;
    private GameType selectedGame = GameType.MEMORY;
    private int score;
    private int level = 1;
    private String feedback = "";
    private String resultTitle = "Nice work";
    private int resultBest;
    private float homeScrollOffset;
    private float homeTouchStartY;
    private float homeTouchLastY;
    private boolean homeDragging;

    // Pattern Memory state.
    private final ArrayList<Integer> memorySequence = new ArrayList<>();
    private boolean memoryShowing;
    private int memoryFlashIndex = -1;
    private int memoryInputIndex;

    // Quick Math state.
    private final ArrayList<Integer> mathChoices = new ArrayList<>();
    private String mathQuestion = "";
    private int mathCorrect;
    private boolean mathLocked;
    private long mathEndsAt;

    // Digit Recall state.
    private String digitValue = "";
    private String digitInput = "";
    private int digitLength = 3;
    private boolean digitShowing;

    // Arrow Path state.
    private final ArrayList<Integer> arrowSequence = new ArrayList<>();
    private boolean arrowShowing;
    private int arrowFlashPosition = -1;
    private int arrowInputIndex;

    // Connect Lines state.
    private final ArrayList<ConnectNode> connectNodes = new ArrayList<>();
    private final ArrayList<ConnectLine> connectLines = new ArrayList<>();
    private int connectStage;
    private int activeNode = -1;
    private float dragX;
    private float dragY;

    // Word Match state.
    private final ArrayList<String> wordChoices = new ArrayList<>();
    private String wordPrompt = "";
    private int wordCorrectIndex;
    private boolean wordLocked;
    private long wordEndsAt;

    // Color Focus state.
    private String colorWord = "";
    private int colorInkIndex;
    private boolean colorLocked;
    private long colorEndsAt;

    // Reaction Tap state.
    private final RectF reactionTarget = new RectF();
    private boolean reactionReady;
    private boolean reactionWaiting;
    private int reactionRound;
    private long reactionStartedAt;

    // Number Order state.
    private final int[] orderValues = new int[4];
    private final boolean[] orderUsed = new boolean[4];
    private int orderExpected;

    // Odd One Out state.
    private int oddTarget;

    // Sequence Logic state.
    private final ArrayList<Integer> sequenceChoices = new ArrayList<>();
    private String sequencePrompt = "";
    private int sequenceCorrectIndex;
    private boolean sequenceLocked;
    private long sequenceEndsAt;

    // Shape Rotate state.
    private int shapeTargetDirection;
    private final int[] shapeOptionDirections = new int[4];
    private int shapeCorrectIndex;

    private static final String[][] WORD_BANK = {
            {"BRISK", "Fast", "Heavy", "Late", "Quiet"},
            {"ANCIENT", "Very old", "Very loud", "Very small", "Very wet"},
            {"GENEROUS", "Willing to give", "Easy to anger", "Hard to see", "Quick to forget"},
            {"FRAGILE", "Easy to break", "Very bright", "Full of food", "Hard to move"},
            {"PRECISE", "Exact", "Playful", "Noisy", "Sleepy"},
            {"CALM", "Peaceful", "Crowded", "Expensive", "Broken"},
            {"ANXIOUS", "Worried", "Hungry", "Certain", "Bored"},
            {"OBVIOUS", "Easy to see", "Difficult to carry", "Safe to eat", "Hard to hear"}
    };
    private static final String[] COLOR_NAMES = {"RED", "BLUE", "GOLD", "MINT"};
    private static final int[] COLOR_VALUES = {CORAL, BLUE, GOLD, MINT};

    IQOlaView(Context context) {
        super(context);
        preferences = context.getSharedPreferences("iqola_progress", Context.MODE_PRIVATE);
        setBackgroundColor(SURFACE);
        setFocusable(true);
        for (int i = 0; i < homeCards.length; i++) homeCards[i] = new RectF();
        for (int i = 0; i < memoryTiles.length; i++) memoryTiles[i] = new RectF();
        for (int i = 0; i < mathChoiceRects.length; i++) mathChoiceRects[i] = new RectF();
        for (int i = 0; i < digitKeyRects.length; i++) digitKeyRects[i] = new RectF();
        for (int i = 0; i < arrowPadRects.length; i++) arrowPadRects[i] = new RectF();
        for (int i = 0; i < orderTiles.length; i++) orderTiles[i] = new RectF();
        for (int i = 0; i < oddTiles.length; i++) oddTiles[i] = new RectF();
        for (int i = 0; i < shapeOptions.length; i++) shapeOptions[i] = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(SURFACE);
        if (screen == Screen.HOME) {
            drawHome(canvas);
        } else if (screen == Screen.GAME) {
            drawGame(canvas);
        } else {
            drawResult(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        if (screen == Screen.HOME) {
            if (action == MotionEvent.ACTION_DOWN) {
                homeTouchStartY = y;
                homeTouchLastY = y;
                homeDragging = false;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float delta = y - homeTouchLastY;
                if (Math.abs(y - homeTouchStartY) > dp(6)) homeDragging = true;
                homeScrollOffset = clampHomeScroll(homeScrollOffset - delta);
                homeTouchLastY = y;
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                performClick();
                if (!homeDragging) {
                    for (int i = 0; i < homeCards.length; i++) {
                        if (homeCards[i].contains(x, y)) {
                            openGame(GameType.values()[i]);
                            return true;
                        }
                    }
                }
            }
            return true;
        }

        if (screen == Screen.RESULT) {
            if (action == MotionEvent.ACTION_UP) {
                performClick();
                if (resultReplayRect.contains(x, y)) {
                    openGame(selectedGame);
                } else if (resultHomeRect.contains(x, y)) {
                    goHome();
                }
            }
            return true;
        }

        // Every game shares these controls.
        if (action == MotionEvent.ACTION_UP) {
            performClick();
            if (backRect.contains(x, y)) {
                goHome();
                return true;
            }
            if (restartRect.contains(x, y)) {
                openGame(selectedGame);
                return true;
            }
            if (removeAdsRect.contains(x, y)) {
                showAdsPreview();
                return true;
            }
        }

        if (selectedGame == GameType.CONNECT) {
            return handleConnectTouch(event);
        }

        if (action != MotionEvent.ACTION_UP) return true;
        if (selectedGame == GameType.MEMORY) {
            handleMemoryTap(x, y);
        } else if (selectedGame == GameType.MATH) {
            handleMathTap(x, y);
        } else if (selectedGame == GameType.DIGITS) {
            handleDigitTap(x, y);
        } else if (selectedGame == GameType.ARROWS) {
            handleArrowTap(x, y);
        } else if (selectedGame == GameType.WORD_MATCH) {
            handleWordMatchTap(x, y);
        } else if (selectedGame == GameType.COLOR_FOCUS) {
            handleColorFocusTap(x, y);
        } else if (selectedGame == GameType.REACTION) {
            handleReactionTap(x, y);
        } else if (selectedGame == GameType.NUMBER_ORDER) {
            handleNumberOrderTap(x, y);
        } else if (selectedGame == GameType.ODD_ONE_OUT) {
            handleOddOneOutTap(x, y);
        } else if (selectedGame == GameType.SEQUENCE_LOGIC) {
            handleSequenceLogicTap(x, y);
        } else if (selectedGame == GameType.SHAPE_ROTATE) {
            handleShapeRotateTap(x, y);
        }
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
        screen = Screen.GAME;
        score = 0;
        level = 1;
        feedback = "";
        preferences.edit().putInt("sessions", preferences.getInt("sessions", 0) + 1).apply();

        if (game == GameType.MEMORY) {
            startMemoryGame();
        } else if (game == GameType.MATH) {
            startMathGame();
        } else if (game == GameType.DIGITS) {
            startDigitGame();
        } else if (game == GameType.ARROWS) {
            startArrowGame();
        } else if (game == GameType.CONNECT) {
            startConnectGame();
        } else if (game == GameType.WORD_MATCH) {
            startWordMatchGame();
        } else if (game == GameType.COLOR_FOCUS) {
            startColorFocusGame();
        } else if (game == GameType.REACTION) {
            startReactionGame();
        } else if (game == GameType.NUMBER_ORDER) {
            startNumberOrderGame();
        } else if (game == GameType.ODD_ONE_OUT) {
            startOddOneOutGame();
        } else if (game == GameType.SEQUENCE_LOGIC) {
            startSequenceLogicGame();
        } else {
            startShapeRotateGame();
        }
        invalidate();
    }

    private void goHome() {
        if (screen == Screen.GAME) recordBest();
        clearScheduledWork();
        screen = Screen.HOME;
        invalidate();
    }

    private void endGame(String title) {
        clearScheduledWork();
        resultTitle = title;
        resultBest = recordBest();
        screen = Screen.RESULT;
        invalidate();
    }

    private void clearScheduledWork() {
        handler.removeCallbacksAndMessages(null);
    }

    private boolean isPlaying(GameType game) {
        return screen == Screen.GAME && selectedGame == game;
    }

    private int recordBest() {
        String key = bestKey(selectedGame);
        int best = Math.max(preferences.getInt(key, 0), score);
        preferences.edit().putInt(key, best).apply();
        return best;
    }

    private int bestFor(GameType game) {
        return preferences.getInt(bestKey(game), 0);
    }

    private String bestKey(GameType game) {
        return "best_" + game.name().toLowerCase();
    }

    private int combinedBest() {
        int total = 0;
        for (GameType game : GameType.values()) total += bestFor(game);
        return total;
    }

    private void startMemoryGame() {
        memorySequence.clear();
        memorySequence.add(random.nextInt(9));
        memorySequence.add(random.nextInt(9));
        beginMemoryRound();
    }

    private void beginMemoryRound() {
        memoryInputIndex = 0;
        memoryShowing = true;
        memoryFlashIndex = -1;
        feedback = "Watch the tiles";
        showMemoryStep(0);
    }

    private void showMemoryStep(final int position) {
        if (!isPlaying(GameType.MEMORY)) return;
        if (position >= memorySequence.size()) {
            handler.postDelayed(() -> {
                if (!isPlaying(GameType.MEMORY)) return;
                memoryFlashIndex = -1;
                memoryShowing = false;
                feedback = "Repeat the pattern";
                invalidate();
            }, 280);
            return;
        }
        handler.postDelayed(() -> {
            if (!isPlaying(GameType.MEMORY)) return;
            memoryFlashIndex = memorySequence.get(position);
            invalidate();
            handler.postDelayed(() -> {
                if (!isPlaying(GameType.MEMORY)) return;
                memoryFlashIndex = -1;
                invalidate();
                showMemoryStep(position + 1);
            }, 520);
        }, position == 0 ? 420 : 150);
    }

    private void handleMemoryTap(float x, float y) {
        if (memoryShowing) return;
        layoutMemoryTiles();
        for (int i = 0; i < memoryTiles.length; i++) {
            if (!memoryTiles[i].contains(x, y)) continue;
            if (memorySequence.get(memoryInputIndex) == i) {
                memoryInputIndex++;
                if (memoryInputIndex == memorySequence.size()) {
                    score += memorySequence.size() * 10;
                    level++;
                    memorySequence.add(random.nextInt(9));
                    feedback = "Perfect. One more tile.";
                    memoryShowing = true;
                    handler.postDelayed(this::beginMemoryRound, 500);
                } else {
                    feedback = "Keep going";
                }
            } else {
                endGame("Pattern slipped away");
            }
            invalidate();
            return;
        }
    }

    private void startMathGame() {
        mathLocked = false;
        mathEndsAt = SystemClock.elapsedRealtime() + 30_000L;
        makeMathQuestion();
        tickMathTimer();
    }

    private void tickMathTimer() {
        if (!isPlaying(GameType.MATH)) return;
        if (SystemClock.elapsedRealtime() >= mathEndsAt) {
            endGame("Time is up");
            return;
        }
        invalidate();
        handler.postDelayed(this::tickMathTimer, 160);
    }

    private void makeMathQuestion() {
        int difficulty = Math.min(4, score / 4);
        int operation = random.nextInt(difficulty >= 2 ? 3 : 2);
        int first;
        int second;
        if (operation == 0) {
            first = 4 + random.nextInt(10 + difficulty * 9);
            second = 2 + random.nextInt(8 + difficulty * 7);
            mathCorrect = first + second;
            mathQuestion = first + " + " + second;
        } else if (operation == 1) {
            first = 10 + random.nextInt(15 + difficulty * 12);
            second = 2 + random.nextInt(Math.max(3, first - 1));
            if (second >= first) second = first - 1;
            mathCorrect = first - second;
            mathQuestion = first + " − " + second;
        } else {
            first = 2 + random.nextInt(4 + difficulty);
            second = 2 + random.nextInt(5 + difficulty);
            mathCorrect = first * second;
            mathQuestion = first + " × " + second;
        }
        Set<Integer> values = new LinkedHashSet<>();
        values.add(mathCorrect);
        while (values.size() < 4) {
            int offset = 1 + random.nextInt(8 + difficulty * 4);
            if (random.nextBoolean()) offset = -offset;
            values.add(mathCorrect + offset);
        }
        mathChoices.clear();
        mathChoices.addAll(values);
        Collections.shuffle(mathChoices, random);
        feedback = "Choose the answer";
    }

    private void handleMathTap(float x, float y) {
        if (mathLocked) return;
        layoutMathChoices();
        for (int i = 0; i < mathChoiceRects.length; i++) {
            if (!mathChoiceRects[i].contains(x, y)) continue;
            if (mathChoices.get(i) == mathCorrect) {
                score++;
                level = 1 + score / 4;
                feedback = "Correct";
                makeMathQuestion();
            } else {
                mathLocked = true;
                feedback = "It was " + mathCorrect;
                handler.postDelayed(() -> {
                    if (!isPlaying(GameType.MATH)) return;
                    mathLocked = false;
                    makeMathQuestion();
                    invalidate();
                }, 420);
            }
            invalidate();
            return;
        }
    }

    private void startDigitGame() {
        digitLength = 3;
        beginDigitRound();
    }

    private void beginDigitRound() {
        digitValue = makeDigits(digitLength);
        digitInput = "";
        digitShowing = true;
        feedback = "Memorize the number";
        int showFor = Math.max(1300, 2500 - (digitLength - 3) * 150);
        handler.postDelayed(() -> {
            if (!isPlaying(GameType.DIGITS)) return;
            digitShowing = false;
            feedback = "Type what you saw";
            invalidate();
        }, showFor);
    }

    private String makeDigits(int length) {
        StringBuilder builder = new StringBuilder();
        builder.append(1 + random.nextInt(9));
        for (int i = 1; i < length; i++) builder.append(random.nextInt(10));
        return builder.toString();
    }

    private void handleDigitTap(float x, float y) {
        if (digitShowing) return;
        layoutDigitKeys();
        for (int i = 0; i < digitKeyRects.length; i++) {
            if (!digitKeyRects[i].contains(x, y)) continue;
            if (i == 9) {
                if (!digitInput.isEmpty()) digitInput = digitInput.substring(0, digitInput.length() - 1);
            } else if (i == 11) {
                digitInput = "";
            } else if (digitInput.length() < digitValue.length()) {
                String number = i == 10 ? "0" : String.valueOf(i + 1);
                digitInput += number;
            }
            if (digitInput.length() == digitValue.length()) {
                handler.postDelayed(this::verifyDigits, 240);
            }
            invalidate();
            return;
        }
    }

    private void verifyDigits() {
        if (!isPlaying(GameType.DIGITS) || digitShowing) return;
        if (digitInput.equals(digitValue)) {
            score += digitLength * 10;
            level++;
            digitLength++;
            feedback = "Sharp memory";
            handler.postDelayed(this::beginDigitRound, 580);
            return;
        }
        endGame("The number was " + digitValue);
    }

    private void startArrowGame() {
        arrowSequence.clear();
        for (int i = 0; i < 3; i++) arrowSequence.add(random.nextInt(4));
        beginArrowRound();
    }

    private void beginArrowRound() {
        arrowShowing = true;
        arrowFlashPosition = -1;
        arrowInputIndex = 0;
        feedback = "Watch the route";
        showArrowStep(0);
    }

    private void showArrowStep(final int position) {
        if (!isPlaying(GameType.ARROWS)) return;
        if (position >= arrowSequence.size()) {
            handler.postDelayed(() -> {
                if (!isPlaying(GameType.ARROWS)) return;
                arrowFlashPosition = -1;
                arrowShowing = false;
                feedback = "Repeat the route";
                invalidate();
            }, 260);
            return;
        }
        handler.postDelayed(() -> {
            if (!isPlaying(GameType.ARROWS)) return;
            arrowFlashPosition = position;
            invalidate();
            handler.postDelayed(() -> {
                if (!isPlaying(GameType.ARROWS)) return;
                arrowFlashPosition = -1;
                invalidate();
                showArrowStep(position + 1);
            }, 530);
        }, position == 0 ? 400 : 160);
    }

    private void handleArrowTap(float x, float y) {
        if (arrowShowing) return;
        layoutArrowPad();
        for (int direction = 0; direction < arrowPadRects.length; direction++) {
            if (!arrowPadRects[direction].contains(x, y)) continue;
            if (arrowSequence.get(arrowInputIndex) == direction) {
                arrowInputIndex++;
                if (arrowInputIndex == arrowSequence.size()) {
                    score += arrowSequence.size() * 10;
                    level++;
                    arrowSequence.add(random.nextInt(4));
                    feedback = "Route complete";
                    arrowShowing = true;
                    handler.postDelayed(this::beginArrowRound, 520);
                } else {
                    feedback = "Keep moving";
                }
            } else {
                endGame("Wrong turn");
            }
            invalidate();
            return;
        }
    }

    private void startConnectGame() {
        connectStage = 0;
        makeConnectBoard();
        feedback = "Join matching colors";
    }

    private void makeConnectBoard() {
        connectNodes.clear();
        connectLines.clear();
        activeNode = -1;
        int stage = connectStage % 3;
        if (stage == 0) {
            addNodes(0.16f, 0.22f, CORAL, 0.42f, 0.36f, CORAL);
            addNodes(0.74f, 0.24f, BLUE, 0.82f, 0.51f, BLUE);
            addNodes(0.22f, 0.73f, MINT, 0.62f, 0.82f, MINT);
        } else if (stage == 1) {
            addNodes(0.18f, 0.20f, GOLD, 0.78f, 0.27f, GOLD);
            addNodes(0.18f, 0.48f, BLUE, 0.44f, 0.69f, BLUE);
            addNodes(0.75f, 0.53f, CORAL, 0.83f, 0.77f, CORAL);
            addNodes(0.24f, 0.86f, MINT, 0.61f, 0.85f, MINT);
        } else {
            addNodes(0.18f, 0.18f, LILAC, 0.46f, 0.30f, LILAC);
            addNodes(0.71f, 0.19f, CORAL, 0.82f, 0.42f, CORAL);
            addNodes(0.22f, 0.58f, GOLD, 0.47f, 0.78f, GOLD);
            addNodes(0.66f, 0.66f, AQUA, 0.82f, 0.84f, AQUA);
        }
    }

    private void addNodes(float x1, float y1, int color, float x2, float y2, int color2) {
        connectNodes.add(new ConnectNode(x1, y1, color));
        connectNodes.add(new ConnectNode(x2, y2, color2));
    }

    private boolean handleConnectTouch(MotionEvent event) {
        layoutConnectArea();
        float x = event.getX();
        float y = event.getY();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            int node = findConnectNode(x, y);
            if (node >= 0 && !nodeIsConnected(node)) {
                activeNode = node;
                dragX = x;
                dragY = y;
            }
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (activeNode >= 0) {
                dragX = x;
                dragY = y;
                invalidate();
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (activeNode >= 0) {
                int target = findConnectNode(x, y);
                finishConnectDrag(target);
                activeNode = -1;
                invalidate();
            }
            return true;
        }
        return true;
    }

    private void finishConnectDrag(int target) {
        if (target < 0 || target == activeNode) {
            feedback = "Drag to the matching dot";
            return;
        }
        ConnectNode start = connectNodes.get(activeNode);
        ConnectNode end = connectNodes.get(target);
        if (start.color != end.color || nodeIsConnected(target)) {
            feedback = "Connect matching colors";
            return;
        }
        if (wouldCrossExistingLine(activeNode, target)) {
            feedback = "Those paths would cross";
            return;
        }
        connectLines.add(new ConnectLine(activeNode, target));
        score += 10;
        feedback = "Good connection";
        if (connectLines.size() == connectNodes.size() / 2) {
            score += 20;
            level++;
            feedback = "Board clear! +20 bonus";
            handler.postDelayed(() -> {
                if (!isPlaying(GameType.CONNECT)) return;
                connectStage++;
                makeConnectBoard();
                feedback = "New board";
                invalidate();
            }, 650);
        }
    }

    private boolean nodeIsConnected(int node) {
        for (ConnectLine line : connectLines) {
            if (line.start == node || line.end == node) return true;
        }
        return false;
    }

    private int findConnectNode(float x, float y) {
        float radius = dp(23);
        for (int i = 0; i < connectNodes.size(); i++) {
            PointF point = connectPoint(i);
            float dx = point.x - x;
            float dy = point.y - y;
            if (dx * dx + dy * dy <= radius * radius) return i;
        }
        return -1;
    }

    private boolean wouldCrossExistingLine(int from, int to) {
        PointF a = connectPoint(from);
        PointF b = connectPoint(to);
        for (ConnectLine line : connectLines) {
            PointF c = connectPoint(line.start);
            PointF d = connectPoint(line.end);
            if (segmentsCross(a, b, c, d)) return true;
        }
        return false;
    }

    private boolean segmentsCross(PointF a, PointF b, PointF c, PointF d) {
        float abC = cross(a, b, c);
        float abD = cross(a, b, d);
        float cdA = cross(c, d, a);
        float cdB = cross(c, d, b);
        return ((abC > 0f && abD < 0f) || (abC < 0f && abD > 0f))
                && ((cdA > 0f && cdB < 0f) || (cdA < 0f && cdB > 0f));
    }

    private float cross(PointF a, PointF b, PointF c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private void startWordMatchGame() {
        wordLocked = false;
        wordEndsAt = SystemClock.elapsedRealtime() + 30_000L;
        makeWordQuestion();
        tickWordTimer();
    }

    private void makeWordQuestion() {
        String[] row = WORD_BANK[random.nextInt(WORD_BANK.length)];
        wordPrompt = row[0];
        wordChoices.clear();
        for (int i = 1; i < row.length; i++) wordChoices.add(row[i]);
        Collections.shuffle(wordChoices, random);
        wordCorrectIndex = wordChoices.indexOf(row[1]);
        feedback = "Choose the closest meaning";
    }

    private void tickWordTimer() {
        if (!isPlaying(GameType.WORD_MATCH)) return;
        if (SystemClock.elapsedRealtime() >= wordEndsAt) {
            endGame("Time is up");
            return;
        }
        invalidate();
        handler.postDelayed(this::tickWordTimer, 180);
    }

    private void handleWordMatchTap(float x, float y) {
        if (wordLocked) return;
        layoutGenericChoices(dp(357));
        for (int i = 0; i < mathChoiceRects.length; i++) {
            if (!mathChoiceRects[i].contains(x, y)) continue;
            if (i == wordCorrectIndex) {
                score++;
                level = 1 + score / 4;
                makeWordQuestion();
            } else {
                wordLocked = true;
                feedback = "The closest meaning was " + wordChoices.get(wordCorrectIndex);
                handler.postDelayed(() -> {
                    if (!isPlaying(GameType.WORD_MATCH)) return;
                    wordLocked = false;
                    makeWordQuestion();
                    invalidate();
                }, 450);
            }
            invalidate();
            return;
        }
    }

    private void startColorFocusGame() {
        colorLocked = false;
        colorEndsAt = SystemClock.elapsedRealtime() + 30_000L;
        makeColorQuestion();
        tickColorTimer();
    }

    private void makeColorQuestion() {
        int wordIndex = random.nextInt(COLOR_NAMES.length);
        colorInkIndex = random.nextInt(COLOR_NAMES.length);
        while (colorInkIndex == wordIndex) colorInkIndex = random.nextInt(COLOR_NAMES.length);
        colorWord = COLOR_NAMES[wordIndex];
        feedback = "Tap the ink color, not the word";
    }

    private void tickColorTimer() {
        if (!isPlaying(GameType.COLOR_FOCUS)) return;
        if (SystemClock.elapsedRealtime() >= colorEndsAt) {
            endGame("Time is up");
            return;
        }
        invalidate();
        handler.postDelayed(this::tickColorTimer, 180);
    }

    private void handleColorFocusTap(float x, float y) {
        if (colorLocked) return;
        layoutGenericChoices(dp(357));
        for (int i = 0; i < mathChoiceRects.length; i++) {
            if (!mathChoiceRects[i].contains(x, y)) continue;
            if (i == colorInkIndex) {
                score++;
                level = 1 + score / 4;
                makeColorQuestion();
            } else {
                colorLocked = true;
                feedback = "Follow the ink color";
                handler.postDelayed(() -> {
                    if (!isPlaying(GameType.COLOR_FOCUS)) return;
                    colorLocked = false;
                    makeColorQuestion();
                    invalidate();
                }, 380);
            }
            invalidate();
            return;
        }
    }

    private void startReactionGame() {
        reactionRound = 0;
        prepareReactionRound();
    }

    private void prepareReactionRound() {
        reactionReady = false;
        reactionWaiting = true;
        feedback = "Wait for GO";
        handler.postDelayed(() -> {
            if (!isPlaying(GameType.REACTION)) return;
            reactionReady = true;
            reactionStartedAt = SystemClock.elapsedRealtime();
            feedback = "TAP NOW";
            invalidate();
        }, 700 + random.nextInt(1300));
    }

    private void handleReactionTap(float x, float y) {
        layoutReactionTarget();
        if (!reactionTarget.contains(x, y)) return;
        if (!reactionReady) {
            endGame("Too early");
            return;
        }
        long reactionTime = SystemClock.elapsedRealtime() - reactionStartedAt;
        score += Math.max(5, 120 - (int) reactionTime / 4);
        reactionRound++;
        reactionReady = false;
        reactionWaiting = true;
        if (reactionRound >= 5) {
            level++;
            reactionRound = 0;
            feedback = "Level up";
        } else {
            feedback = reactionTime + " ms";
        }
        handler.postDelayed(this::prepareReactionRound, 500);
        invalidate();
    }

    private void startNumberOrderGame() {
        makeNumberOrderRound();
    }

    private void makeNumberOrderRound() {
        ArrayList<Integer> values = new ArrayList<>();
        int base = 10 + random.nextInt(40);
        int step = 2 + random.nextInt(9);
        for (int i = 0; i < 4; i++) values.add(base + i * step);
        ArrayList<Integer> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < 4; i++) {
            orderValues[i] = shuffled.get(i);
            orderUsed[i] = false;
        }
        orderExpected = values.get(0);
        feedback = "Tap the numbers from low to high";
    }

    private void handleNumberOrderTap(float x, float y) {
        layoutOrderTiles();
        for (int i = 0; i < orderTiles.length; i++) {
            if (!orderTiles[i].contains(x, y) || orderUsed[i]) continue;
            if (orderValues[i] != orderExpected) {
                endGame("Wrong order");
                return;
            }
            orderUsed[i] = true;
            int next = Integer.MAX_VALUE;
            for (int j = 0; j < orderValues.length; j++) {
                if (!orderUsed[j]) next = Math.min(next, orderValues[j]);
            }
            if (next == Integer.MAX_VALUE) {
                score += 20;
                level++;
                feedback = "Order complete";
                handler.postDelayed(this::makeNumberOrderRound, 480);
            } else {
                orderExpected = next;
                score += 3;
                feedback = "Good. Find " + orderExpected;
            }
            invalidate();
            return;
        }
    }

    private void startOddOneOutGame() {
        makeOddRound();
    }

    private void makeOddRound() {
        oddTarget = random.nextInt(16);
        feedback = "Find the one different tile";
    }

    private void handleOddOneOutTap(float x, float y) {
        layoutOddTiles();
        for (int i = 0; i < oddTiles.length; i++) {
            if (!oddTiles[i].contains(x, y)) continue;
            if (i != oddTarget) {
                endGame("Not that tile");
                return;
            }
            score += 10;
            level = 1 + score / 40;
            feedback = "Sharp eyes";
            makeOddRound();
            invalidate();
            return;
        }
    }

    private void startSequenceLogicGame() {
        sequenceLocked = false;
        sequenceEndsAt = SystemClock.elapsedRealtime() + 30_000L;
        makeSequenceQuestion();
        tickSequenceTimer();
    }

    private void makeSequenceQuestion() {
        int type = random.nextInt(3);
        int first;
        int second;
        int third;
        int correct;
        if (type == 0) {
            first = 2 + random.nextInt(8);
            second = 2 + random.nextInt(6);
            third = first + second;
            correct = third + second;
            sequencePrompt = first + ", " + third + ", " + correct + "?";
        } else if (type == 1) {
            first = 1 + random.nextInt(4);
            second = first * 2;
            third = second * 2;
            correct = third * 2;
            sequencePrompt = first + ", " + second + ", " + third + ", ?";
        } else {
            first = 1 + random.nextInt(4);
            second = first * first;
            third = (first + 1) * (first + 1);
            correct = (first + 2) * (first + 2);
            sequencePrompt = second + ", " + third + ", " + correct + "?";
        }
        sequenceChoices.clear();
        sequenceChoices.add(correct);
        while (sequenceChoices.size() < 4) {
            int candidate = Math.max(1, correct + (random.nextBoolean() ? 1 : -1) * (1 + random.nextInt(9)));
            if (!sequenceChoices.contains(candidate)) sequenceChoices.add(candidate);
        }
        Collections.shuffle(sequenceChoices, random);
        sequenceCorrectIndex = sequenceChoices.indexOf(correct);
        feedback = "Find the missing number";
    }

    private void tickSequenceTimer() {
        if (!isPlaying(GameType.SEQUENCE_LOGIC)) return;
        if (SystemClock.elapsedRealtime() >= sequenceEndsAt) {
            endGame("Time is up");
            return;
        }
        invalidate();
        handler.postDelayed(this::tickSequenceTimer, 180);
    }

    private void handleSequenceLogicTap(float x, float y) {
        if (sequenceLocked) return;
        layoutGenericChoices(dp(357));
        for (int i = 0; i < mathChoiceRects.length; i++) {
            if (!mathChoiceRects[i].contains(x, y)) continue;
            if (i == sequenceCorrectIndex) {
                score++;
                level = 1 + score / 4;
                makeSequenceQuestion();
            } else {
                sequenceLocked = true;
                feedback = "Try the pattern again";
                handler.postDelayed(() -> {
                    if (!isPlaying(GameType.SEQUENCE_LOGIC)) return;
                    sequenceLocked = false;
                    makeSequenceQuestion();
                    invalidate();
                }, 400);
            }
            invalidate();
            return;
        }
    }

    private void startShapeRotateGame() {
        makeShapeRound();
    }

    private void makeShapeRound() {
        shapeTargetDirection = random.nextInt(4);
        ArrayList<Integer> directions = new ArrayList<>();
        directions.add(0);
        directions.add(1);
        directions.add(2);
        directions.add(3);
        Collections.shuffle(directions, random);
        for (int i = 0; i < 4; i++) shapeOptionDirections[i] = directions.get(i);
        shapeCorrectIndex = directions.indexOf(shapeTargetDirection);
        feedback = "Tap the matching rotation";
    }

    private void handleShapeRotateTap(float x, float y) {
        layoutShapeOptions();
        for (int i = 0; i < shapeOptions.length; i++) {
            if (!shapeOptions[i].contains(x, y)) continue;
            if (i == shapeCorrectIndex) {
                score += 10;
                level = 1 + score / 40;
                makeShapeRound();
            } else {
                endGame("Wrong rotation");
            }
            invalidate();
            return;
        }
    }

    private int secondsRemaining(long endsAt) {
        return Math.max(0, (int) Math.ceil(Math.max(0L, endsAt - SystemClock.elapsedRealtime()) / 1000f));
    }

    private void drawChallengeTimer(Canvas canvas, long endsAt, int color) {
        int seconds = secondsRemaining(endsAt);
        RectF timer = new RectF(getWidth() / 2f - dp(31), dp(184), getWidth() / 2f + dp(31), dp(218));
        fillRound(canvas, timer, dp(17), seconds <= 8 ? PALE_CORAL : PALE_BLUE);
        text(canvas, seconds + "s", timer.centerX(), dp(207), 14, seconds <= 8 ? CORAL : color, Paint.Align.CENTER, true);
    }

    private void layoutGenericChoices(float top) {
        float margin = dp(24);
        float gap = dp(12);
        float width = (getWidth() - margin * 2f - gap) / 2f;
        float available = gameFooterTop() - dp(16) - top;
        float height = Math.max(dp(58), (available - gap) / 2f);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = margin + column * (width + gap);
            float y = top + row * (height + gap);
            mathChoiceRects[i].set(left, y, left + width, y + height);
        }
    }

    private void layoutReactionTarget() {
        reactionTarget.set(dp(28), dp(220), getWidth() - dp(28), gameFooterTop() - dp(28));
    }

    private void layoutOrderTiles() {
        float margin = dp(26);
        float gap = dp(14);
        float width = (getWidth() - margin * 2f - gap) / 2f;
        float height = Math.min(dp(124), (gameFooterTop() - dp(236)) / 2f);
        float top = dp(238);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = margin + column * (width + gap);
            float y = top + row * (height + gap);
            orderTiles[i].set(left, y, left + width, y + height);
        }
    }

    private void layoutOddTiles() {
        float margin = dp(28);
        float gap = dp(8);
        float total = Math.min(getWidth() - margin * 2f, Math.max(dp(220), gameFooterTop() - dp(220)));
        float tile = (total - gap * 3f) / 4f;
        float left = (getWidth() - total) / 2f;
        float top = dp(206);
        for (int i = 0; i < 16; i++) {
            int row = i / 4;
            int column = i % 4;
            float x = left + column * (tile + gap);
            float y = top + row * (tile + gap);
            oddTiles[i].set(x, y, x + tile, y + tile);
        }
    }

    private void layoutShapeOptions() {
        float top = dp(363);
        layoutGenericChoices(top);
        for (int i = 0; i < shapeOptions.length; i++) shapeOptions[i].set(mathChoiceRects[i]);
    }

    private void drawWordMatchGame(Canvas canvas) {
        drawGameLabel(canvas, "A little language workout");
        drawChallengeTimer(canvas, wordEndsAt, LILAC);
        RectF card = new RectF(dp(24), dp(234), getWidth() - dp(24), dp(336));
        fillRound(canvas, card, dp(24), CARD);
        strokeRound(canvas, card, dp(24), withAlpha(LILAC, 38), dp(1));
        text(canvas, wordPrompt, card.centerX(), card.centerY() + dp(12), 31, INK, Paint.Align.CENTER, true);
        layoutGenericChoices(dp(357));
        for (int i = 0; i < 4; i++) drawChoiceCard(canvas, mathChoiceRects[i], wordChoices.get(i), LILAC, 16);
    }

    private void drawColorFocusGame(Canvas canvas) {
        drawGameLabel(canvas, "The written word is a distraction");
        drawChallengeTimer(canvas, colorEndsAt, CORAL);
        RectF card = new RectF(dp(24), dp(234), getWidth() - dp(24), dp(336));
        fillRound(canvas, card, dp(24), CARD);
        strokeRound(canvas, card, dp(24), withAlpha(CORAL, 38), dp(1));
        text(canvas, colorWord, card.centerX(), card.centerY() + dp(12), 31, COLOR_VALUES[colorInkIndex], Paint.Align.CENTER, true);
        layoutGenericChoices(dp(357));
        for (int i = 0; i < 4; i++) drawChoiceCard(canvas, mathChoiceRects[i], COLOR_NAMES[i], COLOR_VALUES[i], 14);
    }

    private void drawReactionGame(Canvas canvas) {
        drawGameLabel(canvas, "Round " + (reactionRound + 1) + " of 5");
        layoutReactionTarget();
        fillRound(canvas, reactionTarget, dp(30), reactionReady ? MINT : CARD);
        strokeRound(canvas, reactionTarget, dp(30), withAlpha(AQUA, 60), dp(2));
        if (reactionReady) {
            text(canvas, "TAP!", reactionTarget.centerX(), reactionTarget.centerY() + dp(12), 38, Color.WHITE, Paint.Align.CENTER, true);
            drawTargetIcon(canvas, reactionTarget.centerX(), reactionTarget.centerY() - dp(70), dp(44), Color.WHITE);
        } else {
            text(canvas, "WAIT", reactionTarget.centerX(), reactionTarget.centerY() + dp(12), 32, INK, Paint.Align.CENTER, true);
            text(canvas, "Tap only when the panel turns green", reactionTarget.centerX(), reactionTarget.centerY() + dp(48), 12, MUTED, Paint.Align.CENTER, false);
        }
    }

    private void drawNumberOrderGame(Canvas canvas) {
        drawGameLabel(canvas, "Sort four numbers in your head");
        layoutOrderTiles();
        for (int i = 0; i < orderTiles.length; i++) {
            RectF tile = orderTiles[i];
            boolean used = orderUsed[i];
            fillRound(canvas, tile, dp(22), used ? PALE_MINT : CARD);
            strokeRound(canvas, tile, dp(22), withAlpha(BLUE, 45), dp(1));
            text(canvas, used ? "✓" : String.valueOf(orderValues[i]), tile.centerX(), tile.centerY() + dp(10), used ? 28 : 25,
                    used ? MINT : INK, Paint.Align.CENTER, true);
        }
        text(canvas, "Next: " + orderExpected, getWidth() / 2f, gameFooterTop() - dp(16), 13, BLUE, Paint.Align.CENTER, true);
    }

    private void drawOddOneOutGame(Canvas canvas) {
        drawGameLabel(canvas, "One tile is slightly different");
        layoutOddTiles();
        for (int i = 0; i < oddTiles.length; i++) {
            RectF tile = oddTiles[i];
            fillRound(canvas, tile, dp(14), CARD);
            strokeRound(canvas, tile, dp(14), withAlpha(GOLD, 38), dp(1));
            paint.setColor(i == oddTarget ? GOLD : withAlpha(GOLD, 150));
            canvas.drawCircle(tile.centerX(), tile.centerY(), tile.width() * (i == oddTarget ? .27f : .20f), paint);
        }
    }

    private void drawSequenceLogicGame(Canvas canvas) {
        drawGameLabel(canvas, "Patterns are hiding in plain sight");
        drawChallengeTimer(canvas, sequenceEndsAt, MINT);
        RectF card = new RectF(dp(24), dp(234), getWidth() - dp(24), dp(336));
        fillRound(canvas, card, dp(24), CARD);
        strokeRound(canvas, card, dp(24), withAlpha(MINT, 38), dp(1));
        text(canvas, sequencePrompt, card.centerX(), card.centerY() + dp(12), 28, INK, Paint.Align.CENTER, true);
        layoutGenericChoices(dp(357));
        for (int i = 0; i < 4; i++) drawChoiceCard(canvas, mathChoiceRects[i], String.valueOf(sequenceChoices.get(i)), MINT, 21);
    }

    private void drawShapeRotateGame(Canvas canvas) {
        drawGameLabel(canvas, "Same shape, different rotation");
        RectF target = new RectF(dp(34), dp(194), getWidth() - dp(34), dp(332));
        fillRound(canvas, target, dp(24), PALE_LILAC);
        text(canvas, "TARGET", target.centerX(), target.top + dp(25), 10, LILAC, Paint.Align.CENTER, true);
        drawShapeSymbol(canvas, target.centerX(), target.centerY() + dp(10), dp(58), shapeTargetDirection, LILAC);
        layoutShapeOptions();
        for (int i = 0; i < 4; i++) {
            RectF option = shapeOptions[i];
            fillRound(canvas, option, dp(20), CARD);
            strokeRound(canvas, option, dp(20), withAlpha(LILAC, 45), dp(1));
            drawShapeSymbol(canvas, option.centerX(), option.centerY(), dp(32), shapeOptionDirections[i], LILAC);
        }
    }

    private void drawChoiceCard(Canvas canvas, RectF rect, String label, int color, float size) {
        fillRound(canvas, rect, dp(20), CARD);
        strokeRound(canvas, rect, dp(20), withAlpha(color, 45), dp(1));
        text(canvas, label, rect.centerX(), rect.centerY() + dp(7), size, INK, Paint.Align.CENTER, true);
    }

    private void drawTargetIcon(Canvas canvas, float cx, float cy, float size, int color) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(dp(3));
        strokePaint.setColor(color);
        canvas.drawCircle(cx, cy, size * .4f, strokePaint);
        canvas.drawCircle(cx, cy, size * .12f, strokePaint);
        canvas.drawLine(cx - size * .62f, cy, cx - size * .42f, cy, strokePaint);
        canvas.drawLine(cx + size * .42f, cy, cx + size * .62f, cy, strokePaint);
        canvas.drawLine(cx, cy - size * .62f, cx, cy - size * .42f, strokePaint);
        canvas.drawLine(cx, cy + size * .42f, cx, cy + size * .62f, strokePaint);
    }

    private void drawHome(Canvas canvas) {
        float width = getWidth();
        float margin = dp(20);
        RectF hero = new RectF(-dp(32), -dp(44), width + dp(32), dp(174));
        fillRound(canvas, hero, dp(42), NAVY);
        paint.setColor(withAlpha(BLUE, 95));
        canvas.drawCircle(width - dp(24), dp(31), dp(68), paint);
        paint.setColor(withAlpha(LILAC, 120));
        canvas.drawCircle(width - dp(74), dp(124), dp(42), paint);

        text(canvas, "IQOla", margin, dp(62), 32, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "A little brain spark, every day", margin, dp(88), 14, withAlpha(Color.WHITE, 185), Paint.Align.LEFT, false);
        text(canvas, "YOUR BEST", margin, dp(130), 11, withAlpha(Color.WHITE, 165), Paint.Align.LEFT, true);
        text(canvas, combinedBest() + " pts", margin, dp(153), 20, Color.WHITE, Paint.Align.LEFT, true);

        float startY = dp(194);
        float gap = dp(12);
        float cardWidth = (width - margin * 3f) / 2f;
        float cardHeight = dp(108);
        homeScrollOffset = clampHomeScroll(homeScrollOffset);

        canvas.save();
        canvas.clipRect(0, dp(176), width, getHeight());
        for (int i = 0; i < homeCards.length; i++) {
            int column = i % 2;
            int row = i / 2;
            float left = margin + column * (cardWidth + gap);
            float top = startY + row * (cardHeight + gap) - homeScrollOffset;
            homeCards[i].set(left, top, left + cardWidth, top + cardHeight);
            if (homeCards[i].bottom >= dp(176) && homeCards[i].top <= getHeight()) {
                drawHomeCard(canvas, homeCards[i], GameType.values()[i], i);
            }
        }
        canvas.restore();
        if (maxHomeScroll() > 0) {
            text(canvas, homeScrollOffset < maxHomeScroll() - dp(2) ? "Swipe for more games" : "Top of list",
                    width / 2f, getHeight() - dp(10), 10, MUTED, Paint.Align.CENTER, true);
        }
    }

    private void drawHomeCard(Canvas canvas, RectF rect, GameType game, int index) {
        fillRound(canvas, rect, dp(22), CARD);
        strokeRound(canvas, rect, dp(22), withAlpha(game.color, 25), dp(1));
        float iconSize = dp(48);
        float iconX = rect.left + dp(18);
        float iconY = rect.top + dp(17);
        fillRound(canvas, new RectF(iconX, iconY, iconX + iconSize, iconY + iconSize), dp(18), game.pale);
        drawGameIcon(canvas, game, iconX + iconSize / 2f, iconY + iconSize / 2f, iconSize * .52f, game.color);

        float textX = iconX + iconSize + dp(12);
        float titleY = rect.top + dp(34);
        text(canvas, game.title, textX, titleY, 14, INK, Paint.Align.LEFT, true);
        text(canvas, homeHint(game), textX, titleY + dp(22), 10, MUTED, Paint.Align.LEFT, false);
        drawMiniBest(canvas, bestFor(game) + " best", rect.right - dp(12), rect.bottom - dp(13), game.color);
    }

    private String homeHint(GameType game) {
        if (game == GameType.MEMORY) return "Glow tiles";
        if (game == GameType.MATH) return "30 sec sprint";
        if (game == GameType.DIGITS) return "Recall numbers";
        if (game == GameType.ARROWS) return "Hidden directions";
        if (game == GameType.CONNECT) return "Clean paths";
        if (game == GameType.WORD_MATCH) return "Language";
        if (game == GameType.COLOR_FOCUS) return "Attention";
        if (game == GameType.REACTION) return "Speed";
        if (game == GameType.NUMBER_ORDER) return "Logic";
        if (game == GameType.ODD_ONE_OUT) return "Visual focus";
        if (game == GameType.SEQUENCE_LOGIC) return "Patterns";
        return "Visual thinking";
    }

    private float maxHomeScroll() {
        float startY = dp(194);
        float gap = dp(12);
        float cardHeight = dp(108);
        float contentBottom = startY + 5f * (cardHeight + gap) + cardHeight + dp(20);
        return Math.max(0f, contentBottom - getHeight());
    }

    private float clampHomeScroll(float value) {
        return Math.max(0f, Math.min(maxHomeScroll(), value));
    }

    private void drawMiniBest(Canvas canvas, String value, float right, float baseline, int color) {
        paint.setTextSize(dp(10));
        paint.setTypeface(bold);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(color);
        canvas.drawText(value, right, baseline, paint);
    }

    private void drawGame(Canvas canvas) {
        drawGameHeader(canvas);
        if (selectedGame == GameType.MEMORY) {
            drawMemoryGame(canvas);
        } else if (selectedGame == GameType.MATH) {
            drawMathGame(canvas);
        } else if (selectedGame == GameType.DIGITS) {
            drawDigitGame(canvas);
        } else if (selectedGame == GameType.ARROWS) {
            drawArrowGame(canvas);
        } else if (selectedGame == GameType.CONNECT) {
            drawConnectGame(canvas);
        } else if (selectedGame == GameType.WORD_MATCH) {
            drawWordMatchGame(canvas);
        } else if (selectedGame == GameType.COLOR_FOCUS) {
            drawColorFocusGame(canvas);
        } else if (selectedGame == GameType.REACTION) {
            drawReactionGame(canvas);
        } else if (selectedGame == GameType.NUMBER_ORDER) {
            drawNumberOrderGame(canvas);
        } else if (selectedGame == GameType.ODD_ONE_OUT) {
            drawOddOneOutGame(canvas);
        } else if (selectedGame == GameType.SEQUENCE_LOGIC) {
            drawSequenceLogicGame(canvas);
        } else {
            drawShapeRotateGame(canvas);
        }
        drawAdSlot(canvas);
    }

    private void drawGameHeader(Canvas canvas) {
        float width = getWidth();
        fillRound(canvas, new RectF(0, 0, width, dp(102)), 0, CARD);
        paint.setColor(withAlpha(INK, 18));
        canvas.drawRect(0, dp(101), width, dp(102), paint);

        backRect.set(dp(16), dp(26), dp(56), dp(66));
        fillRound(canvas, backRect, dp(15), PALE_BLUE);
        text(canvas, "‹", backRect.centerX(), dp(56), 34, BLUE, Paint.Align.CENTER, false);

        text(canvas, selectedGame.title, dp(70), dp(47), 18, INK, Paint.Align.LEFT, true);
        text(canvas, "LEVEL " + level + "  •  " + score + " PTS", dp(70), dp(68), 11, MUTED, Paint.Align.LEFT, true);

        restartRect.set(width - dp(88), dp(29), width - dp(18), dp(63));
        fillRound(canvas, restartRect, dp(14), selectedGame.pale);
        text(canvas, "Restart", restartRect.centerX(), dp(51), 11, selectedGame.color, Paint.Align.CENTER, true);
    }

    private void drawGameLabel(Canvas canvas, String hint) {
        text(canvas, hint, getWidth() / 2f, dp(132), 14, MUTED, Paint.Align.CENTER, false);
        fillRound(canvas, new RectF(getWidth() / 2f - dp(82), dp(143), getWidth() / 2f + dp(82), dp(171)), dp(14), selectedGame.pale);
        text(canvas, feedback, getWidth() / 2f, dp(162), 11, selectedGame.color, Paint.Align.CENTER, true);
    }

    private void drawMemoryGame(Canvas canvas) {
        drawGameLabel(canvas, "Build a longer pattern each round");
        layoutMemoryTiles();
        for (int i = 0; i < memoryTiles.length; i++) {
            RectF tile = memoryTiles[i];
            boolean active = memoryFlashIndex == i;
            fillRound(canvas, tile, dp(18), active ? selectedGame.color : CARD);
            if (!active) strokeRound(canvas, tile, dp(18), withAlpha(BLUE, 38), dp(1));
            if (active) {
                strokeRound(canvas, tile, dp(18), withAlpha(Color.WHITE, 190), dp(2));
            }
            if (memoryShowing && !active) {
                paint.setColor(withAlpha(PALE_BLUE, 90));
                canvas.drawCircle(tile.centerX(), tile.centerY(), tile.width() * .13f, paint);
            }
        }
        text(canvas, memoryShowing ? "Memorizing " + memorySequence.size() + " tiles" : "Tap tile " + (memoryInputIndex + 1) + " of " + memorySequence.size(),
                getWidth() / 2f, gameFooterTop() - dp(18), 12, MUTED, Paint.Align.CENTER, false);
    }

    private void layoutMemoryTiles() {
        float margin = dp(28);
        float gap = dp(12);
        float total = Math.min(getWidth() - margin * 2f, Math.max(dp(180), getHeight() - dp(330)));
        float tile = (total - gap * 2f) / 3f;
        float left = (getWidth() - total) / 2f;
        float top = dp(196);
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int column = i % 3;
            float x = left + column * (tile + gap);
            float y = top + row * (tile + gap);
            memoryTiles[i].set(x, y, x + tile, y + tile);
        }
    }

    private void drawMathGame(Canvas canvas) {
        drawGameLabel(canvas, "Answer fast. The clock keeps moving.");
        long remaining = Math.max(0L, mathEndsAt - SystemClock.elapsedRealtime());
        int seconds = (int) Math.ceil(remaining / 1000f);
        float timerWidth = dp(62);
        RectF timer = new RectF(getWidth() / 2f - timerWidth / 2f, dp(184), getWidth() / 2f + timerWidth / 2f, dp(218));
        fillRound(canvas, timer, dp(17), seconds <= 8 ? PALE_CORAL : PALE_BLUE);
        text(canvas, seconds + "s", timer.centerX(), dp(207), 14, seconds <= 8 ? CORAL : BLUE, Paint.Align.CENTER, true);

        RectF question = new RectF(dp(24), dp(234), getWidth() - dp(24), dp(336));
        fillRound(canvas, question, dp(24), CARD);
        strokeRound(canvas, question, dp(24), withAlpha(LILAC, 38), dp(1));
        text(canvas, mathQuestion, question.centerX(), question.centerY() + dp(11), 34, INK, Paint.Align.CENTER, true);

        layoutMathChoices();
        for (int i = 0; i < mathChoiceRects.length; i++) {
            RectF choice = mathChoiceRects[i];
            fillRound(canvas, choice, dp(20), CARD);
            strokeRound(canvas, choice, dp(20), withAlpha(LILAC, 45), dp(1));
            text(canvas, String.valueOf(mathChoices.get(i)), choice.centerX(), choice.centerY() + dp(8), 22, INK, Paint.Align.CENTER, true);
        }
    }

    private void layoutMathChoices() {
        float margin = dp(24);
        float gap = dp(12);
        float width = (getWidth() - margin * 2f - gap) / 2f;
        float top = dp(357);
        float available = gameFooterTop() - dp(16) - top;
        float height = Math.max(dp(58), (available - gap) / 2f);
        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int column = i % 2;
            float left = margin + column * (width + gap);
            float y = top + row * (height + gap);
            mathChoiceRects[i].set(left, y, left + width, y + height);
        }
    }

    private void drawDigitGame(Canvas canvas) {
        drawGameLabel(canvas, "Look once, then trust your memory");
        RectF display = new RectF(dp(24), dp(194), getWidth() - dp(24), dp(324));
        fillRound(canvas, display, dp(24), digitShowing ? PALE_GOLD : CARD);
        strokeRound(canvas, display, dp(24), withAlpha(GOLD, 45), dp(1));
        if (digitShowing) {
            text(canvas, digitValue, display.centerX(), display.centerY() + dp(13), 37, INK, Paint.Align.CENTER, true);
            text(canvas, "Hold it in your mind", display.centerX(), display.bottom - dp(18), 12, MUTED, Paint.Align.CENTER, false);
        } else {
            float boxSize = Math.min(dp(42), (display.width() - dp(32)) / Math.max(4, digitValue.length()));
            float total = digitValue.length() * boxSize + (digitValue.length() - 1) * dp(7);
            float start = display.centerX() - total / 2f;
            for (int i = 0; i < digitValue.length(); i++) {
                RectF box = new RectF(start + i * (boxSize + dp(7)), display.centerY() - boxSize / 2f,
                        start + i * (boxSize + dp(7)) + boxSize, display.centerY() + boxSize / 2f);
                fillRound(canvas, box, dp(12), PALE_BLUE);
                String shown = i < digitInput.length() ? digitInput.substring(i, i + 1) : "•";
                text(canvas, shown, box.centerX(), box.centerY() + dp(8), 22, i < digitInput.length() ? INK : BLUE, Paint.Align.CENTER, true);
            }
        }

        layoutDigitKeys();
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "Clear"};
        for (int i = 0; i < digitKeyRects.length; i++) {
            RectF key = digitKeyRects[i];
            fillRound(canvas, key, dp(16), i == 11 ? PALE_GOLD : CARD);
            strokeRound(canvas, key, dp(16), withAlpha(GOLD, 45), dp(1));
            text(canvas, labels[i], key.centerX(), key.centerY() + dp(6), i == 11 ? 12 : 18, i == 11 ? GOLD : INK, Paint.Align.CENTER, true);
        }
    }

    private void layoutDigitKeys() {
        float margin = dp(24);
        float gap = dp(9);
        float width = (getWidth() - margin * 2f - gap * 2f) / 3f;
        float keyHeight = dp(48);
        float total = keyHeight * 4f + gap * 3f;
        float top = gameFooterTop() - dp(14) - total;
        for (int i = 0; i < 12; i++) {
            int row = i / 3;
            int column = i % 3;
            float left = margin + column * (width + gap);
            float y = top + row * (keyHeight + gap);
            digitKeyRects[i].set(left, y, left + width, y + keyHeight);
        }
    }

    private void drawArrowGame(Canvas canvas) {
        drawGameLabel(canvas, "The arrows vanish. Your route stays.");
        RectF stage = new RectF(dp(34), dp(192), getWidth() - dp(34), dp(358));
        fillRound(canvas, stage, dp(28), arrowShowing ? PALE_MINT : CARD);
        strokeRound(canvas, stage, dp(28), withAlpha(AQUA, 50), dp(1));
        if (arrowShowing && arrowFlashPosition >= 0) {
            int direction = arrowSequence.get(arrowFlashPosition);
            drawArrowSymbol(canvas, stage.centerX(), stage.centerY() - dp(6), dp(70), direction, AQUA);
            text(canvas, (arrowFlashPosition + 1) + " / " + arrowSequence.size(), stage.centerX(), stage.bottom - dp(20), 12, AQUA, Paint.Align.CENTER, true);
        } else if (arrowShowing) {
            text(canvas, "Get ready", stage.centerX(), stage.centerY() + dp(7), 22, MUTED, Paint.Align.CENTER, true);
        } else {
            text(canvas, "Your turn", stage.centerX(), stage.centerY() - dp(8), 24, INK, Paint.Align.CENTER, true);
            text(canvas, arrowInputIndex + " of " + arrowSequence.size() + " directions", stage.centerX(), stage.centerY() + dp(24), 13, MUTED, Paint.Align.CENTER, false);
        }
        layoutArrowPad();
        for (int direction = 0; direction < 4; direction++) {
            RectF key = arrowPadRects[direction];
            fillRound(canvas, key, dp(18), CARD);
            strokeRound(canvas, key, dp(18), withAlpha(AQUA, 55), dp(1));
            drawArrowSymbol(canvas, key.centerX(), key.centerY(), key.width() * .44f, direction, AQUA);
        }
    }

    private void layoutArrowPad() {
        float size = dp(62);
        float gap = dp(10);
        float center = getWidth() / 2f;
        float top = gameFooterTop() - dp(12) - (size * 3f + gap * 2f);
        arrowPadRects[0].set(center - size / 2f, top, center + size / 2f, top + size);
        float middle = top + size + gap;
        arrowPadRects[3].set(center - size - gap / 2f, middle, center - gap / 2f, middle + size);
        arrowPadRects[1].set(center + gap / 2f, middle, center + size + gap / 2f, middle + size);
        float bottom = middle + size + gap;
        arrowPadRects[2].set(center - size / 2f, bottom, center + size / 2f, bottom + size);
    }

    private void drawConnectGame(Canvas canvas) {
        drawGameLabel(canvas, "Match each color. No crossing paths.");
        layoutConnectArea();
        fillRound(canvas, connectArea, dp(28), CARD);
        strokeRound(canvas, connectArea, dp(28), withAlpha(CORAL, 38), dp(1));
        for (ConnectLine line : connectLines) {
            PointF start = connectPoint(line.start);
            PointF end = connectPoint(line.end);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeWidth(dp(8));
            strokePaint.setColor(connectNodes.get(line.start).color);
            canvas.drawLine(start.x, start.y, end.x, end.y, strokePaint);
        }
        if (activeNode >= 0) {
            PointF start = connectPoint(activeNode);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeWidth(dp(5));
            strokePaint.setColor(withAlpha(connectNodes.get(activeNode).color, 170));
            canvas.drawLine(start.x, start.y, dragX, dragY, strokePaint);
        }
        for (int i = 0; i < connectNodes.size(); i++) {
            PointF point = connectPoint(i);
            int color = connectNodes.get(i).color;
            paint.setColor(withAlpha(color, 38));
            canvas.drawCircle(point.x, point.y, dp(20), paint);
            paint.setColor(color);
            canvas.drawCircle(point.x, point.y, dp(12), paint);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(2));
            strokePaint.setColor(Color.WHITE);
            canvas.drawCircle(point.x, point.y, dp(12), strokePaint);
        }
    }

    private void layoutConnectArea() {
        connectArea.set(dp(20), dp(190), getWidth() - dp(20), gameFooterTop() - dp(14));
    }

    private PointF connectPoint(int index) {
        ConnectNode node = connectNodes.get(index);
        return new PointF(connectArea.left + node.x * connectArea.width(), connectArea.top + node.y * connectArea.height());
    }

    private void drawAdSlot(Canvas canvas) {
        float top = gameFooterTop();
        RectF slot = new RectF(dp(16), top + dp(7), getWidth() - dp(16), getHeight() - dp(7));
        fillRound(canvas, slot, dp(17), NAVY);
        paint.setColor(withAlpha(BLUE, 100));
        canvas.drawCircle(slot.left + dp(26), slot.centerY(), dp(11), paint);
        text(canvas, "TEST AD SPACE", slot.left + dp(46), slot.centerY() - dp(2), 9, withAlpha(Color.WHITE, 155), Paint.Align.LEFT, true);
        text(canvas, "AdMob banner goes here", slot.left + dp(46), slot.centerY() + dp(13), 11, Color.WHITE, Paint.Align.LEFT, false);
        removeAdsRect.set(slot.right - dp(98), slot.centerY() - dp(17), slot.right - dp(11), slot.centerY() + dp(17));
        fillRound(canvas, removeAdsRect, dp(13), Color.WHITE);
        text(canvas, "Remove ads", removeAdsRect.centerX(), removeAdsRect.centerY() + dp(4), 10, NAVY, Paint.Align.CENTER, true);
    }

    private float gameFooterTop() {
        return getHeight() - dp(72);
    }

    private void drawResult(Canvas canvas) {
        float width = getWidth();
        fillRound(canvas, new RectF(-dp(30), -dp(30), width + dp(30), dp(218)), dp(42), NAVY);
        paint.setColor(withAlpha(BLUE, 80));
        canvas.drawCircle(width - dp(34), dp(45), dp(60), paint);
        text(canvas, "IQOla", dp(22), dp(58), 25, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, resultTitle, width / 2f, dp(124), 25, Color.WHITE, Paint.Align.CENTER, true);
        text(canvas, selectedGame.title, width / 2f, dp(151), 14, withAlpha(Color.WHITE, 180), Paint.Align.CENTER, false);

        RectF card = new RectF(dp(24), dp(180), width - dp(24), dp(404));
        fillRound(canvas, card, dp(28), CARD);
        strokeRound(canvas, card, dp(28), withAlpha(selectedGame.color, 40), dp(1));
        text(canvas, "SESSION SCORE", card.centerX(), dp(222), 11, MUTED, Paint.Align.CENTER, true);
        text(canvas, score + "", card.centerX(), dp(284), 48, INK, Paint.Align.CENTER, true);
        text(canvas, "BEST  " + resultBest + " pts", card.centerX(), dp(317), 14, selectedGame.color, Paint.Align.CENTER, true);
        text(canvas, score >= resultBest && score > 0 ? "New personal best" : "Every round builds speed", card.centerX(), dp(360), 14, MUTED, Paint.Align.CENTER, false);

        float buttonTop = Math.min(dp(432), getHeight() - dp(144));
        resultReplayRect.set(dp(24), buttonTop, width - dp(24), buttonTop + dp(54));
        fillRound(canvas, resultReplayRect, dp(18), selectedGame.color);
        text(canvas, "Play again", resultReplayRect.centerX(), resultReplayRect.centerY() + dp(6), 16, Color.WHITE, Paint.Align.CENTER, true);
        resultHomeRect.set(dp(24), buttonTop + dp(66), width - dp(24), buttonTop + dp(116));
        fillRound(canvas, resultHomeRect, dp(18), PALE_BLUE);
        text(canvas, "All games", resultHomeRect.centerX(), resultHomeRect.centerY() + dp(6), 15, BLUE, Paint.Align.CENTER, true);
    }

    private void drawGameIcon(Canvas canvas, GameType game, float cx, float cy, float size, int color) {
        if (game == GameType.MEMORY) {
            float box = size * .34f;
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 2; column++) {
                    RectF square = new RectF(cx - box - size * .06f + column * (box + size * .12f),
                            cy - box - size * .06f + row * (box + size * .12f),
                            cx - size * .06f + column * (box + size * .12f),
                            cy - size * .06f + row * (box + size * .12f));
                    fillRound(canvas, square, size * .10f, (row == 0 && column == 1) ? color : withAlpha(color, 95));
                }
            }
        } else if (game == GameType.MATH) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeWidth(dp(3));
            strokePaint.setColor(color);
            canvas.drawLine(cx - size * .34f, cy, cx + size * .34f, cy, strokePaint);
            canvas.drawLine(cx, cy - size * .34f, cx, cy + size * .34f, strokePaint);
        } else if (game == GameType.DIGITS) {
            text(canvas, "123", cx, cy + size * .22f, size * .46f, color, Paint.Align.CENTER, true);
        } else if (game == GameType.ARROWS) {
            drawArrowSymbol(canvas, cx, cy, size * .72f, 1, color);
        } else if (game == GameType.CONNECT) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeWidth(dp(3));
            strokePaint.setColor(color);
            canvas.drawLine(cx - size * .35f, cy + size * .24f, cx + size * .35f, cy - size * .24f, strokePaint);
            paint.setColor(color);
            canvas.drawCircle(cx - size * .35f, cy + size * .24f, size * .12f, paint);
            canvas.drawCircle(cx + size * .35f, cy - size * .24f, size * .12f, paint);
        } else if (game == GameType.WORD_MATCH) {
            text(canvas, "Aa", cx, cy + size * .2f, size * .62f, color, Paint.Align.CENTER, true);
        } else if (game == GameType.COLOR_FOCUS) {
            int[] dots = {CORAL, BLUE, GOLD, MINT};
            for (int i = 0; i < dots.length; i++) {
                paint.setColor(dots[i]);
                canvas.drawCircle(cx + (i % 2 == 0 ? -size * .2f : size * .2f),
                        cy + (i < 2 ? -size * .2f : size * .2f), size * .14f, paint);
            }
        } else if (game == GameType.REACTION) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(3));
            strokePaint.setColor(color);
            canvas.drawCircle(cx, cy, size * .3f, strokePaint);
            canvas.drawCircle(cx, cy, size * .08f, paint);
            canvas.drawLine(cx - size * .48f, cy, cx - size * .28f, cy, strokePaint);
            canvas.drawLine(cx + size * .28f, cy, cx + size * .48f, cy, strokePaint);
        } else if (game == GameType.NUMBER_ORDER) {
            text(canvas, "1→4", cx, cy + size * .18f, size * .46f, color, Paint.Align.CENTER, true);
        } else if (game == GameType.ODD_ONE_OUT) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    paint.setColor((row == 1 && column == 1) ? color : withAlpha(color, 100));
                    canvas.drawCircle(cx + (column - 1) * size * .25f, cy + (row - 1) * size * .25f, size * .08f, paint);
                }
            }
        } else if (game == GameType.SEQUENCE_LOGIC) {
            text(canvas, "?", cx, cy + size * .24f, size * .72f, color, Paint.Align.CENTER, true);
        } else {
            drawShapeSymbol(canvas, cx, cy, size * .7f, 0, color);
        }
    }

    private void drawShapeSymbol(Canvas canvas, float cx, float cy, float size, int direction, int color) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(direction * 90f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Math.max(dp(3), size * .1f));
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(color);
        Path shape = new Path();
        shape.moveTo(-size * .35f, size * .35f);
        shape.lineTo(-size * .35f, -size * .35f);
        shape.lineTo(size * .35f, -size * .35f);
        canvas.drawPath(shape, strokePaint);
        canvas.restore();
    }

    private void drawArrowSymbol(Canvas canvas, float cx, float cy, float size, int direction, int color) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(direction * 90f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeWidth(Math.max(dp(3), size * .12f));
        strokePaint.setColor(color);
        canvas.drawLine(0, size * .34f, 0, -size * .18f, strokePaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        Path head = new Path();
        head.moveTo(0, -size * .48f);
        head.lineTo(-size * .28f, -size * .12f);
        head.lineTo(size * .28f, -size * .12f);
        head.close();
        canvas.drawPath(head, paint);
        canvas.restore();
    }

    private void showAdsPreview() {
        new AlertDialog.Builder(getContext())
                .setTitle("Ad-free preview")
                .setMessage("This button is reserved for a permanent one-time Remove Ads purchase. It will connect to Google Play Billing before launch. Until then, the app is using a safe test-ad placeholder.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void fillRound(Canvas canvas, RectF rect, float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private void strokeRound(Canvas canvas, RectF rect, float radius, int color, float width) {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(width);
        strokePaint.setColor(color);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    private void text(Canvas canvas, String value, float x, float baseline, float sp, int color, Paint.Align align, boolean isBold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(sp));
        paint.setTypeface(isBold ? bold : regular);
        paint.setTextAlign(align);
        paint.setColor(color);
        canvas.drawText(value, x, baseline, paint);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class ConnectNode {
        final float x;
        final float y;
        final int color;

        ConnectNode(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private static final class ConnectLine {
        final int start;
        final int end;

        ConnectLine(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
