/**
 * Simulated rewarded-ad overlay.
 * Shows a Google-style text ad with a countdown timer, then auto-closes.
 * Used when real AdMob ads aren't loaded yet (e.g. pre-review builds).
 */
import { useEffect, useState } from "react";
import { View, Text, Pressable, StyleSheet } from "react-native";
import Animated, { FadeIn, FadeOut } from "react-native-reanimated";
import { Ionicons } from "@expo/vector-icons";

interface SimulatedAdOverlayProps {
  visible: boolean;
  /** Called when the ad "completes" (countdown finished and user closes) */
  onComplete: () => void;
  durationSeconds?: number;
}

const AD_SAMPLES = [
  { headline: "Galaxy Quest — Space RPG", body: "Explore 200+ planets. Free to play.", url: "galaxyquest.example.com" },
  { headline: "FitTrack Pro — Step Counter", body: "Track steps, calories & sleep. 4.8★", url: "fittrackpro.example.com" },
  { headline: "BrainTeaser Puzzles", body: "1000+ puzzles to sharpen your mind.", url: "brainteaser.example.com" },
  { headline: "SnapRecipe — Cook Anything", body: "Scan ingredients, get recipes instantly.", url: "snaprecipe.example.com" },
];

export default function SimulatedAdOverlay({
  visible,
  onComplete,
  durationSeconds = 5,
}: SimulatedAdOverlayProps) {
  const [remaining, setRemaining] = useState(durationSeconds);
  const [canClose, setCanClose] = useState(false);
  const [ad] = useState(() => AD_SAMPLES[Math.floor(Math.random() * AD_SAMPLES.length)]);

  useEffect(() => {
    if (!visible) {
      setRemaining(durationSeconds);
      setCanClose(false);
      return;
    }

    const interval = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setCanClose(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [visible, durationSeconds]);

  if (!visible) return null;

  return (
    <Animated.View entering={FadeIn.duration(200)} exiting={FadeOut.duration(200)} style={styles.overlay}>
      {/* Top bar */}
      <View style={styles.topBar}>
        <Text style={styles.adLabel}>Ad</Text>
        <Text style={styles.timerText}>
          {canClose ? "" : `Closes in ${remaining}s`}
        </Text>
        {canClose && (
          <Pressable onPress={onComplete} style={styles.closeBtn}>
            <Ionicons name="close" size={20} color="#fff" />
          </Pressable>
        )}
      </View>

      {/* Ad content — Google text-ad style */}
      <View style={styles.adCard}>
        <Text style={styles.adBadge}>Sponsored</Text>
        <Text style={styles.adHeadline}>{ad.headline}</Text>
        <Text style={styles.adBody}>{ad.body}</Text>
        <Text style={styles.adUrl}>{ad.url}</Text>
      </View>

      {/* Bottom info */}
      <Text style={styles.bottomText}>
        Thanks for supporting the game!
      </Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "#111",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 999,
  },
  topBar: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    height: 56,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    backgroundColor: "#1a1a1a",
  },
  adLabel: {
    fontSize: 12,
    color: "#888",
    fontWeight: "600",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  timerText: {
    fontSize: 13,
    color: "#aaa",
    fontWeight: "500",
  },
  closeBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: "rgba(255,255,255,0.15)",
    alignItems: "center",
    justifyContent: "center",
  },
  adCard: {
    backgroundColor: "#1e1e2e",
    borderRadius: 16,
    padding: 28,
    width: "85%",
    maxWidth: 340,
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.08)",
  },
  adBadge: {
    fontSize: 10,
    color: "#888",
    fontWeight: "600",
    letterSpacing: 1,
    textTransform: "uppercase",
    marginBottom: 12,
  },
  adHeadline: {
    fontSize: 22,
    color: "#fff",
    fontWeight: "700",
    marginBottom: 8,
  },
  adBody: {
    fontSize: 15,
    color: "#bbb",
    lineHeight: 22,
    marginBottom: 12,
  },
  adUrl: {
    fontSize: 13,
    color: "#4a9eff",
  },
  bottomText: {
    position: "absolute",
    bottom: 40,
    fontSize: 13,
    color: "#555",
  },
});
