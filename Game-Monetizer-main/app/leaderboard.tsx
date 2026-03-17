import { useState, useEffect, useCallback } from "react";
import {
  View,
  Text,
  Pressable,
  StyleSheet,
  FlatList,
  Platform,
  TextInput,
  ActivityIndicator,
} from "react-native";
import { router } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons, MaterialCommunityIcons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import { useQuery, useMutation } from "@tanstack/react-query";
import Colors from "@/constants/colors";
import { getDeviceId, getDisplayName, setDisplayName } from "@/lib/device-id";
import { queryClient, apiRequest } from "@/lib/query-client";
import { useGame } from "@/contexts/GameContext";

interface LeaderboardEntry {
  id: string;
  deviceId: string;
  displayName: string;
  date: string;
  score: number;
  survivalTime: number;
}

export default function LeaderboardScreen() {
  const insets = useSafeAreaInsets();
  const { getDailySeed } = useGame();
  const today = getDailySeed();

  const topPad = Platform.OS === "web" ? 67 : insets.top;
  const bottomPad = Platform.OS === "web" ? 34 : insets.bottom;

  const [myDeviceId, setMyDeviceId] = useState("");
  const [myName, setMyName] = useState("");
  const [editingName, setEditingName] = useState(false);
  const [nameInput, setNameInput] = useState("");

  useEffect(() => {
    (async () => {
      const id = await getDeviceId();
      const name = await getDisplayName();
      setMyDeviceId(id);
      setMyName(name);
    })();
  }, []);

  const { data: leaderboard = [], isLoading } = useQuery<LeaderboardEntry[]>({
    queryKey: ["/api/daily-scores", today],
    enabled: !!today,
    staleTime: 30000,
    refetchInterval: 30000,
  });

  const handleSaveName = useCallback(async () => {
    const trimmed = nameInput.trim();
    if (trimmed.length > 0) {
      await setDisplayName(trimmed);
      setMyName(trimmed);
    }
    setEditingName(false);
  }, [nameInput]);

  const handleStartEdit = useCallback(() => {
    setNameInput(myName);
    setEditingName(true);
  }, [myName]);

  const myRank = leaderboard.findIndex((e) => e.deviceId === myDeviceId) + 1;
  const myBest = leaderboard.find((e) => e.deviceId === myDeviceId);

  const renderItem = useCallback(
    ({ item, index }: { item: LeaderboardEntry; index: number }) => {
      const rank = index + 1;
      const isMe = item.deviceId === myDeviceId;
      return (
        <View
          style={[
            styles.row,
            isMe && styles.rowMe,
            rank <= 3 && styles.rowTop3,
          ]}
        >
          <View style={styles.rankCol}>
            {rank === 1 && <Text style={styles.medal}>🥇</Text>}
            {rank === 2 && <Text style={styles.medal}>🥈</Text>}
            {rank === 3 && <Text style={styles.medal}>🥉</Text>}
            {rank > 3 && <Text style={styles.rankText}>{rank}</Text>}
          </View>
          <View style={styles.nameCol}>
            <Text
              style={[styles.nameText, isMe && styles.nameTextMe]}
              numberOfLines={1}
            >
              {item.displayName}
              {isMe ? " (you)" : ""}
            </Text>
          </View>
          <View style={styles.scoreCol}>
            <Text style={[styles.scoreText, rank <= 3 && styles.scoreTextTop3]}>
              {item.score}
            </Text>
            <Text style={styles.timeText}>{item.survivalTime}s</Text>
          </View>
        </View>
      );
    },
    [myDeviceId]
  );

  const todayLabel = new Date().toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

  return (
    <View style={[styles.container, { paddingTop: topPad }]}>
      <View style={styles.header}>
        <Pressable
          onPress={() => {
            if (Platform.OS !== "web")
              Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
            router.back();
          }}
          style={styles.backBtn}
        >
          <Ionicons
            name="chevron-back"
            size={24}
            color={Colors.textSecondary}
          />
        </Pressable>
        <Text style={styles.headerTitle}>LEADERBOARD</Text>
        <View style={{ width: 44 }} />
      </View>

      <View style={styles.dateRow}>
        <MaterialCommunityIcons
          name="calendar-today"
          size={16}
          color={Colors.neonCyan}
        />
        <Text style={styles.dateText}>{todayLabel}</Text>
      </View>

      <View style={styles.nameRow}>
        {editingName ? (
          <View style={styles.nameEditRow}>
            <TextInput
              style={styles.nameInput}
              value={nameInput}
              onChangeText={setNameInput}
              maxLength={32}
              autoFocus
              onSubmitEditing={handleSaveName}
              placeholder="Enter name..."
              placeholderTextColor={Colors.textMuted}
            />
            <Pressable onPress={handleSaveName} style={styles.saveBtn}>
              <Ionicons name="checkmark" size={20} color={Colors.neonGreen} />
            </Pressable>
          </View>
        ) : (
          <Pressable onPress={handleStartEdit} style={styles.nameDisplay}>
            <Text style={styles.myNameText}>{myName}</Text>
            <Ionicons
              name="pencil"
              size={14}
              color={Colors.textMuted}
            />
          </Pressable>
        )}
      </View>

      {myBest && (
        <View style={styles.myScoreCard}>
          <Text style={styles.myScoreLabel}>YOUR BEST TODAY</Text>
          <View style={styles.myScoreRow}>
            <Text style={styles.myScoreValue}>{myBest.score}</Text>
            <Text style={styles.myScoreTime}>{myBest.survivalTime}s</Text>
            {myRank > 0 && (
              <View style={styles.rankBadge}>
                <Text style={styles.rankBadgeText}>#{myRank}</Text>
              </View>
            )}
          </View>
        </View>
      )}

      {isLoading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={Colors.neonCyan} />
        </View>
      ) : leaderboard.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Ionicons
            name="trophy-outline"
            size={48}
            color={Colors.textMuted}
          />
          <Text style={styles.emptyText}>No scores yet today</Text>
          <Text style={styles.emptySubtext}>
            Play the daily challenge to be the first!
          </Text>
        </View>
      ) : (
        <FlatList
          data={leaderboard}
          renderItem={renderItem}
          keyExtractor={(item) => item.id}
          contentContainerStyle={[
            styles.listContent,
            { paddingBottom: bottomPad + 20 },
          ]}
          showsVerticalScrollIndicator={false}
          scrollEnabled={leaderboard.length > 0}
        />
      )}
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
  dateRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 6,
    marginBottom: 12,
  },
  dateText: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 13,
    color: Colors.neonCyan,
  },
  nameRow: {
    paddingHorizontal: 20,
    marginBottom: 12,
  },
  nameDisplay: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 8,
    paddingVertical: 8,
  },
  myNameText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 14,
    color: Colors.white,
  },
  nameEditRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 8,
  },
  nameInput: {
    flex: 1,
    maxWidth: 200,
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 14,
    color: Colors.white,
    borderWidth: 1,
    borderColor: Colors.neonCyan,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    textAlign: "center" as const,
  },
  saveBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surface,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  myScoreCard: {
    marginHorizontal: 20,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    alignItems: "center" as const,
    borderWidth: 1,
    borderColor: Colors.neonGreen + "20",
    marginBottom: 16,
  },
  myScoreLabel: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 10,
    color: Colors.textMuted,
    letterSpacing: 2,
    marginBottom: 6,
  },
  myScoreRow: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    gap: 12,
  },
  myScoreValue: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 28,
    color: Colors.neonGreen,
  },
  myScoreTime: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 14,
    color: Colors.textMuted,
  },
  rankBadge: {
    backgroundColor: Colors.neonCyan + "20",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  rankBadgeText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 13,
    color: Colors.neonCyan,
  },
  loadingContainer: {
    flex: 1,
    alignItems: "center" as const,
    justifyContent: "center" as const,
  },
  emptyContainer: {
    flex: 1,
    alignItems: "center" as const,
    justifyContent: "center" as const,
    gap: 8,
  },
  emptyText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 16,
    color: Colors.textSecondary,
    marginTop: 8,
  },
  emptySubtext: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 13,
    color: Colors.textMuted,
  },
  listContent: {
    paddingHorizontal: 16,
    gap: 4,
  },
  row: {
    flexDirection: "row" as const,
    alignItems: "center" as const,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 12,
    backgroundColor: Colors.surface,
  },
  rowMe: {
    borderWidth: 1,
    borderColor: Colors.neonGreen + "40",
    backgroundColor: Colors.neonGreen + "08",
  },
  rowTop3: {
    backgroundColor: Colors.surfaceLight,
  },
  rankCol: {
    width: 36,
    alignItems: "center" as const,
  },
  medal: {
    fontSize: 18,
  },
  rankText: {
    fontFamily: "SpaceGrotesk_600SemiBold",
    fontSize: 14,
    color: Colors.textMuted,
  },
  nameCol: {
    flex: 1,
    paddingHorizontal: 8,
  },
  nameText: {
    fontFamily: "SpaceGrotesk_500Medium",
    fontSize: 14,
    color: Colors.white,
  },
  nameTextMe: {
    color: Colors.neonGreen,
  },
  scoreCol: {
    alignItems: "flex-end" as const,
  },
  scoreText: {
    fontFamily: "SpaceGrotesk_700Bold",
    fontSize: 16,
    color: Colors.white,
  },
  scoreTextTop3: {
    color: Colors.neonYellow,
  },
  timeText: {
    fontFamily: "SpaceGrotesk_400Regular",
    fontSize: 11,
    color: Colors.textMuted,
  },
});
