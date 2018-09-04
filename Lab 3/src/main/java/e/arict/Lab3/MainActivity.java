package e.arict.Lab3;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    // CODE TO HAVE THE APP TAKE A PICTURE

    private String mCurrentPhotoPath;
    private ImageView mImageView;
    private Uri photoURI;

    //CODE TO HAVE APP SAVE A PICTURE WITH LOCATION INFORMATION

    //----------------------- CREATE NAME FOR NEW PHOTO ------------------------
    private File createImageFile() throws IOException {
        // Create an image file name
        Log.d("Camera Test","Checkpoint 1");
        File imageDirectory =
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "Lab 3");
        if(!imageDirectory.exists()){
            if(!imageDirectory.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        String coords = toMGRS(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        mCurrentPhotoPath = imageDirectory.getPath() + File.separator + coords + "_" + timeStamp + ".jpg";
        Log.d("Camera Test",mCurrentPhotoPath);

        File image = new File(mCurrentPhotoPath);
        // Save a file: path for use with ACTION_VIEW intents

        return image;
    }

    //----------------------- CAPTURE AND SHOW THUMBNAIL ------------------------
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntentThumbnail() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //----------------------- CREATE AND SHOW NEW PHOTO ------------------------
    static final int REQUEST_TAKE_PHOTO = 2;

    private void dispatchTakePictureIntentSave() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try { photoFile = createImageFile();}
            catch(IOException ex) {
             // Error occurred while creating the File
             Log.d("Exception", "Not getting file URI");
            }

            if (photoFile != null) {
                Log.d("ImagePath", photoFile.getAbsolutePath());
                photoURI = FileProvider.getUriForFile(MainActivity.this,
                     BuildConfig.APPLICATION_ID + ".fileprovider", photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }

    }

    /*
    THIS CODE WORKS FOR THUMBNAIL ONLY, BUT WILL RUN AFTER FULL-SIZE PHOTO TAKEN,
    SO MUST CHANGE REQUEST_IMAGE_CAPTURE to 0
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Toast.makeText(MainActivity.this,Integer.compare(requestCode,1), Toast.LENGTH_LONG).show();

        Bundle extras = null;
        switch(requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bitmap thumbnailBitmap = (Bitmap) data.getExtras().get("data");
                    ImageView imageView = (ImageView) findViewById(R.id.thumbView);
                    imageView.setImageBitmap(thumbnailBitmap);
                }
            case REQUEST_TAKE_PHOTO:
                if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
                    Log.d("galleryAddPic", "should print path next");
                    Uri selectedImage = photoURI;
                    getContentResolver().notifyChange(selectedImage, null);
                    ImageView imageView = (ImageView) findViewById(R.id.photoView);
                    ContentResolver cr = getContentResolver();
                    Bitmap photoBitmap;
                    try{
                        photoBitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
                        imageView.setImageBitmap(photoBitmap);
                        Toast.makeText(this,selectedImage.toString(), Toast.LENGTH_SHORT).show();
                        galleryAddPic();
                    } catch (Exception e){
                        Toast.makeText(this,"Failed to Load", Toast.LENGTH_SHORT).show();
                        Log.e("Camera", e.toString());
                    }
                }
        }
    }
/*
The following example method demonstrates how to invoke the system's media scanner to
add your photo to the Media Provider's database, making it available in the Android Gallery
 application and to other apps.
 */
    protected void galleryAddPic(){
        Log.d("galleryAddPic", mCurrentPhotoPath);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    // CODE TO GET LOCATION UPDATES

    TextView txtLatLong;
    TextView txtAlt;
    TextView MGRSCoordinates;
    protected String latitude, longitude;
    protected boolean gps_enabled, network_enabled;

    private String rightButtonTester = "Test";
    private String savedRightButtonTester;
    String myLocationProvider;
    Location lastKnownLocation;
    LocationManager myLocationManager;
    LocationListener myLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            String latNum = Double.toString( Math.round( location.getLatitude() * 10000.0 ) / 10000.0 );
            String longNum = Double.toString( Math.round( location.getLongitude() * 10000.0 ) / 10000.0 );
            String altNum = Double.toString( Math.round( location.getAltitude() * 10000.0 ) / 10000.0 );
            //Toast.makeText(getApplicationContext(), "Location Changed!", Toast.LENGTH_LONG).show();
            //set the text of current textView pointers to display current info coming in from GPS or NETWORK
            txtLatLong.setText("Latitude:" + latNum + ", Longitude:" + longNum);
            txtAlt.setText( "Altitude:" + altNum );

            MGRSCoordinates.setText( toMGRS(location.getLatitude(), location.getLongitude()) );
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("Latitude","status");
        }
        @Override
        public void onProviderDisabled(String provider) {
            Log.d("Latitude","disable");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d("Latitude","enable");
        }
    };
    //------------------------------------------------  onCREATE() -----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.my_title);

        //For GeoLocation Determination
        final Button leftButton = (Button) findViewById(R.id.leftButton);
        final Button rightButton = (Button) findViewById(R.id.rightButton);
        final ImageButton launchAccelActivity = (ImageButton) findViewById((R.id.imageButton));

        txtLatLong = findViewById(R.id.gpsLatLong);
        txtAlt = findViewById(R.id.gpsAltitude);
        MGRSCoordinates = findViewById(R.id.MGRSCoords);
        myLocationProvider = LocationManager.NETWORK_PROVIDER;
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                return;
            }
            myLocationManager.requestLocationUpdates(myLocationProvider, 1000, 1, myLocationListener);
        }catch(Exception e) {
            Toast.makeText(getApplicationContext(), "Exception!", Toast.LENGTH_LONG).show();
        }

        //Have onCreate - now need to look at the other 'on' methods and implement them
        if(savedInstanceState != null){
            rightButtonTester = savedInstanceState.getString(savedRightButtonTester);
            //leftButton.setText(myLocationProvider);
            rightButton.setText(rightButtonTester);
            //Toast.makeText(getApplicationContext(), myLocationProvider, Toast.LENGTH_LONG).show();
        }
        else{
            leftButton.setText(myLocationProvider);
            rightButton.setText("Turn Services Off");
        }

        //------------------------------------------------CAMERA CODE-----------------------------------------------------------------------------

        lastKnownLocation = myLocationManager.getLastKnownLocation(myLocationProvider);

        //Issue is that the app takes both pictures one after the other, but then puts the second picture taken in the 'photoView' instead of the first
        //This makes no sense

        // MAIN LOCATION DISPLAY SCREEN BUTTONS
        launchAccelActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                launchAccelActivity();
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplicationContext(), myLocationProvider, Toast.LENGTH_LONG).show();
                rightButton.setText("Turn Services Off");
                if(lastKnownLocation != null){
                    mImageView = findViewById(R.id.photoView);
                    dispatchTakePictureIntentSave();
                    mImageView = findViewById(R.id.thumbView);
                    dispatchTakePictureIntentThumbnail();
                }
                myLocationProvider = changeProvider(view, myLocationProvider);
                leftButton.setText(myLocationProvider);
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                        return;
                    }
                    myLocationManager.requestLocationUpdates(myLocationProvider, 3000, 1, (LocationListener) myLocationListener);
                }catch(Exception e) {
                    Toast.makeText(getApplicationContext(), "Exception!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Look Here Exp: " + e.getMessage());
                }
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                    return;
                }

                lastKnownLocation = myLocationManager.getLastKnownLocation(myLocationProvider);
                String latNum = "";
                String longNum = "";
                String altNum = "";
                if(lastKnownLocation != null) {
                     latNum = Double.toString(Math.round(lastKnownLocation.getLatitude() * 10000.0) / 10000.0);
                     longNum = Double.toString(Math.round(lastKnownLocation.getLongitude() * 10000.0) / 10000.0);
                     altNum = Double.toString(Math.round(lastKnownLocation.getAltitude() * 10000.0) / 10000.0);
                }else{
                    latNum = "N/A";
                    longNum = "N/A";
                    altNum = "N/A";
                }
                //Toast.makeText(getApplicationContext(), "made it", Toast.LENGTH_LONG).show();
                myLocationManager.removeUpdates(myLocationListener);
                rightButton.setText("Loc Services Off" );
                leftButton.setText("Turn Services On" );
                //point textView pointers to 'lastKnown' text objects on screen
                txtLatLong = findViewById(R.id.lastKnownLatLong);
                txtAlt = findViewById(R.id.lastKnownAltitude);
                //set current textView pointers to lastKnownLocation info
                txtLatLong.setText("Latitude:" + latNum + ", Longitude:" + longNum );
                txtAlt.setText( "Altitude:" + altNum );
                //point textView pointers to 'network' text objects on screen
                txtLatLong = findViewById(R.id.networkLatLong);
                txtAlt = findViewById(R.id.networkAltitude);
                //set current textView pointers to 'OFF'
                txtLatLong.setText("NETWORK OFF");
                txtAlt.setText( "NETWORK OFF" );
                //point textView pointers to 'gps' text objects on screen
                txtLatLong = findViewById(R.id.gpsLatLong);
                txtAlt = findViewById(R.id.gpsAltitude);
                //set current textView pointers to 'OFF'
                txtLatLong.setText("GPS OFF");
                txtAlt.setText( "GPS OFF" );
            }
        });
    }
    private void launchAccelActivity() {

        Intent intent = new Intent(this, Acceleration.class);
        if(latitude == null){
            intent.putExtra("MGRS", toMGRS(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude() ));
        }else {
            intent.putExtra("MGRS", toMGRS(Double.parseDouble(latitude), Double.parseDouble(longitude)));
        }
        startActivity(intent);
    }
    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){

        //Toast.makeText(getApplicationContext(), rightButtonTester, Toast.LENGTH_LONG).show();
        outState.putString(savedRightButtonTester, rightButtonTester);
        //Toast.makeText(getApplicationContext(), savedRightButtonTester, Toast.LENGTH_LONG).show();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }
        lastKnownLocation = myLocationManager.getLastKnownLocation(myLocationProvider);

    }

    public String changeProvider(View view, String myLocationProvider) {

        Log.d("changeProvider","button pressed");
        if(myLocationProvider == "gps") {
            myLocationProvider = "network";
            //set current textView pointers to 'OFF'
            txtLatLong.setText("GPS OFF");
            txtAlt.setText( "GPS OFF" );
            //point textView pointers to 'network' text objects on screen
            txtLatLong = findViewById(R.id.networkLatLong);
            txtAlt = findViewById(R.id.networkAltitude);
            //set current textView pointers to 'Searching'
            txtLatLong.setText("Network Searching");
            txtAlt.setText( "Network Searching" );
            //Toast.makeText(getApplicationContext(), "if-statement", Toast.LENGTH_LONG).show();
        }else {
            //Toast.makeText(getApplicationContext(), "else-statement", Toast.LENGTH_LONG).show();
            myLocationProvider = "gps";
            //set current textView pointers to 'OFF'
            txtLatLong.setText("NETWORK OFF");
            txtAlt.setText( "NETWORK OFF" );
            //point textView pointers to 'gps' text objects on screen
            txtLatLong = findViewById(R.id.gpsLatLong);
            txtAlt = findViewById(R.id.gpsAltitude);
            //set current textView pointers to 'Searching'
            txtLatLong.setText("GPS Searching");
            txtAlt.setText( "GPS Searching" );

        }
        return myLocationProvider;
    }

    public String toMGRS(double latitude, double longitude) {
        if (latitude < -80) return "Too far South" ; if (longitude > 84) return "Too far North" ;
        double c = 1 + Math.floor ((longitude+180)/6);
        double e = c*6 - 183 ;
        double k = latitude*Math.PI/180;
        double l = longitude*Math.PI/180;
        double m = e*Math.PI/180;
        double n = Math.cos (k);
        double o = 0.006739496819936062*Math.pow (n,2);
        double p = 40680631590769.0/(6356752.314*Math.sqrt(1 + o));
        double q = Math.tan (k);
        double r = q*q;
        double s = (r*r*r) - Math.pow (q,6);
        double t = l - m;
        double u = 1.0 - r + o;
        double v = 5.0 - r + 9*o + 4.0*(o*o);
        double w = 5.0 - 18.0*r + (r*r) + 14.0*o - 58.0*r*o;
        double x = 61.0 - 58.0*r + (r*r) + 270.0*o - 330.0*r*o;
        double y = 61.0 - 479.0*r + 179.0*(r*r) - (r*r*r);
        double z = 1385.0 - 3111.0*r + 543.0*(r*r) - (r*r*r);
        double aa = p*n*t + (p/6.0*Math.pow (n,3)*u*Math.pow (t,3)) + (p/120.0*Math.pow (n,5)*w*Math.pow (t,5)) + (p/5040.0*Math.pow (n,7)*y*Math.pow (t,7));
        double ab = 6367449.14570093*(k - (0.00251882794504*Math.sin (2*k)) + (0.00000264354112*Math.sin (4*k)) - (0.00000000345262*Math.sin (6*k)) + (0.000000000004892*Math.sin (8*k))) + (q/2.0*p*Math.pow (n,2)*Math.pow (t,2)) + (q/24.0*p*Math.pow (n,4)*v*Math.pow (t,4)) + (q/720.0*p*Math.pow (n,6)*x*Math.pow (t,6)) + (q/40320.0*p*Math.pow (n,8)*z*Math.pow (t,8));
        aa = aa*0.9996 + 500000.0;
        ab = ab*0.9996; if (ab < 0.0) ab += 10000000.0;

        char ad = "CDEFGHJKLMNPQRSTUVWXX".charAt( (int)Math.floor(latitude/8 + 10) );

        int ae = (int) Math.floor (aa/100000);
        //char af = ["ABCDEFGH","JKLMNPQR","STUVWXYZ"][(c-1)%3].charAt(ae-1);
        char af = "ABCDEFGH".charAt(ae-1);
        if((c-1)%3 == 0){ af = "ABCDEFGH".charAt(ae-1);}
        else if((c-1)%3 == 1){ af = "JKLMNPQR".charAt(ae-1);}
        else if((c-1)%3 == 2){ af = "STUVWXYZ".charAt(ae-1);};

        int ag = (int) Math.floor (ab/100000)%20;
        //char ah = ["ABCDEFGHJKLMNPQRSTUV","FGHJKLMNPQRSTUVABCDE"][(c-1)%2].charAt(ag);
        char ah = "ABCDEFGHJKLMNPQRSTUV".charAt(ag);
        if((c-1)%2 == 0){
            ah = "ABCDEFGHJKLMNPQRSTUV".charAt(ag);
        } else{// ((c-1)%2 == 1)
            ah = "FGHJKLMNPQRSTUVABCDE".charAt(ag);
        };

        aa = Math.floor (aa%100000);
        ab = Math.floor (ab%100000);

        return Integer.toString((int) c) + ad + ' ' + af + ah + ' ' + pad( (int) aa) + ' ' + pad((int) ab);
    }

    public String pad(int val) {
        String string = Integer.toString(val);
        if (val < 10) {string = "0000" + Integer.toString(val);}
        else if (val < 100) {string = "000" + Integer.toString(val);}
        else if (val < 1000) {string = "00" + Integer.toString(val);}
        else if (val < 10000) {string = "0" + Integer.toString(val);};
        return string;
    }
//    @Override
//    public void onLocationChanged(Location location) {
//            rightButtonTester = toMGRS(location.getLatitude(), location.getLongitude());
//
//            String latNum = Double.toString( java.lang.Math.round( location.getLatitude() * 10000.0 ) / 10000.0 );
//            String longNum = Double.toString( java.lang.Math.round( location.getLongitude() * 10000.0 ) / 10000.0 );
//            String altNum = Double.toString( java.lang.Math.round( location.getAltitude() * 10000.0 ) / 10000.0 );
//            Toast.makeText(getApplicationContext(), "Location Changed!", Toast.LENGTH_LONG).show();
//            set the text of current textView pointers to display current info coming in from GPS or NETWORK
//            txtLatLong.setText("Latitude:" + latNum + ", Longitude:" + longNum);
//            txtAlt.setText( "Altitude:" + altNum );
//    }
//
//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) {
//        Log.d("Latitude","status");
//    }
//    @Override
//    public void onProviderDisabled(String provider) {
//        Log.d("Latitude","disable");
//    }
//
//    @Override
//    public void onProviderEnabled(String provider) {
//        Log.d("Latitude","enable");
//    }



}