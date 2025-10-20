package com.example.botonpanicovertice;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AlertPieView.OnAlertListener {

    // Constantes
    private static final String PREFS_NAME = "UserInfoPrefs";
    private static final String PREF_FIRST_RUN = "isFirstRun";
    private static final String PREF_USER_NAME = "userName";
    private static final String PREF_USER_PHONE = "userPhone";
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2; // Nuevo
    private static final int MAX_IMAGES = 3; // Nuevo

    // UI y Red
    private AlertPieView alertPieView;
    private RequestQueue requestQueue;
    private FloatingActionButton fabCamera; // Nuevo
    private HorizontalScrollView imagePreviewScrollView; // Nuevo
    private LinearLayout imagePreviewContainer; // Nuevo

    // Servicios de Ubicación
    private FusedLocationProviderClient fusedLocationClient;

    // --- NUEVO: Lógica de selección de imágenes ---
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickImagesLauncher;
    // --- FIN NUEVO ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Volley y cliente de ubicación
        requestQueue = Volley.newRequestQueue(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Comprobar si es la primera vez que se ejecuta
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
        if (isFirstRun) {
            showUserInfoDialog();
        }

        // Configurar la vista del botón de pánico
        alertPieView = findViewById(R.id.alert_pie_view);
        alertPieView.setOnAlertListener(this);

        // --- NUEVO: Configuración del botón de cámara y vistas previas ---
        fabCamera = findViewById(R.id.fabCamera);
        imagePreviewScrollView = findViewById(R.id.imagePreviewScrollView);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

        // Inicializar el launcher para el selector de imágenes
        pickImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleImageSelection(result.getData());
                    }
                });

        // Configurar OnClick para el botón de cámara
        fabCamera.setOnClickListener(v -> openImagePicker());
        // --- FIN NUEVO ---

        // Solicitar permisos de ubicación
        requestLocationPermission();
    }

    // --- NUEVO: Inicia el proceso de selección de imágenes ---
    private void openImagePicker() {
        if (!checkAndRequestStoragePermission()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Seleccionar hasta 3 imágenes"));
    }

    // --- NUEVO: Maneja el resultado del selector de imágenes ---
    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int count = clipData.getItemCount();
            for (int i = 0; i < count; i++) {
                if (selectedImageUris.size() >= MAX_IMAGES) {
                    Toast.makeText(this, "Límite de 3 imágenes alcanzado", Toast.LENGTH_SHORT).show();
                    break;
                }
                Uri imageUri = clipData.getItemAt(i).getUri();
                if (!selectedImageUris.contains(imageUri)) {
                    selectedImageUris.add(imageUri);
                }
            }
        } else if (data.getData() != null) {
            if (selectedImageUris.size() < MAX_IMAGES) {
                Uri imageUri = data.getData();
                if (!selectedImageUris.contains(imageUri)) {
                    selectedImageUris.add(imageUri);
                }
            } else {
                Toast.makeText(this, "Límite de 3 imágenes alcanzado", Toast.LENGTH_SHORT).show();
            }
        }
        updateImagePreviews();
    }

    // --- NUEVO: Muestra las imágenes seleccionadas en la UI ---
    private void updateImagePreviews() {
        imagePreviewContainer.removeAllViews();

        if (selectedImageUris.isEmpty()) {
            imagePreviewScrollView.setVisibility(View.GONE);
            return;
        }

        imagePreviewScrollView.setVisibility(View.VISIBLE);

        for (Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (80 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
            imageView.setLayoutParams(params);
            imageView.setImageURI(uri);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(v -> {
                selectedImageUris.remove(uri);
                updateImagePreviews();
            });
            imagePreviewContainer.addView(imageView);
        }
    }

    // --- NUEVO: Revisa y solicita permisos de almacenamiento ---
    private boolean checkAndRequestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        }
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

    @Override
    public void onAlert(String alertType) {
        Log.d(TAG, "Tipo de alerta recibida: '" + alertType + "'");
        Log.d(TAG, "Imágenes seleccionadas: " + selectedImageUris.size());
        getCurrentLocationAndSendAlert(alertType);
    }

    private void getCurrentLocationAndSendAlert(String alertType) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de ubicación no concedido.", Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, "Ubicación obtenida: Lat=" + latitude + ", Lon=" + longitude);
                String address = getAddressFromCoordinates(latitude, longitude);
                sendPanicAlert(alertType, latitude, longitude, address);
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación. Asegúrate de que el GPS esté activado.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getAddressFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressText = "Dirección no encontrada";
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                addressText = address.getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error de Geocoder", e);
        }
        return addressText;
    }

    private void sendPanicAlert(String alertType, double latitude, double longitude, String address) {
        // En un futuro, aquí se procesarían las 'selectedImageUris' para enviarlas.
        String url = "https://prototipo-vertice-production.up.railway.app/api/v1/alertas/panico/";
        final String token = "4ZUEJQnaSzT54Q0Dq61Pk5iCiFkC6Mj_hw0-rf7lN4X8kvABhYEIc97SIOVmYG_E";

        JSONObject postData = new JSONObject();
        try {
            String categoria;
            switch (alertType.toUpperCase()) {
                case "SEGURIDAD": categoria = "4"; break;
                case "SALUD": categoria = "1"; break;
                case "INCENDIO": categoria = "2"; break;
                case "ASISTENCIA": categoria = "5"; break;
                default:
                    Log.e(TAG, "Tipo de alerta desconocido: '" + alertType + "'");
                    return;
            }
            postData.put("categoria", categoria);
            postData.put("descripcion", "Alerta de pánico tipo: " + alertType);
            postData.put("latitud", latitude);
            postData.put("longitud", longitude);
            postData.put("direccion", address);
            postData.put("procedencia", "2");
        } catch (JSONException e) {
            Log.e(TAG, "Error creando el objeto JSON", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    Toast.makeText(this, "Alerta enviada correctamente", Toast.LENGTH_LONG).show();
                    selectedImageUris.clear();
                    updateImagePreviews();
                },
                error -> {
                    Log.e(TAG, "ERROR EN LA RESPUESTA: " + error.toString());
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Toast.makeText(this, "Error del servidor: " + responseBody, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error al decodificar el cuerpo del error", e);
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de galería concedido", Toast.LENGTH_SHORT).show();
                openImagePicker();
            } else {
                Toast.makeText(this, "Permiso de galería denegado.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
