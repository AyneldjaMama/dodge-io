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

  const loadTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadAd = useCallback(() => {
    if (!canShowAds) return;

    cleanupListeners();
    if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
    setAdLoaded(false);
    setAdLoading(true);

    // Safety timeout — if the ad SDK never responds, retry
    loadTimeoutRef.current = setTimeout(() => {
      setAdLoading(false);
      setAdLoaded(false);
      console.warn("[RewardedAd] Load timed out after 15s, retrying...");
      setTimeout(loadAd, 2000);
    }, 15000);

    const ad = RewardedAdClass.createForAdRequest(ADMOB_REWARDED_AD_UNIT_ID, {
      requestNonPersonalizedAdsOnly: true,
    });

    const unsubs: (() => void)[] = [];

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.LOADED, () => {
        if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
        console.log("[RewardedAd] Test ad loaded successfully");
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
        if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
        console.warn("[RewardedAd] Failed to load:", error?.message || error?.code || error);
        setAdLoading(false);
        setAdLoaded(false);
        // Retry after a short delay
        setTimeout(loadAd, 3000);
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
      .then(() => {
        console.log("[RewardedAd] SDK initialized, loading test ad...");
        loadAd();
      })
      .catch((e: any) => {
        console.warn("[RewardedAd] SDK init failed:", e);
      });

    return () => {
      cleanupListeners();
      if (loadTimeoutRef.current) clearTimeout(loadTimeoutRef.current);
    };
  }, [loadAd, cleanupListeners]);

  const showAd = useCallback(
    (onRewarded: () => void): Promise<boolean> => {
      // SDK available and ad loaded — show the real test video ad
      if (canShowAds && adLoaded && adRef.current) {
        rewardCallbackRef.current = onRewarded;
        try {
          adRef.current.show();
          return Promise.resolve(true);
        } catch (e) {
          console.warn("[RewardedAd] Failed to show:", e);
          rewardCallbackRef.current = null;
          // Fall through to return false so GameScreen shows simulated overlay
        }
      }

      // Ad not ready — return false so GameScreen can show simulated overlay
      return Promise.resolve(false);
    },
    [adLoaded]
  );

  return {
    adAvailable: canShowAds ? adLoaded : false,
    adLoading,
    showAd,
  };
}
