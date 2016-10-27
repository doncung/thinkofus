package com.box.box_preview_sample.fragments;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public final class CreatePhotoHelper {

    private static final int CREATION_MONITOR_PERIOD = 3000;

        /**
     * Static class. No instantiation allowed.
     */
    private CreatePhotoHelper() {

    }


    /**
     * @param photoUri
     *            URI to a photo.
     * @return Returns an intent that launches the camera to take a photo.
     */
    public static Intent getPhotoIntent(final Uri photoUri) {
        Intent createAndUploadPhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        createAndUploadPhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        createAndUploadPhotoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        return createAndUploadPhotoIntent;
    }

    /**
     * @return Returns a Uri that points to a temporary file where a photo should be stored.
     */
    public static Uri getNewPhotoUri(final Context context) {
        // Do Not localize this Date String!
        SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmssSSS");
        String filename = "IMG_" + df.format(new Date()) + ".jpg";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File file = new File(storageDir, filename);
        return Uri.fromFile(file);
    }

    public static void deleteCameraCopyOf(final Context context, final File tempFile) {
        new Thread() {

            public void run() {
                String sha1;
                try {
                    sha1 = SdkUtils.sha1(new FileInputStream(tempFile));
                } catch (FileNotFoundException e) {
                    return;
                } catch (IOException e) {
                    return;
                } catch (NoSuchAlgorithmException e){
                    return;
                }
                deleteCameraCopyOf(context, tempFile, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), System.currentTimeMillis()
                        - CREATION_MONITOR_PERIOD, sha1);
            };

        }.start();

    }

    private static void deleteCameraCopyOf(final Context context, final File tempFile, final File directory, final long acceptedModifiedCutoff, final String sha1) {
        // because there is some danger of deleting something we shouldn't make sure the tempFile name is long enough so having the same name is unlikely.
        if (tempFile.isFile() && tempFile.getName().length() > 6) {
            File files[] = directory.listFiles();
            if (files != null && files.length > 0) {
                try {
                    for (File file : files) {
                        if (file.isFile() && file.lastModified() > acceptedModifiedCutoff) {
                            // the file matches the one given to us.
                            if (sha1.equals(SdkUtils.sha1(new FileInputStream(file)))) {
                                deleteImageFromGallery(context, file);
                            }

                        } else if (file.isDirectory() && file.lastModified() > acceptedModifiedCutoff) {
                            deleteCameraCopyOf(context, tempFile, file, acceptedModifiedCutoff, sha1);
                        }
                    }
                } catch (IOException e) {
                } catch (NoSuchAlgorithmException e){

                }
            }
        }
    }

    private static  void deleteImageFromGallery(final Context context, final File fileToDelete) {
        final String filePath = fileToDelete.getAbsolutePath();
        if (fileToDelete.delete()) {
            MediaScannerConnection.scanFile(context, new String[] {filePath}, null, null);
        }
    }
}
