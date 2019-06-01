package com.example.android.voicecommand;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,TextToSpeech.OnInitListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int REQUEST_PHONE_CALL = 1;

    private static final String TAG = "MainActivity";

    public ListView mList;
    public Button speakButton;
    public TextView txt;
    final HashMap<String, String> onlineSpeech = new HashMap<>();
    boolean Dial;
    public int sum=0;
    private TextToSpeech textToSpeechSystem;
    public EditText commandList;
    public boolean waitingForName,waitingForCity,waitingForNationalID,waitingForVotedFor,firstTime=true;
    private boolean isVoted=false,isFound=false;

    private String voterName,voterCity,voterNationalID,votedFor;
    private String[] canditates={"Hillary Clinton","Donald Trump"};
    private int[] canditatesVotes={0,0};


    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        changeStatusBarColor();

        //Ask for Permissions if not granted (Call & Read contacts)
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE,Manifest.permission.READ_CONTACTS}, 1);
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


        commandList = findViewById(R.id.MultiText);
        commandList.setText("Commands:\n\n"+"Hello || Hi || Hey , then: My name is\n\n"+"Open Contacts\n\n"+"Dial Number, then: 123\n\n"+"Call 123\n\n"+"Call QNB\n\n"+"Open Calendar\n\n"+"Open Gallery\n\n");
        speakButton = findViewById(R.id.speakButt);
        speakButton.setOnClickListener(this);
        voiceinputbuttons();
        textToSpeechSystem = new TextToSpeech(this,this);








    }

    private void searchForVoter()
    {
        final DatabaseReference voterRef = FirebaseDatabase.getInstance().getReference("Person").child(voterNationalID);
        voterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    isFound = true;
                }
                else {isFound=false;}
                for(DataSnapshot ds : dataSnapshot.getChildren())
                {
                    if(ds.getKey().equals("Voted")){
                        isVoted=true;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    public void setVoterAsVoted(){
        DatabaseReference voterRef = FirebaseDatabase.getInstance().getReference("Person").child(voterNationalID).child("Voted");
        Log.v("setVoterAsVoted", "NID:"+ voterNationalID);
        voterRef.setValue(true);
        isVoted=true;
    }

    public void setVoteCounts (String key,int value){
        for(int i=0;i<canditates.length;i++) {
            if (key.equals(canditates[i]) ) {
                canditatesVotes[i] = value;
            }
        }
    }


    public void getAndUpdateVotes(final boolean updateVote) {
        final DatabaseReference candidatesRef = FirebaseDatabase.getInstance().getReference("Candidates");
        final DatabaseReference candidateVFRef = FirebaseDatabase.getInstance().getReference("Candidates").child(votedFor);
        candidatesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String key = ds.getKey();
                    int value = ds.getValue(Integer.class);
                    setVoteCounts(key,value);
                    if( !isVoted &&( updateVote && key.equals(votedFor) ) ){
                        isVoted=true;
                        candidateVFRef.setValue(++value);
                        setVoterAsVoted();
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }


    public void startElectionProcess(){

    if(!isVoted) {
        // Getting voterName
        if (voterName == null) {
            speak("Could you please tell me your name?");
            waitingForName = true;
            startVoiceRecognitionActivity();
        } else if (voterCity == null) {
            // Getting voterCity
            speak("Could you please tell me which city are you from US?");
            waitingForCity = true;
            startVoiceRecognitionActivity();
        } else if (voterNationalID == null) {
            speak("Could you please tell me your National ID?");
            //speak("it should have 3 digits");
            waitingForNationalID = true;
            startVoiceRecognitionActivity();
        } else if (votedFor == null) {
            searchForVoter();
            speak("wait a moment");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (isFound && !isVoted) {
                        speak("US Elections has two candidates");
                        for (int i = 0; i < canditates.length; i++) {
                            speak(canditates[i]);
                        }
                        speak("Which one will you vote for?");
                        waitingForVotedFor = true;
                        Log.d("VotedFor condition", "NID:"+voterNationalID);
                        startVoiceRecognitionActivity();

                    }
                    else if(isVoted) {
                        speak("Don't trick me you voted before it's not a game dude");
                    }
                    else
                    {
                        speak("Unfortunately you aren't registered to our database ");
                    }
                }
            }, 5000);




        }


    }
    else{
        speak("Don't trick me you voted before it's not a game dude");
    }

    }

    public void voiceinputbuttons() {
        speakButton = findViewById(R.id.speakButt);
        mList = (ListView) findViewById(R.id.list);
    }

    public void startVoiceRecognitionActivity() {
        while (textToSpeechSystem.isSpeaking()){}
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Election System");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
                //ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                //mList.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, matches));
            ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String inSpeech = res.get(0);
            while (textToSpeechSystem.isSpeaking()){}
            recognition(inSpeech);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {

            case 1: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }

    private void speak(String text){
        while (textToSpeechSystem.isSpeaking()){}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeechSystem.setSpeechRate((float) 0.9);
            textToSpeechSystem.speak(text, TextToSpeech.QUEUE_FLUSH, onlineSpeech);

        }else{
            textToSpeechSystem.speak(text, TextToSpeech.QUEUE_FLUSH, onlineSpeech);
        }
    }



    private void recognition(String text) {
        Log.e("Speech", "" + text);
        String[] speech = text.split(" ");

        if(textToSpeechSystem.isSpeaking()){startElectionProcess();}
        if(text.contains("reset"))
        {
            isVoted=false;
            isFound=false;
            firstTime=true;
            votedFor=null;
            voterNationalID=null;
            voterCity=null;
            voterName=null;
            speak("done resetting");
        }
        else if(text.contains("vote") || text.contains("elections") || text.contains("start"))
        {
            startElectionProcess();
        }
        else if(text.contains("show commands") || text.contains("show list") || text.contains("show command")){
            commandList.setVisibility(View.VISIBLE);
        }
        else if(text.contains("hide commands") || text.contains("hide list") || text.contains("hide command") || text.contains("hide"))
        {
            commandList.setVisibility(View.INVISIBLE);
        }
        else if (text.contains("hello") || text.contains("hey") || text.contains("hi") )
        {
            if(firstTime) {
                speak("Hi, What is your name?");
                waitingForName = true;
            }
            else {
                speak("Hi, nice to meet you");
            }
            startVoiceRecognitionActivity();
        }
        else if (waitingForName && firstTime){
            firstTime=false;
            waitingForName=false;
            if ( text.contains("my name is"))
            {
             voterName = Arrays.toString(Arrays.copyOfRange(speech,3,speech.length ));
            }
                else{
                voterName = Arrays.toString(speech);
            }

            speak("Hello Mr."+voterName);
            startElectionProcess();
        }
        else if(waitingForCity || text.contains("i'm from"))
        {
            waitingForCity=false;
            if(text.contains("i'm from") )
            {
                voterCity = Arrays.toString(Arrays.copyOfRange(speech,2,speech.length ));
            }
            else if(text.contains("from"))
            {
                voterCity = Arrays.toString(Arrays.copyOfRange(speech,1,speech.length ));
            }
            else
            {
                voterCity=Arrays.toString(speech);
            }
            speak(voterCity+"is a beautiful city i love it !");
            startElectionProcess();
        }
        else if(text.contains("wrong ID")|| text.contains("confirm ID") || ( (waitingForNationalID || text.contains("my national ID is") || text.contains("my ID is") )&& text.matches(".*\\d.*")) )
        {
            waitingForNationalID=false;
            if( !( text.contains("wrong ID") || text.contains("confirm ID") ) ){
            voterNationalID = text.replaceAll("[^0-9]", "");
            }


            if(text.contains("wrong ID") || voterNationalID.length()>3)
            {
                voterNationalID=null;
                waitingForNationalID=true;
                startElectionProcess();
            }
            else if(text.contains("confirm ID"))
            {
                Log.d("Confirm ID", "NID:"+voterNationalID);
                startElectionProcess();
            }
            else{
                speak("your national ID is");
                for(int i=0;i<voterNationalID.length();i++)
                {
                    speak(voterNationalID.charAt(i)+"");
                }
                speak("say confirm ID or wrong ID");
                startVoiceRecognitionActivity();
            }


        }
        else if( waitingForVotedFor &&(text.contains("Hillary") || text.contains("Clinton")) )
        {
            waitingForVotedFor=false;
            votedFor="Hillary Clinton";
            speak("Congratulations you successfully voted for"+votedFor);
            getAndUpdateVotes(true);
        }
        else if( waitingForVotedFor &&(text.contains("Donald") || text.contains("Trump"))){
            waitingForVotedFor=false;
            votedFor="Donald Trump";
            speak("Congratulations you successfully voted for"+votedFor);
            getAndUpdateVotes(true);
        }
        else if(text.contains("show results"))
        {
            for(int i=0;i<canditates.length;i++)
            {
                speak("Candidate"+canditates[i]+"has"+canditatesVotes[i]+"votes");
            }
        }
        else if(text.contains("open contacts"))
        {

            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(intent, 1);
        }
        else if(text.contains("dial number"))
        {

            speak("Ok, Give me the number");
            Dial =true;
            startVoiceRecognitionActivity();

        }
        else if( ( text.contains("call") && text.matches(".*\\d.*") )|| (Dial&& text.matches(".*\\d.*")) )
        {

            String number  = text.replaceAll("[^0-9]", "");

            if(Dial)
            {
                Dial=false;
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)));
            }
            else {
                speak("Calling");
                startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)));
            }
           /* if(isPermissionGranted()){
                call_action();
            }*/

        }
        else if(text.contains("call qnb"))
        {

            String number = getPhoneNumber("QNB",this);
            speak("Calling");
            startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)));

        }
        else if(text.contains("open calendar"))
        {
            speak("Ok");
            Uri calendarUri = CalendarContract.CONTENT_URI
                    .buildUpon()
                    .appendPath("time")
                    .build();
            startActivity(new Intent(Intent.ACTION_VIEW, calendarUri));
        }
        else if(text.contains("open gallery"))
        {
            speak("Ok");
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setType("image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        else if(text.contains("go back"))
        {
            speak("plz no");
            onBackPressed();
        }
        else{
            speak("Again, Please");
            startVoiceRecognitionActivity();
        }





    }
    // Get phone number by giving it the name of contact
    public String getPhoneNumber(String name, Context context) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if(ret==null)
            ret = "Unsaved";
        return ret;
    }

    public  boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG","Permission is granted");
                return true;
            } else {

                Log.v("TAG","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG","Permission is granted");
            return true;
        }
    }


    public void hideStatusBar() {
        // Hide status bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }
    public void changeStatusBarColor(){
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.rgb(57,57,57));
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){

        case R.id.speakButt: startVoiceRecognitionActivity();
        break;

        }
    }



    // TTS initializer
    @Override
    public void onInit(int ttsInitResult) {
        if (TextToSpeech.SUCCESS == ttsInitResult) {
            final HashMap<String, String> onlineSpeech = new HashMap<>();
            onlineSpeech.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Greetings !!
            speak("Welcome to the United States Voice election system, I'm here to help you with voting");

        } else {
            Log.e("TTS", "Initilization Failed");
        }
        }



}
