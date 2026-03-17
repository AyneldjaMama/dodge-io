import { useState, useEffect, useCallback, useRef } from "react";
import Constants from "expo-constants";
import { ADMOB_INTERSTITIAL_AD_UNIT_ID, INTERSTITIAL_INTERVAL } from "@/constants/ads";

const isExpoGo = Constants.appOwnership === "expo";

let MobileAdsModule: any = null;
let InterstitialAdClass: any = null;
let AdEventTypeEnum: any = null;

if (!isExpoGo) {
  try {
    const admob = require("react-native-google-mobile-ads");
    MobileAdsModule = admob.default;
    InterstitialAdClass = admob.InterstitialAd;
    AdEventTypeEnum = admob.AdEventType;
  } catch {}
}

const canShowAds = !isExpoGo && InterstitialAdClass != null;

export function useInterstitialAd() {
  const [adLoaded, setAdLoaded] = useState(false);
  const adRef = useRef<any>(null);
  const unsubscribersRef = useRef<(() => void)[]>([]);
  const deathCountRef = useRef(0);
  const closedResolveRef = useRef<(() => void) | null>(null);
  const initialized = useRef(false);

  const cleanupListeners = useCallback(() => {
    unsubscribersRef.current.forEach((unsub) => unsub());
    unsubscribersRef.current = [];
  }, []);

  const loadAd = useCallback(() => {
    if (!canShowAds) return;

    cleanupListeners();
    setAdLoaded(false);

    const ad = InterstitialAdClass.createForAdRequest(ADMOB_INTERSTITIAL_AD_UNIT_ID, {
      requestNonPersonalizedAdsOnly: true,
    });

    const unsubs: (() => void)[] = [];

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.LOADED, () => {
        setAdLoaded(true);
      })
    );

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.CLOSED, () => {
        if (closedResolveRef.current) {
          closedResolveRef.current();
          closedResolveRef.current = null;
        }
        setAdLoaded(false);
        loadAd();
      })
    );

    unsubs.push(
      ad.addAdEventListener(AdEventTypeEnum.ERROR, (error: any) => {
        console.warn("[InterstitialAd] Failed to load:", error?.message || error?.code || error);
        if (closedResolveRef.current) {
          closedResolveRef.current();
          closedResolveRef.current = null;
        }
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

  const maybeShowInterstitial = useCallback(
    (): Promise<void> => {
      deathCountRef.current += 1;

      if (deathCountRef.current % INTERSTITIAL_INTERVAL !== 0) {
        return Promise.resolve();
      }

      if (canShowAds && adLoaded && adRef.current) {
        return new Promise<void>((resolve) => {
          closedResolveRef.current = resolve;
          adRef.current.show();
        });
      }

      return Promise.resolve();
    },
    [adLoaded]
  );

  return { maybeShowInterstitial };
}
