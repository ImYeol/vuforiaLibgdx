package com.github.daemontus.renderer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.daemontus.ar.libgdx.Engine;
import com.github.daemontus.ar.vuforia.AppSession;
import com.github.daemontus.ar.vuforia.RefFreeFrame;
import com.github.daemontus.ar.vuforia.SessionControl;
import com.github.daemontus.ar.vuforia.VuforiaException;
import com.github.daemontus.ar.vuforia.VuforiaRenderer;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ImageTargetBuilder;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;


public class ArActivity extends AndroidApplication implements SessionControl {

    private static final String LOGTAG = "MAIN";

    private AppSession session;

    private DataSet dataSetUserDef = null;
    private Engine mEngine;

    VuforiaRenderer mRenderer;

    private boolean mExtendedTracking = false;
    int targetBuilderCounter = 1;

    public RefFreeFrame refFreeFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        Log.d(LOGTAG, "onCreate");

        session = new AppSession(this);
        session.initAR(this, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mRenderer = new VuforiaRenderer(this,session);

        FrameLayout container = (FrameLayout) findViewById(R.id.ar_container);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        //config.useGL20 = true;

        mEngine = new Engine(mRenderer);
        View glView = initializeForView(mEngine, config);

        container.addView(glView);

    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        //we do not resume AR here if splash screen is visible
        try {
            session.resumeAR();
        } catch (VuforiaException e) {
            Toast.makeText(this, "Unable to start augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, e.getString());
        }
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        try {
            session.pauseAR();
        } catch (VuforiaException e) {
            Toast.makeText(this, "Unable to stop augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, e.getString());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        session.onConfigurationChanged();
    }

    // Initializes AR application components.
    private void initApplicationAR()
    {
        // Do application initialization
        refFreeFrame = new RefFreeFrame(this, session);
        refFreeFrame.init();


        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

       //create GLView and set GLView.Renderer


        // startUserDefinedTargets
        startUserDefinedTargets();



    }

    @Override
    public void onInitARDone(VuforiaException exception) {
        if (exception == null) {

            initApplicationAR();

            mRenderer.mIsActive = true;

            try {
                session.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            } catch (VuforiaException e) {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (!result) Log.e(LOGTAG, "Unable to enable continuous autofocus");

            try {
                mEngine.resume();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Toast.makeText(this, "Unable to start augmented reality.", Toast.LENGTH_LONG).show();
            Log.e(LOGTAG, exception.getString());
            finish();
        }

    }


    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            session.stopAR();
        } catch (VuforiaException e) {
            Log.e(LOGTAG, e.getString());
        }

        System.gc();
    }

    boolean startUserDefinedTargets()
    {
        Log.d(LOGTAG, "startUserDefinedTargets");

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager
                .getTracker(ObjectTracker.getClassType()));
        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();

            if (targetBuilder != null)
            {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();

                objectTracker.stop();

                targetBuilder.startScan();

            }
        } else
            return false;

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if( keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
            if (isUserDefinedTargetsRunning())
            {
                // Shows the loading dialog
             /*   loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
*/
                // Builds the new target
                startBuild();
            }
        }
        return false;
    }

    boolean isUserDefinedTargetsRunning()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                Log.e(LOGTAG, "Quality> " + targetBuilder.getFrameQuality());
                return (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) ? true
                        : false;
            }
        }

        return false;
    }

    void startBuild()
    {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker != null)
        {
            ImageTargetBuilder targetBuilder = objectTracker
                    .getImageTargetBuilder();
            if (targetBuilder != null)
            {
                if (targetBuilder.getFrameQuality() == ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW)
                {
                   // showErrorDialogInUIThread();
                }

                String name;
                do
                {
                    name = "UserTarget-" + targetBuilderCounter;
                    Log.d(LOGTAG, "TRYING " + name);
                    targetBuilderCounter++;
                } while (!targetBuilder.build(name, 320.0f));

                refFreeFrame.setCreating();
            }
        }
    }

    public void targetCreated()
    {
        // Hides the loading dialog
       /* loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
*/
        if (refFreeFrame != null)
        {
            refFreeFrame.reset();
        }

    }

    public void updateRendering()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        refFreeFrame.initGL(metrics.widthPixels, metrics.heightPixels);
    }

    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker tracker = trackerManager.initTracker(ObjectTracker.getClassType());

        if (tracker == null) {
            Log.d(LOGTAG, "Failed to initialize ImageTracker.");
            result = false;
        } else
        {
            Log.d(LOGTAG, "Successfully initialized ObjectTracker.");
        }

        return result;
    }


    @Override
    public boolean doLoadTrackersData() {
        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

        if (imageTracker == null) {
            Log.d(LOGTAG, "Failed to load tracking data set because the ImageTracker has not been initialized.");
            return false;
        }

        // Create the data sets:
        dataSetUserDef = imageTracker.createDataSet();
        if (dataSetUserDef == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        // Load the data sets:
      /*  if (!dataSetUserDef.load("StonesAndChips.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            Log.d(LOGTAG, "Failed to load data set.");
            return false;
        }
       */
        // Activate the data set:
        if (!imageTracker.activateDataSet(dataSetUserDef)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

        Log.d(LOGTAG, "Successfully loaded and activated data set.");
        return true;
    }


    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null) {
            imageTracker.start();
          //  Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 1);
        }/* else
            result = false;*/

        return result;
    }


    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        Tracker imageTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (imageTracker != null)
            imageTracker.stop();
       /* else
            result = false;*/

        return result;
    }


    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        // Get the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker imageTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (imageTracker == null) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set because the ImageTracker has not been initialized.");
            return false;
        }

        if (dataSetUserDef != null) {
            if (imageTracker.getActiveDataSet() == dataSetUserDef && !imageTracker.deactivateDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set StonesAndChips because the data set could not be deactivated.");
                result = false;
            } else if (!imageTracker.destroyDataSet(dataSetUserDef)) {
                Log.d(LOGTAG, "Failed to destroy the tracking data set StonesAndChips.");
                result = false;
            }
            Log.d(LOGTAG, "Successfully destroyed the data set.");
            dataSetUserDef = null;
        }

        return result;
    }

    @Override
    public boolean doDeinitTrackers() {

        if (refFreeFrame != null)
            refFreeFrame.deInit();

        // Deinit the image tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());

        return true;
    }

    @Override
    public void onQCARUpdate(State state) {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if (refFreeFrame.hasNewTrackableSource())
        {
            Log.d(LOGTAG,
                    "Attempting to transfer the trackable source to the dataset");

            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSet());

            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            if (dataSetUserDef.hasReachedTrackableLimit()
                    || dataSetUserDef.getNumTrackables() >= 5)
                dataSetUserDef.destroy(dataSetUserDef.getTrackable(0));

            if (mExtendedTracking && dataSetUserDef.getNumTrackables() > 0)
            {
                // We need to stop the extended tracking for the previous target
                // so we can enable it for the new one
                int previousCreatedTrackableIndex =
                        dataSetUserDef.getNumTrackables() - 1;

                objectTracker.resetExtendedTracking();
                dataSetUserDef.getTrackable(previousCreatedTrackableIndex)
                        .stopExtendedTracking();
            }

            // Add new trackable source
            Trackable trackable = dataSetUserDef
                    .createTrackable(refFreeFrame.getNewTrackableSource());

            // Reactivate current dataset
            objectTracker.activateDataSet(dataSetUserDef);

            if (mExtendedTracking)
            {
                trackable.startExtendedTracking();
            }

        }

    }
}
