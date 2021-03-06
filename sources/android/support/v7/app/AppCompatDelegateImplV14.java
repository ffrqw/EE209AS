package android.support.v7.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.view.SupportActionModeWrapper.CallbackWrapper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Window;
import android.view.Window.Callback;

@TargetApi(14)
class AppCompatDelegateImplV14 extends AppCompatDelegateImplV11 {
    private boolean mApplyDayNightCalled;
    private AutoNightModeManager mAutoNightModeManager;
    private boolean mHandleNativeActionModes = true;
    private int mLocalNightMode = -100;

    class AppCompatWindowCallbackV14 extends AppCompatWindowCallbackBase {
        AppCompatWindowCallbackV14(Callback callback) {
            super(callback);
        }

        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            if (AppCompatDelegateImplV14.this.isHandleNativeActionModesEnabled()) {
                return startAsSupportActionMode(callback);
            }
            return super.onWindowStartingActionMode(callback);
        }

        final ActionMode startAsSupportActionMode(ActionMode.Callback callback) {
            CallbackWrapper callbackWrapper = new CallbackWrapper(AppCompatDelegateImplV14.this.mContext, callback);
            android.support.v7.view.ActionMode supportActionMode = AppCompatDelegateImplV14.this.startSupportActionMode(callbackWrapper);
            if (supportActionMode != null) {
                return callbackWrapper.getActionModeWrapper(supportActionMode);
            }
            return null;
        }
    }

    final class AutoNightModeManager {
        private BroadcastReceiver mAutoTimeChangeReceiver;
        private IntentFilter mAutoTimeChangeReceiverFilter;
        private boolean mIsNight;
        private TwilightManager mTwilightManager;

        AutoNightModeManager(TwilightManager twilightManager) {
            this.mTwilightManager = twilightManager;
            this.mIsNight = twilightManager.isNight();
        }

        final int getApplyableNightMode() {
            this.mIsNight = this.mTwilightManager.isNight();
            return this.mIsNight ? 2 : 1;
        }

        final void dispatchTimeChanged() {
            boolean isNight = this.mTwilightManager.isNight();
            if (isNight != this.mIsNight) {
                this.mIsNight = isNight;
                AppCompatDelegateImplV14.this.applyDayNight();
            }
        }

        final void setup() {
            cleanup();
            if (this.mAutoTimeChangeReceiver == null) {
                this.mAutoTimeChangeReceiver = new BroadcastReceiver() {
                    public final void onReceive(Context context, Intent intent) {
                        AutoNightModeManager.this.dispatchTimeChanged();
                    }
                };
            }
            if (this.mAutoTimeChangeReceiverFilter == null) {
                this.mAutoTimeChangeReceiverFilter = new IntentFilter();
                this.mAutoTimeChangeReceiverFilter.addAction("android.intent.action.TIME_SET");
                this.mAutoTimeChangeReceiverFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
                this.mAutoTimeChangeReceiverFilter.addAction("android.intent.action.TIME_TICK");
            }
            AppCompatDelegateImplV14.this.mContext.registerReceiver(this.mAutoTimeChangeReceiver, this.mAutoTimeChangeReceiverFilter);
        }

        final void cleanup() {
            if (this.mAutoTimeChangeReceiver != null) {
                AppCompatDelegateImplV14.this.mContext.unregisterReceiver(this.mAutoTimeChangeReceiver);
                this.mAutoTimeChangeReceiver = null;
            }
        }
    }

    AppCompatDelegateImplV14(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && this.mLocalNightMode == -100) {
            this.mLocalNightMode = savedInstanceState.getInt("appcompat:local_night_mode", -100);
        }
    }

    Callback wrapWindowCallback(Callback callback) {
        return new AppCompatWindowCallbackV14(callback);
    }

    public final boolean isHandleNativeActionModesEnabled() {
        return this.mHandleNativeActionModes;
    }

    public final boolean applyDayNight() {
        int nightMode;
        boolean applied = false;
        if (this.mLocalNightMode != -100) {
            nightMode = this.mLocalNightMode;
        } else {
            nightMode = AppCompatDelegate.getDefaultNightMode();
        }
        int modeToApply = mapNightMode(nightMode);
        if (modeToApply != -1) {
            Resources resources = this.mContext.getResources();
            Configuration configuration = resources.getConfiguration();
            int i = configuration.uiMode & 48;
            int i2 = modeToApply == 2 ? 32 : 16;
            if (i != i2) {
                if (shouldRecreateOnNightModeChange()) {
                    ((Activity) this.mContext).recreate();
                } else {
                    Configuration configuration2 = new Configuration(configuration);
                    DisplayMetrics displayMetrics = resources.getDisplayMetrics();
                    configuration2.uiMode = i2 | (configuration2.uiMode & -49);
                    resources.updateConfiguration(configuration2, displayMetrics);
                    ResourcesFlusher.flush(resources);
                }
                applied = true;
            } else {
                applied = false;
            }
        }
        if (nightMode == 0) {
            ensureAutoNightModeManager();
            this.mAutoNightModeManager.setup();
        }
        this.mApplyDayNightCalled = true;
        return applied;
    }

    public final void onStart() {
        super.onStart();
        applyDayNight();
    }

    public final void onStop() {
        super.onStop();
        if (this.mAutoNightModeManager != null) {
            this.mAutoNightModeManager.cleanup();
        }
    }

    int mapNightMode(int mode) {
        switch (mode) {
            case -100:
                return -1;
            case 0:
                ensureAutoNightModeManager();
                return this.mAutoNightModeManager.getApplyableNightMode();
            default:
                return mode;
        }
    }

    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mLocalNightMode != -100) {
            outState.putInt("appcompat:local_night_mode", this.mLocalNightMode);
        }
    }

    public final void onDestroy() {
        super.onDestroy();
        if (this.mAutoNightModeManager != null) {
            this.mAutoNightModeManager.cleanup();
        }
    }

    private void ensureAutoNightModeManager() {
        if (this.mAutoNightModeManager == null) {
            this.mAutoNightModeManager = new AutoNightModeManager(TwilightManager.getInstance(this.mContext));
        }
    }

    private boolean shouldRecreateOnNightModeChange() {
        if (!this.mApplyDayNightCalled || !(this.mContext instanceof Activity)) {
            return false;
        }
        try {
            if ((this.mContext.getPackageManager().getActivityInfo(new ComponentName(this.mContext, this.mContext.getClass()), 0).configChanges & 512) == 0) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            Log.d("AppCompatDelegate", "Exception while getting ActivityInfo", e);
            return true;
        }
    }
}
