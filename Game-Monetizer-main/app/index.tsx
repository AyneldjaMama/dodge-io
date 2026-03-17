import { useCallback, useEffect, useRef } from "react";
import {
  View,
  Text,
  Pressable,
  StyleSheet,
  Dimensions,
  Platform,
} from "react-native";
import { router } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons, MaterialCommunityIcons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  withSequence,
  Easing,
  withDelay,
} from "react-native-reanimated";
import Colors from "@/constants/colors";
import { useGame } from "@/contexts/GameContext";

const { width } = Dimensions.get("window");

export default function MenuScreen() {
  const insets = useSafeAreaInsets();
  const { stats, isLoaded, hasDoneToday } = useGame();
  const titleGlow = useSharedValue(0.3);
  const dotPulse = useSharedValue(1);
  const buttonScale = useSharedValue(1);

  useEffect(() => {
    titleGlow.value = withRepeat(
      withTiming(1, { duration: 2000, easing: Easing.inOut(Easing.sin) }),
      -1,
      true
    );
    dotPulse.value = withRepeat(
      withSequence(
        withTiming(1.2, { duration: 800 }),
        withTiming(1, { duration: 800 })
      ),
      -1,
      false
    );
    buttonScale.value = withDelay(
      500,
      withRepeat(
        withSequence(
          withTiming(1.03, { duration: 1500, easing: Easing.inOut(Easing.sin) }),
          withTiming(1, { duration: 1500, easing: Easing.inOut(Easing.sin) })
        ),
        -1,
        false
      )
    );
  }, []);

  const titleGlowStyle = useAnimatedStyle(() => ({
    textShadowRadius: 30 + titleGlow.value * 20,
  }));

  const dotStyle = useAnimatedStyle(() => ({
    transform: [{ scale: dotPulse.value }],
  }));

  const playBtnStyle = useAnimatedStyle(() => ({
    transform: [{ scale: buttonScale.value }],
  }));

  const handlePlay = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    router.push("/game");
  }, []);

  const handleDaily = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    router.push("/daily");
  }, []);

  const handleStats = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    router.push("/stats");
  }, []);

  const handleLeaderboard = useCallback(() => {
    if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    router.push("/leaderboard");
  }, []);

  const topPad = Platform.OS === "web" ? 67 : insets.top;
  const bottomPad = Platform.OS === "web" ? 34 : insets.bottom;
  const dailyDone = hasDoneToday();

  return (
    <View style={[styles.container, { paddingTop: topPad + 20, paddingBottom: bottomPad + 20 }]}>
      <View style={styles.topBar}>
        <Pressable onPress={handleLeaderboard} style={styles.iconBtn} testID="leaderboard-btn">
          <Ionicons name="trophy" size={24} color={Colors.textSecondary} />
        </Pressable>
        <Pressable onPress={handleStats} style={styles.iconBtn} testID="stats-btn">
          <Ionicons name="stats-chart" size={24} color={Colors.textSecondary} />
        </Pressable>
      </View>

      <View style={styles.titleArea}>
        <View style={styles.titleRow}>
          <Animated.Text style={[styles.title, titleGlowStyle]}>DODGE</Animated.Text>
          <Animated.View style={dotStyle}>
            <Text style={styles.titleDot}>.IO</Text>
          </Animated.View>
        </View>
        <Text style={styles.subtitle}>drag to move. dodge everything. survive.</Text>
      </View>

      <View style={styles.buttonArea}>
        <Animated.View style={playBtnStyle}>
          <Pressable
            onPress={handlePlay}
            style={({ pressed }) => [styles.playBtn, pressed && styles.playBtnPressed]}
            testID="play-btn"
          >
            <Ionicons name="play" size={28} color={Colors.background} />
            <Text style={styles.playBtnText}>PLAY</Text>
          </Pressable>
        </Animated.View>

        <Pressable
          onPress={handleDaily}
          style={({ pressed }) => [styles.dailyBtn, pressed && styles.dailyBtnPressed]}
          testID="daily-btn"
        >
          <MaterialCommunityIcons name="calendar-today" size={22} color={Colors.neonCyan} />
          <Text style={styles.dailyBtnText}>DAILY CHALLENGE</Text>
          {dailyDone && (
            <View style={styles.doneBadge}>
              <Ionicons name="checkmark" size={12} color={Colors.background} />
            </View>
          )}
        </Pressable>

        {stats.highScore > 0 && (
          <View style={styles.highScoreRow}>
            <Text style={styles.highScoreLabel}>BEST</Text>
            <Text style={styles.highScoreValue}>{stats.highScore}</Text>
          </View>
        )}
      </View>

      <View style={styles.legendArea}>
        <Text style={styles.legendTitle}>THREATS</Text>
        <View style={styles.legendGrid}>
          <LegendItem color={Colors.neonPink} label="Bullets" />
          <LegendItem color={Colors.neonOrange} label="Seekers" />
          <LegendItem color={Colors.neonYellow} label="Waves" />
          <LegendItem color={Colors.neonRed} label="Bombers" />
          <LegendItem color={Colors.neonCyan} label="Lasers" />
          <LegendItem color={Colors.neonPurple} label="Spirals" />
          <LegendItem color="#e0e0ff" label="Splitters" />
          <LegendItem color="#ff66ff" label="Teleporters" />
        </View>
        <Text style={styles.legendHint}>near misses = bonus points + streak multiplier</Text>
      </View>
    </View>
  );
}

