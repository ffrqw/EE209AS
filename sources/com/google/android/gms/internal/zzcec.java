package com.google.android.gms.internal;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import com.google.android.gms.common.internal.zzbo;
import com.google.android.gms.common.util.zze;
import com.google.android.gms.measurement.AppMeasurement.zzb;
import java.util.Map;

public final class zzcec extends zzchi {
    private final Map<String, Long> zzboq = new ArrayMap();
    private final Map<String, Integer> zzbor = new ArrayMap();
    private long zzbos;

    public zzcec(zzcgl zzcgl) {
        super(zzcgl);
    }

    private final void zzK(long j) {
        for (String put : this.zzboq.keySet()) {
            this.zzboq.put(put, Long.valueOf(j));
        }
        if (!this.zzboq.isEmpty()) {
            this.zzbos = j;
        }
    }

    private final void zza(long j, zzb zzb) {
        if (zzb == null) {
            super.zzwF().zzyD().log("Not logging ad exposure. No active activity");
        } else if (j < 1000) {
            super.zzwF().zzyD().zzj("Not logging ad exposure. Less than 1000 ms. exposure", Long.valueOf(j));
        } else {
            Bundle bundle = new Bundle();
            bundle.putLong("_xt", j);
            zzchz.zza(zzb, bundle);
            super.zzwt().zzd("am", "_xa", bundle);
        }
    }

    private final void zza(String str, long j, zzb zzb) {
        if (zzb == null) {
            super.zzwF().zzyD().log("Not logging ad unit exposure. No active activity");
        } else if (j < 1000) {
            super.zzwF().zzyD().zzj("Not logging ad unit exposure. Less than 1000 ms. exposure", Long.valueOf(j));
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("_ai", str);
            bundle.putLong("_xt", j);
            zzchz.zza(zzb, bundle);
            super.zzwt().zzd("am", "_xu", bundle);
        }
    }

    static /* synthetic */ void zzb(zzcec zzcec, String str, long j) {
        super.zzwp();
        super.zzjC();
        zzbo.zzcF(str);
        Integer num = (Integer) zzcec.zzbor.get(str);
        if (num != null) {
            zzb zzzh = super.zzwx().zzzh();
            int intValue = num.intValue() - 1;
            if (intValue == 0) {
                zzcec.zzbor.remove(str);
                Long l = (Long) zzcec.zzboq.get(str);
                if (l == null) {
                    super.zzwF().zzyx().log("First ad unit exposure time was never set");
                } else {
                    long longValue = j - l.longValue();
                    zzcec.zzboq.remove(str);
                    zzcec.zza(str, longValue, zzzh);
                }
                if (!zzcec.zzbor.isEmpty()) {
                    return;
                }
                if (zzcec.zzbos == 0) {
                    super.zzwF().zzyx().log("First ad exposure time was never set");
                    return;
                }
                zzcec.zza(j - zzcec.zzbos, zzzh);
                zzcec.zzbos = 0;
                return;
            }
            zzcec.zzbor.put(str, Integer.valueOf(intValue));
            return;
        }
        super.zzwF().zzyx().zzj("Call to endAdUnitExposure for unknown ad unit id", str);
    }

    public final void beginAdUnitExposure(String str) {
        if (str == null || str.length() == 0) {
            super.zzwF().zzyx().log("Ad unit id must be a non-empty string");
            return;
        }
        super.zzwE().zzj(new zzced(this, str, super.zzkq().elapsedRealtime()));
    }

    public final void endAdUnitExposure(String str) {
        if (str == null || str.length() == 0) {
            super.zzwF().zzyx().log("Ad unit id must be a non-empty string");
            return;
        }
        super.zzwE().zzj(new zzcee(this, str, super.zzkq().elapsedRealtime()));
    }

    public final /* bridge */ /* synthetic */ Context getContext() {
        return super.getContext();
    }

    public final void zzJ(long j) {
        zzb zzzh = super.zzwx().zzzh();
        for (String str : this.zzboq.keySet()) {
            zza(str, j - ((Long) this.zzboq.get(str)).longValue(), zzzh);
        }
        if (!this.zzboq.isEmpty()) {
            zza(j - this.zzbos, zzzh);
        }
        zzK(j);
    }

    public final /* bridge */ /* synthetic */ void zzjC() {
        super.zzjC();
    }

    public final /* bridge */ /* synthetic */ zze zzkq() {
        return super.zzkq();
    }

    public final /* bridge */ /* synthetic */ zzcfj zzwA() {
        return super.zzwA();
    }

    public final /* bridge */ /* synthetic */ zzcjl zzwB() {
        return super.zzwB();
    }

    public final /* bridge */ /* synthetic */ zzcgf zzwC() {
        return super.zzwC();
    }

    public final /* bridge */ /* synthetic */ zzcja zzwD() {
        return super.zzwD();
    }

    public final /* bridge */ /* synthetic */ zzcgg zzwE() {
        return super.zzwE();
    }

    public final /* bridge */ /* synthetic */ zzcfl zzwF() {
        return super.zzwF();
    }

    public final /* bridge */ /* synthetic */ zzcfw zzwG() {
        return super.zzwG();
    }

    public final /* bridge */ /* synthetic */ zzcem zzwH() {
        return super.zzwH();
    }

    public final void zzwn() {
        super.zzwE().zzj(new zzcef(this, super.zzkq().elapsedRealtime()));
    }

    public final /* bridge */ /* synthetic */ void zzwo() {
        super.zzwo();
    }

    public final /* bridge */ /* synthetic */ void zzwp() {
        super.zzwp();
    }

    public final /* bridge */ /* synthetic */ void zzwq() {
        super.zzwq();
    }

    public final /* bridge */ /* synthetic */ zzcec zzwr() {
        return super.zzwr();
    }

    public final /* bridge */ /* synthetic */ zzcej zzws() {
        return super.zzws();
    }

    public final /* bridge */ /* synthetic */ zzchl zzwt() {
        return super.zzwt();
    }

    public final /* bridge */ /* synthetic */ zzcfg zzwu() {
        return super.zzwu();
    }

    public final /* bridge */ /* synthetic */ zzcet zzwv() {
        return super.zzwv();
    }

    public final /* bridge */ /* synthetic */ zzcid zzww() {
        return super.zzww();
    }

    public final /* bridge */ /* synthetic */ zzchz zzwx() {
        return super.zzwx();
    }

    public final /* bridge */ /* synthetic */ zzcfh zzwy() {
        return super.zzwy();
    }

    public final /* bridge */ /* synthetic */ zzcen zzwz() {
        return super.zzwz();
    }

    static /* synthetic */ void zza(zzcec zzcec, String str, long j) {
        super.zzwp();
        super.zzjC();
        zzbo.zzcF(str);
        if (zzcec.zzbor.isEmpty()) {
            zzcec.zzbos = j;
        }
        Integer num = (Integer) zzcec.zzbor.get(str);
        if (num != null) {
            zzcec.zzbor.put(str, Integer.valueOf(num.intValue() + 1));
        } else if (zzcec.zzbor.size() >= 100) {
            super.zzwF().zzyz().log("Too many ads visible");
        } else {
            zzcec.zzbor.put(str, Integer.valueOf(1));
            zzcec.zzboq.put(str, Long.valueOf(j));
        }
    }
}
