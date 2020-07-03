package com.source.iqueue.manager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.Ticket;

public class LocalTicketActivity extends AppCompatActivity {

    private TextView viewShopName, viewPeopleInQueue, viewBigNumber, viewStatus, snackBar;
    private MaterialButton btnGetTicket;
    private String queueId, managerId, queueState;

    //Firebase reference
    private DatabaseReference mCurrentQueue, mTicketsDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_ticket);

        managerId = FirebaseAuth.getInstance().getUid();
        queueId = getIntent().getStringExtra("queueId");
        viewStatus = (TextView) findViewById(R.id.biglietteria_queueStatus);
        viewShopName = (TextView) findViewById(R.id.biglietteria_shopName);
        viewBigNumber = (TextView) findViewById(R.id.biglietteria_bigNumber);
        viewPeopleInQueue = (TextView) findViewById(R.id.biglietteria_peopleCount);
        btnGetTicket = (MaterialButton) findViewById(R.id.biglietteria_btnGetTicket);
        snackBar = (TextView) findViewById(R.id.biglietteria_snackBar);

        setTextViewShopName();
        mTicketsDatabase = FirebaseDatabase.getInstance().getReference().child("Tickets").child(queueId);
        mTicketsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                viewPeopleInQueue.setText("" + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mCurrentQueue = FirebaseDatabase.getInstance().getReference().child("Queues").child(queueId);
        mCurrentQueue.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                viewBigNumber.setText("" + dataSnapshot.child("currentNumber").getValue());
                queueState = dataSnapshot.child("state").getValue().toString();
                if(queueState.equals("opened")) {
                    viewStatus.setText(R.string.queue_opened);
                    viewStatus.setTextColor(getResources().getColor(R.color.queue_opened));
                } else {
                    viewStatus.setText(R.string.queue_closed);
                    viewStatus.setTextColor(getResources().getColor(R.color.queue_closed));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        btnGetTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(queueState.equals("opened"))
                    getNewTicket();
                else
                    Snackbar.make(snackBar, R.string.managing_queueClosedSnackBarText, Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    private void setTextViewShopName() {
        FirebaseDatabase.getInstance().getReference().child("Users").child("manager").child(managerId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String shopName = dataSnapshot.child("shopName").getValue().toString();
                        viewShopName.setText(shopName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void getNewTicket() {
        mCurrentQueue.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ticketNumber = Integer.parseInt(dataSnapshot.child("totalNumber").getValue().toString()) + 1;
                long ticketCode = System.currentTimeMillis();
                addNewTicket(ticketNumber, ticketCode);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addNewTicket(int ticketNumber, long ticketCode) {
        mCurrentQueue.child("totalNumber").setValue(ticketNumber);
        Ticket newTicket = new Ticket(ticketCode, ticketNumber, "");
        mTicketsDatabase.push().setValue(newTicket);
        showTicketDialog(ticketNumber, ticketCode);
    }

    private void showTicketDialog(int ticketNumber, long ticketCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LocalTicketActivity.this, R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(LocalTicketActivity.this).inflate(
                R.layout.local_ticket_dialog,
                (RelativeLayout) findViewById(R.id.dialogLayoutContainer)
        );
        builder.setView(dialogView);

        ((TextView) dialogView.findViewById(R.id.dialogNumber)).setText("" + ticketNumber);
        ((TextView) dialogView.findViewById(R.id.dialogCode)).setText("" + ticketCode);

        final AlertDialog alertDialog = builder.create();

        dialogView.findViewById(R.id.dialogBtnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        if(alertDialog.getWindow() != null)
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        alertDialog.show();
    }

}
