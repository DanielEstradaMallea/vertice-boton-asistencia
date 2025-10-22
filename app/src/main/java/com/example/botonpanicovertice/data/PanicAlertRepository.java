package com.example.botonpanicovertice.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.botonpanicovertice.utils.AlertCategoryMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona la comunicación con el backend para el envío de alertas de pánico.
 */
public class PanicAlertRepository {

    private static final String TAG = "PanicAlertRepository";

    public interface AlertCallback {
        void onSuccess();

        void onError(String message);
    }

    private final RequestQueue requestQueue;
    private final PanicAlertConfig config;

    public PanicAlertRepository(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.config = new PanicAlertConfig();
    }

    public void sendPanicAlert(
            String alertType,
            double latitude,
            double longitude,
            String address,
            String userName,
            String userPhone,
            AlertCallback callback
    ) {
        String categoryId = AlertCategoryMapper.getCategoryFor(alertType);
        if (categoryId == null) {
            Log.e(TAG, "Tipo de alerta desconocido: '" + alertType + "'");
            if (callback != null) {
                callback.onError("Tipo de alerta desconocido");
            }
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("categoria", categoryId);
            payload.put("descripcion", "Alerta de pánico tipo: " + alertType);
            payload.put("latitud", latitude);
            payload.put("longitud", longitude);
            payload.put("direccion", address);
            payload.put("procedencia", "2");
        } catch (JSONException exception) {
            Log.e(TAG, "Error creando el payload de la alerta", exception);
            if (callback != null) {
                callback.onError("No se pudo preparar la alerta");
            }
            return;
        }

        logPayload(alertType, categoryId, latitude, longitude, address, userName, userPhone, payload);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                config.getEndpointUrl(),
                payload,
                response -> {
                    Log.i(TAG, "Alerta enviada correctamente: " + response);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                },
                error -> {
                    String errorMessage = parseError(error);
                    Log.e(TAG, "Error al enviar la alerta: " + errorMessage, error);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Bearer " + config.getAuthorizationToken());
                return headers;
            }
        };

        Log.d(TAG, "Enviando POST a " + config.getEndpointUrl());
        requestQueue.add(request);
    }

    private void logPayload(String alertType, String categoryId, double latitude, double longitude, String address,
                             String userName, String userPhone, JSONObject payload) {
        Log.d(TAG, "=== DATOS DE ALERTA ===");
        Log.d(TAG, "Usuario: " + userName + " (" + userPhone + ")");
        Log.d(TAG, "Tipo de alerta: " + alertType);
        Log.d(TAG, "Categoría ID: " + categoryId);
        Log.d(TAG, "Latitud: " + latitude);
        Log.d(TAG, "Longitud: " + longitude);
        Log.d(TAG, "Dirección: " + address);
        Log.d(TAG, "Payload JSON: " + payload);
    }

    private String parseError(VolleyError error) {
        if (error == null) {
            return "Error desconocido";
        }
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                return new String(error.networkResponse.data, "utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "No se pudo decodificar el mensaje de error", e);
            }
        }
        return error.getMessage() != null ? error.getMessage() : "Fallo de red";
    }
}
