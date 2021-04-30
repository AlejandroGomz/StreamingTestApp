package com.mgg.streamingtestapp;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {

    private final String TAG = this.getClass().getSimpleName();
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;


    public DriveServiceHelper(Drive mDriveService) {
        this.mDriveService = mDriveService;
    }

    public Task<FileList> getAudioFiles(String folderID) {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                Log.d(TAG, "getAudioFiles..mDriveService: "+mDriveService);
                String pageToken = null;
                String query = "'"+folderID+"' in parents";
                Log.d(TAG, "getAudioFiles..query: "+query);
                FileList fileList = null;
                do {
                    fileList = mDriveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("nextPageToken,files(id, name, size, fileExtension,mimeType, webContentLink)")
                            .execute();
                    pageToken = fileList.getNextPageToken();

                } while (pageToken != null);
                Log.d(TAG, "getAudioFiles..fileList: "+fileList.getFiles().size());
                for (File file : fileList.getFiles()) {
                    Log.d(TAG, "getAudioFiles..fileList: "+file.getWebContentLink());
                }

                return fileList;
            }
        });
    }

    public Task<FileList> streamAndPlay() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                Log.d(TAG, "streamAndPlay..mDriveService: "+mDriveService);
                String pageToken = null;
                String folderID = "1MhJyPmI_tAsI92rJXpVrUIi-NE6dLEy";
                String query = "trashed = false";
                query = "mimeType = 'application/vnd.google-apps.folder' and trashed = false";
                Log.d(TAG, "streamAndPlay..Query: "+query);
                FileList folderList = null;
                FileList fileList = null;
                do {
                    folderList = mDriveService.files().list()
                            .setQ(query)
                            .setSpaces("drive")
                            .setFields("nextPageToken,files(id, name)")
                            .execute();
                    pageToken = folderList.getNextPageToken();

                } while (pageToken != null);
                Log.d(TAG, "streamAndPlay...fileInformationList.getFiles().size(): "+folderList.getFiles().size());
                for (int i = 0; i < folderList.getFiles().size(); i++) {
                    Log.d(TAG, "streamAndPlay...getWebContentLink: "+folderList.getFiles().get(i).getId());

                }

                return folderList;
            }
        });
    }
}
