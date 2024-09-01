package com.plguerra.geocam;

import java.util.Locale;
import android.view.View;
import java.io.IOException;
import java.util.ArrayList;
import android.content.Intent;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import androidx.recyclerview.widget.RecyclerView;



//Data Adapter used for RecyclerView
public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<ImageUrl> imageUrls;      //Stores a list of Image URLs
    private ArrayList<Integer> PhotoIDs;        //Stores a list of Photo IDs
    private ArrayList <Double> LatitudeValues;  //Sores a list of Latitude Values
    private ArrayList <Double> LongitudeValues; //Stores a list of Longitude Values
    private Context context;


    //DataAdapter Constructor
    public DataAdapter(Context context, ArrayList<ImageUrl> imageUrls, ArrayList<Integer> photoIDs, ArrayList <Double> latitudeValues, ArrayList <Double> longitudeValues) {
        //Assigning all values
        this.context = context;
        this.imageUrls = imageUrls;
        PhotoIDs = photoIDs;
        LatitudeValues = latitudeValues;
        LongitudeValues = longitudeValues;

    }



    //Inflates the Recycle View
    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_view, viewGroup, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        //Load image using the Glide Library
        Glide.with(context).load(imageUrls.get(i).getImageUrl()).into(viewHolder.img);

        viewHolder.img.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get the city where the picture was taken
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String city = null;
                try {
                    ArrayList <Address> addressList = (ArrayList<Address>) geocoder.getFromLocation(LatitudeValues.get(i), LongitudeValues.get(i), 1);
                    city = addressList.get(0).getLocality();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Open New Activity and pass information needed
                Intent intent= new Intent(context, LargeImageView.class);
                intent.putExtra("ImgURL", imageUrls.get(i).getImageUrl());
                intent.putExtra("photoID", PhotoIDs.get(i).toString());
                intent.putExtra("city", city);

                context.startActivity(intent);
            }
        }));
    }



    @Override
    public int getItemCount() {
        return imageUrls.size();
    }



    //Individual images in RecycleView
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        public ViewHolder(View view) {
            super(view);
            img = view.findViewById(R.id.imageView);
        }
    }
}