package io.resana;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static io.resana.FileManager.DOWNLOADED_SPLASHES_FILE_NAME;
import static io.resana.FileManager.Delegate;
import static io.resana.FileManager.FileSpec;
import static io.resana.FileManager.PersistableObject;
import static io.resana.FileManager.SPLASHES_FILE_NAME;

class SplashAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "SplashAdProvider";

    private static SplashAdProvider instance;

    private Context appContext;

    private int adsQueueLength;
    private List<Ad> ads;
    private String adsFileName;
    private int downloadedAdsQueueLength;
    private PersistableObject<LinkedHashSet<Ad>> downloadedAds;
    private String downloadedAdsFileName;

    private int currentlyDownloadingAds;
    private boolean isLoadingCachedAds;
    private HashMap<String, Integer> locks = new HashMap<>();
    private List<Ad> toBeDeletedAds = new ArrayList<>();

    private List<Ad> waitingToRender = new ArrayList<>();
    private List<Ad> waitingToClick = new ArrayList<>();
    private List<Ad> waitingToLandingClick = new ArrayList<>();

    private boolean needsFlushCache;
    private WeakReference<SplashAdView> adViewerRef;

    private SplashAdProvider(Context context) {
        this.appContext = context.getApplicationContext();
        this.adsQueueLength = 4;
        ads = Collections.synchronizedList(new ArrayList<Ad>());
        this.adsFileName = SPLASHES_FILE_NAME;
        this.downloadedAdsQueueLength = 3;
        this.downloadedAdsFileName = DOWNLOADED_SPLASHES_FILE_NAME;
        NetworkManager.getInstance().getSplashAds(new AdsReceivedDelegate(context));
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
        return downloadedAds == null || downloadedAds.get().size() > 0;
    }

    void attachViewer(SplashAdView adView) {
        ResanaLog.d(TAG, "attachViewer: ");
        adViewerRef = new WeakReference<>(adView);
        updateAdQueues();
        serveViewerIfPossible();
    }

    void detachViewer(SplashAdView adView) {
        ResanaLog.d(TAG, "detachViewer: ");
        if (adViewerRef != null && adView == adViewerRef.get())
            adViewerRef = null;
    }

    void releaseAd(Ad ad) {
        ResanaLog.d(TAG, "releaseAd: ");
        unlockAdFiles(ad);
        garbageCollectAdFiles();
    }

    void flushCache() {
        ResanaLog.d(TAG, "flushCache: ");
        if (isLoadingCachedAds) {
            needsFlushCache = true;
            return;
        }
        needsFlushCache = false;
        ads.clear();
        final Iterator<Ad> itr = downloadedAds.get().iterator();
        Ad ad;
        while (itr.hasNext()) {
            ad = itr.next();
            unlockAdFiles(ad);
            toBeDeletedAds.add(ad);
            itr.remove();
        }
        downloadedAds.persist();
        garbageCollectAdFiles();
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
            if (item.data.hot)
                ads.add(0, item);
            else ads.add(item);
        }
        updateAdQueues();
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

    private PersistableObject<BoundedLinkedHashSet<Ad>> createAdsPersistableObject(BoundedLinkedHashSet<Ad> ads) {
        ResanaLog.d(TAG, "createAdsPersistableObject: ");
        if (ads == null)
            ads = new BoundedLinkedHashSet<>(adsQueueLength);
        return new PersistableObject<BoundedLinkedHashSet<Ad>>(ads) {
            @Override
            void onPersist() {
                final FileSpec file = new FileSpec(adsFileName);
                final BoundedLinkedHashSet<Ad> adsCopy = new BoundedLinkedHashSet<>(adsQueueLength, get());
                FileManager.getInstance(appContext).persistObjectToFile(adsCopy, file, new FilePersistedDelegate(this));
            }
        };
    }

    private PersistableObject<LinkedHashSet<Ad>> createDownloadedAdsPersistableObject() {
        ResanaLog.d(TAG, "createDownloadedAdsPersistableObject: ");
        return new PersistableObject<LinkedHashSet<Ad>>(new LinkedHashSet<Ad>(downloadedAdsQueueLength)) {
            @Override
            void onPersist() {
                final FileSpec file = new FileSpec(downloadedAdsFileName);
                final LinkedHashSet<Ad> adsCopy = new LinkedHashSet<>(get());
                FileManager.getInstance(appContext).persistObjectToFile(adsCopy, file, new FilePersistedDelegate(this));
            }
        };
    }

    private void addToDownloadedAds(Ad ad) {
        ResanaLog.d(TAG, "addToDownloadedAds: ");
        downloadedAds.get().add(ad);
        lockAdFiles(ad);
    }

    private void serveViewerIfPossible() {
        ResanaLog.d(TAG, "serveViewerIfPossible: isLoadingCachedAds=" + isLoadingCachedAds);
        if (isLoadingCachedAds)
            return;
        final SplashAdView viewer = adViewerRef.get();
        if (shouldCoolDownSplashViewing()) {
            viewer.cancelShowingAd("Cool Down Showing Splash.");
        } else {
            final Ad ad = getNextReadyToRenderAd();
            if (ad != null) {
                lockAdFiles(ad);
                viewer.startShowingAd(ad);
                AdVersionKeeper.adRendered(ad);
                roundRobinOnAds();
                waitingToRender.add(ad);
                downloadedAds.persist();
                updateAdQueues();
            } else {
                viewer.cancelShowingAd("No Splash Ad Available.");
            }
        }
        adViewerRef = null;
    }

    private Ad getNextReadyToRenderAd() {
        ResanaLog.d(TAG, "getNextReadyToRenderAd: ");
        final Iterator<Ad> iterator = downloadedAds.get().iterator();
        Ad res = null;
        Ad ad;
        while (iterator.hasNext()) {
            ad = iterator.next();
            res = ad;
            break;
        }
        downloadedAds.persistIfNeeded();
        garbageCollectAdFiles();
        return res;
    }

    private void roundRobinOnAds() {
        ResanaLog.d(TAG, "roundRobinOnAds: ");
        final Iterator<Ad> iterator = downloadedAds.get().iterator();
        final Ad removed = iterator.next();
        iterator.remove();
        downloadedAds.get().add(removed);
    }

    private int numberOfAdsInQueue(long adId) {
        int res = 0;
        Iterator<Ad> adsItr = ads.iterator();
        while (adsItr.hasNext()) {
            Ad ad = adsItr.next();
            if (ad.data.id == adId)
                res++;
        }
        return res;
    }

    private boolean shouldCoolDownSplashViewing() {
        ResanaLog.d(TAG, "shouldCoolDownSplashViewing: ");
        return !CoolDownHelper.shouldShowSplash(appContext);
    }

    private boolean shouldServeViewer() {
        ResanaLog.d(TAG, "shouldServeViewer: ");
        return adViewerRef != null && adViewerRef.get() != null;
    }

    private void garbageCollectAdFiles() {
        ResanaLog.d(TAG, "garbageCollectAdFiles: ");
        final Iterator<Ad> itr = toBeDeletedAds.iterator();
        while (itr.hasNext()) {
            final Ad ad = itr.next();
            if (locks.get(ad.getId()) != null && locks.get(ad.getId()) <= 0) {
                locks.remove(ad.getId());
                itr.remove();
                FileManager.getInstance(appContext).deleteAdFiles(ad, null);
            }
        }
    }

    private void updateAdQueues() {
        ResanaLog.d(TAG, "updateAdQueues: ");
        pruneDownloadedAds();
        downloadMoreAdsIfNeeded();
    }

    private void downloadMoreAdsIfNeeded() {
        ResanaLog.d(TAG, "downloadMoreAdsIfNeeded: ");
        Ad ad;
        while (shouldDownloadMoreAds()) {
            ad = getNextReadyToDownloadAd();
            if (ad != null)
                downloadAndCacheAd(ad);
        }
    }

    private void downloadAndCacheAd(final Ad ad) {
        ResanaLog.d(TAG, "downloadAndCacheAd: ");
        lockAdFiles(ad);
        currentlyDownloadingAds++;
        FileManager.getInstance(appContext).downloadAdFiles(ad, new DownloadAndCacheAdDelegate(this, ad));
    }

    private void downloadAndCacheAdFinished(boolean success, Ad ad) {
        ResanaLog.d(TAG, "downloadAndCacheAdFinished: ");
        unlockAdFiles(ad);
        currentlyDownloadingAds--;
        if (success && downloadedAds.get().size() < downloadedAdsQueueLength) {
            addToDownloadedAds(ad);
            downloadedAds.persist();
        }
        if (!success) {
            toBeDeletedAds.add(ad);
            garbageCollectAdFiles();
        }
    }

    private Ad getNextReadyToDownloadAd() {
        ResanaLog.d(TAG, "getNextReadyToDownloadAd: ");
        Iterator<Ad> itr = ads.iterator();
        Ad ad = null;
        while (itr.hasNext() && ad == null) {
            ad = itr.next();
            itr.remove();
//            if (ad.isInvalid()) //todo
//                ad = null;
        }
        return ad;
    }

    private boolean shouldDownloadMoreAds() {
        ResanaLog.d(TAG, "shouldDownloadMoreAds: ");
        return downloadedAds.get().size() + currentlyDownloadingAds < downloadedAdsQueueLength
                && ads.size() > 0;
    }

    private void lockAdFiles(Ad ad) {
        ResanaLog.d(TAG, "lockAdFiles: ");
        /* a splash is locked when
            - it is added to downloadedSplashes
            - it is given to a splashViewer
            - it is being downloaded and cached in storage
        * */
        final String id = ad.getId();
        Integer lock = locks.get(id);
        if (lock == null)
            lock = 0;
        lock++;
        locks.put(id, lock);
    }

    private void unlockAdFiles(Ad ad) {
        ResanaLog.d(TAG, "unlockAdFiles: ");
        final String id = ad.getId();
        Integer lock = locks.get(id);
        if (lock == null)
            lock = 0;
        lock--;
        locks.put(id, lock);
    }

    private void pruneDownloadedAds() {
        ResanaLog.d(TAG, "pruneDownloadedAds: ");
        final Iterator<Ad> iterator = downloadedAds.get().iterator();
        Ad ad;
        while (iterator.hasNext()) {
            ad = iterator.next();//todo handle here
//            if (ad.isInvalid()) {
//                iterator.remove();
//                unlockAdFiles(ad);
//                toBeDeletedAds.add(ad);
//                downloadedAds.needsPersist();
//            }
        }
        downloadedAds.persistIfNeeded();
        garbageCollectAdFiles();
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

    private static class DownloadAndCacheAdDelegate extends Delegate {
        WeakReference<SplashAdProvider> providerRef;
        Ad ad;

        DownloadAndCacheAdDelegate(SplashAdProvider provider, Ad ad) {
            providerRef = new WeakReference<>(provider);
            this.ad = ad;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SplashAdProvider provider = providerRef.get();
            if (provider != null)
                provider.downloadAndCacheAdFinished(success, ad);
        }
    }
}
