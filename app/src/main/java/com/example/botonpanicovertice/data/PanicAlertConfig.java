package com.example.botonpanicovertice.data;

import com.example.botonpanicovertice.BuildConfig;

/**
 * Centraliza la configuración sensible relacionada con el envío de alertas.
 * Permite mantener las credenciales fuera de la lógica de interfaz de usuario.
 */
public class PanicAlertConfig {

    public String getEndpointUrl() {
        return BuildConfig.PANIC_ALERT_URL;
    }

    public String getAuthorizationToken() {
        return BuildConfig.PANIC_ALERT_TOKEN;
    }
}
