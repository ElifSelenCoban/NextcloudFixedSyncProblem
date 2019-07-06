package project.android.nextcloud;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.Stack;

public class MainActivity extends Activity implements OnRemoteOperationListener, OnDatatransferProgressListener {
    protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;

    private static String LOG_TAG = MainActivity.class.getCanonicalName();

    private Handler mHandler;

    private OwnCloudClient mClient;

    private FilesArrayAdapter mFilesAdapter;

    private View mFrame;

    private String localFolder;
    Stack<String> stackOfpath,stackdelete;
    ListView listView;
    TextView text_view1;
    String serverurl,username,pass;
    ArrayList<String> folders = new ArrayList<>();
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = this.getSharedPreferences("logininfo",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        sharedread();
        findViewById(R.id.uploadfile).setClickable(false);
        mHandler = new Handler();
        stackOfpath = new Stack<>();
        stackdelete= new Stack<>();
        Uri serverUri = Uri.parse(serverurl);
        mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
        mClient.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(username,pass)
        );
        listView = findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                text_view1 = (TextView) ((View) view.findViewById(R.id.textViewUserName));
                stackOfpath.push(text_view1.getText().toString());
                stackdelete.push(text_view1.getText().toString());
                startRefresh();
            }
        });
        mFilesAdapter = new FilesArrayAdapter(this, R.layout.file_in_list, this);
        ((ListView) findViewById(R.id.list)).setAdapter(mFilesAdapter);


        mFrame = findViewById(R.id.frame);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 0x4);
        }
    }

    private void sharedread()
    {
        sharedPreferences = this.getSharedPreferences("logininfo", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
         username = sharedPreferences.getString("username", "");
         AESCrypt aesCrypt = new AESCrypt();
        try {
            pass = aesCrypt.decrypt(sharedPreferences.getString("pass", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverurl = sharedPreferences.getString("serverurl", "");
    }
    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        }
    }


    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    public void onClickHandler(View button) {
        switch (button.getId()) {
            case R.id.remotefilebutton:
                startRefresh();
                break;
            case R.id.uploadfile:
                startUpload();
                break;

            case R.id.logout:
                startDownload();
                break;
            case R.id.selectfolder:
                pickDirectory();
                break;
            default:
        }
    }


    private void startRefresh() {
        if (stackOfpath.empty() || stackdelete.empty()) {
            ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation(FileUtils.PATH_SEPARATOR);
            refreshOperation.execute(mClient, this, mHandler);
        } else {
            ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation(stackOfpath.peek());
            refreshOperation.execute(mClient, this, mHandler);
        }
    }

    private void pickDirectory() {
        // Note the different intent: PICK_DIRECTORY
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);

        // Set fancy title and button (optional)
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.pick_directory_title));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getString(R.string.pick_directory_button));

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
        } catch (ActivityNotFoundException e) {
        }

    }


    private void createRemoteFolder(String path, boolean createFullPath) {
        recursiveUpload(path);
        folders.remove(path);
        CreateFolderRemoteOperation createOperation = new CreateFolderRemoteOperation(path, createFullPath);
        createOperation.execute(mClient, this, mHandler);
    }

    private void uploadSingleFile(File fileToUpload) {
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileToUpload.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        // Get the last modification date of the file from the file system
        Long timeStampLong = fileToUpload.lastModified() / 1000;
        String timeStamp = timeStampLong.toString();

        UploadFileRemoteOperation uploadOperation =
                new UploadFileRemoteOperation(fileToUpload.getAbsolutePath(), fileToUpload.getPath(), mimeType, timeStamp);
        uploadOperation.addDataTransferProgressListener(this);
        uploadOperation.execute(mClient, this, mHandler);
    }


    private void recursiveUpload(String path) {
        File upFolder = new File(path);

        for (File fileToUpload : upFolder.listFiles()) {
            if (fileToUpload.isDirectory()) {
                String remotePath = path + "/" + fileToUpload.getName();
                folders.add(remotePath);
            }
        }
    }

    private void recursiveFileUpload(String path) {
        File upFolder = new File(path);

        for (File fileToUpload : upFolder.listFiles()) {
            if (fileToUpload.isDirectory()) {
                recursiveFileUpload(path + "/" + fileToUpload.getName());
            } else {
                uploadSingleFile(fileToUpload);
            }
        }
    }

    private void startUpload() {
        createRemoteFolder(localFolder, true);
    }

    private void startRemoteDeletion() {
        File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
        File fileToUpload = upFolder.listFiles()[0];
        String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();
        RemoveFileRemoteOperation removeOperation = new RemoveFileRemoteOperation(remotePath);
        removeOperation.execute(mClient, this, mHandler);
    }

    private void startDownload() {
      editor.putString("username","");
      editor.commit();
      finish();
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CreateFolderRemoteOperation) {
            if (folders.size() > 0)
                createRemoteFolder(folders.get(0), true);
            else
                recursiveFileUpload(localFolder);
        }

        if (!result.isSuccess() && !(operation instanceof CreateFolderRemoteOperation)) {
            Log.e(LOG_TAG, result.getLogMessage(), result.getException());

        } else if (operation instanceof ReadFolderRemoteOperation) {
            onSuccessfulRefresh((ReadFolderRemoteOperation) operation, result);

        } else if (operation instanceof UploadFileRemoteOperation) {
            onSuccessfulUpload((UploadFileRemoteOperation) operation, result);

        } else if (operation instanceof RemoveFileRemoteOperation) {
            onSuccessfulRemoteDeletion((RemoveFileRemoteOperation) operation, result);

        } else if (operation instanceof DownloadFileRemoteOperation) {
            onSuccessfulDownload((DownloadFileRemoteOperation) operation, result);

        } else {
           // findViewById(R.id.uploadfile).setVisibility(View.INVISIBLE);
            findViewById(R.id.uploadfile).setClickable(false);
        }
    }

    private void onSuccessfulRefresh(ReadFolderRemoteOperation operation, RemoteOperationResult result) {
        mFilesAdapter.clear();
        List<RemoteFile> files = new ArrayList<RemoteFile>();
        for (Object obj : result.getData()) {
            files.add((RemoteFile) obj);
        }
        if (files != null) {
            Iterator<RemoteFile> it = files.iterator();
            while (it.hasNext()) {
                mFilesAdapter.add(it.next());
            }
            mFilesAdapter.remove(mFilesAdapter.getItem(0));
        }
        mFilesAdapter.notifyDataSetChanged();
    }

    private void onSuccessfulUpload(UploadFileRemoteOperation operation, RemoteOperationResult result) {
        startRefresh();
    }

    private void onSuccessfulRemoteDeletion(RemoveFileRemoteOperation operation, RemoteOperationResult result) {
        startRefresh();
        TextView progressView = findViewById(R.id.proses);
    }

    @SuppressWarnings("deprecation")
    private void onSuccessfulDownload(DownloadFileRemoteOperation operation, RemoteOperationResult result) {
        File downFolder = new File(getCacheDir(), getString(R.string.download_folder_path));
        File downloadedFile = downFolder.listFiles()[0];
        BitmapDrawable bDraw = new BitmapDrawable(getResources(), downloadedFile.getAbsolutePath());
        mFrame.setBackgroundDrawable(bDraw);
    }

    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        final long percentage = (totalToTransfer > 0 ? totalTransferredSoFar * 100 / totalToTransfer : 0);
        final boolean upload = fileName.contains(getString(R.string.upload_folder_path));
        Log.d(LOG_TAG, "progressRate " + percentage);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                TextView progressView = null;
                progressView = findViewById(R.id.proses);
                if (progressView != null) {
                    progressView.setText(Long.toString(percentage) + "%");
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
                if (resultCode == RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            localFolder = filePath;

                            findViewById(R.id.uploadfile).setClickable(true);
                        }
                    }
                }
                break;
        }
    }

    public void backfolder(View view) {
        if (stackOfpath.size() >= 1) {
            if(stackdelete.size() >= 1)
            {stackdelete.pop();}
            ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation(stackOfpath.pop());
            refreshOperation.execute(mClient, MainActivity.this, mHandler);

        } else {


            ReadFolderRemoteOperation refreshOperation = new ReadFolderRemoteOperation("/");
            refreshOperation.execute(mClient, MainActivity.this, mHandler);

        }
        startRefresh();

    }

    public void deletefile(View view) {
try{
       /* File upFolder = new File(getCacheDir(), stackdelete.pop());
        File fileToUpload = upFolder.listFiles()[0];
        String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();*/
        RemoveFileRemoteOperation removeOperation = new RemoveFileRemoteOperation(stackdelete.pop());
        removeOperation.execute(mClient, this, mHandler);
        stackdelete.clear();
        stackOfpath.clear();
        startRefresh();
    }catch (Exception f)
{
    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
}


    }

}