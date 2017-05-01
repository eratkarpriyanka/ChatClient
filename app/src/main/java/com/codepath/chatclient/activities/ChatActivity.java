package com.codepath.chatclient.activities;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.codepath.chatclient.R;
import com.codepath.chatclient.adapters.ChatListAdapter;
import com.codepath.chatclient.models.Message;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = ChatActivity.class.getSimpleName();
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;
    // Create a handler which can run code periodically
    static final int POLL_INTERVAL = 1000; // milliseconds

    private String userId;
    private Button btnSend;
    private EditText etMessage;
    private ListView listView;
    private ArrayList<Message> listMessages;
    private ChatListAdapter adapter;
    private boolean isFirstLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (ParseUser.getCurrentUser() != null) {

            userId="11";
              // start w/ existing user
            //startWithCurrentUser();
        } else {

            // login as anonymous user
            login();
        }
        setViews();
    }

    /**
     * set views on screen
     */
    private void setViews() {

        btnSend = (Button) findViewById(R.id.btSend);
        // Setup button event handler which posts the entered message to Parse
        btnSend.setOnClickListener(this);
        etMessage = (EditText) findViewById(R.id.etMessage);

        listView = (ListView) findViewById(R.id.lvChat);

        listMessages = new ArrayList<Message>();
        // Automatically scroll to the bottom when a data set change notification is received
        // and only if the last item is already visible on screen. Don't scroll to the bottom otherwise.
        listView.setTranscriptMode(1);
        adapter = new ChatListAdapter(ChatActivity.this, userId, listMessages);
        listView.setAdapter(adapter);
        isFirstLoad = true;

        //primitive "polling" rather than the more efficient "push" technique for refreshing new messages
        handler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);


    }

    private void startWithCurrentUser() {
        // Get the userId from the cached currentUser object
        userId = ParseUser.getCurrentUser().getObjectId();
        Log.d(TAG,"Current User"+userId);
    }

    private void login() {

        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException exception) {

                if (exception == null) {

                    startWithCurrentUser();
                } else {
                    Log.e(TAG, "Anonymous Login failed!" + exception);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {

        // When send button is clicked, create message object on Parse
        if(view.getId() == R.id.btSend) {

            String body = etMessage.getText().toString();
/*            ParseObject message = ParseObject.create("Message");
            message.put(USER_ID_KEY,userId);
            message.put(BODY_KEY,body);*/

            Message message = new Message();
            message.setUserId(userId);
            message.setBody(body);
            message.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException exception) {

                    if(exception == null){

                        Toast.makeText(ChatActivity.this,"Successfully created message on Parse",Toast.LENGTH_LONG).show();;
                    }else{
                        Log.e(TAG, "Failed to save message!" + exception);
                    }
                }
            });
            etMessage.setText(null);
        }
    }

    // Query messages from Parse so we can load them into the chat adapter
    void refreshMessages() {

        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);

        // get the latest 500 messages, order will show up newest to oldest of this group
        query.orderByAscending("createdAt");
        // Execute query to fetch all messages from Parse asynchronously
        // This is equivalent to a SELECT query with SQL
        query.findInBackground(new FindCallback<Message>() {
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    listMessages.clear();
                    listMessages.addAll(messages);
                    adapter.notifyDataSetChanged(); // update adapter
                    // Scroll to the bottom of the list on initial load
                    if (isFirstLoad) {
                        listView.setSelection(adapter.getCount() - 1);
                        isFirstLoad = false;
                    }
                } else {
                    Log.e("message", "Error Loading Messages" + e);
                }
            }
        });

    }

    Handler handler = new Handler();
    // Create a handler which can run code periodically
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {

            refreshMessages();
            handler.postDelayed(this, POLL_INTERVAL);
        }
    };

}

