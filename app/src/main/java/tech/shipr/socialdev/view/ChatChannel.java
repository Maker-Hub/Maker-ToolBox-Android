package tech.shipr.socialdev.view;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import tech.shipr.socialdev.R;
import tech.shipr.socialdev.adapter.MessageAdapter;
import tech.shipr.socialdev.model.DeveloperMessage;


public class ChatChannel extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String ANONYMOUS = "anonymous";
    private static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN = 1;
    private static final int RC_CHAT_PHOTO_PICKER = 3;
    private MessageAdapter mMessageAdapter;
    //private EditText mMessageEditText;
    private Button mSendButton;
    private String mName;
    final String  mPlatform = "Android";
    private String mDate;
    private String mTime;
    private String mMessage;
    private String mProfilePic;
    private String mVersion;

    // Firebase instance variable
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private EmojiconEditText mEmojiconEditText;
    private EmojIconActions mEmojiconActions;
    private ImageView mEmojiImageView;

    private String mChannel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_general, container, false);

        mName = ANONYMOUS;

        mChannel = "help";
        initFirebase();


        // Initialize references to views
        ProgressBar mProgressBar = rootView.findViewById(R.id.progressBar);
        ListView mMessageListView = rootView.findViewById(R.id.messageListView);
        //mMessageEditText = rootView.findViewById(R.id.messageEditText);
        mEmojiImageView = rootView.findViewById(R.id.emojiButton);
        mEmojiconEditText = rootView.findViewById(R.id.emojicon_edit_text);
        mEmojiconActions = new EmojIconActions(getContext().getApplicationContext(), rootView, mEmojiconEditText, mEmojiImageView);
        mEmojiconActions.ShowEmojIcon();

        mSendButton = rootView.findViewById(R.id.sendButton);

        mEmojiconActions.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {

                mEmojiconActions.setUseSystemEmoji(true);

            }

            @Override
            public void onKeyboardClose() {

            }
        });

        mEmojiconActions.addEmojiconEditTextList(mEmojiconEditText);


        //Initialize spinner'
        Spinner spinner = rootView.findViewById(R.id.chatChannelSpinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.chat_channels, android.R.layout.simple_spinner_item);

         // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        // Initialize message ListView and its adapter
        List<DeveloperMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(getActivity(), R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);


        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
        mSendButton.setEnabled(false);

        FirebaseMessaging.getInstance().subscribeToTopic(mChannel);

        // Enable Send button when there's text to send
        editTextWatcher();

       /* addPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImagePicker();
            }
        });
*/

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
                sendNotificationToUser(null);
                mEmojiconEditText.setText("");
            }
        });
        authStateCheck();
        return rootView;
    }

    private void initFirebase() {
        FirebaseApp.initializeApp(getActivity());
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(mChannel);
    }


    private void authStateCheck() {
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //User is signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(
                                            Collections.singletonList(
                                                    new AuthUI.IdpConfig.EmailBuilder().build()
                                            ))
                                    .build(),
                            RC_SIGN_IN);


                }

            }
        };
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_CHAT_PHOTO_PICKER);

    }

    private void editTextWatcher() {
        mEmojiconEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mEmojiconEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
    }

    private void initVariable() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("IST"));
        mTime = sdf.format(new Date());

        // Getting the date
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        mDate = String.valueOf(day) + "-" + String.valueOf(month) + "-" + String.valueOf(year);

        mMessage = mEmojiconEditText.getText().toString();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    }

    private void sendMessage() {
        initVariable();
        // Sending the Message
        DeveloperMessage developerMessage = new DeveloperMessage(
                mName,
                mProfilePic,
                mMessage,
                null,
                mTime,
                mDate,
                mPlatform,
                mVersion
        );
        mMessagesDatabaseReference.push().setValue(developerMessage);


    }

    private void sendNotificationToUser(String user) {
        DatabaseReference mNotificationsDatabaseReference = mFirebaseDatabase.getReference().child("notificationRequests");


        Map<String, String> notification = new HashMap<String, String>();
        String mChannel = "general";
        notification.put("channel", mChannel);
        notification.put("username", user);
        notification.put("message", mMessage);

        mNotificationsDatabaseReference.push().setValue(notification);
    }

    private void onSignedInInitialize(String username) {
        mName = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mName = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();

    }

    private void attachDatabaseReadListener() {

        if (mChildEventListener == null) {


            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    DeveloperMessage developerMessage = dataSnapshot.getValue(DeveloperMessage.class);
                    mMessageAdapter.add(developerMessage);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };

            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String items = parent.getSelectedItem().toString();
        Log.i("Selected item : ", items);
        mChannel = items;
        updateChannel(items);
    }

    public void updateChannel(String channelName) {
        mMessageAdapter.clear();
        detachDatabaseReadListener();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("chat/" + channelName);
        attachDatabaseReadListener();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}