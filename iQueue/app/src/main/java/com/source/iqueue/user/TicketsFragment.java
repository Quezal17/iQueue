package com.source.iqueue.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.Ticket;
import com.source.iqueue.manager.Manager;

public class TicketsFragment extends Fragment{

    //View references
    private RecyclerView recyclerView;

    private String userId;

    private DatabaseReference mTicketsDatabase, mQueuesDatabase, mManagersDatabase;
    private FirebaseRecyclerAdapter recyclerAdapter;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tickets, null);

        userId = FirebaseAuth.getInstance().getUid();
        mTicketsDatabase = FirebaseDatabase.getInstance().getReference().child("Tickets");
        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");
        mManagersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");

        recyclerView = (RecyclerView) rootView.findViewById(R.id.userTicketsList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return rootView;
    }

    private void retrieveShopInfo(String managerId, final TicketViewHolder holder) {
        mManagersDatabase.child(managerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String shopName = dataSnapshot.child("shopName").getValue().toString();
                holder.shopNameView.setText(shopName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void retrieveShopName(final String queueId, final TicketViewHolder holder) {
        mQueuesDatabase.child(queueId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String managerId = dataSnapshot.child("managerId").getValue().toString();
                retrieveShopInfo(managerId, holder);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void retrieveData() {
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Ticket>()
                .setQuery(mTicketsDatabase.orderByChild(userId), Ticket.class)
                .build();

        recyclerAdapter = new FirebaseRecyclerAdapter<Ticket, TicketViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final TicketViewHolder holder, int position, @NonNull Ticket model) {
                final String queueId = getRef(position).getKey();
                if(queueId != null) {
                    mTicketsDatabase.child(queueId).child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Ticket ticket = dataSnapshot.getValue(Ticket.class);
                            if (ticket != null) {
                                holder.ticketNumberView.setText("" + ticket.getNumber());
                                holder.ticketCodeView.setText("" + ticket.getCode());
                                retrieveShopName(queueId, holder);
                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent ticketIntent = new Intent(getContext(), TicketActivity.class);
                                        ticketIntent.putExtra("queueId", queueId);
                                        startActivity(ticketIntent);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @NonNull
            @Override
            public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_item_list, parent, false);
                TicketViewHolder viewHolder = new TicketViewHolder(view);
                return viewHolder;
            }
        };

        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
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

    private static class TicketViewHolder extends RecyclerView.ViewHolder{
        TextView shopNameView, ticketCodeView;
        TextView ticketNumberView;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            shopNameView = itemView.findViewById(R.id.ticket_shopName);
            ticketCodeView = itemView.findViewById(R.id.ticket_code);
            ticketNumberView = itemView.findViewById(R.id.ticket_number);
        }
    }
}
