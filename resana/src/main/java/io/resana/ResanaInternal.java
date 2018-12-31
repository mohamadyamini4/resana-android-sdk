package io.resana;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import io.resana.NetworkManager.Reports;

import static io.resana.ResanaPreferences.PREF_DISMISS_ENABLE;
import static io.resana.ResanaPreferences.PREF_DISMISS_OPTIONS;
import static io.resana.ResanaPreferences.PREF_DISMISS_REST_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_DISMISS;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_DURATION;
import static io.resana.ResanaPreferences.PREF_LAST_SESSION_START_TIME;
import static io.resana.ResanaPreferences.UUID;
import static io.resana.ResanaPreferences.UUID_EXISTS;
import static io.resana.ResanaPreferences.getBoolean;
import static io.resana.ResanaPreferences.getLong;
import static io.resana.ResanaPreferences.getPrefs;
import static io.resana.ResanaPreferences.getString;
import static io.resana.ResanaPreferences.saveBoolean;
import static io.resana.ResanaPreferences.saveLong;
import static io.resana.ResanaPreferences.saveString;

class ResanaInternal {
    private static final String TAG = ResanaLog.TAG_PREF + "Resana";
    static final String SDK_VERSION = "8.0.0";
    static final int SDK_VERSION_NUM = 19;
    static final int DB_VERSION = 9;

    static final String DEFAULT_RESANA_INFO_TEXT =
            "تبلیغات رسانا" + "\n"
                    + "اپلیکیشن نمایش‌دهنده هیچ مسئولیتی در قبال محتوای این تبلیغات ندارد." + "\n"
                    + "اطلاعات بیشتر در:" + "\n"
                    + "www.resana.io" + "\n"
                    + "تماس با ما:" + "\n"
                    + "info@resana.io";

    static ResanaInternal instance;

    private Context appContext;
    static String mediaId;
    static String deviceId;

    boolean adsAreDismissible;
    List<DismissOption> dismissOptions;
    private long lastDismissTime;
    private int dismissRestDuration;

    private ResanaInternal(Context context) {
        ResanaLog.v(TAG, "Starting Resana");
        this.appContext = context.getApplicationContext();
        loadDismissOptions();
        GoalActionMeter.getInstance(appContext);
        AdVersionKeeper.init(appContext);
        ApkManager.getInstance(appContext);
        mediaId = AdViewUtil.getMediaId(appContext);
        if (mediaId == null)
            throw new IllegalArgumentException("ResanaMediaId is not defined properly");
        initializeDeviceId();
        if (shouldGetControls())
            NetworkManager.getInstance().getControls(appContext);
        FileManager.getInstance(appContext).cleanupOldFilesIfNeeded();
        FileManager.getInstance(appContext).deleteOldAndCorruptedFiles();
        NativeAdProvider.getInstance(appContext);
        SplashAdProvider.getInstance(appContext);
        NetworkManager.checkUserAgent(appContext);
        start();
    }

    private void start() {
        saveLong(appContext, PREF_LAST_SESSION_START_TIME, System.currentTimeMillis());
//        DataCollector.reportSessionDuration(getLong(appContext, PREF_LAST_SESSION_DURATION, -1));
    }

    private void initializeDeviceId() {
        if (getBoolean(appContext, UUID_EXISTS, false)) {
            deviceId = getString(appContext, UUID, "9e8f6aba-f02f-46a5-a4e4-90b6c5e9a2eb");
        } else {
            deviceId = DeviceCredentials.getDeviceUniqueId(appContext);
            saveString(appContext, UUID, deviceId);
            saveBoolean(appContext, UUID_EXISTS, true);
        }
    }

    static ResanaInternal getInstance(Context context) {
        ResanaInternal localInstance = instance;
        if (localInstance == null) {
            synchronized (ResanaInternal.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = instance = new ResanaInternal(context);
                }
            }
        }
        return localInstance;
    }

    private boolean shouldGetControls() {
        return ((System.currentTimeMillis()) - (Util.getControlsTS(appContext)) * 1000) >= Util.getControlsTTL(appContext);
    }

    NativeAd getNativeAd(String zone) {
        return NativeAdProvider.getInstance(appContext).getAd(zone);
    }

    void attachSplashViewer(SplashAdView adView) {
        if (!ResanaConfig.gettingSplashAds(appContext)) {
            ResanaLog.e(TAG, "You didn't mention splash ads in resana config");
            return;
        }
        SplashAdProvider.getInstance(appContext).attachViewer(adView);
    }

    void releaseSplash(Ad ad) {
        SplashAdProvider.getInstance(appContext).releaseAd(ad);
    }

    void detachSplashViewer(SplashAdView adView) {
        SplashAdProvider.getInstance(appContext).detachViewer(adView);
    }

    boolean isSplashAvailable() {
        return SplashAdProvider.getInstance(appContext).isAdAvailable();
    }

    private void loadDismissOptions() {
        adsAreDismissible = getPrefs(appContext).getBoolean(PREF_DISMISS_ENABLE, false);
        dismissRestDuration = getPrefs(appContext).getInt(PREF_DISMISS_REST_DURATION, 0);
        lastDismissTime = getPrefs(appContext).getLong(PREF_LAST_DISMISS, 0);
        String s = getPrefs(appContext).getString(PREF_DISMISS_OPTIONS, null);
        if (s != null) {
            dismissOptions = new Gson().fromJson(s, new TypeToken<ArrayList<DismissOption>>() {
            }.getType());
        }
    }

    private void saveSessionDuration() {
        final long start = getLong(appContext, PREF_LAST_SESSION_START_TIME, -1);
        if (start > 0) {
            final long d = System.currentTimeMillis() - start;
            saveLong(appContext, PREF_LAST_SESSION_DURATION, d);
        }
    }

    void onNativeAdRendered(NativeAd ad) {
        NativeAdProvider.getInstance(appContext).onNativeAdRendered(ad);
    }

    void onSplashRendered(Ad ad) {
        NetworkManager.getInstance().sendReports(Reports.view, ad.getOrder());
    }

    void onNativeAdClicked(Context context, NativeAd ad) {
        NativeAdProvider.getInstance(appContext).onNativeAdClicked(context, ad);
    }

    void onSplashClicked(Ad ad) {
        NetworkManager.getInstance().sendReports(Reports.click, ad.getOrder());
        GoalActionMeter.getInstance(appContext).checkInstall(ad.getOrder(), ad.getPackageName());
    }

    void onNativeAdLongClick(Context context, NativeAd ad) {
        if (adsAreDismissible)
            NativeAdProvider.getInstance(appContext).showDismissOptions(context, ad, dismissOptions, instance);
    }

    void onAdDismissed(String secretKey, DismissOption reason) {
        if (adsAreDismissible) {
            lastDismissTime = System.currentTimeMillis();
            getPrefs(appContext).edit().putLong(PREF_LAST_DISMISS, System.currentTimeMillis()).apply();
            String ad = AdDatabase.getInstance(appContext).getOrderIdForSecretKey(secretKey);
//            if (ad != null)
//                DataCollector.reportAdDismissed(ad, reason);
        }
    }

    boolean isInDismissRestTime() {
        return System.currentTimeMillis() < lastDismissTime + dismissRestDuration * 1000;
    }
}