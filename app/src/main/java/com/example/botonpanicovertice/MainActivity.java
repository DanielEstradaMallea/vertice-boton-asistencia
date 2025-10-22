package com.example.botonpanicovertice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import com.example.botonpanicovertice.data.PanicAlertRepository;
import com.example.botonpanicovertice.permissions.PermissionManager;
import com.example.botonpanicovertice.preferences.UserPreferences;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AlertPieView.OnAlertListener {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    private static final int MAX_IMAGES = 3;

    private AlertPieView alertPieView;
    private HorizontalScrollView imagePreviewScrollView;
    private LinearLayout imagePreviewContainer;

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<Intent> pickImagesLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri tempImageUri;

    private FusedLocationProviderClient fusedLocationClient;
    private PanicAlertRepository panicAlertRepository;
    private UserPreferences userPreferences;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        panicAlertRepository = new PanicAlertRepository(this);
        userPreferences = new UserPreferences(this);
    }

    private void initializeViews() {
        alertPieView = findViewById(R.id.alert_pie_view);
        alertPieView.setOnAlertListener(this);

        imagePreviewScrollView = findViewById(R.id.imagePreviewScrollView);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

        ExtendedFloatingActionButton fabTakePhoto = findViewById(R.id.fabTakePhoto);
        ExtendedFloatingActionButton fabGallery = findViewById(R.id.fabGallery);

        fabTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        fabGallery.setOnClickListener(v -> dispatchPickFromGalleryIntent());
    }

    private void initializeLaunchers() {
        pickImagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                handleImageSelection(result.getData());
            }
        });

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

    private void dispatchTakePictureIntent() {
        if (!PermissionManager.requestPermissionIfNeeded(this, Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE)) {
            return;
        }
        try {
            tempImageUri = createImageFileUri();
            takePictureLauncher.launch(tempImageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "No se pudo iniciar la cámara.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickFromGalleryIntent() {
        String[] permissions = PermissionManager.getStoragePermissions();
        if (!PermissionManager.requestPermissionsIfNeeded(this, permissions, STORAGE_PERMISSION_REQUEST_CODE)) {
            return;
        }
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
        return androidx.core.content.FileProvider.getUriForFile(this, authority, image);
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
            ArrayList<Uri> limitedList = new ArrayList<>(selectedImageUris.subList(0, MAX_IMAGES));
            selectedImageUris.clear();
            selectedImageUris.addAll(limitedList);
        }
        updateImagePreviews();
    }

    private void updateImagePreviews() {
        imagePreviewContainer.removeAllViews();
        imagePreviewScrollView.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
        for (Uri uri : selectedImageUris) {
            ImageView imageView = new ImageView(this);
            int imageSize = (int) (96 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageSize, imageSize);
            params.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
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

    private void checkFirstRun() {
        if (userPreferences.isFirstRun()) {
            showUserInfoDialog();
        }
    }

    private void showUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Información de Usuario");
        View customLayout = getLayoutInflater().inflate(R.layout.dialog_user_info, null);
        builder.setView(customLayout);

        EditText etNombre = customLayout.findViewById(R.id.etNombre);
        EditText etTelefono = customLayout.findViewById(R.id.etTelefono);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = etNombre.getText().toString();
            String telefono = etTelefono.getText().toString();
            if (!nombre.isEmpty() && !telefono.isEmpty()) {
                userPreferences.saveUserInfo(nombre, telefono);
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
        if (!PermissionManager.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestLocationPermission();
            Toast.makeText(this, "Se necesita permiso de ubicación para enviar la alerta.", Toast.LENGTH_LONG).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String address = getAddressFromCoordinates(this, location.getLatitude(), location.getLongitude());
                sendPanicAlert(alertType, location.getLatitude(), location.getLongitude(), address);
            } else {
                Toast.makeText(this, "Ubicación no disponible. Active el GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendPanicAlert(String alertType, double latitude, double longitude, String address) {
        String userName = userPreferences.getUserName();
        String userPhone = userPreferences.getUserPhone();

        panicAlertRepository.sendPanicAlert(alertType, latitude, longitude, address, userName, userPhone,
                new PanicAlertRepository.AlertCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Alerta enviada correctamente", Toast.LENGTH_LONG).show();
                        selectedImageUris.clear();
                        updateImagePreviews();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MainActivity.this, "Error al enviar alerta: " + message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String getAddressFromCoordinates(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
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

    private void requestLocationPermission() {
        PermissionManager.requestPermissionIfNeeded(this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (!PermissionManager.wasPermissionGranted(grantResults)) {
                    Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show();
                }
                break;
            case STORAGE_PERMISSION_REQUEST_CODE:
                if (PermissionManager.wasAnyPermissionGranted(grantResults)) {
                    dispatchPickFromGalleryIntent();
                } else {
                    Toast.makeText(this, "Permiso de galería denegado.", Toast.LENGTH_LONG).show();
                }
                break;
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (PermissionManager.wasPermissionGranted(grantResults)) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }
}
