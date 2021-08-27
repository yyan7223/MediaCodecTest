package com.example.mediacodectest;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TextureRendererPlugIn implements SurfaceTexture.OnFrameAvailableListener {
    private static TextureRendererPlugIn _instance;
    private Activity mUnityActivity;
    private int mTextureWidth;
    private int mTextureHeight;
    private static String TAG = "TextureRendererPlugIn";

    private static EGLContext unityContext = EGL14.EGL_NO_CONTEXT;
    private static EGLDisplay unityDisplay = EGL14.EGL_NO_DISPLAY;
    private static EGLSurface unityDrawSurface = EGL14.EGL_NO_SURFACE;
    private static EGLSurface unityReadSurface = EGL14.EGL_NO_SURFACE;

    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private int unityTextureID;

    private AsyncDecoder mAsyncDecoder;
    private SyncDecoder mSyncDecoder;
    private MediaExtractor mExtractor = new MediaExtractor();
    private MediaFormat mFormat;
    TextView mAttribView = null;

    private boolean mNewFrameAvailable;
    private Rect rec;
    private Paint p;
    private Random rnd;
    Handler hnd;

    public static ConcurrentLinkedQueue<byte[]> h264FrameQueue = new ConcurrentLinkedQueue<byte[]>();

    public int glCreateExternalTexture() {
        Log.d(TAG, "glCreateExternalTexture() called");
        int[] texId = new int[1];
        GLES20.glGenTextures(1, texId, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId[0]);
        Log.d(TAG, "glCreateExternalTexture() GLES20.glGetError() is: " + GLES20.glGetError());
        return texId[0];
    }

    public int getTextureId() {
        Log.d(TAG, "getTextureId() called");
        return unityTextureID;
    }

    private TextureRendererPlugIn(Activity unityActivity, int width, int height) {
        Log.d(TAG, "constructor called");
        mUnityActivity = unityActivity;
        mTextureWidth = width;
        mTextureHeight = height;
        unityTextureID = glCreateExternalTexture();
        mNewFrameAvailable = false;

        initSurface();

//        startAsyncUrlPlayback(); // Test URL decoding
//        startAsyncH264Decoding(); // Test H264 byte stream decoding
        initSyncH264Decoder(); // init synchronized H264 decoder

        // Test drawing circles
//        rec = new Rect(0,0,width,height);
//        p = new Paint();
//        rnd = new Random();
//        hnd = new Handler(Looper.getMainLooper());
//
//        drawRandomCirclesInSurface();
    }

    private void drawRandomCirclesInSurface() {
        Log.d(TAG, "drawRandomCirclesInSurface() called");
        Canvas c = mSurface.lockCanvas(rec);
        p.setColor(Color.argb( 255, rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)));
        int radius = rnd.nextInt(100);
        c.drawCircle(rnd.nextInt(mTextureWidth),rnd.nextInt(mTextureHeight),radius,p);
        mSurface.unlockCanvasAndPost(c);

        hnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawRandomCirclesInSurface();
            }
        },100);
    }

    private void initSurface() {
        Log.d(TAG, "initSurface() called");
        unityContext = EGL14.eglGetCurrentContext();
        unityDisplay = EGL14.eglGetCurrentDisplay();
        unityDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW);
        unityReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ);

        if (unityContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "UnityEGLContext is invalid -> Most probably wrong thread");
        }

        EGL14.eglMakeCurrent(unityDisplay, unityDrawSurface, unityReadSurface, unityContext);

        if(unityTextureID == 0){
            unityTextureID = glCreateExternalTexture();
        }
        Log.d(TAG, "Texture id is: " + unityTextureID);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, unityTextureID);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        Log.d(TAG, "initSurface() GLES20.glGetError() is: " + GLES20.glGetError());

        mSurfaceTexture = new SurfaceTexture(unityTextureID);
        mSurfaceTexture.setDefaultBufferSize(mTextureWidth, mTextureHeight);
        mSurface = new Surface(mSurfaceTexture);
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    public static TextureRendererPlugIn Instance(Activity context, int viewPortWidth,
                                                 int viewPortHeight) {
        Log.d(TAG, "Instance() called");
        if (_instance == null) {
            _instance = new TextureRendererPlugIn(context, viewPortWidth, viewPortHeight);
        }

        return _instance;
    }

    public void updateSurfaceTexture()
    {
        if(mNewFrameAvailable) {
            Log.d(TAG, "updateSurfaceTexture() called");
            if(!Thread.currentThread().getName().equals("UnityMain"))
                Log.e(TAG, "Not called from render thread and hence update texture will fail");
            mSurfaceTexture.updateTexImage();
            mNewFrameAvailable = false;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onFrameAvailable() called");
        mNewFrameAvailable = true;
    }

    public void startAsyncUrlPlayback() {

        try {

            // BEGIN_INCLUDE(initialize_extractor)
            mExtractor.setDataSource("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
            int nTracks = mExtractor.getTrackCount();

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.
            for (int i = 0; i < nTracks; ++i) {
                mExtractor.unselectTrack(i);
            }

            // Find the first video track in the stream. In a real-world application
            // it's possible that the stream would contain multiple tracks, but this
            // sample assumes that we just want to play the first one.
            for (int i = 0; i < nTracks; ++i) {
                // Try to create a video codec for this track. This call will return null if the
                // track is not a video track, or not a recognized video format. Once it returns
                // a valid MediaCodecWrapper, we can break out of the loop.
                mFormat = mExtractor.getTrackFormat(i);
                String mimeType = mFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.contains("video/")) {
                    mExtractor.selectTrack(i);
                    mAsyncDecoder = new AsyncDecoder(mExtractor, mFormat, mSurface);
                    mAsyncDecoder.startDecoder();
                    break;
                }
            }
            // END_INCLUDE(initialize_extractor)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // https://blog.csdn.net/DTTYYY/article/details/105851081
    public void startAsyncH264Decoding(){
        String mimeType = "video/avc";
        MediaFormat mFormat = MediaFormat.createVideoFormat(mimeType, mTextureWidth, mTextureHeight);
        mAsyncDecoder = new AsyncDecoder(mFormat, mSurface);
        mAsyncDecoder.startDecoder();
    }

    public void initSyncH264Decoder(){
        String mimeType = "video/avc";
        MediaFormat mFormat = MediaFormat.createVideoFormat(mimeType, mTextureWidth, mTextureHeight);
        mSyncDecoder = new SyncDecoder(mFormat, mSurface);
        mSyncDecoder.startDecoder();
    }

    // 原文链接：https://blog.csdn.net/qjh5606/article/details/85298981
    public void queueH264FrameData(byte[] frame)
    {
        Log.i("Unity", "H264 frame []: " + frame + " and length: " + frame.length);
        h264FrameQueue.add(frame);
//        Log.i("Unity", h264FrameQueue.size() + " H264 frames are queued");

        // if using Synchronized decoder, one should perform decoding every time after Unity pass the H264 data to java
        mSyncDecoder.writeSample();
        mSyncDecoder.popSample();
    }
}
