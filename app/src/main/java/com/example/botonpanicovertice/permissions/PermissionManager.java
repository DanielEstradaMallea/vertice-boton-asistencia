package com.example.botonpanicovertice.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utilidad para gestionar de forma centralizada las solicitudes de permisos.
 */
public final class PermissionManager {

    private PermissionManager() {
        // Utility class
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context == null || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean requestPermissionIfNeeded(Activity activity, String permission, int requestCode) {
        if (hasPermissions(activity, permission)) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        return false;
    }

    public static boolean requestPermissionsIfNeeded(Activity activity, String[] permissions, int requestCode) {
        if (hasPermissions(activity, permissions)) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
        return false;
    }

    public static boolean wasPermissionGranted(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean wasAnyPermissionGranted(@NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            };
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{android.Manifest.permission.READ_MEDIA_IMAGES};
        }
        return new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
    }
}
