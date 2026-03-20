package io.dodge.android.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdMobManager(private val activity: Activity) {

    companion object {
        private const val TAG = "AdMobManager"

        // false = simulated text ads only, true = real Google ads
        const val USE_REAL_ADS = true

        // Google test rewarded ad unit (always used until you have a real ad unit)
        private const val TEST_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"

        // Replace with your real ad unit ID for production, then set IS_PRODUCTION = true
        private const val PROD_AD_UNIT = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        private const val IS_PRODUCTION = false

        private val AD_UNIT_ID = if (IS_PRODUCTION) PROD_AD_UNIT else TEST_AD_UNIT
    }

    private var rewardedAd: RewardedAd? = null
    private val _adAvailable = MutableStateFlow(false)
    val adAvailable: StateFlow<Boolean> = _adAvailable

    fun initialize() {
        MobileAds.initialize(activity) {
            Log.d(TAG, "AdMob initialized")
            loadAd()
        }
    }

    private fun loadAd() {
        Log.d(TAG, "Loading ad: $AD_UNIT_ID")
        RewardedAd.load(
            activity,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    _adAvailable.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Ad failed to load: ${error.message} (code ${error.code})")
                    rewardedAd = null
                    _adAvailable.value = false
                    activity.window.decorView.postDelayed({ loadAd() }, 5000)
                }
            }
        )
    }

    fun showAd(onRewarded: () -> Unit): Boolean {
        val ad = rewardedAd ?: run {
            Log.d(TAG, "No ad loaded, returning false")
            return false
        }
        ad.show(activity) { _ ->
            Log.d(TAG, "User earned reward")
            onRewarded()
        }
        rewardedAd = null
        _adAvailable.value = false
        loadAd()
        return true
    }
}
