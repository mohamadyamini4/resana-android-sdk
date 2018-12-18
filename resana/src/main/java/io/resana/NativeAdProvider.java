package io.resana;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.resana.FileManager.Delegate;
import static io.resana.FileManager.NATIVE_ADS_FILE_NAME;
import static io.resana.FileManager.PersistableObject;

class NativeAdProvider {
    private static final String TAG = ResanaLog.TAG_PREF + "NativeAdProvider";
    private static final String CLICK_ACK_PREFS = "RESANA_CLICK_ACK_PREFS" + 2;

    private static NativeAdProvider instance;
    private Context appContext;
    private String adsFileName;
    private int adsQueueLength;
    private PersistableObject<Set<Ad>> ads;
    private Map<String, List<Ad>> adsList;
    private Map<String, Acks> waitingToBeRenderedByClient = new HashMap<>();//todo these should be removed
    private Map<String, Acks> waitingForLandingClick = new HashMap<>();
    private static String[] blockedZones;

    private boolean needsFlushCache;

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
        this.adsFileName = NATIVE_ADS_FILE_NAME;
        this.adsQueueLength = 4;
        adsList = new HashMap<>();
        loadBlockedZones();
    }

    static boolean isBlockedZone(String zone) {
        if (blockedZones == null || zone == null || zone.equals(""))
            return false;
        return Arrays.asList(blockedZones).contains(zone);
    }

    private void persistBlockedZones() {
        ResanaLog.d(TAG, "persistBlockedZones: ");
        if (blockedZones == null || blockedZones.length == 0)
            ResanaPreferences.remove(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES);
        else {
            String s = "";
            for (int i = 0; i < blockedZones.length; i++) {
                s += blockedZones[i];
                s += ";";
            }
            ResanaPreferences.saveString(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES, s);
        }
    }

    void handleBlockedZones(ControlDto ctrl) {
        ControlDto.BlockedZonesParams params = (ControlDto.BlockedZonesParams) ctrl.params;
        if (params.zones == null || params.zones.length == 0) {
            blockedZones = null;
        } else {
            blockedZones = new String[params.zones.length];
            for (int i = 0; i < params.zones.length; i++) {
                blockedZones[i] = params.zones[i];
            }
            persistBlockedZones();
        }
    }

    void newAdsReceived(List<Ad> items) {
        ResanaLog.d(TAG, "newAdsReceived: new ads received");
        for (Ad item : items) {
            String[] zones = item.data.zones;
            for (String zone : zones) {
                List<Ad> list = adsList.get(zone);
                if (list == null)
                    list = new ArrayList<>();
                if (list.size() >= adsQueueLength)
                    break;
                if (item.data.hot)
                    list.add(0, item);
                else list.add(item);
                adsList.put(zone, list);
            }
        }
//            if (!ApkManager.getInstance(appContext).isApkInstalled(item)) {
//                if (item.hasApk()) {
//                    if (ApkManager.canDownloadApk(appContext))
//                        downloadAdFiles(item);
//                } else
//                    downloadAdFiles(item);
//            }
    }

    private void downloadAdFiles(final Ad ad) {
        VisualsManager.saveVisualsIndex(appContext, ad);
        FileManager.getInstance(appContext).downloadAdFiles(ad, new Delegate() {
            @Override
            void onFinish(boolean success, Object... args) {
                ResanaLog.d(TAG, "onFinish: downloading native ad files finishes. success " + success);
                if (success) {
                    adDownloaded(ad);
                }
            }
        });
    }

    void downloadFistAd() {
        for (Map.Entry<String, List<Ad>> entry : adsList.entrySet()) {
            downloadAdFiles(entry.getValue().get(0));
        }
    }

    void adDownloaded(Ad ad) {
        ResanaPreferences.saveBoolean(appContext, ad.getId() + "downloaded", true);
    }

    boolean isDownloaded(Ad ad) {
        return ResanaPreferences.getBoolean(appContext, ad.getId() + "downloaded", false);
    }

    private void roundRobinOnAds() {
        ResanaLog.d(TAG, "roundRobinOnAds: ");
        final Iterator<Ad> itr = ads.get().iterator();
        Ad remove = itr.next();
        itr.remove();
        ads.get().add(remove);
    }

    private int numberOfAdsInQueue(long adId) {
        int res = 0;
        Iterator<Ad> adsItr = ads.get().iterator();
        while (adsItr.hasNext()) {
            Ad ad = adsItr.next();
            if (ad.data.id == adId)
                res++;
        }
        return res;
    }

    private void loadBlockedZones() {
        ResanaLog.d(TAG, "loadBlockedZones: ");
        String s = ResanaPreferences.getString(appContext, ResanaPreferences.PREEF_BLOCKED_ZONES, null);
        if (s != null && s.length() > 0) {
            String[] zones = s.split(";");
            blockedZones = new String[zones.length];
            for (int i = 0; i < zones.length; i++) {
                blockedZones[i] = zones[i];
                ResanaLog.d(TAG, "blockedZone: " + blockedZones[i]);
            }
        } else {
            blockedZones = null;
            ResanaLog.d(TAG, "loadBlockedZones: there is no block zone");
        }

    }

    private Ad internalGetAd(boolean hasTitle, String zone) {
        if (ads == null || ads.get().isEmpty()) {
            if (ads == null)
                ResanaLog.e(TAG, "get: ads is null");
            if (ads.get().isEmpty())
                ResanaLog.e(TAG, "get: ads is empty");
            ResanaLog.e(TAG, "get: no native ad available.");
            return null;
        }
        final boolean cooldown = !CoolDownHelper.shouldShowNativeAd(appContext);
        Ad result = null;
        final Ad hotAd = nextReadyToRenderAd(true, zone, hasTitle);
        if (hotAd != null) {
            result = hotAd;
        } else {
            final Ad notHotAd = nextReadyToRenderAd(false, zone, hasTitle);
            if (notHotAd != null) {
                result = notHotAd;
            }
        }

        if (result != null) {
            if (!result.data.hot) {
                roundRobinOnAds();
                ads.persist();
            }
            if (!cooldown)
                return result;
            else return null;
        }
        return null;
    }

    private Ad nextReadyToRenderAd(boolean hotOnly, String zone, boolean hasTitle) {
        final Iterator<Ad> iterator = ads.get().iterator();
        if (!hotOnly) {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle) {
                    if (validZone(ad, zone))
                        return ad;
                } else if (validZone(ad, zone) && hasTitle(ad))
                    return ad;
            }
        } else {
            while (iterator.hasNext()) {
                Ad ad = iterator.next();
                if (!hasTitle) {
                    if (ad.data.hot && validZone(ad, zone))
                        return ad;
                } else if (ad.data.hot && validZone(ad, zone) && hasTitle(ad))
                    return ad;
            }
        }
        return null;
    }

    /**
     * will check that should show an ad with a specific zone
     *
     * @param zones zones of an ad
     * @param zone  a zone we want to get ad for
     * @return
     */
    private boolean validZone(String[] zones, String zone) {
        ResanaLog.d(TAG, "validZone: zone: " + zone);
        if (zones == null)
            ResanaLog.d(TAG, "validZone: ad has no zone.");
        else
            for (int i = 0; i < zones.length; i++) {
                ResanaLog.d(TAG, "validZone: ad zone: " + zones[i]);
            }
        if (zone.equals(""))
            if (zones == null || zones.length == 0)
                return true;
        if (!zone.equals("")) {
            if (zones == null || zones.length == 0)
                return true;
            if (Arrays.asList(zones).contains(zone))
                return true;
        }
        return false;
    }

    private boolean validZone(Ad ad, String zone) {
        String[] zones = ad.data.zones;
        return validZone(zones, zone);
    }

    /**
     * will check whether an ad has a title or not
     *
     * @param ad
     * @return
     */
    private boolean hasTitle(Ad ad) {
        return ((NativeDto) ad.data).texts != null && ((NativeDto) ad.data).texts.titleText != null;
    }

    NativeAd getAd(boolean hasTitle, String zone) {
        if (ResanaInternal.instance.isInDismissRestTime()) {
            ResanaLog.d(TAG, "get: Native dismissRestTime");
            return null;
        }

        if (isBlockedZone(zone)) {
            ResanaLog.e(TAG, "get: zone " + zone + " is blocked");
            return null;
        }
        final Ad ad = internalGetAd(hasTitle, zone);
        if (ad != null) {
            final NativeAd nativeAd = new NativeAd(appContext, ad, AdDatabase.getInstance(appContext).generateSecretKey(ad));
            final Acks acks = new Acks(ad);
            waitingToBeRenderedByClient.put(nativeAd.getSecretKey(), acks);
            GoalActionMeter.getInstance(appContext).persistReport(nativeAd.getSecretKey(), ad.data.report);
            ClickSimulator.getInstance(appContext).persistSimulateClicks(nativeAd.getSecretKey(), ad.data.simulateClicks);
            return nativeAd;
        }
        return null;
    }

    void showLanding(final Context context, final NativeAd ad) {
        showLanding(context, ad, null);
    }

    void showLanding(final Context context, final NativeAd ad, final AdDelegate adDelegate) {
        final NativeLandingView nativeLandingView = new NativeLandingView(context, ad);
        nativeLandingView.setDelegate(new LandingView.Delegate() {
            @Override
            public void closeLanding() {
                nativeLandingView.dismiss();
            }

            @Override
            public void landingActionClicked() {
                handleLandingClick(context, ad, adDelegate);
                ResanaInternal.instance.onNativeAdLandingClicked(ad);
                nativeLandingView.dismiss();
            }
        });
        nativeLandingView.show();
    }

    void handleLandingClick(final Context context, final NativeAd ad) {
        handleLandingClick(context, ad, null);
    }

    void handleLandingClick(final Context context, final NativeAd ad, AdDelegate adDelegate) {
        if (ResanaInternal.instance == null)
            return;
        if (ApkManager.getInstance(context).isApkDownloading(context, ad)) {
            if (adDelegate == null)
                Toast.makeText(appContext, "در حال آماده سازی", Toast.LENGTH_SHORT).show();
            else adDelegate.onPreparingProgram();
            return;
        }
        if (ad.hasApk()) {
            ApkManager.getInstance(context).downloadAndInstallApk(ad, adDelegate);
        } else if (ad.hasIntent()) {
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

    private static class Acks {

        String order;

        String render;
        String click;
        String landing;

        Acks() {

        }

        Acks(Ad ad) {
            this.order = ad.getOrder();
            this.render = ad.getRenderAck();
            this.click = ad.getClickAck();
            this.landing = ad.getLandingClickAck();
        }

        void saveToPrefs(SharedPreferences prefs, String key) {
            final SharedPreferences.Editor editor = prefs.edit();
            if (order != null)
                editor.putString(key + "_order", order);
            if (render != null)
                editor.putString(key + "_render", render);
            if (click != null)
                editor.putString(key + "_click", click);
            if (landing != null)
                editor.putString(key + "_land", landing);
            editor.apply();
        }

        Acks fromPrefs(SharedPreferences prefs, String key) {
            order = prefs.getString(key + "_order", null);
            render = prefs.getString(key + "_render", null);
            click = prefs.getString(key + "_click", null);
            landing = prefs.getString(key + "_land", null);
            return this;
        }

        void removeFromPref(SharedPreferences prefs, String key) {
            prefs.edit()
                    .remove(key + "_order")
                    .remove(key + "_render")
                    .remove(key + "_click")
                    .remove(key + "_land")
                    .apply();
        }
    }
}
