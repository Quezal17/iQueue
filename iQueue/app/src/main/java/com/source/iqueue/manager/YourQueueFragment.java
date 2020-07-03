package com.source.iqueue.manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.source.iqueue.manager.HomeManagerActivity;
import com.source.iqueue.Queue;
import com.source.iqueue.R;

public class YourQueueFragment extends Fragment {

    private String managerId;

    //Views references
    private RecyclerView queuesList;
    private FloatingActionButton btnAddQueue;

    //Firebase reference
    private FirebaseAuth mAuth;
    private DatabaseReference mQueuesDatabase;
    private FirebaseRecyclerAdapter recyclerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_your_queue,null);

        mAuth = FirebaseAuth.getInstance();
        managerId = mAuth.getCurrentUser().getUid();
        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");

        queuesList = (RecyclerView) rootView.findViewById(R.id.managerQueuesList);
        queuesList.setHasFixedSize(true);
        queuesList.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAddQueue = (FloatingActionButton) rootView.findViewById(R.id.floating_btnAddQueue);
        btnAddQueue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addQueue();
            }
        });

        return rootView;
    }

    private void incrementManagerIterator(int iterator) {
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("manager")
                .child(managerId)
                .child("queueIterator")
                .setValue(iterator+1);
    }

    private void addQueue() {
        FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("manager")
                .child(managerId)
                .child("queueIterator")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int iterator = Integer.parseInt(dataSnapshot.getValue().toString());
                        String name = getResources().getString(R.string.manager_queueName) + " " + (iterator+1);
                        Queue newQueue = new Queue(name, managerId,0,0,"closed");
                        mQueuesDatabase.push().setValue(newQueue);
                        incrementManagerIterator(iterator);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void deleteQueue(String queueId) {
        mQueuesDatabase.child(queueId).removeValue();
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference().child("Tickets").child(queueId);
        ticketsRef.removeValue();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Queue>()
                .setQuery(mQueuesDatabase.orderByChild("managerId").equalTo(managerId), Queue.class)
                .build();

        recyclerAdapter = new FirebaseRecyclerAdapter<Queue, QueueViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final QueueViewHolder holder, int position, @NonNull Queue model) {
                if(model.getManagerId().equals(managerId)) {
                    final String queueId = getRef(position).getKey();
                    final String queueName = model.getName();
                    holder.queueName.setText(queueName);
                    if (model.getState().equals("opened")) {
                        holder.stateView.setText(R.string.queue_opened);
                        holder.stateView.setTextColor(getResources().getColor(R.color.queue_opened));
                    } else if (model.getState().equals("closed")) {
                        holder.stateView.setText(R.string.queue_closed);
                        holder.stateView.setTextColor(getResources().getColor(R.color.queue_closed));
                    }

                    holder.imgDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteQueue(queueId);
                        }
                    });

                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent queueIntent = new Intent(getContext(), QueueManagingActivity.class);
                            queueIntent.putExtra("from", "YourQueueFragment");
                            queueIntent.putExtra("queueId",queueId);
                            queueIntent.putExtra("managerId",managerId);
                            queueIntent.putExtra("queueName",queueName);
                            startActivity(queueIntent);
                        }
                    });
                }
            }

            @NonNull
            @Override
            public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manager_queue_item_list, parent,false);
                QueueViewHolder viewHolder = new QueueViewHolder(view);
                return viewHolder;
            }
        };

        queuesList.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        recyclerAdapter.stopListening();
    }

    private static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView queueName;
        TextView stateView;
        ImageView imgDelete;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            queueName = itemView.findViewById(R.id.manager_itemList_queueName);
            stateView = itemView.findViewById(R.id.manager_itemList_queueState);
            imgDelete = itemView.findViewById(R.id.manager_itemList_imgDelete);
        }
    }
}
