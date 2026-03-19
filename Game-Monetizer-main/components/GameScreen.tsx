/**
 * Shared game screen used by both Arcade and Daily modes.
 *
 * mode="arcade"  — standard game, tracks high score, shows "DESTROYED" death screen
 * mode="daily"   — seeded daily challenge, submits to leaderboard, shows streak + leaderboard button
 */
import { useCallback, useRef, useState, useEffect } from "react";
import {
  View,
  Text,
  Pressable,
  StyleSheet,
  Platform,
  AppState,
} from "react-native";
import { router } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons, MaterialCommunityIcons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  FadeIn,
} from "react-native-reanimated";
import Colors from "@/constants/colors";
import { useGame } from "@/contexts/GameContext";
import { getGameHTML } from "@/lib/game-html";
import GameWebView, { GameWebViewRef } from "@/components/GameWebView";
import { useRewardedAd } from "@/lib/use-rewarded-ad";
import SimulatedAdOverlay from "@/components/SimulatedAdOverlay";
import { getDeviceId, getDisplayName } from "@/lib/device-id";
import { apiRequest, queryClient } from "@/lib/query-client";

type GameState = "ready" | "playing" | "continue" | "dead";

interface GameScreenProps {
  mode: "arcade" | "daily";
}

export default function GameScreen({ mode }: GameScreenProps) {
  const isDaily = mode === "daily";
  const insets = useSafeAreaInsets();
  const {
    stats,
    recordGame,
    recordDailyGame,
    recordNearMiss,
    recordShieldUsed,
    recordAdWatched,
    getDailySeed,
  } = useGame();

  const gameRef = useRef<GameWebViewRef>(null);
  const [gameState, setGameState] = useState<GameState>("ready");
  const [finalScore, setFinalScore] = useState(0);
  const [finalTime, setFinalTime] = useState(0);
  const [isNewHigh, setIsNewHigh] = useState(false);
  const [currentScore, setCurrentScore] = useState(0);
  const [countdown, setCountdown] = useState(5);
  const { adAvailable, showAd } = useRewardedAd();

  const [showingSimAd, setShowingSimAd] = useState(false);
  const simAdRewardRef = useRef<(() => void) | null>(null);
  const continueUsedRef = useRef(false);
  const savedScoreRef = useRef(0);
  const savedTimeRef = useRef(0);
  const countdownIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const scoreScale = useSharedValue(1);

  const topPad = Platform.OS === "web" ? 67 : insets.top;
  const bottomPad = Platform.OS === "web" ? 34 : insets.bottom;

  // Daily-specific state
  const seed = isDaily ? getDailySeed() : undefined;
  const html = getGameHTML(seed ? { dailySeed: seed } : undefined);

  // --- Daily: submit score to leaderboard API ---
  const submitScore = useCallback(
    async (score: number, time: number) => {
      if (!isDaily || !seed) return;
      try {
        const deviceId = await getDeviceId();
        const displayName = await getDisplayName();
        await apiRequest("POST", "/api/daily-scores", {
          deviceId,
          displayName,
          date: seed,
          score,
          survivalTime: time,
        });
        queryClient.invalidateQueries({ queryKey: ["/api/daily-scores", seed] });
      } catch {}
    },
    [isDaily, seed]
  );

  // --- Show death screen ---
  const showDeadScreen = useCallback(
    (score: number, time: number) => {
      if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);

      setFinalScore(score);
      setFinalTime(time);

      if (isDaily) {
        recordDailyGame(score, time);
        submitScore(score, time);
      } else {
        const isHigh = score > stats.highScore;
        setIsNewHigh(isHigh);
        recordGame(score, time);
      }

      setGameState("dead");
      if (Platform.OS !== "web") Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    },
    [isDaily, stats.highScore, recordGame, recordDailyGame, submitScore]
  );

  // --- Handle messages from HTML game ---
  const handleMessage = useCallback(
    (event: { nativeEvent: { data: string } }) => {
      try {
        const msg = JSON.parse(event.nativeEvent.data);

        if (msg.type === "gameStart") {
          setGameState("playing");
          setCurrentScore(0);
          continueUsedRef.current = false;
        }

        if (msg.type === "scoreUpdate") {
          setCurrentScore(msg.score);
        }

        if (msg.type === "nearMiss") {
          recordNearMiss();
          if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        }

        if (msg.type === "powerUp" && msg.data?.type === "shield") {
          recordShieldUsed();
        }

        if (msg.type === "shieldBreak") {
          if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
        }

        if (msg.type === "playerDied") {
          savedScoreRef.current = msg.score;
          savedTimeRef.current = msg.time;
          if (Platform.OS !== "web") Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);

          // Skip continue screen if already used, or (daily only) if ad not available
          if (continueUsedRef.current || (isDaily && !adAvailable)) {
            showDeadScreen(msg.score, msg.time);
          } else {
            setCountdown(5);
            setGameState("continue");
          }
        }

        if (msg.type === "playerRespawned") {
          setGameState("playing");
        }

        if (msg.type === "gameOver") {
          showDeadScreen(savedScoreRef.current, savedTimeRef.current);
        }
      } catch {}
    },
    [recordNearMiss, recordShieldUsed, adAvailable, showDeadScreen, isDaily]
  );

  // --- Countdown timer for continue screen ---
  useEffect(() => {
    if (gameState !== "continue") return;
    setCountdown(5);
    countdownIntervalRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(countdownIntervalRef.current!);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => {
      if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);
    };
  }, [gameState]);

  useEffect(() => {
    if (countdown === 0 && gameState === "continue") {
      showDeadScreen(savedScoreRef.current, savedTimeRef.current);
    }
  }, [countdown, gameState, showDeadScreen]);

  // --- Actions ---
  const startGame = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    gameRef.current?.postMessage(JSON.stringify({ type: "start" }));
  }, []);

  const handleRetry = useCallback(() => {
    startGame();
  }, [startGame]);

  const handleBack = useCallback(() => {
    router.back();
  }, []);

  const handleWatchAd = useCallback(async () => {
    if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    continueUsedRef.current = true;

    const onRewarded = () => {
      gameRef.current?.postMessage(JSON.stringify({ type: "respawn" }));
      recordAdWatched();
    };

    // Try real Google test/video ads first
    const shown = await showAd(onRewarded);

    if (!shown) {
      // Real ad unavailable — fall back to simulated overlay
      simAdRewardRef.current = onRewarded;
      setShowingSimAd(true);
    }
  }, [recordAdWatched, showAd]);

  const handleSimAdComplete = useCallback(() => {
    setShowingSimAd(false);
    if (simAdRewardRef.current) {
      simAdRewardRef.current();
      simAdRewardRef.current = null;
    }
  }, []);

  const handleNoThanks = useCallback(() => {
    showDeadScreen(savedScoreRef.current, savedTimeRef.current);
  }, [showDeadScreen]);

  const scoreStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scoreScale.value }],
  }));

  // Pause game when app goes to background (prevents Android crash on Activity destroy)
  useEffect(() => {
    if (Platform.OS === "web") return;
    const sub = AppState.addEventListener("change", (nextState) => {
      if (nextState === "background" || nextState === "inactive") {
        gameRef.current?.postMessage(JSON.stringify({ type: "pause" }));
      }
    });
    return () => sub.remove();
  }, []);

  // Auto-start game on mount
  useEffect(() => {
    if (gameState === "ready") {
      const t = setTimeout(startGame, 500);
      return () => clearTimeout(t);
    }
  }, [gameState]);

  // Daily-specific label
  const todayLabel = isDaily
    ? new Date().toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
    : "";

  return (
    <View style={styles.container}>
      <GameWebView ref={gameRef} html={html} onMessage={handleMessage} />

      {/* ===== HUD (playing) ===== */}
      {gameState === "playing" && (
        <View style={[styles.hudOverlay, { top: topPad + 8 }]}>
          <Pressable onPress={handleBack} style={styles.backBtn}>
            <Ionicons name="chevron-back" size={22} color={Colors.textSecondary} />
          </Pressable>
          {isDaily ? (
            <View style={styles.hudCenter}>
              <Text style={styles.hudDaily}>DAILY</Text>
              <Text style={styles.hudScore}>{currentScore}</Text>
            </View>
          ) : (
            <Animated.Text style={[styles.hudScore, scoreStyle]}>
              {currentScore}
            </Animated.Text>
          )}
          <View style={{ width: 44 }} />
        </View>
      )}

      {/* ===== Continue screen ===== */}
      {gameState === "continue" && !showingSimAd && (
        <Animated.View
          entering={FadeIn.duration(300)}
          style={[styles.continueOverlay, { paddingTop: topPad + 40, paddingBottom: bottomPad + 20 }]}
        >
          <Text style={styles.continueLabel}>CONTINUE?</Text>
          <Text style={styles.countdownText}>{countdown}</Text>
          <Text style={styles.continueScore}>{savedScoreRef.current} pts</Text>

          <Pressable
            onPress={handleWatchAd}
            style={({ pressed }) => [
              styles.watchAdBtn,
              pressed && { opacity: 0.85, transform: [{ scale: 0.97 }] },
            ]}
            testID="watch-ad-btn"
          >
            <MaterialCommunityIcons name="play-circle-outline" size={24} color={Colors.background} />
            <Text style={styles.watchAdBtnText}>WATCH AD TO CONTINUE</Text>
          </Pressable>

          <Pressable
            onPress={handleNoThanks}
            style={({ pressed }) => [styles.noThanksBtn, pressed && { opacity: 0.7 }]}
          >
            <Text style={styles.noThanksBtnText}>No Thanks</Text>
          </Pressable>
        </Animated.View>
      )}

      {/* ===== Simulated ad overlay ===== */}
      <SimulatedAdOverlay visible={showingSimAd} onComplete={handleSimAdComplete} />

      {/* ===== Death screen ===== */}
      {gameState === "dead" && (
        <Animated.View
          entering={FadeIn.duration(400)}
          style={[styles.deathOverlay, { paddingTop: topPad + 40, paddingBottom: bottomPad + 20 }]}
        >
          {/* Daily header */}
          {isDaily && (
            <>
              <View style={styles.dailyBadge}>
                <MaterialCommunityIcons name="calendar-today" size={16} color={Colors.neonCyan} />
                <Text style={styles.dailyBadgeText}>DAILY CHALLENGE</Text>
              </View>
              <Text style={styles.dailyDate}>{todayLabel}</Text>
            </>
          )}

          {/* Arcade header */}
          {!isDaily && <Text style={styles.deathLabel}>DESTROYED</Text>}

          {/* Score */}
          <Text style={styles.deathScore}>{finalScore}</Text>
          <Text style={styles.deathTime}>survived {finalTime}s</Text>

          {/* Arcade: high score badge */}
          {!isDaily && isNewHigh && (
            <Animated.Text entering={FadeIn.delay(200)} style={styles.newHighText}>
              NEW HIGH SCORE
            </Animated.Text>
          )}
          {!isDaily && !isNewHigh && stats.highScore > 0 && (
            <Text style={styles.bestText}>best: {stats.highScore}</Text>
          )}

          {/* Daily: today's best + streak */}
          {isDaily && stats.dailyHighScore > 0 && finalScore >= stats.dailyHighScore && (
            <Text style={styles.newHighText}>TODAY'S BEST</Text>
          )}
          {isDaily && stats.dailyChallengeStreak > 1 && (
            <View style={styles.streakRow}>
              <Ionicons name="flame" size={16} color={Colors.neonOrange} />
              <Text style={styles.streakText}>{stats.dailyChallengeStreak} day streak</Text>
            </View>
          )}

          {/* Buttons */}
          <View style={styles.deathButtons}>
            <Pressable
              onPress={handleRetry}
              style={({ pressed }) => [styles.retryBtn, pressed && { opacity: 0.85, transform: [{ scale: 0.97 }] }]}
              testID="retry-btn"
            >
              <Ionicons name="refresh" size={22} color={Colors.background} />
              <Text style={styles.retryBtnText}>RETRY</Text>
            </Pressable>

            {isDaily && (
              <Pressable
                onPress={() => {
                  if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
                  router.push("/leaderboard");
                }}
                style={({ pressed }) => [styles.leaderboardBtn, pressed && { opacity: 0.85 }]}
                testID="leaderboard-btn"
              >
                <Ionicons name="trophy" size={18} color={Colors.neonYellow} />
                <Text style={styles.leaderboardBtnText}>LEADERBOARD</Text>
              </Pressable>
            )}

            <Pressable
              onPress={handleBack}
              style={({ pressed }) => [styles.menuBtn, pressed && { opacity: 0.7 }]}
            >
              <Text style={styles.menuBtnText}>MENU</Text>
            </Pressable>
          </View>
        </Animated.View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  // --- HUD ---
  hudOverlay: {
    position: "absolute" as const,
    left: 0,
    right: 0,
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "space-between" as const,
    paddingHorizontal: 16,
  },
  backBtn: {
    width: 44,
    height: 44,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  hudCenter: {
    alignItems: "center" as const,
  },
  hudDaily: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 10,
    color: Colors.neonCyan,
    letterSpacing: 3,
  },
  hudScore: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 24,
    color: Colors.white,
  },
  // --- Continue overlay ---
  continueOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(10,10,26,0.93)",
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  continueLabel: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 14,
    color: Colors.neonCyan,
    letterSpacing: 6,
    marginBottom: 12,
  },
  countdownText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 72,
    color: Colors.white,
    marginBottom: 4,
  },
  continueScore: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
    marginBottom: 32,
  },
  watchAdBtn: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 10,
    backgroundColor: Colors.neonCyan,
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 14,
    width: "100%",
    maxWidth: 300,
    marginBottom: 16,
  },
  watchAdBtnText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 14,
    color: Colors.background,
    letterSpacing: 2,
  },
  noThanksBtn: {
    paddingVertical: 12,
    paddingHorizontal: 24,
  },
  noThanksBtnText: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 13,
    color: Colors.textMuted,
    letterSpacing: 1,
  },
  // --- Death overlay ---
  deathOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(10,10,26,0.88)",
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  deathLabel: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 14,
    color: Colors.neonRed,
    letterSpacing: 4,
    marginBottom: 12,
  },
  deathScore: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 56,
    color: Colors.white,
    marginBottom: 4,
  },
  deathTime: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
    marginBottom: 12,
  },
  newHighText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 14,
    color: Colors.neonYellow,
    textShadowColor: Colors.neonYellow,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
    letterSpacing: 2,
    marginBottom: 8,
  },
  bestText: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
    marginBottom: 20,
  },
  // --- Daily-specific ---
  dailyBadge: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 6,
    marginBottom: 4,
  },
  dailyBadgeText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 11,
    color: Colors.neonCyan,
    letterSpacing: 2,
  },
  dailyDate: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 13,
    color: Colors.textMuted,
    marginBottom: 16,
  },
  streakRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 6,
    marginBottom: 16,
  },
  streakText: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 14,
    color: Colors.neonOrange,
  },
  // --- Buttons ---
  deathButtons: {
    alignItems: "center" as const,
    gap: 12,
    width: "100%",
    paddingHorizontal: 40,
    marginTop: 8,
  },
  retryBtn: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 10,
    backgroundColor: Colors.neonGreen,
    paddingVertical: 14,
    paddingHorizontal: 40,
    borderRadius: 14,
    width: "100%",
    maxWidth: 280,
  },
  retryBtnText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 16,
    color: Colors.background,
    letterSpacing: 3,
  },
  leaderboardBtn: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 8,
    borderWidth: 1.5,
    borderColor: Colors.neonYellow + "60",
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 12,
    width: "100%",
    maxWidth: 280,
  },
  leaderboardBtnText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 13,
    color: Colors.neonYellow,
    letterSpacing: 2,
  },
  menuBtn: {
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  menuBtnText: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 13,
    color: Colors.textMuted,
    letterSpacing: 2,
  },
});
