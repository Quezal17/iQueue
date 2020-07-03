package com.source.iqueue.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.Ticket;
import com.source.iqueue.posthttp.FirebaseNotificationAPI;
import com.source.iqueue.posthttp.PostRequestBody;
import com.source.iqueue.posthttp.PostRequestNotification;

import static com.source.iqueue.posthttp.RequestConstantsValue.BASE_URL;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QueueManagingActivity extends AppCompatActivity {

    private static final int QRCODE_RESULT = 5;
    private static final int PERMISSIONS_DENIED = 6;
    private static final int CAMERA_NOT_EXIST = 7;

    private String queueId;
    private String managerId;
    private String queueName;

    //View references
    private TextView textQueueName, textPeopleCount, textBigNumber, snackbarText;
    private MaterialButton btnDecrementNumber, btnIncrementNumber, btnLocalTicket, btnScanTicketCode, btnScanQRCode;
    private SwitchMaterial switchQueueState;
    private TextInputLayout ticketCodeInput;

    //Firebase references
    DatabaseReference mCurrentQueueRef, mTicketsDatabase, mTokensDatabase;
    StorageReference qrImagesDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_managing);

        qrImagesDatabase = FirebaseStorage.getInstance().getReference().child("QRCodeImages");
        mTokensDatabase = FirebaseDatabase.getInstance().getReference().child("Tokens");

        textQueueName = (TextView) findViewById(R.id.managing_queueName);
        textPeopleCount = (TextView) findViewById(R.id.managing_peopleCount);
        textBigNumber = (TextView) findViewById(R.id.bigNumber);
        btnDecrementNumber = (MaterialButton) findViewById(R.id.managing_btnDecrementNumber);
        btnIncrementNumber = (MaterialButton) findViewById(R.id.managing_btnIncrementNumber);
        switchQueueState = (SwitchMaterial) findViewById(R.id.switchQueueState);
        snackbarText = (TextView) findViewById(R.id.snackBar_text);
        btnLocalTicket = (MaterialButton) findViewById(R.id.managing_btnLocalTicket);
        btnScanTicketCode = (MaterialButton) findViewById(R.id.btnScanTicketCode);
        btnScanQRCode = (MaterialButton) findViewById(R.id.btnScanQRCode);
        ticketCodeInput = (TextInputLayout) findViewById(R.id.ticketCodeInput);

        queueId = getIntent().getStringExtra("queueId");
        managerId = getIntent().getStringExtra("managerId");
        queueName = getIntent().getStringExtra("queueName");
        textQueueName.setText(queueName);

        retrieveData();

        btnDecrementNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrementCurrentNumberQueue();
            }
        });

        btnIncrementNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementCurrentNumberQueue();
            }
        });

        switchQueueState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeQueueState(switchQueueState.isChecked());
            }
        });

        btnLocalTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent biglietteriaIntent = new Intent(QueueManagingActivity.this, LocalTicketActivity.class);
                biglietteriaIntent.putExtra("queueId", queueId);
                startActivity(biglietteriaIntent);
            }
        });

        btnScanTicketCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();
                String ticketCode = ticketCodeInput.getEditText().getText().toString().trim();
                if(!ticketCode.isEmpty())
                    checkTicketFromCode(ticketCode);
                ticketCodeInput.getEditText().setText("");
            }
        });

        btnScanQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanQRIntent = new Intent(QueueManagingActivity.this, QRCodeActivity.class);
                startActivityForResult(scanQRIntent, QRCODE_RESULT);
            }
        });

    }

    private void closeKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            ticketCodeInput.clearFocus();
        }catch (NullPointerException e){}
    }

    private void checkTicketFromCode(final String ticketCode) {
        Query query = mTicketsDatabase.child(queueId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    Ticket ticket = ds.getValue(Ticket.class);
                    if (ticket != null && ticket.getCode() == Long.parseLong(ticketCode)) {
                        String identifier = ds.getKey();
                        checkTicketWithCurrentNumber(identifier, ticket);
                    } else {
                        Snackbar.make(snackbarText, R.string.snackBar_qrCodeInvalidTicket, Snackbar.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void decrementCurrentNumberQueue() {
        mCurrentQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int currentNumber = Integer.parseInt(dataSnapshot.child("currentNumber").getValue().toString());
                if(currentNumber > 0) {
                    mCurrentQueueRef.child("currentNumber").setValue(currentNumber - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkIfNeedSendNotification(final int ticketNumber) {
        mTicketsDatabase.child(queueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    if(ds != null) {
                        Ticket ticket = ds.getValue(Ticket.class);
                        if(ticket.getNumber() == ticketNumber && !ticket.getQrCodeName().isEmpty()) {
                            sendNotification(ds.getKey());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(String userId) {
        PostRequestNotification notification = new PostRequestNotification("iQueue", "Sei il prossimo, preparati!");
        PostRequestBody body = new PostRequestBody("/topics/" + userId, notification);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        FirebaseNotificationAPI service = retrofit.create(FirebaseNotificationAPI.class);
        retrofit2.Call<ResponseBody> responseBodyCall = service.sendNotification(body);
        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                Log.d("sendNotification","inviata");
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.d("sendNotification","fallito");
            }
        });
    }

    private void _incrementCurrentNumber() {
        mCurrentQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String state = dataSnapshot.child("state").getValue().toString();
                if(state.equals("opened")) {
                        int currentNumber = Integer.parseInt(dataSnapshot.child("currentNumber").getValue().toString());
                        mCurrentQueueRef.child("currentNumber").setValue(currentNumber + 1);
                        checkIfNeedSendNotification(currentNumber+2);
                } else
                    Snackbar.make(snackbarText, R.string.managing_queueClosedSnackBarText, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void incrementCurrentNumberQueue() {
        mTicketsDatabase.child(queueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    final long peopleInQueue = dataSnapshot.getChildrenCount();
                    if (peopleInQueue > 0) {
                        _incrementCurrentNumber();
                    }
                    else
                        Snackbar.make(snackbarText, R.string.managing_noPeopleInQueueSnackBarText, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void changeQueueState(Boolean isChecked) {
        if(isChecked) {
            Queue q = new Queue(queueName,managerId,0,0,"opened");
            mCurrentQueueRef.setValue(q);
            mTicketsDatabase.child(queueId).removeValue();
        } else {
            mCurrentQueueRef.child("state").setValue("closed");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == QRCODE_RESULT) {
            String qrData = data.getStringExtra("qrData");
            checkTicketFromQR(qrData);
        } else if(resultCode == PERMISSIONS_DENIED) {
            Snackbar.make(snackbarText, R.string.cameraPermissionRequired, Snackbar.LENGTH_SHORT).show();
        } else if(resultCode == CAMERA_NOT_EXIST) {
            Snackbar.make(snackbarText, R.string.cameraNotExist, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void checkTicketFromQR(String qrData) {
        final String[] arrayData = qrData.split(",");
        final DatabaseReference ticketRef = mTicketsDatabase.child(arrayData[2]).child(arrayData[3]);
        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Ticket ticket = dataSnapshot.getValue(Ticket.class);

                if(ticket != null && ticket.getCode() == Long.parseLong(arrayData[0]) && ticket.getNumber() == Integer.parseInt(arrayData[1])) {
                    checkTicketWithCurrentNumber(arrayData[3], ticket);
                } else {
                    Snackbar.make(snackbarText, R.string.snackBar_qrCodeInvalidTicket, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkTicketWithCurrentNumber(final String identifier, final Ticket ticket) {
        mCurrentQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int queueNumber = Integer.parseInt(dataSnapshot.child("currentNumber").getValue().toString());
                if(queueNumber == ticket.getNumber()) {
                    Snackbar.make(snackbarText, R.string.snackBar_qrCodeSuccess, Snackbar.LENGTH_SHORT).show();
                    removeTicket(identifier, ticket.getQrCodeName());
                } else {
                    Snackbar.make(snackbarText, R.string.snackBar_qrCodeWrongTurn, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void removeTicket(final String identifier, String qrName) {
        if(!qrName.isEmpty()) {
            qrImagesDatabase.child(qrName).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mTicketsDatabase.child(queueId).child(identifier).removeValue();
                    incrementCurrentNumberQueue();
                }
            });
        } else {
            mTicketsDatabase.child(queueId).child(identifier).removeValue();
            incrementCurrentNumberQueue();
        }
    }


    private void retrieveData() {
        mTicketsDatabase = FirebaseDatabase.getInstance().getReference().child("Tickets");
        mTicketsDatabase.child(queueId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                textPeopleCount.setText("" + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mCurrentQueueRef = FirebaseDatabase.getInstance().getReference().child("Queues").child(queueId);
        mCurrentQueueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    String state = dataSnapshot.child("state").getValue().toString();
                    int currentNumber = Integer.parseInt(dataSnapshot.child("currentNumber").getValue().toString());
                    textBigNumber.setText("" + currentNumber);
                    if (state.equals("opened")) {
                        switchQueueState.setChecked(true);
                        switchQueueState.setText(R.string.queue_opened);
                        switchQueueState.setTextColor(getResources().getColor(R.color.queue_opened));
                    } else {
                        switchQueueState.setChecked(false);
                        switchQueueState.setText(R.string.queue_closed);
                        switchQueueState.setTextColor(getResources().getColor(R.color.queue_closed));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
