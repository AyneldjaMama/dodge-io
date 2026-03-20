package io.dodge.android.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdMobManager(private val activity: Activity) {

    companion object {
        // Google test ad unit ID
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private var rewardedAd: RewardedAd? = null
    private val _adAvailable = MutableStateFlow(false)
    val adAvailable: StateFlow<Boolean> = _adAvailable

    fun initialize() {
        MobileAds.initialize(activity) {}
        loadAd()
    }

    private fun loadAd() {
        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adAvailable.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _adAvailable.value = false
                    // Retry after 3s
                    activity.window.decorView.postDelayed({ loadAd() }, 3000)
                }
            }
        )
    }

    /**
     * Shows a rewarded ad. Returns true if the ad was shown and the user earned the reward.
     */
    fun showAd(onRewarded: () -> Unit): Boolean {
        val ad = rewardedAd ?: return false
        ad.show(activity) { _ ->
            onRewarded()
        }
        rewardedAd = null
        _adAvailable.value = false
        loadAd() // Pre-load next ad
        return true
    }
}