function LegendItem({ color, label }: { color: string; label: string }) {
  return (
    <View style={styles.legendItem}>
      <View style={[styles.legendDot, { backgroundColor: color, shadowColor: color }]} />
      <Text style={styles.legendText}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: "center" as const,
    justifyContent: "space-between" as const,
  },
  topBar: {
    width: "100%",
    flexDirection: "row" as const,
    justifyContent: "flex-end" as const,
    paddingHorizontal: 20,
    gap: 4,
  },
  iconBtn: {
    width: 44,
    height: 44,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  titleArea: {
    alignItems: "center" as const,
  },
  titleRow: {
    flexDirection: "row" as const,
    alignItems: "baseline" as const,
  },
  title: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: Math.min(width * 0.15, 64),
    color: Colors.neonGreen,
    textShadowColor: Colors.neonGreen,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 40,
  },
  titleDot: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: Math.min(width * 0.15, 64),
    color: Colors.neonPink,
    textShadowColor: Colors.neonPink,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 20,
  },
  subtitle: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
    marginTop: 8,
  },
  buttonArea: {
    alignItems: "center" as const,
    gap: 16,
    width: "100%",
    paddingHorizontal: 40,
  },
  playBtn: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 10,
    backgroundColor: Colors.neonGreen,
    paddingVertical: 16,
    paddingHorizontal: 48,
    borderRadius: 16,
    width: "100%",
    maxWidth: 300,
  },
  playBtnPressed: {
    opacity: 0.85,
    transform: [{ scale: 0.97 }],
  },
  playBtnText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 20,
    color: Colors.background,
    letterSpacing: 3,
  },
  dailyBtn: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 10,
    borderWidth: 1.5,
    borderColor: Colors.neonCyan,
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 14,
    width: "100%",
    maxWidth: 300,
  },
  dailyBtnPressed: {
    opacity: 0.7,
    backgroundColor: "rgba(0,212,255,0.05)",
  },
  dailyBtnText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 14,
    color: Colors.neonCyan,
    letterSpacing: 2,
  },
  doneBadge: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: Colors.neonGreen,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  highScoreRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 8,
    marginTop: 8,
  },
  highScoreLabel: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 12,
    color: Colors.textMuted,
    letterSpacing: 2,
  },
  highScoreValue: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 18,
    color: Colors.neonYellow,
    textShadowColor: Colors.neonYellow,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  legendArea: {
    alignItems: "center" as const,
    paddingHorizontal: 30,
  },
  legendTitle: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 11,
    color: Colors.textMuted,
    letterSpacing: 3,
    marginBottom: 10,
  },
  legendGrid: {
    flexDirection: "row" as const,
    flexWrap: "wrap" as const,
    justifyContent: "center" as const,
    gap: 6,
  },
  legendItem: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 4,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 4,
    elevation: 2,
  },
  legendText: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 11,
    color: Colors.textMuted,
  },
  legendHint: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 11,
    color: "rgba(255,255,255,0.15)",
    marginTop: 10,
    textAlign: "center" as const,
  },
});
