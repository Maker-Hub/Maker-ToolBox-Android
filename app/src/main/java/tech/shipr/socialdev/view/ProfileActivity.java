package tech.shipr.socialdev.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import tech.shipr.socialdev.model.Profile;

import tech.shipr.socialdev.R;

public class ProfileActivity extends Fragment {


    TextView usernameEdit;
    TextView emailEdit;
    EditText nameEdits;
    EditText ageEditemailEdit;
    EditText langEdit;
    EditText gitEdit;
    EditText twitEdit;
    EditText linkEdit;
    private TextView pUsername;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mprofileDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private String fullName;
    private String username;
    private String email;
    private String age;
    private String languages;
    private String github;
    private String twitter;
    private String linkedin;
    private Profile mProfile;
    //    private Boolean mProgressBarPresent;
//    private ProgressBar mProgressBar;
    private ValueEventListener postListener;

    private static final int RC_PROFILE_PHOTO_PICKER = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        nameEdits = rootView.findViewById(R.id.nameEdit);

        emailEdit = rootView.findViewById(R.id.emailEdit);
        usernameEdit = rootView.findViewById(R.id.usernameEdit);
        ageEditemailEdit = rootView.findViewById(R.id.ageEditemailEdit);
        langEdit = rootView.findViewById(R.id.langEdit);
        gitEdit = rootView.findViewById(R.id.gitEdit);
        twitEdit = rootView.findViewById(R.id.twitEdit);
        linkEdit = rootView.findViewById(R.id.linkEdit);
        //    mProgressBar = rootView.findViewById(R.id.pProgressBar);

        FirebaseApp.initializeApp(getActivity());

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        String id = user.getUid();
        mprofileDatabaseReference = mFirebaseDatabase.getReference().child("users" + "/" + id + "/" + "profile");
       //mProgressBarPresent = true;


        if (user != null) {
            // Name, email address, and profile photo Url

            String username = user.getDisplayName();
            String email = user.getEmail();
            // Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();
            //TODO Add a listener and if false, add verift email button


            emailEdit.setText(email);
            usernameEdit.setText(username);
        }


        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI

                mProfile = dataSnapshot.getValue(Profile.class);
                if (mProfile != null) {
                    fullName = mProfile.getFullName();
                    email = mProfile.getEmail();
                    username = mProfile.getUsername();
                    age = mProfile.getAge();
                    languages = mProfile.getLanguages();
                    github = mProfile.getGithub();
                    twitter = mProfile.getTwitter();
                    linkedin = mProfile.getLinkedin();


                    setEditIfNotEmpty(fullName, nameEdits);
                    setTextIfNotEmpty(email, emailEdit);
                    setTextIfNotEmpty(username, usernameEdit);
                    setEditIfNotEmpty(age, ageEditemailEdit);
                    setEditIfNotEmpty(languages, langEdit);
                    setEditIfNotEmpty(github, gitEdit);
                    setEditIfNotEmpty(twitter, twitEdit);
                    setEditIfNotEmpty(linkedin, linkEdit);
                }

                // mProgressBarCheck();

            }

            private void setEditIfNotEmpty(String sstring, EditText editText) {
                if (sstring != null && !sstring.isEmpty()) {
                    editText.setText(sstring);
                }
            }

            private void setTextIfNotEmpty(String ssstring, TextView seditText) {
                if (ssstring != null && !ssstring.isEmpty()) {
                    seditText.setText(ssstring);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("ProfileActivity", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        mprofileDatabaseReference.addListenerForSingleValueEvent(postListener);

        FloatingActionButton button = rootView.findViewById(R.id.submitButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getVariablesFromEditText();
                mProfile = new Profile(
                        fullName,
                        username,
                        email,
                        age,
                        languages,
                        github,
                        twitter,
                        linkedin
                );
                mprofileDatabaseReference.setValue(mProfile);
            }
        });

        return rootView;
    }

    private void getVariablesFromEditText() {
        fullName = nameEdits.getText().toString();
        email = emailEdit.getText().toString();
        username = usernameEdit.getText().toString();
        age = ageEditemailEdit.getText().toString();
        languages = langEdit.getText().toString();
        github = gitEdit.getText().toString();
        twitter = twitEdit.getText().toString();
        linkedin = linkEdit.getText().toString();


    }

    private void clearPic(View view){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build();

    }
    public void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PROFILE_PHOTO_PICKER);

    }

 /*   private void mProgressBarCheck(){
        if(mProgressBarPresent){
            mProgressBar.setVisibility(View.GONE);
            mProgressBarPresent=false;

        }
    }*/

}
