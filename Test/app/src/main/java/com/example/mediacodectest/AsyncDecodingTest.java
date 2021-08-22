package com.example.mediacodectest;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaExtractor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Environment;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.animation.TimeAnimator;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.content.ContextWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class AsyncDecodingTest implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = AsyncDecodingTest.class.getSimpleName();

    private static EGLContext unityContext = EGL14.EGL_NO_CONTEXT;
    private static EGLDisplay unityDisplay = EGL14.EGL_NO_DISPLAY;
    private static EGLSurface unityDrawSurface = EGL14.EGL_NO_SURFACE;
    private static EGLSurface unityReadSurface = EGL14.EGL_NO_SURFACE;

    private int mTextureId;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private boolean mNewFrameAvailable = false;

    private AsyncDecoder mAsyncDecoder;
    private MediaExtractor mExtractor = new MediaExtractor();
    private MediaFormat mFormat;


    // create a openGL texture and return its object name
    public int glCreateExternalTexture() {
        int[] texId = new int[1];
        GLES20.glGenTextures(1, IntBuffer.wrap(texId));
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId[0]);
        return texId[0];
    }

    public String JAVAInterfaceTest(){
        String videoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        return "JAVAInterfaceTest success";
        return videoPath;
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void createSurface(int width, int height) {

        Log.d(TAG, "Have entered createSurface fucntion");
        unityContext = EGL14.eglGetCurrentContext();
        unityDisplay = EGL14.eglGetCurrentDisplay();
        unityDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        unityReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);

        if (unityContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "UnityEGLContext is invalid -> Most probably wrong thread");
        }

        EGL14.eglMakeCurrent(unityDisplay, unityDrawSurface, unityReadSurface, unityContext);

        // get the openGL texture object name
        mTextureId = glCreateExternalTexture();

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Create a new SurfaceTexture to stream images to a given OpenGL texture.
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        // Create a surface from the surfaceTexture. The created surface will then be configured as the decoder output
        mSurface = new Surface(mSurfaceTexture);

        Log.d(TAG, "Have finished createSurface fucntion");

    }

    public Surface getSurface() {
        return mSurface;
    }

    public void updateTexImage() {
        if (mNewFrameAvailable) {
            if (!Thread.currentThread().getName().equals("UnityMain"))
                Log.e(TAG, "Not called from render thread and hence update texture will fail");
            Log.d(TAG, "updateTexImage");
            mSurfaceTexture.updateTexImage();
            mNewFrameAvailable = false;
        }
    }

    public long getTimestamp() {
        return mSurfaceTexture.getTimestamp();
    }

    public float[] getTransformMatrix() {
        float[] textureTransform = new float[16];
        mSurfaceTexture.getTransformMatrix(textureTransform);
        return textureTransform;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mNewFrameAvailable = true;
    }



    public void startPlayBack() {

        try {

            // BEGIN_INCLUDE(initialize_extractor)
            mExtractor.setDataSource("https://stream7.iqilu.com/10339/upload_transcode/202002/18/20200218114723HDu3hhxqIT.mp4");
            int nTracks = mExtractor.getTrackCount();

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < nTracks; ++i) {
                mExtractor.unselectTrack(i);
            }

            // Find the first video track in the stream. In a real-world application
            // it's possible that the stream would contain multiple tracks, but this
            // sample assumes that we just want to play the first one.
            createSurface(960, 540);
            for (int i = 0; i < nTracks; ++i) {
                // Try to create a video codec for this track. This call will return null if the
                // track is not a video track, or not a recognized video format. Once it returns
                // a valid MediaCodecWrapper, we can break out of the loop.
                mFormat = mExtractor.getTrackFormat(i);
                String mimeType = mFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.contains("video/")) {
                    mExtractor.selectTrack(i);
                    mAsyncDecoder = new AsyncDecoder(mExtractor, mFormat, getSurface());
                    break;
                }
            }
            // END_INCLUDE(initialize_extractor)
            Log.d(TAG, "Successfully initialize the decoder and configure its output to be a surface");

        } catch (IOException e) {
            Log.e(TAG, "Fucking wrong");
            e.printStackTrace();
        }
    }
}
