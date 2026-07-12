package com.github.sinotca529.fflaunch;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class AppInfo {
    private final String appName;
    private final String packageName;
    private final ResolveInfo resolveInfo;
    private final PackageManager packageManager;
    private volatile Drawable appIcon;

    public AppInfo(ResolveInfo resolveInfo, PackageManager packageManager) {
        this.appName = resolveInfo.loadLabel(packageManager).toString();
        this.packageName = resolveInfo.activityInfo.packageName;
        this.resolveInfo = resolveInfo;
        this.packageManager = packageManager;
    }

    public String getAppName() {
        return appName;
    }

    // アイコンは初回アクセス時に読み込んでキャッシュする (起動時に全件読み込むと重いため)
    public Drawable getAppIcon() {
        Drawable icon = appIcon;
        if (icon == null) {
            synchronized (this) {
                icon = appIcon;
                if (icon == null) {
                    icon = resolveInfo.loadIcon(packageManager);
                    appIcon = icon;
                }
            }
        }
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }
}
