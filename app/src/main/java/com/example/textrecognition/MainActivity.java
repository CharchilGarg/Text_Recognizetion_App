package com.example.textrecognition;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button inputImage,recognizeImage;
    private ImageView imageView;
    private TextView textView;

    private static final String tag = "tag";

    private Uri imageUri = null;

    private static final int Camera_Request_code = 100;
    private static final int Storage_Request_code = 101;

    private String[] cameraPermission;
    private String[] storagePermission;

    private TextRecognizer textRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputImage = findViewById(R.id.inputImage);
        recognizeImage = findViewById(R.id.recognize);
        imageView = findViewById(R.id.imageVi);
        textView = findViewById(R.id.output);
        cameraPermission = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);


        inputImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }


        });

        recognizeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(imageUri == null)
                {
                    Toast.makeText(MainActivity.this, "Pick an image first...", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    recognizeTextFromImage();
                }
            }
        });

    }

    private void recognizeTextFromImage() {

        try
        {
            InputImage inputImage = InputImage.fromFilePath(this,imageUri);

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            String outputText = text.getText();

                            textView.setText(outputText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        catch (IOException e)
        {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            //e.printStackTrace();
        }

    }

    private void showInputImageDialog() {

        PopupMenu popupMenu = new PopupMenu(this,inputImage);

        popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                int id = menuItem.getItemId();

                if(id == 1)
                {
                    if(checkCameraPermission())
                    {
                        pickImageCamera();
                    }
                    else
                    {
                        requestCameraPermission();
                    }
                }
                else if(id == 2)
                {
                    if(checkStoragePermission())
                    {
                        pickImageGallery();
                    }
                    else
                    {
                        requestStoragePermission();
                    }
                }

                return true;
            }
        });

    }

    private void pickImageGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        gallery.launch(intent);
    }

    private ActivityResultLauncher<Intent> gallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK)
                    {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        imageView.setImageURI(imageUri);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Cancelled..", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void pickImageCamera()
    {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Sample description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        camera.launch(intent);
    }

    private ActivityResultLauncher<Intent> camera = registerForActivityResult
            (new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if(result.getResultCode() == Activity.RESULT_OK)
                            {
                                imageView.setImageURI(imageUri);
                            }
                            else
                            {
                                Toast.makeText(MainActivity.this, "Canceled....", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private boolean checkStoragePermission()
    {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission()
    {
        ActivityCompat.requestPermissions(this,storagePermission,Storage_Request_code);
    }

    private boolean checkCameraPermission()
    {
        boolean resultStorage = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean resultCamera = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        return resultStorage && resultCamera;
    }

    private void requestCameraPermission()
    {
        ActivityCompat.requestPermissions(this,cameraPermission,Camera_Request_code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case Camera_Request_code:
            {
                if(grantResults.length > 0)
                {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && storageAccepted)
                    {
                        pickImageCamera();
                    }
                    else
                    {
                        Toast.makeText(this, "Permission requested..... ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            case Storage_Request_code:
            {
                if(grantResults.length > 0)
                {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storageAccepted)
                    {
                        pickImageGallery();
                    }
                    else
                    {
                        Toast.makeText(this, "Storage permission requested....", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}