package com.google.android.gms.common;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import com.google.android.gms.common.internal.zzai;
import com.google.android.gms.common.util.zzj;
import com.google.android.gms.internal.zzbha;

public class zze {
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = zzo.GOOGLE_PLAY_SERVICES_VERSION_CODE;
    private static final zze zzaAc = new zze();

    zze() {
    }

    public static PendingIntent zza(Context context, int i, int i2, String str) {
        Intent zza = zza(context, i, str);
        return zza == null ? null : PendingIntent.getActivity(context, i2, zza, 268435456);
    }

    public static Intent zza(Context context, int i, String str) {
        switch (i) {
            case 1:
            case 2:
                return (context == null || !zzj.zzaH(context)) ? zzai.zzw("com.google.android.gms", zzx(context, str)) : zzai.zzrD();
            case 3:
                return zzai.zzcD("com.google.android.gms");
            default:
                return null;
        }
    }

    @Deprecated
    public static Intent zzak(int i) {
        return zza(null, i, null);
    }

    public static void zzas(Context context) throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException {
        zzo.zzah(context);
    }

    public static void zzat(Context context) {
        zzo.zzat(context);
    }

    public static boolean zze(Context context, int i) {
        return zzo.zze(context, i);
    }

    public static zze zzoW() {
        return zzaAc;
    }

    private static String zzx(Context context, String str) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gcore_");
        stringBuilder.append(GOOGLE_PLAY_SERVICES_VERSION_CODE);
        stringBuilder.append("-");
        if (!TextUtils.isEmpty(str)) {
            stringBuilder.append(str);
        }
        stringBuilder.append("-");
        if (context != null) {
            stringBuilder.append(context.getPackageName());
        }
        stringBuilder.append("-");
        if (context != null) {
            try {
                stringBuilder.append(zzbha.zzaP(context).getPackageInfo(context.getPackageName(), 0).versionCode);
            } catch (NameNotFoundException e) {
            }
        }
        return stringBuilder.toString();
    }

    public PendingIntent getErrorResolutionPendingIntent(Context context, int i, int i2) {
        return zza(context, i, i2, null);
    }

    public String getErrorString(int i) {
        return zzo.getErrorString(i);
    }

    public int isGooglePlayServicesAvailable(Context context) {
        int isGooglePlayServicesAvailable = zzo.isGooglePlayServicesAvailable(context);
        return zzo.zze(context, isGooglePlayServicesAvailable) ? 18 : isGooglePlayServicesAvailable;
    }

    public boolean isUserResolvableError(int i) {
        return zzo.isUserRecoverableError(i);
    }
}
