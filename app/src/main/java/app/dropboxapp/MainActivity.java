package app.dropboxapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    // Replace APP_KEY from your APP_KEY
    final static private String APP_KEY = "wz9uuehcldzls2pnsr";
    // Relace APP_SECRET from your APP_SECRET
    final static private String APP_SECRET = "obnwyt8xvix8emnnsr";
    private Context ctx;
    //
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private Uri fileURI;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int GALLERY_PICKED_IMAGE_REQUEST_CODE = 101;
    private String filePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = MainActivity.this;
        // callback method
        initialize_session();
    }

    /**
     *  Initialize the Session of the Key pair to authenticate with dropbox
     *
     */
    protected void initialize_session(){

        // store app key and secret key
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        //Pass app key pair to the new DropboxAPI object.
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        // MyActivity below should be your activity class name
        //start session
        mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
    }

    /**
     * Callback register method to execute the upload method
     * @param view
     */
    public void uploadFiles(View view){
        selectORpickImage();

    }


    private void selectORpickImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Gallery", "Cancel" };
//        final CharSequence[] items = { "Choose from Gallery", "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    fileURI = Uri.fromFile(getOutputMediaFile());
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileURI);

                    startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
                    dialog.dismiss();
                } else if (items[item].equals("Choose from Gallery")) {

                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult( i, GALLERY_PICKED_IMAGE_REQUEST_CODE);
                    dialog.dismiss();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    public  File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }


        return new File(mediaStorageDir.getPath() + File.separator +
                "test_"+ (System.currentTimeMillis()/1000) + "_img.jpg");
    }



    /**
     *  Asynchronous method to upload any file to dropbox
     */
    public class Upload extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){

        }

        protected String doInBackground(String... arg0) {

            DropboxAPI.Entry response = null;

            try {

                // Define path of file to be upload
                File file = new File(filePath);
                FileInputStream inputStream = new FileInputStream(file);

                //put the file to dropbox
                response = mDBApi.putFile("/screens.png", inputStream,
                        file.length(), null, null);
                Log.e("DbExampleLog", "The uploaded file's rev is: " + response.rev);

            } catch (Exception e){

                e.printStackTrace();
            }

            return response.rev;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty() == false){
                Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                Log.e("DbExampleLog", "The uploaded file's rev is: " + result);
            }
        }
    }

    protected void onResume() {
        super.onResume();

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        try {
            if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
                if (resultCode == RESULT_OK && fileURI!=null) {
                    //                new ImageCompressionAsyncTask(false, ctx, this).execute(fileUri.toString());


//                    Uri selectedImage = data.getData();
                    filePath = getRealPathFromURI(fileURI);

                    new Upload().execute();


                } else if (resultCode == RESULT_CANCELED) {
                    // user cancelled Image capture
                    Toast.makeText(ctx , "User cancelled image capture.", Toast.LENGTH_SHORT).show();
                } else {
                    // failed to capture image
                    Toast.makeText(ctx , "Sorry! Failed to capture image.", Toast.LENGTH_SHORT).show();
                }
            }else if (requestCode == GALLERY_PICKED_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                //          Check for file if exists
                File file = new File(picturePath);
                if(file.exists())
                {
                    filePath = picturePath;
                    new Upload().execute();

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public String getRealPathFromURI(Uri contentUri) {
        try{
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        catch (Exception e){
            return contentUri.getPath();
        }
    }




}
