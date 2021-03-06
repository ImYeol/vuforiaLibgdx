package com.github.daemontus.ar.vuforia;

import android.opengl.GLES20;
import android.util.Log;

import com.github.daemontus.renderer.ArActivity;
import com.vuforia.CameraCalibration;
import com.vuforia.CameraDevice;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.TrackableResult;
import com.vuforia.Vec2F;
import com.vuforia.Vuforia;

/**
 * Vuforia renderer, responsible for video background rendering, tracking and position calculations
 */
public class VuforiaRenderer {

    private static final String LOGTAG = "VuforiaRenderer";

    public static String lastTrackableName = "";

    private AppSession vuforiaAppSession;

    private Renderer mRenderer;

    public boolean mIsActive = false;

    public float fieldOfViewRadians;

    private ArActivity mActivity;

    // Constants:
    static final float kObjectScale = 3.f;


    public VuforiaRenderer(ArActivity activity, AppSession session) {
        mActivity=activity;
        vuforiaAppSession = session;
    }


    // Called when the surface changed size.
    public void onSurfaceChanged(int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

      //  mActivity.updateRendering();
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }


    // Function for initializing the renderer.
    public void initRendering()
    {
        Log.d(LOGTAG, "GLRenderer.initRendering");

        mRenderer = Renderer.getInstance();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);


        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }


    // The render function.
    public TrackableResult[] processFrame()
    {
        if (!mIsActive)
            return null;

        State state = mRenderer.begin();
    //    mRenderer.drawVideoBackground();

        // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        // Render the RefFree UI elements depending on the current state
        mActivity.refFreeFrame.render(); // start tracker
        // did we find any trackables this frame?
        TrackableResult[] results = new TrackableResult[state.getNumTrackableResults()];
  /*      for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(trackableResult.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            float[] modelViewProjection = new float[16];
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, kObjectScale);
            Matrix.scaleM(modelViewMatrix, 0, kObjectScale, kObjectScale,
                    kObjectScale);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
        }*/

        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            //remember trackable
            TrackableResult result = state.getTrackableResult(tIdx);
            lastTrackableName = result.getTrackable().getName();
            results[tIdx] = result;

            //calculate filed of view
            CameraCalibration calibration = CameraDevice.getInstance().getCameraCalibration();
            Vec2F size = calibration.getSize();
            Vec2F focalLength = calibration.getFocalLength();
            fieldOfViewRadians = (float) (2 * Math.atan(0.5f * size.getData()[0] / focalLength.getData()[0]));
        }

        mRenderer.end();

        return results;
    }

}
