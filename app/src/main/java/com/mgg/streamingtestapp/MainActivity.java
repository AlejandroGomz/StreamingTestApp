package com.mgg.streamingtestapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnErrorListener {

    /**
    Fehler:
    2021-05-01 00:50:32.780 26414-26745/com.mgg.streamingtestapp E/MediaPlayerNative: error (1, -2147483648)
    2021-05-01 00:50:32.783 26414-26414/com.mgg.streamingtestapp E/MediaPlayer: Error (1,-2147483648)
     */

    private final String TAG = this.getClass().getSimpleName();
    private final int REQUEST_CODE_SIGN_IN = 42;
    private Context mContext;
    private static final String PREFS_NAME = "PREFS";
    private String accessToken;
    private SharedPreferences prefs;
    private FileList fileInformationList;
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
        mMediaPlayer.setOnErrorListener(this);

        prefs = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
        accessToken = prefs.getString("accessTokenDrive", null);
        if (accessToken == null) {
            Log.d(TAG, "requestSignIn call");
            requestSignIn();
        }

    }

    private void requestSignIn() {

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA), new Scope(DriveScopes.DRIVE_READONLY),new Scope(DriveScopes.DRIVE_METADATA_READONLY),new Scope(DriveScopes.DRIVE_FILE))
                        .build();

        Log.d(TAG, "requestSignIn...signInOptions:" +signInOptions);
        GoogleSignInClient client = GoogleSignIn.getClient(mContext, signInOptions);
        Log.d(TAG, "requestSignIn...client.getApiKey:" +client.getApiKey().getClientKey());

        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG,"onActivityResult requestCode: "+requestCode);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInResult(data);
                }
        }

    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Thread getTokenThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                accessToken = credential.getToken();
                                prefs.edit()
                                        .putString("accessTokenDrive",accessToken)
                                        .apply();
                            } catch (IOException | GoogleAuthException e) {
                                e.printStackTrace();
                                Log.e(TAG, "getToken: "+e);
                            }
                        }
                    });
                    getTokenThread.start();


                    Drive googleDriveService =
                            new Drive.Builder(
                                    new NetHttpTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Test Streaming")
                                    .build();

                    DriveServiceHelper mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    mDriveServiceHelper.streamAndPlay()
                            .addOnSuccessListener(folderList -> {
                                if (folderList != null) {
                                    fileInformationList = folderList;
                                    Log.d(TAG, "streamAndPlay finish: "+fileInformationList.getFiles().size());
                                    for (int i = 0; i < fileInformationList.getFiles().size();i++) {
                                        if (i == 0) {
                                            String folderID = fileInformationList.getFiles().get(i).getId();
                                            filePlay(mDriveServiceHelper,folderID);
                                        }
                                    }
                                }



                            })
                            .addOnFailureListener(e -> Log.e(TAG, "streamAndPlay Error: "+e));
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));

    }

    private void filePlay(DriveServiceHelper mDriveServiceHelper, String folderID) {
        mDriveServiceHelper.getAudioFiles("1ETMVe0fJlGN0ZJuZoEBTYOSa-ISMqXC0")
                .addOnSuccessListener(linkList -> {
                    Log.d(TAG, "streamAndPlay...Linklist.size: "+linkList.getFiles().size());
                    for (int i = 0; i < linkList.getFiles().size(); i++ ) {
                        if (i == 0) {
                            String webContentLink = linkList.getFiles().get(i).getWebContentLink();

                            Log.d(TAG, "streamAndPlay...link: "+webContentLink);
                            int index = webContentLink.indexOf("&");
                            webContentLink = webContentLink.substring(0,index);
                            Log.d(TAG, "streamAndPlay...link 2: "+webContentLink);
                            webContentLink = webContentLink
                                    +"&access_token="+accessToken
                                    +"&alt=media";
                            Log.d(TAG, "streamAndPlay...link 3: "+webContentLink);
                            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    Log.d(TAG, "streamAndPlay...MediaPlayer prepared");
                                    mp.start();
                                }
                            });
                            try {
                                mMediaPlayer.setDataSource(mContext, Uri.parse(webContentLink));
                                mMediaPlayer.prepareAsync();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, "streamAndPlay...MediaPlayer Fehler: "+e);
                            }
                        }
                    }



                })
                .addOnFailureListener(e -> Log.e(TAG, "streamAndPlay...Linklist Fehler: "+e));
    }
}