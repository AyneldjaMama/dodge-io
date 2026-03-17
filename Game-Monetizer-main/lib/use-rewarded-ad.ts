import { useCallback } from "react";

export function useRewardedAd() {
  const showAd = useCallback(
    (onRewarded: () => void): Promise<boolean> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          onRewarded();
          resolve(true);
        }, 1500);
      });
    },
    []
  );

  return {
    adAvailable: true,
    adLoading: false,
    showAd,
  };
}
