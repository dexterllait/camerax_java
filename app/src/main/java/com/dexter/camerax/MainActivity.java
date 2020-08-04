package com.dexter.camerax;

import android.content.pm.PackageManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Variables
    private Preview preview;
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isBackLens;
    private boolean isFlash;

    // UI Variable
    private PreviewView viewFinder;
    private ImageView imgCapture, imgFlash, imgSwitchCamera;

    private int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVariable();
        initUI();

        checkPermissions();
    }

    private void initVariable() {
        isBackLens = true;
        isFlash = false;
    }

    private void initUI() {
        viewFinder = findViewById(R.id.viewFinder);
        imgCapture = findViewById(R.id.img_capture);
        imgFlash = findViewById(R.id.img_flash);
        imgSwitchCamera = findViewById(R.id.img_switch_camera);

        imgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capture();
            }
        });
        imgFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFlash = !isFlash;
                startCamera();
            }
        });
        imgSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isBackLens = !isBackLens;
                startCamera();
            }
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    void checkPermissions() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // Used to bind the lifecycle of cameras to the lifecycle owner
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Preview
                    preview = new Preview.Builder()
                            .build();
                    // Select back camera
                    CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(isBackLens ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                            .build();

                    // Image Capture
                    int flashMode = ImageCapture.FLASH_MODE_OFF;
                    if (isBackLens && isFlash) {
                        flashMode = ImageCapture.FLASH_MODE_ON;
                    }
                    imageCapture = new ImageCapture.Builder()
                            .setFlashMode(flashMode)
                            .build();

                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture);
                    preview.setSurfaceProvider(viewFinder.createSurfaceProvider());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void capture() {
        shootSound();
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PHOTO.jpg");
        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(file);
                        Log.e(TAG, savedUri.toString());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, exception.getLocalizedMessage());
                    }
                }
        );
    }

    public void shootSound() {
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
    }
}
