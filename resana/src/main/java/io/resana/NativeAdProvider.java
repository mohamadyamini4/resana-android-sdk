package io.resana;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.resana.NetworkManager.Reports;

import static io.resana.FileManager.Delegate;

class NativeAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "NativeAdProvider";

    private static NativeAdProvider instance;
    private Context appContext;
    private int adsQueueLength;
    private Map<String, List<Ad>> adsMap;
    private List<String> downloadedAds;
    private static String[] blockedZones;

    static NativeAdProvider getInstance(Context context) {
        NativeAdProvider localInstance = instance;
        synchronized (NativeAdProvider.class) {
            localInstance = instance;
            if (localInstance == null) {
                localInstance = instance = new NativeAdProvider(context);
            }
        }
        return localInstance;
    }

    private NativeAdProvider(Context context) {
        this.appContext = context;
        this.adsQueueLength = 4;
        adsMap = Collections.synchronizedMap(new HashMap<String, List<Ad>>());
        downloadedAds = Collections.synchronizedList(new ArrayList<String>());
        blockedZones = Util.getBlockedZones(context);
        if (ResanaConfig.gettingNativeAds(appContext))
            NetworkManager.getInstance().getNativeAds(new AdsReceivedDelegate(appContext));
        else ResanaLog.e(TAG, "NativeAdProvider: nativeAd is not mentioned in resana config");
    }

    private boolean isBlockedZone(String zone) {
        if (blockedZones == null || zone == null || zone.equals(""))
            return false;
        return Arrays.asList(blockedZones).contains(zone);
    }

    private void newAdsReceived(List<Ad> items, String zone) {
        pruneAds(items);
        ResanaLog.e(TAG, "newAdsReceived: ads size=" + items.size() + (zone.equals("") ? "" : (" zone=" + zone)));
        if (!zone.equals("")) {
            List<Ad> list = adsMap.get(zone);
            if (list == null)
                list = Collections.synchronizedList(new ArrayList<Ad>());
            if (list.size() >= adsQueueLength)
                return;
            for (Ad item : items) {
                if (item.data.hot)
                    list.add(0, item);
                else list.add(item);
            }
        } else {
            for (Ad item : items) {
                String[] zones = item.data.zones;
                for (String adZone : zones) {
                    List<Ad> list = adsMap.get(adZone);
                    if (list == null)
                        list = Collections.synchronizedList(new ArrayList<Ad>());
                    if (list.size() >= adsQueueLength)
                        break;
                    if (item.data.hot)
                        list.add(0, item);
                    else list.add(item);
                    adsMap.put(adZone, list);
                }
            }
        }

        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            Log.e(TAG, "zone: " + entry.getKey());
            List<Ad> ad = entry.getValue();
            for (Ad a :
                    ad) {
                Log.e(TAG, "ads: " + a.getId());
            }
        }
        downloadFirstAdOfList();
    }

    /**
     * prune all ads
     */
    private void pruneAds() {
        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            pruneAds(entry.getValue());
        }
    }

    /**
     * prune ads of a list (zone)
     *
     * @param ads
     */
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

    private void downloadAdFiles(final Ad ad, Delegate delegate) {
        VisualsManager.saveVisualsIndex(appContext, ad);
        FileManager.getInstance(appContext).downloadAdFiles(ad, delegate);
    }

    private void downloadAdFiles(String zone) {
        ResanaLog.d(TAG, "downloadAdFiles: Downloading ad of list " + zone);
        List<Ad> ads = adsMap.get(zone);
        if (ads == null || ads.size() == 0)
            return;
        Ad shouldDownloadAd = ads.get(0);
        if (isDownloaded(shouldDownloadAd)) {
            ResanaLog.d(TAG, "downloadAdFiles: ad " + shouldDownloadAd.getId() + " is downloaded before");
            return;
        }
        DownloadAdFilesDelegate delegate = new DownloadAdFilesDelegate(appContext, zone, shouldDownloadAd);
        downloadAdFiles(shouldDownloadAd, delegate);
    }

    private void downloadFirstAdOfList() {
        ResanaLog.d(TAG, "downloadFirstAdOfList: Downloading first ad of every list");
        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {
            downloadAdFiles(entry.getKey());
        }
    }

    private void adDownloaded(Ad ad) {
        ResanaLog.d(TAG, "adDownloaded: ad " + ad.getId() + " downloaded");
        downloadedAds.add(ad.getId());
    }

    private boolean isDownloaded(Ad ad) {
        return downloadedAds.contains(ad.getId());
    }

    private Ad internalGetAd(String zone) {
        if (adsMap == null) {
            ResanaLog.e(TAG, "internalGetAd: ads map is null");
            return null;
        }
        List<Ad> adList = adsMap.get(zone);
        if (adList == null) {
            ResanaLog.e(TAG, "getAd: no such " + zone + " zone");
            return null;
        }
        if (adList.size() <= 1) {
            NetworkManager.getInstance().getNativeAds(new AdsReceivedDelegate(appContext, zone), zone);
            if (adList.size() == 0) {
                return null;
            }
        }
        Ad ad = adList.get(0);
        if (isDownloaded(ad)) { //round robin on ads
            adList.remove(ad);
            if (ad.data.hot)
                adList.add(0, ad);
            else adList.add(ad);
            if (CoolDownHelper.shouldShowNativeAd(appContext))
                return ad;
            else {
                ResanaLog.e(TAG, "internalGetAd: should not show ad");
                return null;
            }
        } else {
            ResanaLog.e(TAG, "internalGetAd: ad is not downloaded");
            adList.remove(ad);
            if (!ad.data.hot)
                adList.add(ad);
            return null;
        }
    }

    NativeAd getAd(String zone) {
        if (!ResanaConfig.gettingNativeAds(appContext)) {
            ResanaLog.e(TAG, "getAd: nativeAd is not mentioned in resana config");
            return null;
        }
        ResanaLog.d(TAG, "getAd: ");
        if (ResanaInternal.instance.isInDismissRestTime()) {
            ResanaLog.d(TAG, "getAd: Native dismissRestTime");
            return null;
        }

        if (isBlockedZone(zone)) {
            ResanaLog.e(TAG, "getAd: zone " + zone + " is blocked");
            return null;
        }
        final Ad ad = internalGetAd(zone);
        for (Map.Entry<String, List<Ad>> entry : adsMap.entrySet()) {//todo remove this logging all ads
            Log.e(TAG, "zone: " + entry.getKey());
            List<Ad> addd = entry.getValue();
            for (Ad a :
                    addd) {
                Log.e(TAG, "ads: " + a.getId());
            }
        }
        if (ad != null) {
            downloadAdFiles(zone);
            ResanaLog.e(TAG, "getAd: " + ad.getId());//todo remove this log
            return new NativeAd(appContext, ad, AdDatabase.getInstance(appContext).generateSecretKey(ad));
        }
        return null;
    }

    void onNativeAdRendered(NativeAd ad) {
        NetworkManager.getInstance().sendReports(Reports.view, ad.getId() + "");
        AdVersionKeeper.adRendered(ad.getId() + "");
        pruneAds();
    }

    void onNativeAdClicked(Context context, NativeAd ad) {
        NetworkManager.getInstance().sendReports(Reports.click, ad.getId() + "");
        if (ad.shouldCheckApkInstallation())
            GoalActionMeter.getInstance(appContext).checkInstall(ad.getId() + "", ad.getApkPackageName());
        if (ad.hasLanding()) {
            showLanding(context, ad);
        } else {
            handleLandingClick(context, ad);
        }
    }

    private void onNativeAdLandingClicked(NativeAd ad) {
        NetworkManager.getInstance().sendReports(Reports.landingClick, ad.getId() + "");
    }

    private void showLanding(final Context context, final NativeAd ad) {
        final NativeLandingView nativeLandingView = new NativeLandingView(context, ad);
        nativeLandingView.setDelegate(new LandingView.Delegate() {
            @Override
            public void closeLanding() {
                nativeLandingView.dismiss();
            }

            @Override
            public void landingActionClicked() {
                handleLandingClick(context, ad);
                onNativeAdLandingClicked(ad);
                nativeLandingView.dismiss();
            }
        });
        nativeLandingView.show();
    }

    private void handleLandingClick(final Context context, final NativeAd ad) {
        if (ResanaInternal.instance == null)
            return;
        if (ad.hasIntent()) {
            Intent intent = ad.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(intent, "انتخاب کنید"));
            else
                ResanaLog.e(TAG, "handleLandingClick: unable to resolve intent");
        } else if (ad.hasLink()) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(Uri.parse(ad.getLink()));
            if (i.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(i, "انتخاب کنید"));
            else
                ResanaLog.e(TAG, "handleLandingClick: unable to resolve link intent");
        }
    }

    void showDismissOptions(Context context, final NativeAd ad, List<DismissOption> dismissOptions, final ResanaInternal resanaInstance) {
        DismissOptionsView.Delegate delegate = new DismissOptionsView.Delegate() {
            @Override
            public void itemSelected(String key, String reason) {
                resanaInstance.onAdDismissed(ad.getSecretKey(), new DismissOption(key, reason));
            }
        };
        DismissOptionsView dismissOptionsView = new DismissOptionsView(context, dismissOptions, delegate);
        dismissOptionsView.setDismissOptions(dismissOptions);
        dismissOptionsView.show();
    }

    private static class AdsReceivedDelegate extends Delegate {
        Context context;
        String zone = "";

        AdsReceivedDelegate(Context context) {
            this(context, "");
        }

        AdsReceivedDelegate(Context context, String zone) {
            this.context = context;
            this.zone = zone;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            if (success)
                NativeAdProvider.getInstance(context).newAdsReceived((List<Ad>) args[0], zone);
        }
    }

    private static class DownloadAdFilesDelegate extends Delegate {
        Context context;
        String zone;
        Ad downloadedAd;

        DownloadAdFilesDelegate(Context context, String zone, Ad ad) {
            this.context = context;
            this.zone = zone;
            this.downloadedAd = ad;
        }

        @Override
        void onFinish(boolean success, Object... args) {
            ResanaLog.d(TAG, "Download ad files of list " + zone + ". result=" + success);
            if (!success) {
                List<Ad> ad = NativeAdProvider.getInstance(context).adsMap.get(zone);
                if (ad == null || ad.size() == 0)
                    return;
                ad.remove(downloadedAd);
                NativeAdProvider.getInstance(context).downloadAdFiles(zone);
            } else {
                NativeAdProvider.getInstance(context).adDownloaded(downloadedAd);
            }
        }
    }
}
