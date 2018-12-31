package io.resana;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.resana.FileManager.Delegate;

class SplashAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "SplashAdProvider";

    private static SplashAdProvider instance;

    private Context appContext;

    private int adsQueueLength;
    private List<Ad> ads;
    private List<String> downloadedAds;

    private WeakReference<SplashAdView> adViewerRef;

    private SplashAdProvider(Context context) {
        this.appContext = context.getApplicationContext();
        this.adsQueueLength = 4;
        ads = Collections.synchronizedList(new ArrayList<Ad>());
        downloadedAds = Collections.synchronizedList(new ArrayList<String>());
        if (ResanaConfig.gettingSplashAds(appContext))
            NetworkManager.getInstance().getSplashAds(new AdsReceivedDelegate(appContext));
        else ResanaLog.e(TAG, "SplashAdProvider: splashAd is not mentioned in resana config");
    }

    static SplashAdProvider getInstance(Context context) {
        SplashAdProvider localInstance = instance;
        synchronized (SplashAdProvider.class) {
            localInstance = instance;
            if (localInstance == null) {
                localInstance = instance = new SplashAdProvider(context);
            }
        }
        return localInstance;
    }

    boolean isAdAvailable() {
        ResanaLog.d(TAG, "isAdAvailable: ");
        return downloadedAds == null || downloadedAds.size() > 0;
    }

    void attachViewer(SplashAdView adView) {
        ResanaLog.d(TAG, "attachViewer: ");
        adViewerRef = new WeakReference<>(adView);
        serveViewerIfPossible();
    }

    void detachViewer(SplashAdView adView) {
        ResanaLog.d(TAG, "detachViewer: ");
        if (adViewerRef != null && adView == adViewerRef.get())
            adViewerRef = null;
    }

    void releaseAd(Ad ad) {
        ResanaLog.d(TAG, "releaseAd: ");
    }

    /**
     * This function is called when new ads are received from sever.
     * new ads that are received will store in a list.
     *
     * @param items
     */
    private void newAdsReceived(List<Ad> items) {
        pruneAds(items);
        ResanaLog.d(TAG, "newAdsReceived: ads size=" + items.size());
        for (Ad item : items) {
            if (ads.size() >= adsQueueLength)
                return;
            if (item.data.hot)
                ads.add(0, item);
            else ads.add(item);
        }
        downloadFirstAdOfList();
    }

    private void pruneAds(List<Ad> ads) {
        ResanaLog.d(TAG, "pruneAds:");
        List<Ad> toRemove = new ArrayList<>();
        for (Ad ad : ads) {
            if (ad.isInvalid(appContext))
                toRemove.add(ad);
        }
        for (Ad ad : toRemove) {
            ads.remove(ad);
        }
    }

    private void downloadFirstAdOfList() {
        final Ad ad = ads.get(0);
        downloadAdFiles(ad, new Delegate() {
            @Override
            void onFinish(boolean success, Object... args) {
                ResanaLog.e(TAG, "downloadFirstAdOfList: success=" + success + " ad=" + ad.getId());
                if (success)
                    addToDownloadedAds(ad);
                else {
                    ads.remove(ad);
                    downloadFirstAdOfList();
                }
            }
        });
    }

    private void downloadAdFiles(final Ad ad, Delegate delegate) {
        FileManager.getInstance(appContext).downloadAdFiles(ad, delegate);
    }

    private void addToDownloadedAds(Ad ad) {
        ResanaLog.d(TAG, "addToDownloadedAds: ad=" + ad.getId());
        downloadedAds.add(ad.getId());
    }

    private boolean isAdDownloaded(Ad ad) {
        return downloadedAds.contains(ad.getId());
    }

    private void serveViewerIfPossible() {
        if (!ResanaConfig.gettingSplashAds(appContext)) {
            ResanaLog.e(TAG, "getAd: splashAd is not mentioned in resana config");
            return;
        }
        final SplashAdView viewer = adViewerRef.get();
        if (shouldCoolDownSplashViewing()) {
            viewer.cancelShowingAd("Cool Down Showing Splash.");
        } else {
            final Ad ad = getNextReadyToRenderAd();
            if (ad != null) {
                viewer.startShowingAd(ad);
                AdVersionKeeper.adRendered(ad);
            } else {
                viewer.cancelShowingAd("No Splash Ad Available.");
            }
        }
        adViewerRef = null;
    }

    private Ad getNextReadyToRenderAd() {
        Ad ad = ads.get(0);
        if (isAdDownloaded(ad)) {
            ResanaLog.d(TAG, "getNextReadyToRenderAd: ad=" + ad.getId());
            ads.remove(ad);
            if (ad.data.hot)
                ads.add(0, ad);
            else ads.add(ad);
            return ad;
        } else {
            ResanaLog.e(TAG, "getNextReadyToRenderAd: ad " + ad.getId() + " is not downloaded");
            ads.remove(ad);
            downloadFirstAdOfList();
            return null;
        }
    }

    private boolean shouldCoolDownSplashViewing() {
        ResanaLog.d(TAG, "shouldCoolDownSplashViewing: ");
        return !CoolDownHelper.shouldShowSplash(appContext);
    }

    private boolean shouldServeViewer() {
        ResanaLog.d(TAG, "shouldServeViewer: ");
        return adViewerRef != null && adViewerRef.get() != null;
    }

    private static class AdsReceivedDelegate extends Delegate {
        Context context;

        AdsReceivedDelegate(Context context) {
            this.context = context;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            if (success)
                SplashAdProvider.getInstance(context).newAdsReceived((List<Ad>) args[0]);
        }
    }
}
