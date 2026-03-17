import { useCallback, useRef } from "react";
import { INTERSTITIAL_INTERVAL } from "@/constants/ads";

export function useInterstitialAd() {
  const deathCountRef = useRef(0);

  const maybeShowInterstitial = useCallback(
    (): Promise<void> => {
      deathCountRef.current += 1;
      if (deathCountRef.current % INTERSTITIAL_INTERVAL === 0) {
        return new Promise((resolve) => setTimeout(resolve, 1500));
      }
      return Promise.resolve();
    },
    []
  );

  return { maybeShowInterstitial };
}
