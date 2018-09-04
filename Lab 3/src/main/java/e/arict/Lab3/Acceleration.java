package e.arict.Lab3;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

import android.net.Uri;
import android.os.AsyncTask;

import android.os.Environment;
import android.util.Log;

import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.FrameLayout;

import static android.content.ContentValues.TAG;


public class Acceleration extends Activity implements AccelerometerListener {

    private ImageButton mBtGoBack;
    Preview preview;
    Camera camera;
    Activity act;
    Context ctx;
    boolean movedRecently;
    int threshold = 20; //threshold for acceleration (higher means more movement required)

    String MGRS = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceleration);
        ctx = this;
        act = this;
        movedRecently = false;

        MGRS = getIntent().getStringExtra("MGRS");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((LinearLayout) findViewById(R.id.accelLayout)).addView(preview);
        preview.setKeepScreenOn(true);

        mBtGoBack = (ImageButton) findViewById(R.id.accelGoBack_bt);
        mBtGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        int numCams = Camera.getNumberOfCameras();

        if(numCams > 0){
            try{
                //camera = Camera.open(1);

                camera = Camera.open(0);
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(ctx, "Camera Not Found", Toast.LENGTH_LONG).show();
            }
        }

        if (AccelerometerManager.isSupported(this)) {
            AccelerometerManager.startListening(this);
        }
    }

    @Override

    protected void onPause() {

        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }

        AccelerometerManager.stopListening();
        super.onPause();
    }

    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
        movedRecently = false;
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
            //movedRecently = false;
        }
    };

    @Override
    public void onAccelerationChanged(float x, float y, float z) {
        if(x+y+z > threshold){
            movedRecently = true;
            camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            Toast.makeText(getApplicationContext(), "Picture Taken", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShake(float force) {
        if (!movedRecently) {

            //latitude = mLocation.getLatitude();
            //longitude = mLocation.getLongitude();
            //String message = "Lat: " + Double.toString(latitude) + "\nLng: " + Double.toString(longitude);

            movedRecently = true;
            camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            Toast.makeText(getApplicationContext(), "Picture Taken", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//Check device supported Accelerometer sensor or not
        if (AccelerometerManager.isListening()) {
//Start Accelerometer Listening
            AccelerometerManager.stopListening();
            Toast.makeText(this, "onStop Accelerometer Stopped", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AccelerometerManager.isListening()) {
            AccelerometerManager.stopListening();

            Toast.makeText(this, "onDestroy Accelerometer Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;
            // Write to SD Card
            try {

                File imageDirectory =
                        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Lab 3");
                if(!imageDirectory.exists()){
                    if(!imageDirectory.mkdirs()){
                        return null;
                    }
                }
                String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
                String coords = MGRS;//toMGRS(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                String mCurrentPhotoPath = imageDirectory.getPath() + File.separator + coords + "_" + timeStamp + ".jpg";

                File image = new File(mCurrentPhotoPath);


                //Toast.makeText(getApplicationContext(), Double.toString(latitude), Toast.LENGTH_SHORT).show();
                //String fileName = String.format("%d.jpg", System.currentTimeMillis());
                //String fileName = String.format(System.currentTimeMillis()+"lat"+Double.toString(latitude)+"long"+Double.toString(longitude)+".jpg");
                //fileName += "Lat"+Double.toString(latitude)+"Long"+Double.toString(longitude);
                //Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_SHORT).show();
                //File outFile = new File(dir, fileName);
                outStream = new FileOutputStream(image);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + image.getAbsolutePath());
                refreshGallery(image);
                //movedRecently = false;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            //movedRecently = false;
            return null;
        }



    }
}
