package android.support.v4.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build.VERSION;
import android.support.v4.content.IntentCompat;
import android.util.Log;

public final class NavUtils {
    private static final NavUtilsImpl IMPL;

    interface NavUtilsImpl {
        Intent getParentActivityIntent(Activity activity);

        String getParentActivityName(Context context, ActivityInfo activityInfo);

        void navigateUpTo(Activity activity, Intent intent);

        boolean shouldUpRecreateTask(Activity activity, Intent intent);
    }

    static class NavUtilsImplBase implements NavUtilsImpl {
        NavUtilsImplBase() {
        }

        public Intent getParentActivityIntent(Activity activity) {
            Intent intent = null;
            String parentName = NavUtils.getParentActivityName(activity);
            if (parentName != null) {
                ComponentName target = new ComponentName(activity, parentName);
                try {
                    if (NavUtils.getParentActivityName(activity, target) == null) {
                        intent = IntentCompat.makeMainActivity(target);
                    } else {
                        intent = new Intent().setComponent(target);
                    }
                } catch (NameNotFoundException e) {
                    Log.e("NavUtils", "getParentActivityIntent: bad parentActivityName '" + parentName + "' in manifest");
                }
            }
            return intent;
        }

        public boolean shouldUpRecreateTask(Activity activity, Intent targetIntent) {
            String action = activity.getIntent().getAction();
            return (action == null || action.equals("android.intent.action.MAIN")) ? false : true;
        }

        public void navigateUpTo(Activity activity, Intent upIntent) {
            upIntent.addFlags(67108864);
            activity.startActivity(upIntent);
            activity.finish();
        }

        public String getParentActivityName(Context context, ActivityInfo info) {
            if (info.metaData == null) {
                return null;
            }
            String parentActivity = info.metaData.getString("android.support.PARENT_ACTIVITY");
            if (parentActivity == null) {
                return null;
            }
            if (parentActivity.charAt(0) == '.') {
                return context.getPackageName() + parentActivity;
            }
            return parentActivity;
        }
    }

    static class NavUtilsImplJB extends NavUtilsImplBase {
        NavUtilsImplJB() {
        }

        public final Intent getParentActivityIntent(Activity activity) {
            Intent result = activity.getParentActivityIntent();
            if (result == null) {
                return super.getParentActivityIntent(activity);
            }
            return result;
        }

        public final boolean shouldUpRecreateTask(Activity activity, Intent targetIntent) {
            return activity.shouldUpRecreateTask(targetIntent);
        }

        public final void navigateUpTo(Activity activity, Intent upIntent) {
            activity.navigateUpTo(upIntent);
        }

        public final String getParentActivityName(Context context, ActivityInfo info) {
            String result = info.parentActivityName;
            if (result == null) {
                return super.getParentActivityName(context, info);
            }
            return result;
        }
    }

    static {
        if (VERSION.SDK_INT >= 16) {
            IMPL = new NavUtilsImplJB();
        } else {
            IMPL = new NavUtilsImplBase();
        }
    }

    public static boolean shouldUpRecreateTask(Activity sourceActivity, Intent targetIntent) {
        return IMPL.shouldUpRecreateTask(sourceActivity, targetIntent);
    }

    public static void navigateUpTo(Activity sourceActivity, Intent upIntent) {
        IMPL.navigateUpTo(sourceActivity, upIntent);
    }

    public static Intent getParentActivityIntent(Activity sourceActivity) {
        return IMPL.getParentActivityIntent(sourceActivity);
    }

    public static Intent getParentActivityIntent(Context context, ComponentName componentName) throws NameNotFoundException {
        String parentActivity = getParentActivityName(context, componentName);
        if (parentActivity == null) {
            return null;
        }
        Intent parentIntent;
        ComponentName target = new ComponentName(componentName.getPackageName(), parentActivity);
        if (getParentActivityName(context, target) == null) {
            parentIntent = IntentCompat.makeMainActivity(target);
        } else {
            parentIntent = new Intent().setComponent(target);
        }
        return parentIntent;
    }

    public static String getParentActivityName(Activity sourceActivity) {
        try {
            return getParentActivityName(sourceActivity, sourceActivity.getComponentName());
        } catch (NameNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getParentActivityName(Context context, ComponentName componentName) throws NameNotFoundException {
        return IMPL.getParentActivityName(context, context.getPackageManager().getActivityInfo(componentName, 128));
    }
}
