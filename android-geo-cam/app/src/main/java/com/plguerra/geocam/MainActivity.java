package com.plguerra.geocam;

import java.io.File;
import java.util.Date;
import android.net.Uri;
import java.util.Locale;
import android.Manifest;
import android.os.Bundle;
import java.io.IOException;
import java.util.ArrayList;
import android.widget.Toast;
import android.view.MenuItem;
import android.os.Environment;
import android.content.Intent;
import android.widget.TextView;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import java.text.SimpleDateFormat;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.widget.LinearLayout;
import android.content.ContentValues;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import com.google.android.gms.tasks.Task;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import com.google.android.gms.maps.GoogleMap;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    GoogleMap map;                                          //Map object
    TextView textView;                                      //All Photos Label
    int CurrentSelection;                                   //States if Photos or Map view is selected
    String currentPhotoPath;                                //Name of the file Saved by camera
    Location currentLocation;                               //Users current Location
    LinearLayout linearLayout;                              //No photos display
    RecyclerView recyclerView;                              //Recycler view for all images
    SupportMapFragment mapFragment;                         //Map display fragment
    BottomNavigationView Navigation;                        //Bottom Navigation Bar
    ArrayList <Integer> PhotoIDList;                        //Stores list of Photo IDs
    ArrayList <ImageUrl> imageUrlList;                      //Stores list of Image URLs
    ArrayList <Double> LatitudeValues;                      //Stores list of Latitude values
    ArrayList <Double> LongitudeValues;                     //Stores list of Longitude values
    GridLayoutManager gridLayoutManager;                    //Grid Layout for all images

    static final int REQUEST_TAKE_PHOTO = 1;
    public static final int REQUEST_CODE = 101;
    FusedLocationProviderClient mFusedLocationClient;
    FragmentManager fm = getSupportFragmentManager();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout = findViewById(R.id.NoPhotos);
        textView = findViewById(R.id.PhotosText);
        gridLayoutManager = new GridLayoutManager(getApplicationContext(), 4);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(gridLayoutManager);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Navigation = findViewById(R.id.navigation);
        Navigation.setOnNavigationItemSelectedListener(NavListener);
        Navigation.getMenu().findItem(R.id.photos).setChecked(true);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fm.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).hide(mapFragment).commit();
        LoadPhotos();
        }



    @Override
    protected void onResume() {
        super.onResume();
        if(CurrentSelection == 0) {
            Navigation.getMenu().findItem(R.id.photos).setChecked(true);
        }
        else if(CurrentSelection == 2){
            prepareData();
            LoadMarkers();
            Navigation.getMenu().findItem(R.id.map).setChecked(true);
        }
        LoadPhotos();
    }



    //Bottom Navigation Select Listener
    private BottomNavigationView.OnNavigationItemSelectedListener NavListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    switch (menuItem.getItemId())
                    {
                        case R.id.photos:
                            CurrentSelection = 0;
                            //If photo view then hide map and load photos
                            fm.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).hide(mapFragment).commit();
                            LoadPhotos();
                            break;

                        case R.id.camera:
                            //If camera view then update location, launch camera and hide map
                            getLocation();
                            dispatchTakePictureIntent();
                            break;

                        case R.id.map:
                            CurrentSelection = 2;
                            //If map view then load all markers and show map
                            LoadMarkers();
                            fm.beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).show(mapFragment).commit();
                            break;
                    }
                    return true;
                }
            };



    //Get needed information and load images into RecyclerView
    private void LoadPhotos(){
        ArrayList imageUrlList = prepareData();
        DataAdapter dataAdapter = new DataAdapter(this, imageUrlList, PhotoIDList, LatitudeValues, LongitudeValues);
        recyclerView.setAdapter(dataAdapter);
    }



    //Load all necessary information into the lists
    private ArrayList prepareData() {
        imageUrlList = new ArrayList<>();
        LatitudeValues = new ArrayList<>();
        LongitudeValues = new ArrayList<>();
        PhotoIDList = new ArrayList<>();

        //Perform query to get all rows in the DB
        Cursor myCursor = getContentResolver().query(PictureProvider.CONTENT_URI,null,null,null,null);

        assert myCursor != null;
        for(myCursor.moveToFirst(); !myCursor.isAfterLast(); myCursor.moveToNext()) {
            ImageUrl imageUrl = new ImageUrl();
            PhotoIDList.add(myCursor.getInt(0));
            imageUrl.setImageUrl(myCursor.getString(1));
            LatitudeValues.add(Double.valueOf(myCursor.getString(2)));
            LongitudeValues.add(Double.valueOf(myCursor.getString(3)));
            imageUrlList.add(imageUrl);
        }

        //If No Photos Exist then Show feedback
        if(imageUrlList.size() != 0){
            linearLayout.setVisibility(LinearLayout.GONE);
            textView.setVisibility(LinearLayout.VISIBLE);
        }
        else{
            linearLayout.setVisibility(LinearLayout.VISIBLE);
            textView.setVisibility(LinearLayout.GONE);
        }

        return imageUrlList;
    }



    //Call Camera to take a photo
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create file to store photo
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Failed to take photo", Toast.LENGTH_SHORT).show();
            }
            // Continue if File was created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.plguerra.geocam.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Once camera returns photo then add it to the DataBase
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            getLocation();
            //Make sure we get a valid location
            if(currentLocation != null){
                AddPhoto(currentPhotoPath, String.valueOf(currentLocation.getLatitude()), String.valueOf(currentLocation.getLongitude()));
            }
            else {
                AddPhoto(currentPhotoPath, "", "");
            }
        }
    }



    //Create the image File
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



    //Store Photo Information Into DataBase
    void AddPhoto(String photoTitle, String Lat, String Long){
        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Put key_value pairs based on the column names, and the values
        myCV.put(PictureProvider.PHOTO_TABLE_COL_PHOTONAME,photoTitle);
        myCV.put(PictureProvider.PHOTO_TABLE_COL_LATITUDE,Lat);
        myCV.put(PictureProvider.PHOTO_TABLE_COL_LONGITUDE,Long);

        //Perform the insert function using the ContentProvider
        getContentResolver().insert(PictureProvider.CONTENT_URI,myCV);
    }



    //get Location when map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        getLocation();
    }



    //Load all markers based on photos
    public void LoadMarkers(){
        //Clear all marker first
        map.clear();

        for(int i = 0; i < LatitudeValues.size(); i++){
            if(LatitudeValues.get(i) != 0 && LongitudeValues.get(i) != 0){
                LatLng point = new LatLng(LatitudeValues.get(i), LongitudeValues.get(i));
                Marker marker = map.addMarker(new MarkerOptions().position(point));
                marker.setTag(i+1);
                map.setOnMarkerClickListener(this);
            }
        }
    }



    //Get Current Location of the User
    private void getLocation(){

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
        }
            Task<Location> task = mFusedLocationClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        map.setMyLocationEnabled(true);
                        currentLocation = location;
                        LatLng currentlatlng = new LatLng(location.getLatitude(), location.getLongitude());
                        float zoomLevel = 5.0f;
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentlatlng, zoomLevel));
                    }
                }
            });
    }



    //When marker is clicked
    @Override
    public boolean onMarkerClick(Marker marker) {
        //Retrieve the Photo ID from the marker.
        String PhotoID = marker.getTag().toString();

        //Get City from the latlng
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String city = null;
        try {
            ArrayList <Address> addressList = (ArrayList<Address>) geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
            city = addressList.get(0).getLocality();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Start New Activity (Large Image View) and pass it necessary information
        Intent intent= new Intent(this, LargeImageView.class);
        intent.putExtra("ImgURL", imageUrlList.get(Integer.valueOf(PhotoID)-1).getImageUrl());
        intent.putExtra("photoID", PhotoID);
        intent.putExtra("city", city);
        this.startActivity(intent);

        return true;
    }
}
