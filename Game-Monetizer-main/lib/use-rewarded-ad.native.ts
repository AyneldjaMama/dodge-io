import { useState, useEffect, useCallback, useRef } from "react";
import Constants from "expo-constants";
import { ADMOB_REWARDED_AD_UNIT_ID } from "@/constants/ads";

const isExpoGo = Constants.appOwnership === "expo";

let MobileAdsModule: any = null;
let RewardedAdClass: any = null;
let RewardedAdEventTypeEnum: any = null;
let AdEventTypeEnum: any = null;

if (!isExpoGo) {
  try {
    const admob = require("react-native-google-mobile-ads");
    MobileAdsModule = admob.default;
    RewardedAdClass = admob.RewardedAd;
    RewardedAdEventTypeEnum = admob.RewardedAdEventType;
    AdEventTypeEnum = admob.AdEventType;
  } catch {}
}

const canShowAds = !isExpoGo && RewardedAdClass != null;

export function useRewardedAd() {
  const [adLoaded, setAdLoaded] = useState(false);
  const [adLoading, setAdLoading] = useState(false);
  const adRef = useRef<any>(null);
  const unsubscribersRef = useRef<(() => void)[]>([]);
  const rewardCallbackRef = useRef<(() => void) | null>(null);
  const initialized = useRef(false);

  const cleanupListeners = useCallback(() => {
    unsubscribersRef.current.forEach((unsub) => unsub());
    unsubscribersRef.current = [];
  }, []);

  const loadAd = useCallback(() => {
    if (!canShowAds) return;

    cleanupListeners();
    setAdLoaded(false);
    setAdLoading(true);

    const ad = RewardedAdClass.createForAdRequest(ADMOB_REWARDED_AD_UNIT_ID, {
      requestNonPersonalizedAdsOnly: true,
    });

    const unsubs: (() => void)[] = [];

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.LOADED, () => {
        setAdLoaded(true);
        setAdLoading(false);
      })
    );

    unsubs.push(
      ad.addAdEventListener(RewardedAdEventTypeEnum.EARNED_REWARD, () => {
        if (rewardCallbackRef.current) {
          rewardCallbackRef.current();
          rewardCallbackRef.current = null;
        }
      })
    );

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.CLOSED, () => {
        setAdLoaded(false);
        loadAd();
      })
    );

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.ERROR, (error: any) => {
        console.warn("[RewardedAd] Failed to load:", error?.message || error?.code || error);
        setAdLoading(false);
        setAdLoaded(false);
        setTimeout(loadAd, 5000);
      })
    );

    unsubscribersRef.current = unsubs;
    ad.load();
    adRef.current = ad;
  }, [cleanupListeners]);

  useEffect(() => {
    if (!canShowAds || initialized.current) return;
    initialized.current = true;

    MobileAdsModule()
      .initialize()
      .then(() => loadAd())
      .catch(() => {});

    return () => {
      cleanupListeners();
    };
  }, [loadAd, cleanupListeners]);

  const showAd = useCallback(
    (onRewarded: () => void): Promise<boolean> => {
      if (canShowAds && adLoaded && adRef.current) {
        rewardCallbackRef.current = onRewarded;
        try {
          adRef.current.show();
        } catch (e) {
          console.warn("[RewardedAd] Failed to show:", e);
          // Ad failed to show — give reward anyway so user isn't stuck
          rewardCallbackRef.current = null;
          onRewarded();
        }
        return Promise.resolve(true);
      }

      if (!canShowAds) {
        // Web/Expo Go fallback — simulate ad
        return new Promise((resolve) => {
          setTimeout(() => {
            onRewarded();
            resolve(true);
          }, 1500);
        });
      }

      // Native but ad not loaded — don't give free reward
      console.warn("[RewardedAd] Ad not loaded, cannot show");
      return Promise.resolve(false);
    },
    [adLoaded]
  );

  return {
    adAvailable: canShowAds ? adLoaded : true,
    adLoading,
    showAd,
  };
}
