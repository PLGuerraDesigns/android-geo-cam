package com.plguerra.geocam;

import java.io.File;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.database.Cursor;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import androidx.annotation.Nullable;
import android.content.ContentValues;
import androidx.appcompat.app.AppCompatActivity;


//Enlarged Image View
public class LargeImageView extends AppCompatActivity {
    private int PhotoID;        //Store Photo ID
    TextView cityTextview;     //City Text View
    String Imagepath;          //Store Photo Path



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.large_imageview);

        cityTextview = findViewById(R.id.placetext);
        Button back = findViewById(R.id.backbutton);
        Button delete = findViewById(R.id.deletebutton);
        getintent();

        //If back button pressed then return to main activity
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        //If Delete button pressed the delete the image and return to main activity
        delete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deletePhoto(PhotoID);
                finish();
            }
        });

    }



    private void getintent(){
        //Retrieve info and open image
        if(getIntent().hasExtra("ImgURL") && getIntent().hasExtra("photoID") && getIntent().hasExtra("city")){
            String imageURL = getIntent().getStringExtra("ImgURL");
            Imagepath = imageURL;
            PhotoID = Integer.valueOf(getIntent().getStringExtra("photoID"));
            String place = getIntent().getStringExtra("city");
            cityTextview.setText(place);

            setImage(imageURL);
        }
    }



    private void setImage(String imageURL){
        //Load image into image view using Glide Library
        ImageView imageView = findViewById(R.id.PhotoView);
        Glide.with(this).load(imageURL).into(imageView);

    }



    void deletePhoto(int ID) {
        //Get the current number of photos
        Cursor myCursor = getContentResolver().query(PictureProvider.CONTENT_URI, null, null, null, null);
        int TotalTasks = myCursor.getCount();

        //Delete Photo with given ID from Database
        int didWork = getContentResolver().delete(Uri.parse(PictureProvider.CONTENT_URI + "/" + ID), null, null);
        if (didWork == 1) {
            Toast.makeText(getApplicationContext(), "Photo Deleted", Toast.LENGTH_LONG).show();
        }

        //Delete File from Storage
        File file = new File(Imagepath);
        if (file.exists()) {
            file.delete();
        }

        //Create a ContentValues object
        ContentValues myCV = new ContentValues();
        //Based on the previous number of photos and the photo ID deleted, decrement the subsequent IDs.
        for (int i = ID + 1; i < TotalTasks + 1; i++) {
            myCV.put(PictureProvider.PHOTO_TABLE_COL_ID, i - 1);
            getContentResolver().update(Uri.parse(PictureProvider.CONTENT_URI + "/" + i), myCV, null, null);
        }
    }
}
