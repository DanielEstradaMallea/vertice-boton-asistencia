package com.example.botonpanicovertice;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AlertPieView.OnAlertListener {

    // Constantes para SharedPreferences y permisos
    private static final String PREFS_NAME = "UserInfoPrefs";
    private static final String PREF_FIRST_RUN = "isFirstRun";
    private static final String PREF_USER_NAME = "userName";
    private static final String PREF_USER_PHONE = "userPhone";
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // UI y Red
    private AlertPieView alertPieView;
    private RequestQueue requestQueue;

    // Servicios de Ubicación
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Volley y cliente de ubicación
        requestQueue = Volley.newRequestQueue(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Comprobar si es la primera vez que se ejecuta para pedir datos de usuario
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
        if (isFirstRun) {
            showUserInfoDialog();
        }

        // Configurar la vista del botón de pánico
        alertPieView = findViewById(R.id.alert_pie_view);
        alertPieView.setOnAlertListener(this);

        // Solicitar permisos de ubicación
        requestLocationPermission();
    }

    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Información de Usuario");
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_user_info, null);
        builder.setView(customLayout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            EditText etNombre = customLayout.findViewById(R.id.etNombre);
            EditText etTelefono = customLayout.findViewById(R.id.etTelefono);
            String nombre = etNombre.getText().toString();
            String telefono = etTelefono.getText().toString();

            if (!nombre.isEmpty() && !telefono.isEmpty()) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(PREF_FIRST_RUN, false);
                editor.putString(PREF_USER_NAME, nombre);
                editor.putString(PREF_USER_PHONE, telefono);
                editor.apply();
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Este método se activa cuando el usuario completa la acción en AlertPieView.
     */
    @Override
    public void onAlert(String alertType) {
        Log.d(TAG, "Tipo de alerta recibida: '" + alertType + "'");
        // Inicia el proceso para obtener la ubicación y luego enviar la alerta
        getCurrentLocationAndSendAlert(alertType);
    }

    /**
     * Obtiene la ubicación actual y, si tiene éxito, llama a los métodos para procesarla y enviarla.
     */
    private void getCurrentLocationAndSendAlert(String alertType) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido.", Toast.LENGTH_LONG).show();
            // Opcional: Podrías enviar una alerta sin ubicación aquí si quisieras
            // sendPanicAlert(alertType, 0, 0, "Ubicación no disponible");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, "Ubicación obtenida: Lat=" + latitude + ", Lon=" + longitude);

                // Convierte las coordenadas a una dirección legible
                String address = getAddressFromCoordinates(latitude, longitude);

                // Envía la alerta completa al servidor
                sendPanicAlert(alertType, latitude, longitude, address);
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación. Asegúrate de que el GPS esté activado.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Convierte coordenadas (lat, lon) en una dirección de texto usando Geocoder.
     */
    private String getAddressFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressText = "Dirección no encontrada";
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // address.getAddressLine(0) suele devolver la dirección completa formateada
                addressText = address.getAddressLine(0);
                Log.d(TAG, "Dirección encontrada: " + addressText);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error de Geocoder", e);
            addressText = "Error al obtener la dirección desde las coordenadas";
        }
        return addressText;
    }

    /**
     * Construye y envía la petición POST al servidor con todos los datos de la alerta.
     */
    private void sendPanicAlert(String alertType, double latitude, double longitude, String address) {
        String url = "https://prototipo-vertice-production.up.railway.app/api/v1/alertas/panico/";
        final String token = "4ZUEJQnaSzT54Q0Dq61Pk5iCiFkC6Mj_hw0-rf7lN4X8kvABhYEIc97SIOVmYG_E";

        JSONObject postData = new JSONObject();
        try {
            String categoria;
            if (alertType.equalsIgnoreCase("Seguridad")) categoria = "4";
            else if (alertType.equalsIgnoreCase("Salud")) categoria = "1";
            else if (alertType.equalsIgnoreCase("Incendio")) categoria = "2";
            else if (alertType.equalsIgnoreCase("Asistencia")) categoria = "5";
            else {
                Log.e(TAG, "Tipo de alerta desconocido: '" + alertType + "'. No se enviará la petición.");
                Toast.makeText(this, "Error: Tipo de alerta no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            postData.put("categoria", categoria);
            postData.put("descripcion", "Alerta de pánico tipo: " + alertType);
            postData.put("latitud", latitude);
            postData.put("longitud", longitude);
            postData.put("direccion", address);
            postData.put("procedencia", "2");

            Log.d(TAG, "JSON a enviar: " + postData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creando el objeto JSON", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    Log.d(TAG, "RESPUESTA EXITOSA: " + response.toString());
                    Toast.makeText(this, "Alerta enviada correctamente", Toast.LENGTH_LONG).show();
                },
                error -> {
                    Log.e(TAG, "ERROR EN LA RESPUESTA: " + error.toString());
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "CUERPO DEL ERROR DEL SERVIDOR: " + responseBody);
                            Toast.makeText(this, "Error del servidor: " + responseBody, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error al decodificar el cuerpo del error", e);
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    /**
     * Verifica si la app ya tiene permisos de ubicación. Si no, los solicita.
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Maneja el resultado del diálogo de solicitud de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado. La app no podrá enviar la ubicación en tiempo real.", Toast.LENGTH_LONG).show();
            }
        }
    }
}