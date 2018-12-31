package io.resana;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class Resana {
    static ResanaInternal instance;

    private static Resana resana;

    private static Context appContext;

    /**
     * Every Detail Will Be Printed In Logcat.
     */
    public static final int LOG_LEVEL_VERBOSE = 2;
    /**
     * Data Needed For Debug Will Be Printed.
     */
    public static final int LOG_LEVEL_DEBUG = 3;
    /**
     * Standard Level. You Will Be Aware Of BefrestImpl's Main State
     */
    public static final int LOG_LEVEL_INFO = 4;
    /**
     * Only Warning And Errors.
     */
    public static final int LOG_LEVEL_WARN = 5;
    /**
     * Only Errors.
     */
    public static final int LOG_LEVEL_ERROR = 6;
    /**
     * None Of BefrestImpl Logs Will Be Shown.
     */
    public static final int LOG_LEVEL_NO_LOG = 100;

    public static void init(Context context, ResanaConfig resanaConfig) {
        if (resanaConfig == null)
            throw new IllegalArgumentException("ResanaConfig cannot be null");
        appContext = context;
        ResanaConfig.saveConfigs(appContext, resanaConfig);
        instance = ResanaInternal.getInstance(appContext);
    }

    public static Resana getInstance() {
        Resana localInstance = resana;
        if (localInstance == null) {
            synchronized (Resana.class) {
                localInstance = resana;
                if (localInstance == null) {
                    localInstance = resana = new Resana();
                }
            }
        }
        return localInstance;
    }

    private Resana() {

    }

    public void setLogLevel(int logLevel) {
        ResanaLog.setLogLevel(logLevel);
    }

    public NativeAd getNativeAd() {
        return ResanaInternal.getInstance(appContext).getNativeAd("all");
    }

    public NativeAd getNativeAd(String zone) {
        return ResanaInternal.getInstance(appContext).getNativeAd(zone);
    }

    public void onNativeAdRendered(NativeAd ad) {
        ResanaInternal.getInstance(appContext).onNativeAdRendered(ad);
    }

    public void onNativeAdClicked(Context context, NativeAd ad) {
        ResanaInternal.getInstance(context).onNativeAdClicked(context, ad);
    }

    public void onAdDismissed(String secretKey, DismissOption reason) {
        ResanaInternal.getInstance(appContext).onAdDismissed(secretKey, reason);
    }

    public void onNativeAdLongClick(Context context, NativeAd ad) {
        ResanaInternal.getInstance(context).onNativeAdLongClick(context, ad);
    }

    public boolean canDismissAds() {
        return ResanaInternal.getInstance(appContext).adsAreDismissible;
    }

    public List<DismissOption> getDismissOptions() {
        if (ResanaInternal.getInstance(appContext).adsAreDismissible && ResanaInternal.getInstance(appContext).dismissOptions != null) {
            List<DismissOption> options = new ArrayList<>();
            options.addAll(instance.dismissOptions);
            return options;
        }
        return null;
    }

    public String getVersion() {
        return ResanaInternal.SDK_VERSION;
    }

    @Deprecated
    public void release() {

    }
}