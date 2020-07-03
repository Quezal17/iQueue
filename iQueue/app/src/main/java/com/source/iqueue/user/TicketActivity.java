package com.source.iqueue.user;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.source.iqueue.R;
import com.source.iqueue.Ticket;
import com.source.iqueue.manager.Manager;

public class TicketActivity extends AppCompatActivity {

    private String userId, queueId;

    //View reference
    private ImageView shopImageView, qrCodeView, deleteTicketView;
    private TextView shopNameView, shopCityView, shopAddressView, ticketNumberView, ticketCodeView;

    //Firebase reference
    private DatabaseReference mTicketsDatabase, mQueuesDatabase, mManagersDatabase;
    private StorageReference mImagesDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);
        queueId = getIntent().getStringExtra("queueId");
        userId = FirebaseAuth.getInstance().getUid();

        mManagersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");
        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");
        mTicketsDatabase = FirebaseDatabase.getInstance().getReference().child("Tickets");
        mImagesDatabase = FirebaseStorage.getInstance().getReference();

        deleteTicketView = (ImageView) findViewById(R.id.deleteTicketView);
        shopImageView = (ImageView) findViewById(R.id.shopImageView);
        qrCodeView = (ImageView) findViewById(R.id.ticketQrCode);
        shopNameView = (TextView) findViewById(R.id.shopNameView);
        shopCityView = (TextView) findViewById(R.id.shopCityView);
        shopAddressView = (TextView) findViewById(R.id.shopAddressView);
        ticketNumberView = (TextView) findViewById(R.id.ticketNumber);
        ticketCodeView = (TextView) findViewById(R.id.ticketCode);

        deleteTicketView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTicket();
            }
        });

        retrieveTicketsData();
    }

    private void deleteTicket() {
        mTicketsDatabase.child(queueId).child(userId).removeValue();
        finish();
    }

    private void retrieveTicketsData() {
        Query ticketsQuery = mTicketsDatabase.child(queueId).child(userId);
        ticketsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Ticket ticket = dataSnapshot.getValue(Ticket.class);
                    ticketNumberView.setText("" + ticket.getNumber());
                    ticketCodeView.setText("" + ticket.getCode());
                    downloadFromStorage(qrCodeView, "QRCodeImages/" + ticket.getQrCodeName());
                    retrieveQueueInfo();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void retrieveQueueInfo() {
        mQueuesDatabase.child(queueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String managerId = dataSnapshot.child("managerId").getValue().toString();
                retrieveShopInfo(managerId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void retrieveShopInfo(String managerId) {
        mManagersDatabase.child(managerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Manager manager = dataSnapshot.getValue(Manager.class);
                shopNameView.setText(manager.getShopName());
                shopCityView.setText(manager.getShopCity());
                shopAddressView.setText(manager.getShopAddress());
                downloadFromStorage(shopImageView, manager.getShopImage());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void downloadFromStorage(final ImageView imageView, String imagePath) {
        if(!imagePath.isEmpty()) {
            StorageReference imageRef = mImagesDatabase.child(imagePath);
            imageRef.getBytes(1024 * 1024)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imageView.setImageBitmap(bitmap);
                        }
                    });
        }
    }
}
