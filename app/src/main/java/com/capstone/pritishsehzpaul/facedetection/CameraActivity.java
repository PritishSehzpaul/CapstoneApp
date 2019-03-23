package com.capstone.pritishsehzpaul.facedetection;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLEY_IMAGES = 2;
    public static final int REQUEST_STORAGE_PERMISSION = 100;
    private StoragePermissionDialog storagePermissionDialog = new StoragePermissionDialog();
    private String currentPhotoPath;
    private SimpleDraweeView mSimpleDraweeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_camera);

        mSimpleDraweeView = (SimpleDraweeView) findViewById(R.id.simple_drawee_view);
    }


    public void onTakePicture(View view) {
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Error occured while taking photo", ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.capstone.pritishsehzpaul.fileprovider",
                        photoFile);
                Log.d(TAG, "Photo URI: " + photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //Checking if the app should provide the user with an explanation for the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                storagePermissionDialog.show(getSupportFragmentManager(), TAG);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        } else {
            String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
            String imageFileName = "IMG_" + timeStamp + "_";
            File parentDir = Environment.getExternalStorageDirectory();
            File storageDir = new File(parentDir, "Face Detection/Media/Pictures");
            Log.e(TAG, "External Directory Path: " + storageDir.getAbsolutePath());

            boolean isCreated = storageDir.mkdirs();
            Log.w(TAG, "Storage directory created: " + isCreated);

            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = image.getAbsolutePath();
            Log.d(TAG, "Current Photo Path: " + currentPhotoPath);
            return image;
        }
        return null;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        Log.d(TAG, "Picture added to gallery");
    }


//    private void setPic() {
//        // Get the dimensions of the View
//        int targetW = mSimpleDraweeView.getWidth();
//        int targetH = mSimpleDraweeView.getHeight();
//
//        // Get the dimensions of the bitmap
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
//        int photoW = bmOptions.outWidth;
//        int photoH = bmOptions.outHeight;
//
//        // Determine how much to scale down the image
//        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
//
//        // Decode the image file into a Bitmap sized to fill the View
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;
//
//        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
//        mSimpleDraweeView.setImageBitmap(bitmap);
//    }

    private void loadImageUsingFresco() {
        File imageFile = new File(currentPhotoPath);
        Uri imagUri = Uri.fromFile(imageFile);
        mSimpleDraweeView.setImageURI(imagUri);
    }

    public void loadImageFromGallery(View view) {
        Log.d(TAG, "Called loadImageFromGallery");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(intent, REQUEST_GALLEY_IMAGES);
        } catch (ActivityNotFoundException e) {
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLEY_IMAGES);
        }
    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bitmap = null;
        Uri imageUri = null;
        if (data != null) {
            try {
                Log.d(TAG, "DATA OF INTENT IS: " + data.getDataString());
                imageUri = data.getData();
                bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                Log.e(TAG, "Image selected is corrupted", e);
            }
        }

        RoundingParams roundingParams = RoundingParams.fromCornersRadius(45f);
        roundingParams.setBorder(getColor(R.color.imageBorderColor), 6f);
        mSimpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
        mSimpleDraweeView.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
        mSimpleDraweeView.setImageURI(imageUri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    File picFile = new File(currentPhotoPath);
                    if (picFile.exists()) {
                        if (picFile.length() > 0) {
                            galleryAddPic();
//                    setPic();
                            loadImageUsingFresco();
                        } else {
                            boolean isDeleted = picFile.delete();
                            Log.w(TAG, "Picture was not stored correctly. Was it deleted? " + isDeleted);
                        }
                    }
                }
                break;

            case REQUEST_GALLEY_IMAGES:
                if (resultCode == RESULT_OK) {
                    onSelectFromGalleryResult(data);
                }
                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO Add a cool graphic drawable hidden and display it here when permission is given and add animation over it
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Storage permission rejected", Toast.LENGTH_LONG).show();
                }
        }
    }
}
