import { createContext, useContext, useState, useEffect, useMemo, ReactNode, useCallback } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";

interface GameStats {
  totalGames: number;
  highScore: number;
  bestTime: number;
  totalNearMisses: number;
  totalShieldsUsed: number;
  totalAdsWatched: number;
  dailyChallengeStreak: number;
  lastDailyDate: string;
  dailyHighScore: number;
  dailyBestTime: number;
}

interface GameContextValue {
  stats: GameStats;
  isLoaded: boolean;
  recordGame: (score: number, time: number) => Promise<void>;
  recordDailyGame: (score: number, time: number) => Promise<void>;
  recordNearMiss: (count?: number) => void;
  recordShieldUsed: () => void;
  recordAdWatched: () => Promise<void>;
  resetStats: () => Promise<void>;
  getDailySeed: () => string;
  hasDoneToday: () => boolean;
}

const DEFAULT_STATS: GameStats = {
  totalGames: 0,
  highScore: 0,
  bestTime: 0,
  totalNearMisses: 0,
  totalShieldsUsed: 0,
  totalAdsWatched: 0,
  dailyChallengeStreak: 0,
  lastDailyDate: "",
  dailyHighScore: 0,
  dailyBestTime: 0,
};

const STORAGE_KEY = "dodgeio-stats";

const GameContext = createContext<GameContextValue | null>(null);

export function GameProvider({ children }: { children: ReactNode }) {
  const [stats, setStats] = useState<GameStats>(DEFAULT_STATS);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY).then((raw) => {
      if (raw) {
        try {
          setStats({ ...DEFAULT_STATS, ...JSON.parse(raw) });
        } catch {}
      }
      setIsLoaded(true);
    });
  }, []);

  const persistUpdate = useCallback((updater: (prev: GameStats) => GameStats) => {
    setStats((prev) => {
      const next = updater(prev);
      AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  const recordGame = useCallback(
    (score: number, time: number) => {
      persistUpdate((prev) => ({
        ...prev,
        totalGames: prev.totalGames + 1,
        highScore: Math.max(prev.highScore, score),
        bestTime: Math.max(prev.bestTime, time),
      }));
    },
    [persistUpdate]
  );

  const recordDailyGame = useCallback(
    (score: number, time: number) => {
      const today = getDailySeed();
      persistUpdate((prev) => {
        const next = { ...prev, totalGames: prev.totalGames + 1 };

        if (next.lastDailyDate !== today) {
          const yesterday = getPreviousDaySeed();
          next.dailyChallengeStreak =
            next.lastDailyDate === yesterday ? next.dailyChallengeStreak + 1 : 1;
          next.lastDailyDate = today;
          next.dailyHighScore = score;
          next.dailyBestTime = time;
        } else {
          next.dailyHighScore = Math.max(next.dailyHighScore, score);
          next.dailyBestTime = Math.max(next.dailyBestTime, time);
        }

        next.highScore = Math.max(next.highScore, score);
        next.bestTime = Math.max(next.bestTime, time);
        return next;
      });
    },
    [persistUpdate]
  );

  const recordNearMiss = useCallback((count: number = 1) => {
    persistUpdate((prev) => ({
      ...prev,
      totalNearMisses: prev.totalNearMisses + count,
    }));
  }, [persistUpdate]);

  const recordShieldUsed = useCallback(() => {
    persistUpdate((prev) => ({
      ...prev,
      totalShieldsUsed: prev.totalShieldsUsed + 1,
    }));
  }, [persistUpdate]);

  const recordAdWatched = useCallback(async () => {
    persistUpdate((prev) => ({
      ...prev,
      totalAdsWatched: prev.totalAdsWatched + 1,
    }));
  }, [persistUpdate]);

  const resetStats = useCallback(async () => {
    setStats(DEFAULT_STATS);
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_STATS));
  }, []);

  const value = useMemo(
    () => ({
      stats,
      isLoaded,
      recordGame,
      recordDailyGame,
      recordNearMiss,
      recordShieldUsed,
      recordAdWatched,
      resetStats,
      getDailySeed,
      hasDoneToday: () => stats.lastDailyDate === getDailySeed(),
    }),
    [stats, isLoaded, recordGame, recordDailyGame, recordNearMiss, recordShieldUsed, recordAdWatched, resetStats]
  );

  return <GameContext.Provider value={value}>{children}</GameContext.Provider>;
}

export function useGame() {
  const ctx = useContext(GameContext);
  if (!ctx) throw new Error("useGame must be used within GameProvider");
  return ctx;
}

function getDailySeed(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function getPreviousDaySeed(): string {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
