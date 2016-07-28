package it.crs4.most.demo.ecoapp;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.NativeInterface;
import org.artoolkit.ar.base.assets.AssetHelper;
import org.artoolkit.ar.base.camera.CameraEventListener;
import org.artoolkit.ar.base.camera.CameraPreferencesActivity;
import org.artoolkit.ar.base.camera.CaptureCameraPreview;
import org.artoolkit.ar.base.rendering.ARRenderer;
import org.artoolkit.ar.base.rendering.gles20.ARRendererGLES20;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import it.crs4.most.demo.ecoapp.models.Teleconsultation;
import it.crs4.most.visualization.IStreamFragmentCommandListener;
import it.crs4.most.visualization.augmentedreality.OpticalARToolkit;
import it.crs4.most.visualization.augmentedreality.TouchGLSurfaceView;
import it.crs4.most.visualization.augmentedreality.mesh.Arrow;
import it.crs4.most.visualization.augmentedreality.mesh.Mesh;
import it.crs4.most.visualization.augmentedreality.renderer.OpticalRenderer;
import it.crs4.most.visualization.augmentedreality.renderer.PubSubARRenderer;
import it.crs4.most.visualization.utils.zmq.ZMQSubscriber;
import jp.epson.moverio.bt200.DisplayControl;
// For Epson Moverio BT-200. BT200Ctrl.jar must be in libs/ folder.


