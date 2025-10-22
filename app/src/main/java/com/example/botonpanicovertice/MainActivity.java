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
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private static final int MAX_IMAGES = 3;

    // UI y Red
    private AlertPieView alertPieView;
    private RequestQueue requestQueue;
    private HorizontalScrollView imagePreviewScrollView;
    private LinearLayout imagePreviewContainer;

    // Servicios de Ubicación
    private FusedLocationProviderClient fusedLocationClient;

    // Lógica de selección de imágenes
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri tempImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeLogic();
        initializeViews();
        initializeLaunchers();

        checkFirstRun();
        requestLocationPermission();
    }

    private void initializeLogic() {
        requestQueue = Volley.newRequestQueue(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeViews() {
        alertPieView = findViewById(R.id.alert_pie_view);
        alertPieView.setOnAlertListener(this);
        FloatingActionButton fabCamera = findViewById(R.id.fabTakePhoto);
        imagePreviewScrollView = findViewById(R.id.imagePreviewScrollView);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        fabCamera.setOnClickListener(v -> showImageSourceDialog());
    }

    private void initializeLaunchers() {
        // Launcher para seleccionar imágenes de la galería
        pickImagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                handleImageSelection(result.getData());
            }
        });

        // Launcher para tomar una foto con la cámara
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && tempImageUri != null) {
                if (selectedImageUris.size() < MAX_IMAGES) {
                    selectedImageUris.add(tempImageUri);
                    updateImagePreviews();
                } else {
                    Toast.makeText(this, "Límite de " + MAX_IMAGES + " imágenes alcanzado.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Adjuntar Imagen")
                .setItems(new CharSequence[]{"Tomar Foto", "Elegir de la Galería"}, (dialog, which) -> {
                    if (which == 0) { // "Tomar Foto"
                        dispatchTakePictureIntent();
                    } else { // "Elegir de la Galería"
                        dispatchPickFromGalleryIntent();
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        if (!checkAndRequestPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)) return;
        try {
            tempImageUri = createImageFileUri();
            takePictureLauncher.launch(tempImageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "No se pudo iniciar la cámara.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickFromGalleryIntent() {
        if (!checkAndRequestStoragePermission()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(Intent.createChooser(intent, "Seleccionar hasta " + MAX_IMAGES + " imágenes"));
    }

    private Uri createImageFileUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp, ".jpg", storageDir);
        String authority = getApplicationContext().getPackageName() + ".provider";
        return FileProvider.getUriForFile(this, authority, image);
    }

    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                if (selectedImageUris.size() >= MAX_IMAGES) break;
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                if (!selectedImageUris.contains(imageUri)) selectedImageUris.add(imageUri);
            }
        } else if (data.getData() != null) {
            if (selectedImageUris.size() < MAX_IMAGES) {
                Uri imageUri = data.getData();
                if (!selectedImageUris.contains(imageUri)) selectedImageUris.add(imageUri);
            }
        }
        if (selectedImageUris.size() > MAX_IMAGES) {
            selectedImageUris = new ArrayList<>(selectedImageUris.subList(0, MAX_IMAGES));
        }
        updateImagePreviews();
    }

    private void updateImagePreviews() {
        imagePreviewContainer.removeAllViews();
        imagePreviewScrollView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
        for (Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            int imageSize = (int) (80 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageSize, imageSize);
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

    private boolean checkAndRequestStoragePermission() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else { // Android 12 e inferior
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        boolean permissionsGranted = true;
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }
        if (permissionsGranted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, permissionsToRequest, STORAGE_PERMISSION_REQUEST_CODE);
            return false;
        }
    }

    private boolean checkAndRequestPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
    }

    private void checkFirstRun() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_FIRST_RUN, true)) {
            showUserInfoDialog();
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
        builder.create().show();
    }

    @Override
    public void onAlert(String alertType) {
        Log.d(TAG, "Alerta recibida: '" + alertType + "'");
        getCurrentLocationAndSendAlert(alertType);
    }

    private void getCurrentLocationAndSendAlert(String alertType) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            Toast.makeText(this, "Se necesita permiso de ubicación para enviar la alerta.", Toast.LENGTH_LONG).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String address = getAddressFromCoordinates(location.getLatitude(), location.getLongitude());
                sendPanicAlert(alertType, location.getLatitude(), location.getLongitude(), address);
            } else {
                Toast.makeText(this, "Ubicación no disponible. Active el GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getAddressFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addressText = "Dirección no encontrada";
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                addressText = addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e(TAG, "Servicio de Geocoder no disponible o error", e);
        }
        return addressText;
    }

    /**
     * --- LÓGICA DE API RESTAURADA ---
     * Construye y envía la petición POST a tu servidor.
     */
    private void sendPanicAlert(String alertType, double latitude, double longitude, String address) {
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

            // Obtener datos del usuario para incluir en los logs
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userName = prefs.getString(PREF_USER_NAME, "Desconocido");
            String userPhone = prefs.getString(PREF_USER_PHONE, "Sin teléfono");

            postData.put("categoria", categoria);
            postData.put("descripcion", "Alerta de pánico tipo: " + alertType);
            postData.put("latitud", latitude);
            postData.put("longitud", longitude);
            postData.put("direccion", address);
            postData.put("procedencia", "2");

            // 🔹 LOGS DETALLADOS ANTES DEL ENVÍO
            Log.d(TAG, "=== DATOS DE ALERTA A ENVIAR ===");
            Log.d(TAG, "Usuario: " + userName + " (" + userPhone + ")");
            Log.d(TAG, "Tipo de alerta: " + alertType);
            Log.d(TAG, "Categoría ID: " + categoria);
            Log.d(TAG, "Latitud: " + latitude);
            Log.d(TAG, "Longitud: " + longitude);
            Log.d(TAG, "Dirección: " + address);
            Log.d(TAG, "Procedencia: 2");
            Log.d(TAG, "JSON final enviado: " + postData.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Error creando el objeto JSON", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    Log.i(TAG, "✅ Alerta enviada correctamente. Respuesta del servidor: " + response.toString());
                    Toast.makeText(this, "Alerta enviada correctamente", Toast.LENGTH_LONG).show();
                    selectedImageUris.clear();
                    updateImagePreviews();
                },
                error -> {
                    Log.e(TAG, "❌ ERROR EN LA RESPUESTA: " + error.toString());
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Respuesta del servidor: " + responseBody);
                            Toast.makeText(this, "Error del servidor: " + responseBody, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error al decodificar el cuerpo del error", e);
                        }
                    } else {
                        Toast.makeText(this, "Error de red al enviar alerta.", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("Authorization", "Bearer " + token);
                Log.d(TAG, "Encabezados de la petición: " + headers.toString());
                return headers;
            }
        };

        Log.d(TAG, "📡 Enviando petición POST a: " + url);
        requestQueue.add(jsonObjectRequest);
    }

    private void requestLocationPermission() {
        checkAndRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (!granted) Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show();
                break;
            case STORAGE_PERMISSION_REQUEST_CODE:
                boolean storageGranted = false;
                for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) storageGranted = true;
                if (storageGranted) dispatchPickFromGalleryIntent();
                else Toast.makeText(this, "Permiso de galería denegado.", Toast.LENGTH_LONG).show();
                break;
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (granted) dispatchTakePictureIntent();
                else Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_LONG).show();
                break;
        }
    }
}

