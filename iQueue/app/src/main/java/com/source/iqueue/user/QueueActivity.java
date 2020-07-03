package com.source.iqueue.user;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.WriterException;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.Ticket;
import com.source.iqueue.manager.Manager;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class QueueActivity extends AppCompatActivity {

    private TextView shopNameView, shopCityView, shopAddressView, peopleCountView, currentNumberView, snackBar;
    private ImageView shopImageView;
    private MaterialButton btnPickTicket;

    private String queueId;
    private String userId;

    //Firebase reference
    private DatabaseReference mQueuesDatabase, mManagerDatabase, mTicketsDatabase;
    private StorageReference mStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        queueId = getIntent().getStringExtra("queueId");
        userId = FirebaseAuth.getInstance().getUid();

        mTicketsDatabase = FirebaseDatabase.getInstance().getReference().child("Tickets").child(queueId);
        mManagerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");
        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");
        mStorageReference = FirebaseStorage.getInstance().getReference();

        shopNameView = (TextView) findViewById(R.id.shopNameView);
        shopCityView = (TextView) findViewById(R.id.shopCityView);
        shopAddressView = (TextView) findViewById(R.id.shopAddressView);
        peopleCountView = (TextView) findViewById(R.id.peopleInQueue);
        currentNumberView = (TextView) findViewById(R.id.currentNumber);
        shopImageView = (ImageView) findViewById(R.id.shopImageView);
        snackBar = (TextView) findViewById(R.id.snackBar_text);
        btnPickTicket = (MaterialButton) findViewById(R.id.btnPickTicket);
        btnPickTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTicket();
            }
        });

        retrieveData();
    }

    private void pickTicket() {
        mTicketsDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null)
                    checkQueueState();
                else
                    Snackbar.make(snackBar, R.string.snackBar_ticketAlreadyTaken, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkQueueState() {
        mQueuesDatabase.child(queueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Queue queue = dataSnapshot.getValue(Queue.class);
                if(queue != null) {
                    if(queue.getState().equals("opened")) {
                        generateTicket(queue);
                    } else {
                        Snackbar.make(snackBar, R.string.snackBar_queueClosed, Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Snackbar.make(snackBar, R.string.snackBar_queueDeleted, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void generateTicket(Queue queue) {
        int ticketNumber = queue.getTotalNumber() + 1;
        long ticketCode = System.currentTimeMillis();
        mQueuesDatabase.child(queueId).child("totalNumber").setValue(ticketNumber);
        generateQrCode(ticketNumber, ticketCode);
    }

    private void generateQrCode(final int ticketNumber, final long ticketCode) {
        final String imgName = UUID.randomUUID().toString();
        String data = ticketCode + "," + ticketNumber + "," + queueId + "," + userId;
        QRGEncoder qrgEncoder = new QRGEncoder(data,null, QRGContents.Type.TEXT,500);
        try {
            Bitmap qrBits = qrgEncoder.encodeAsBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qrBits.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imgBytes = baos.toByteArray();

            UploadTask uploadTask = mStorageReference.child("QRCodeImages").child(imgName).putBytes(imgBytes);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Ticket newTicket = new Ticket(ticketCode, ticketNumber, imgName);
                    mTicketsDatabase.child(userId).setValue(newTicket);

                    Snackbar.make(snackBar, R.string.snackBar_success, Snackbar.LENGTH_SHORT).show();
                }
            });
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void retrieveData() {
        mTicketsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long peopleCount = dataSnapshot.getChildrenCount();
                peopleCountView.setText("" + peopleCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mQueuesDatabase.child(queueId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Queue queue = dataSnapshot.getValue(Queue.class);
                if(queue != null) {
                    currentNumberView.setText("" + queue.getCurrentNumber());
                    String managerId = queue.getManagerId();
                    retrieveShopData(managerId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void retrieveShopData(String managerId) {
        mManagerDatabase.child(managerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Manager manager = dataSnapshot.getValue(Manager.class);
                if(manager != null) {
                    shopNameView.setText(manager.getShopName());
                    shopCityView.setText(manager.getShopCity());
                    shopAddressView.setText(manager.getShopAddress());
                    String shopImage = manager.getShopImage();
                    downloadFromStorage(shopImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void downloadFromStorage(String imagePath) {
        if(!imagePath.isEmpty()) {
            mStorageReference.child(imagePath).getBytes(1024 * 1024)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            shopImageView.setImageBitmap(bitmap);
                        }
                    });
        }
    }
}
