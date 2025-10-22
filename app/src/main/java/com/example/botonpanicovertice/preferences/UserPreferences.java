package com.example.botonpanicovertice.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Maneja el almacenamiento ligero de los datos del usuario.
 */
public class UserPreferences {

    private static final String PREFS_NAME = "UserInfoPrefs";
    private static final String KEY_FIRST_RUN = "isFirstRun";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_PHONE = "userPhone";

    private final SharedPreferences sharedPreferences;

    public UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstRun() {
        return sharedPreferences.getBoolean(KEY_FIRST_RUN, true);
    }

    public void saveUserInfo(String name, String phone) {
        sharedPreferences.edit()
                .putBoolean(KEY_FIRST_RUN, false)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, phone)
                .apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "Desconocido");
    }

    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, "Sin teléfono");
    }
}
