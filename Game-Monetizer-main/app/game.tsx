import { useCallback, useRef, useState, useEffect } from "react";
import {
  View,
  Text,
  Pressable,
  StyleSheet,
  Platform,
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
import { useInterstitialAd } from "@/lib/use-interstitial-ad";

type GameState = "ready" | "playing" | "continue" | "dead";

export default function GameScreen() {
  const insets = useSafeAreaInsets();
  const { stats, recordGame, recordNearMiss, recordShieldUsed, recordAdWatched } = useGame();
  const gameRef = useRef<GameWebViewRef>(null);
  const [gameState, setGameState] = useState<GameState>("ready");
  const [finalScore, setFinalScore] = useState(0);
  const [finalTime, setFinalTime] = useState(0);
  const [isNewHigh, setIsNewHigh] = useState(false);
  const [currentScore, setCurrentScore] = useState(0);
  const [countdown, setCountdown] = useState(5);
  const { adAvailable, adLoading, showAd } = useRewardedAd();
  const { maybeShowInterstitial } = useInterstitialAd();
  const continueUsedRef = useRef(false);
  const savedScoreRef = useRef(0);
  const savedTimeRef = useRef(0);
  const countdownIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const scoreScale = useSharedValue(1);

  const topPad = Platform.OS === "web" ? 67 : insets.top;
  const bottomPad = Platform.OS === "web" ? 34 : insets.bottom;

  const html = getGameHTML();

  const showDeadScreen = useCallback((score: number, time: number) => {
    if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);
    const isHigh = score > stats.highScore;
    setFinalScore(score);
    setFinalTime(time);
    setIsNewHigh(isHigh);
    recordGame(score, time);
    setGameState("dead");
    if (Platform.OS !== "web") Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
  }, [stats.highScore, recordGame]);

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

          if (continueUsedRef.current) {
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
    [stats.highScore, recordGame, recordNearMiss, recordShieldUsed, showDeadScreen]
  );

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

  const startGame = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    gameRef.current?.postMessage(JSON.stringify({ type: "start" }));
  }, []);

  const handleRetry = useCallback(async () => {
    await maybeShowInterstitial();
    startGame();
  }, [startGame, maybeShowInterstitial]);

  const handleBack = useCallback(() => {
    router.back();
  }, []);

  const handleWatchAd = useCallback(async () => {
    if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    continueUsedRef.current = true;
    const shown = await showAd(() => {
      gameRef.current?.postMessage(JSON.stringify({ type: "respawn" }));
    });
    if (shown) {
      await recordAdWatched();
    } else {
      // Ad wasn't available — go to dead screen
      showDeadScreen(savedScoreRef.current, savedTimeRef.current);
    }
  }, [recordAdWatched, showAd, showDeadScreen]);

  const handleNoThanks = useCallback(() => {
    showDeadScreen(savedScoreRef.current, savedTimeRef.current);
  }, [showDeadScreen]);

  const scoreStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scoreScale.value }],
  }));

  useEffect(() => {
    if (gameState === "ready") {
      const t = setTimeout(startGame, 500);
      return () => clearTimeout(t);
    }
  }, [gameState]);

  return (
    <View style={styles.container}>
      <GameWebView
        ref={gameRef}
        html={html}
        onMessage={handleMessage}
      />

      {gameState === "playing" && (
        <View style={[styles.hudOverlay, { top: topPad + 8 }]}>
          <Pressable onPress={handleBack} style={styles.backBtn}>
            <Ionicons name="chevron-back" size={22} color={Colors.textSecondary} />
          </Pressable>
          <Animated.Text style={[styles.hudScore, scoreStyle]}>
            {currentScore}
          </Animated.Text>
          <View style={{ width: 44 }} />
        </View>
      )}

      {gameState === "continue" && (
        <Animated.View
          entering={FadeIn.duration(300)}
          style={[styles.continueOverlay, { paddingTop: topPad + 40, paddingBottom: bottomPad + 20 }]}
        >
          <Text style={styles.continueLabel}>CONTINUE?</Text>
          <Text style={styles.countdownText}>{countdown}</Text>
          <Text style={styles.continueScore}>{savedScoreRef.current} pts</Text>

          <Pressable
            onPress={adAvailable ? handleWatchAd : undefined}
            disabled={!adAvailable}
            style={({ pressed }) => [
              styles.watchAdBtn,
              !adAvailable && { opacity: 0.45 },
              pressed && adAvailable && { opacity: 0.85, transform: [{ scale: 0.97 }] },
            ]}
            testID="watch-ad-btn"
          >
            <MaterialCommunityIcons name="play-circle-outline" size={24} color={Colors.background} />
            <Text style={styles.watchAdBtnText}>
              {adAvailable ? "WATCH AD TO CONTINUE" : adLoading ? "LOADING AD..." : "AD UNAVAILABLE"}
            </Text>
          </Pressable>

          <Pressable
            onPress={handleNoThanks}
            style={({ pressed }) => [styles.noThanksBtn, pressed && { opacity: 0.7 }]}
          >
            <Text style={styles.noThanksBtnText}>No Thanks</Text>
          </Pressable>
        </Animated.View>
      )}

      {gameState === "dead" && (
        <Animated.View
          entering={FadeIn.duration(400)}
          style={[styles.deathOverlay, { paddingTop: topPad + 40, paddingBottom: bottomPad + 20 }]}
        >
          <Text style={styles.deathLabel}>DESTROYED</Text>
          <Text style={styles.deathScore}>{finalScore}</Text>
          <Text style={styles.deathTime}>survived {finalTime}s</Text>

          {isNewHigh && (
            <Animated.Text entering={FadeIn.delay(200)} style={styles.newHighText}>
              NEW HIGH SCORE
            </Animated.Text>
          )}

          {!isNewHigh && stats.highScore > 0 && (
            <Text style={styles.bestText}>best: {stats.highScore}</Text>
          )}

          <View style={styles.deathButtons}>
            <Pressable
              onPress={handleRetry}
              style={({ pressed }) => [styles.retryBtn, pressed && { opacity: 0.85, transform: [{ scale: 0.97 }] }]}
              testID="retry-btn"
            >
              <Ionicons name="refresh" size={22} color={Colors.background} />
              <Text style={styles.retryBtnText}>RETRY</Text>
            </Pressable>

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
  hudScore: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 24,
    color: Colors.white,
  },
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
    marginBottom: 20,
  },
  bestText: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
    marginBottom: 20,
  },
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
