package fithou.duyphuong.pblive;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import hou.duyphuong.librtmp.core.listener.RESConnectionListener;
import hou.duyphuong.librtmp.model.RESConfig;
import hou.duyphuong.librtmp.model.RESCoreParameters;
import hou.duyphuong.librtmp.model.Size;
import hou.duyphuong.librtmp.rtmp.RESFlvData;
import hou.duyphuong.librtmp.rtmp.RESFlvDataCollecter;
import hou.duyphuong.librtmp.rtmp.RESRtmpSender;
import hou.duyphuong.librtmp.tools.LogTools;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, TextWatcher, View.OnLongClickListener, RESConnectionListener {

    private static final int MULTIPLE_PERMISSIONS = 1;
    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO };
    String MY_PRE = "UserInput";
    SharedPreferences.Editor saver;
    SharedPreferences savedInput;

    private EditText edtLinkServer;
    private EditText edtKeyStream;
    private ToggleButton btnStart;
    private boolean isStreaming = false;

    // stream init
    private RESRtmpSender rtmpSender;
    MediaProjection mediaProjection;
    private String rtmpAddr = "";
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecorder mVideoRecorder;
    private ExecutorService executorService;
    private RESAudioClient audioClient;
    private RESCoreParameters coreParameters;
    private static final int REQUEST_CODE = 1;
    private int mScreenDensity;
    private RESConfig resConfig;
    private int resolutionWidth, resolutionHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadControls();
        loadEvents();
    }

    private void loadControls() {
        edtLinkServer = findViewById(R.id.edtLinkServer);
        edtKeyStream = findViewById(R.id.edtKeyStream);
        loadLastSavedInput();
        getDeviceResolution();
        btnStart = findViewById(R.id.btnOk);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    private void getDeviceResolution() {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        resolutionWidth = size.x;
        resolutionHeight = size.y;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissions) {
                        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                            permissionsDenied += "," + per;
                        }
                    }
                }
                return;
            }
        }
    }

    private boolean checkPermissions() {
        int result;
        List<String> permissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(p);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[permissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void loadLastSavedInput() {
        savedInput = getSharedPreferences(MY_PRE,MODE_PRIVATE);

        String txtLinkServer = savedInput.getString("linkServer", "");
        String txtKeyStream = savedInput.getString("KeyStream", "");

        if (txtLinkServer.isEmpty() && txtKeyStream.isEmpty()) {
            txtLinkServer = "rtmp://a.rtmp.youtube.com/live2/";
            txtKeyStream = "em44-rjz5-qh1x-8qjg";
        }

        edtKeyStream.setText(txtKeyStream);
        edtLinkServer.setText(txtLinkServer);
    }

    private void loadConfigs() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        mScreenDensity = metrics.densityDpi;
        rtmpAddr = edtLinkServer.getText().toString() + edtKeyStream.getText().toString();

        resConfig = RESConfig.obtain();
        resConfig.setTargetVideoSize(new Size(resolutionHeight, resolutionWidth));
        resConfig.setWidthScreen(resolutionWidth);
        resConfig.setHeightScreen(resolutionHeight);
        resConfig.setRtmpAddr(rtmpAddr);
    }

    private void loadEvents() {
        edtLinkServer.addTextChangedListener(this);
        edtKeyStream.addTextChangedListener(this);
        btnStart.setOnCheckedChangeListener(this);
        btnStart.setOnLongClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (checkPermissions()) {
                loadConfigs();
                startScreenCapture();
            } else {
                Toast.makeText(this, "Permission needed", Toast.LENGTH_SHORT).show();
                btnStart.setChecked(false);
            }

        } else {
            stopScreenRecord();
        }
    }

    private void stopScreenRecord() {
        if (isStreaming) {
            if(mVideoRecorder != null) {
                mVideoRecorder.quit();
                if (audioClient != null) {
                    audioClient.stop();
                }
                rtmpSender.stop();
                isStreaming = false;
                LogTools.d("RESClient,stopStreaming()");
            }
            else {
                LogTools.d("videoClient is null");
            }
        } else {

        }
    }

    private void startScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK){
            Toast.makeText(this, "Permissions denied!", Toast.LENGTH_SHORT).show();
            btnStart.setChecked(false);
            return;
        }
        mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }
        // init audioRecord
        coreParameters = new RESCoreParameters();
        coreParameters.rtmpAddr = resConfig.getRtmpAddr();
        coreParameters.printDetailMsg = resConfig.isPrintDetailMsg();
        coreParameters.senderQueueLength = 150;

        audioClient = new RESAudioClient(coreParameters);
        if (!audioClient.prepare(resConfig)) {
            LogTools.d("!!!!!audioClient.prepare()failed");
            return;
        }
        //init rtmp server
        rtmpSender = new RESRtmpSender();
        rtmpSender.prepare(coreParameters);
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                rtmpSender.feed(flvData,type);
            }
        };
        // send data
        mVideoRecorder = new ScreenRecorder(collecter, resConfig.getHeightScreen(), resConfig.getWidthScreen(), resConfig.getBitRate(), mScreenDensity, mediaProjection);
        mVideoRecorder.start();
        if (audioClient != null) {
            audioClient.start(collecter);
        }
        rtmpSender.start(rtmpAddr);
        rtmpSender.setConnectionListener(this);
        isStreaming = true;

        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        saver = getSharedPreferences(MY_PRE, MODE_PRIVATE).edit();
        if (s == edtLinkServer.getEditableText()) {
            saver.putString("linkServer", edtLinkServer.getText().toString());
        } if (s == edtKeyStream.getEditableText()) {
            saver.putString("KeyStream", edtKeyStream.getText().toString());
        }
        saver.apply();
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.btnOk:

                break;
        }
        return true;
    }

    @Override
    public void onOpenConnectionResult(int result) {
        if (result == 0) {
            Toast.makeText(this, "Connected! Server IP = " + rtmpSender.getServerIpAddr(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Cannot connect to server", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWriteError(int errno) {
        stopScreenRecord();
        Toast.makeText(this, "Broadcast has been terminated due to connection was lost", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCloseConnectionResult(int result) {
        if (result == 0) {
            Toast.makeText(this, "Broadcast finished", Toast.LENGTH_SHORT).show();
        }
    }
}
