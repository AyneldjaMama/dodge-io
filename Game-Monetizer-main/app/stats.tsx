import { View, Text, Pressable, StyleSheet, ScrollView, Platform } from "react-native";
import { router } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons, MaterialCommunityIcons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import Colors from "@/constants/colors";
import { useGame } from "@/contexts/GameContext";

export default function StatsScreen() {
  const insets = useSafeAreaInsets();
  const { stats } = useGame();

  const topPad = Platform.OS === "web" ? 67 : insets.top;
  const bottomPad = Platform.OS === "web" ? 34 : insets.bottom;

  return (
    <View style={[styles.container, { paddingTop: topPad }]}>
      <View style={styles.header}>
        <Pressable
          onPress={() => {
            if (Platform.OS !== "web") Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
            router.back();
          }}
          style={styles.backBtn}
        >
          <Ionicons name="chevron-back" size={24} color={Colors.textSecondary} />
        </Pressable>
        <Text style={styles.headerTitle}>STATS</Text>
        <View style={{ width: 44 }} />
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: bottomPad + 20 }]}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.heroCard}>
          <Text style={styles.heroLabel}>HIGH SCORE</Text>
          <Text style={styles.heroValue}>{stats.highScore}</Text>
          <Text style={styles.heroSub}>best survival: {stats.bestTime}s</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>GAMEPLAY</Text>
          <View style={styles.statsGrid}>
            <StatCard
              icon={<Ionicons name="game-controller" size={20} color={Colors.neonGreen} />}
              label="Games Played"
              value={stats.totalGames.toString()}
              accent={Colors.neonGreen}
            />
            <StatCard
              icon={<Ionicons name="flash" size={20} color={Colors.neonYellow} />}
              label="Near Misses"
              value={stats.totalNearMisses.toString()}
              accent={Colors.neonYellow}
            />
            <StatCard
              icon={<Ionicons name="shield" size={20} color={Colors.neonCyan} />}
              label="Shields Used"
              value={stats.totalShieldsUsed.toString()}
              accent={Colors.neonCyan}
            />
            <StatCard
              icon={<MaterialCommunityIcons name="play-circle" size={20} color={Colors.neonPink} />}
              label="Ads Watched"
              value={stats.totalAdsWatched.toString()}
              accent={Colors.neonPink}
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>DAILY CHALLENGE</Text>
          <View style={styles.dailyCard}>
            <View style={styles.dailyRow}>
              <View style={styles.dailyStat}>
                <Text style={styles.dailyStatValue}>{stats.dailyChallengeStreak}</Text>
                <View style={styles.dailyStatLabelRow}>
                  <Ionicons name="flame" size={14} color={Colors.neonOrange} />
                  <Text style={styles.dailyStatLabel}>Streak</Text>
                </View>
              </View>
              <View style={styles.dailyDivider} />
              <View style={styles.dailyStat}>
                <Text style={styles.dailyStatValue}>{stats.dailyHighScore}</Text>
                <Text style={styles.dailyStatLabel}>Today's Best</Text>
              </View>
              <View style={styles.dailyDivider} />
              <View style={styles.dailyStat}>
                <Text style={styles.dailyStatValue}>{stats.dailyBestTime}s</Text>
                <Text style={styles.dailyStatLabel}>Today's Time</Text>
              </View>
            </View>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

function StatCard({
  icon,
  label,
  value,
  accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  accent: string;
}) {
  return (
    <View style={[styles.statCard, { borderColor: accent + "15" }]}>
      {icon}
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "space-between" as const,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  backBtn: {
    width: 44,
    height: 44,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  headerTitle: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 16,
    color: Colors.textSecondary,
    letterSpacing: 4,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    gap: 24,
  },
  heroCard: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: 28,
    alignItems: "center" as const,
    borderWidth: 1,
    borderColor: Colors.neonGreen + "20",
  },
  heroLabel: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 11,
    color: Colors.textMuted,
    letterSpacing: 3,
    marginBottom: 8,
  },
  heroValue: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 48,
    color: Colors.neonGreen,
    textShadowColor: Colors.neonGreen,
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 20,
  },
  heroSub: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 13,
    color: Colors.textMuted,
    marginTop: 4,
  },
  section: {
    gap: 12,
  },
  sectionTitle: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 11,
    color: Colors.textMuted,
    letterSpacing: 3,
    paddingLeft: 4,
  },
  statsGrid: {
    flexDirection: "row" as const,
    flexWrap: "wrap" as const,
    gap: 12,
  },
  statCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    alignItems: "center" as const,
    gap: 6,
    width: "47%",
    flexGrow: 1,
    borderWidth: 1,
  },
  statValue: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 24,
    color: Colors.white,
  },
  statLabel: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 11,
    color: Colors.textMuted,
  },
  dailyCard: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.neonCyan + "15",
  },
  dailyRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "space-around" as const,
  },
  dailyStat: {
    alignItems: "center" as const,
    gap: 4,
    flex: 1,
  },
  dailyStatValue: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 22,
    color: Colors.white,
  },
  dailyStatLabelRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 4,
  },
  dailyStatLabel: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 11,
    color: Colors.textMuted,
  },
  dailyDivider: {
    width: 1,
    height: 36,
    backgroundColor: Colors.border,
  },
});