public class AREcoTeleconsultationActivity extends BaseEcoTeleconsultationActivity implements
        CameraEventListener {
    protected static final String TAG = "LocalARActivity";
    protected PubSubARRenderer renderer;
    protected FrameLayout mainLayout;
    private CaptureCameraPreview preview;
    private TouchGLSurfaceView glView;
    //    private TouchGLSurfaceView glView;
    private boolean firstUpdate = false;
    private OpticalARToolkit mOpticalARToolkit;
    private EditText coordX, coordY, coordZ;
    private HashMap<String, Mesh> meshes = new HashMap<>();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        private boolean toggle = true;
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive, intent.getAction() " + intent.getAction());
            if(intent.getAction().equals("HOLDCALL")){
                toggleHoldCall(toggle);
                toggle = !toggle;
            }
            else if (intent.getAction().equals("HANGUP")){
                hangupCall();
            }
        }
    };


    public static class RemoteControlReceiver extends BroadcastReceiver {
        String TAG = "RemoteControlReceiver";
        final static Timer timer = new Timer();
        static long actionDownTime;
        static boolean actionDownReceived = false;


        @Override
        public void onReceive(Context context, Intent intent) {
            final Context ctx = context;
            Log.d(TAG, "onReceive, intent.getAction() " + intent.getAction());

            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                Log.d(TAG, "OMG key pressed");
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    context.sendBroadcast(new Intent("HOLDCALL"));
                }
                else{
                    int keycode = event.getKeyCode();
                    int action = event.getAction();
                    long eventTime = event.getEventTime();

                    Log.d(TAG, "eventTime " + eventTime);
//                    if (keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        if (action == KeyEvent.ACTION_DOWN && ! actionDownReceived) {
                            Log.d("EVENT", "ACTION_DOWN, eventTime " + eventTime);
                            actionDownTime = eventTime;
                            actionDownReceived = true;
//                            timer.schedule(new TimerTask() {
//                                @Override
//                                public void run() {
//                                    ctx.sendBroadcast(new Intent("HANGUP"));
//                                }
//                            }, 1000);
                        }
                        else if(action == KeyEvent.ACTION_UP){
                            Log.d("EVENT", "ACTION_UP eventTime " + eventTime);
                            Log.d("EVENT", "eventTime - actionDownTime " + (eventTime - actionDownTime));
                            if (eventTime - actionDownTime < 1000){
                                Log.d("EVENT", "HOLDCALL intent");
                                context.sendBroadcast(new Intent("HOLDCALL"));
                            }
                            else{
                                Log.d("EVENT", "HANGUP intent");
                                ctx.sendBroadcast(new Intent("HANGUP"));
                            }
                            actionDownReceived = false;
//                            timer.cancel();
//                            context.sendBroadcast(new Intent("HOLDCALL"));
                        }
//                    }
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//
//        View decorView = getWindow().getDecorView();
//    // Hide both the navigation bar and the status bar.
//    // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
//    // a general rule, you should design your app to hide the status bar whenever you
//    // hide the navigation bar.
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//        decorView.setSystemUiVisibility(uiOptions);


        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE) ;
        ComponentName componentName = new ComponentName(this, RemoteControlReceiver.class);
        am.registerMediaButtonEventReceiver(componentName);

        registerReceiver(broadcastReceiver, new IntentFilter("HOLDCALL"));
        registerReceiver(broadcastReceiver, new IntentFilter("HANGUP"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_eco);

        AssetHelper assetHelper = new AssetHelper(getAssets());
        assetHelper.cacheAssetFolder(this, "Data");


        if((Build.MANUFACTURER.equals("EPSON") && Build.MODEL.equals("embt2"))){
            Log.d(TAG, "loading optical files");
            mOpticalARToolkit = new OpticalARToolkit(ARToolKit.getInstance());
        }

        String configServerIP = QuerySettings.getConfigServerAddress(this);
        int configServerPort = Integer.valueOf(QuerySettings.getConfigServerPort(this));
        rcr = new RemoteConfigReader(this, configServerIP, configServerPort);
//        init();
        setTeleconsultationState(TeleconsultationState.IDLE);
        Intent i = getIntent();
        teleconsultation = (Teleconsultation) i.getExtras().getSerializable("Teleconsultation");
        setupVoipLib();

//        String address = "156.148.33.87:5555";
//        String address = "156.148.33.87:5556";
        String address = "156.148.33.66:5556";
        ZMQSubscriber subscriber = new ZMQSubscriber(address);
        Thread subThread = new Thread(subscriber);
        subThread.start();
        if (mOpticalARToolkit != null){
            Log.d(TAG, "setting OpticalRenderer");
            renderer = new OpticalRenderer(this, subscriber, mOpticalARToolkit);
        }
        else{
            renderer = new PubSubARRenderer(this, subscriber);
        }

        Arrow arrow = new Arrow("arrow");
        meshes.put(arrow.getId(), arrow);
        renderer.setMeshes(meshes);
    }

    protected void onStart() {
        super.onStart();
        Log.i("ARActivity", "onStart(): Activity starting.");
        if(!ARToolKit.getInstance().initialiseNative(this.getCacheDir().getAbsolutePath())) {
            (new AlertDialog.Builder(this)).setMessage("The native library is not loaded. The application cannot continue.").setTitle("Error").setCancelable(true).setNeutralButton(17039360, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    AREcoTeleconsultationActivity.this.finish();
                }
            }).show();
        } else {
            this.mainLayout = this.supplyFrameLayout();
            if(this.mainLayout == null) {
                Log.e("ARActivity", "onStart(): Error: supplyFrameLayout did not return a layout.");
            }
        }

    }
    @Override
    public void onResume() {
        super.onResume();
        preview = new CaptureCameraPreview(this, this);
        preview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (Build.MANUFACTURER.equals("EPSON") && Build.MODEL.equals("embt2")) {
                    DisplayControl displayControl = new DisplayControl(AREcoTeleconsultationActivity.this);
                    boolean stereo = PreferenceManager.getDefaultSharedPreferences(AREcoTeleconsultationActivity.this).
                            getBoolean("pref_stereoDisplay", false);
            displayControl.setMode(DisplayControl.DISPLAY_MODE_3D, stereo);
//            Window win = getWindow();
//            WindowManager.LayoutParams winParams = win.getAttributes();
////        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
//
//                    winParams.flags |= 0x80000000;
//                    win.setAttributes(winParams);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        Log.i("ARActivity", "onResume(): CaptureCameraPreview created");
//        this.glView = new GLSurfaceView(this);
        this.glView = new TouchGLSurfaceView(this);

        ActivityManager activityManager = (ActivityManager)this.getSystemService("activity");
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 131072;
        if(supportsEs2) {
            Log.i("ARActivity", "onResume(): OpenGL ES 2.x is supported");
            if((ARRenderer)renderer instanceof ARRendererGLES20) {
                this.glView.setEGLContextClientVersion(2);
            } else {
                Log.w("ARActivity", "onResume(): OpenGL ES 2.x is supported but only a OpenGL 1.x renderer is available. \n Use ARRendererGLES20 for ES 2.x support. \n Continuing with OpenGL 1.x.");
                this.glView.setEGLContextClientVersion(1);
            }
        } else {
            Log.i("ARActivity", "onResume(): Only OpenGL ES 1.x is supported");
            if((ARRenderer)renderer instanceof ARRendererGLES20) {
                throw new RuntimeException("Only OpenGL 1.x available but a OpenGL 2.x renderer was provided.");
            }

            this.glView.setEGLContextClientVersion(1);
        }

//        this.glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        Log.d(TAG, "ready to call setRenderer with " + (renderer !=null));
        this.glView.getHolder().setFormat(-3);
        this.glView.setRenderer((PubSubARRenderer) renderer);
        Log.d(TAG, "setRenderer called");
        this.glView.setRenderMode(0);
        this.glView.setZOrderMediaOverlay(true);
        Log.i("ARActivity", "onResume(): GLSurfaceView created");
        this.mainLayout.addView(this.preview, new ViewGroup.LayoutParams(-1, -1));
        this.mainLayout.addView(this.glView, new ViewGroup.LayoutParams(-1, -1));
        Log.i("ARActivity", "onResume(): Views added to main layout.");
        if(this.glView != null) {
            this.glView.onResume();
        }
    }

    protected void onPause() {
        super.onPause();
        if(this.glView != null) {
            this.glView.onPause();
        }

        this.mainLayout.removeView(this.glView);
        this.mainLayout.removeView(this.preview);
    }

    public void onStop() {
        Log.i("ARActivity", "onStop(): Activity stopping.");
        super.onStop();
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == org.artoolkit.ar.base.R.id.settings) {
            this.startActivity(new Intent(this, CameraPreferencesActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public CaptureCameraPreview getCameraPreview() {
        return this.preview;
    }

    public GLSurfaceView getGLView() {
        return this.glView;
    }

    public void cameraPreviewStarted(int width, int height, int rate, int cameraIndex, boolean cameraIsFrontFacing) {
        if(ARToolKit.getInstance().initialiseAR(width, height, "Data/camera_para.dat", cameraIndex, cameraIsFrontFacing)) {
            Log.d(TAG, String.format("Build.MANUFACTURER %s", Build.MANUFACTURER));
            Log.d(TAG, String.format("Build.MODEL %s", Build.MODEL));

            if(mOpticalARToolkit != null){
                Log.d(TAG, "loading optical files");
                if ( mOpticalARToolkit.initialiseAR(
                        "Data/optical_param_left.dat", "Data/optical_param_right") > 0){
                    Log.d(TAG, "loaded optical files");
                    Log.d(TAG, "getEyeRproject len " + mOpticalARToolkit.getEyeRproject().length);
                }
                else {
                    Log.e("ARActivity", "Error initialising optical device. Cannot continue.");
                    this.finish();
                }
            }
            Log.i("ARActivity", "getGLView(): Camera initialised");


        } else {
            Log.e("ARActivity", "getGLView(): Error initialising camera. Cannot continue.");
            this.finish();
        }

        Toast.makeText(this, "Camera settings: " + width + "x" + height + "@" + rate + "fps", 0).show();
        this.firstUpdate = true;
    }

    public void cameraPreviewFrame(byte[] frame) {
        if(this.firstUpdate) {

            if(this.renderer.configureARScene()) {
                Log.i("ARActivity", "cameraPreviewFrame(): Scene configured successfully");
            } else {
                Log.e("ARActivity", "cameraPreviewFrame(): Error configuring scene. Cannot continue.");
                this.finish();
            }

            this.firstUpdate = false;
        }

        if(ARToolKit.getInstance().convertAndDetect(frame)) {

            if(this.glView != null) {
                this.glView.requestRender();
            }

            this.onFrameProcessed();
        }

    }

    public void onFrameProcessed() {
    }

    public void cameraPreviewStopped() {
        ARToolKit.getInstance().cleanup();
    }

    protected void showInfo() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage("ARToolKit Version: " + NativeInterface.arwGetARToolKitVersion());
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = dialogBuilder.create();
        alert.setTitle("ARToolKit");
        alert.show();
    }


    protected ARRenderer supplyRenderer() {
        return renderer;
    }

    protected FrameLayout supplyFrameLayout() {
        return (FrameLayout) this.findViewById(R.id.local_ar_frame);
    }

    @Override
    protected void notifyTeleconsultationStateChanged() {
        // FIXME
    }
}