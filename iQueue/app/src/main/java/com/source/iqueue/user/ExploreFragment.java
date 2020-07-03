package com.source.iqueue.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.manager.Manager;

import static android.app.Activity.RESULT_OK;

public class ExploreFragment extends Fragment{

    private static final int LOCATION_REQUEST_CODE = 2;
    private static final int GPS_NOT_EXIST = 6;
    private static final int GPS_NOT_ACTIVE = 7;
    private static final int PERMISSIONS_DENIED = 8;

    //View references
    private RecyclerView recyclerView;
    private MaterialButton btnNearYou;
    private TextView snackBar;

    //Firebase references
    private StorageReference mImagesDatabase;
    private DatabaseReference mQueuesDatabase, mManagersDatabase;
    private FirebaseRecyclerAdapter recyclerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore, null);

        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");
        mManagersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");
        mImagesDatabase = FirebaseStorage.getInstance().getReference();

        snackBar = (TextView) rootView.findViewById(R.id.snackBar_text);
        btnNearYou = (MaterialButton) rootView.findViewById(R.id.btnNearYou);
        btnNearYou.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent locationIntent = new Intent(getContext(), NearYouActivity.class);
                startActivityForResult(locationIntent, LOCATION_REQUEST_CODE);
            }
        });
        recyclerView = (RecyclerView) rootView.findViewById(R.id.explore_queueList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return rootView;
    }

    private void downloadFromStorage(String imagePath, final QueueViewHolder holder) {
        if(!imagePath.isEmpty()) {
            StorageReference imageRef = mImagesDatabase.child(imagePath);
            imageRef.getBytes(1024 * 1024)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            holder.shopIconView.setImageBitmap(bitmap);
                        }
                    });
        } else {
            holder.shopIconView.setImageDrawable(null);
        }
    }

    private void retrieveData() {
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Queue>()
                .setQuery(mQueuesDatabase.orderByChild("state").equalTo("opened"), Queue.class)
                .build();

        recyclerAdapter = new FirebaseRecyclerAdapter<Queue, QueueViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final QueueViewHolder holder, int position, @NonNull Queue model) {
                final String queueId = getRef(position).getKey();
                mManagersDatabase.child(model.getManagerId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Manager manager = dataSnapshot.getValue(Manager.class);
                        holder.shopNameView.setText(manager.getShopName());
                        holder.shopCityView.setText(manager.getShopCity());
                        downloadFromStorage(manager.getShopImage(), holder);
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent queueIntent = new Intent(getContext(), QueueActivity.class);
                                queueIntent.putExtra("queueId", queueId);
                                startActivity(queueIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @NonNull
            @Override
            public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_queue_item_list, parent, false);
                QueueViewHolder viewHolder = new QueueViewHolder(view);
                return viewHolder;
            }
        };

        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            Snackbar.make(snackBar, R.string.locationRegisterSuccess, Snackbar.LENGTH_SHORT).show();
        }else if(requestCode == LOCATION_REQUEST_CODE && resultCode == GPS_NOT_EXIST) {
            Snackbar.make(snackBar, R.string.gpsNotExist, Snackbar.LENGTH_SHORT).show();
        } else if(requestCode == LOCATION_REQUEST_CODE && resultCode == GPS_NOT_ACTIVE) {
            Snackbar.make(snackBar, R.string.gpsNotActive, Snackbar.LENGTH_SHORT).show();
        } else if(requestCode == LOCATION_REQUEST_CODE && resultCode == PERMISSIONS_DENIED) {
            Snackbar.make(snackBar, R.string.gpsPermissionDenied, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        retrieveData();
    }

    @Override
    public void onStop() {
        super.onStop();
        recyclerAdapter.stopListening();
    }

    private static class QueueViewHolder extends RecyclerView.ViewHolder{
        TextView shopNameView, shopCityView;
        ImageView shopIconView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            shopNameView = itemView.findViewById(R.id.user_itemList_shopName);
            shopCityView = itemView.findViewById(R.id.user_itemList_shopCity);
            shopIconView = itemView.findViewById(R.id.user_itemList_shopIcon);
        }
    }
}

