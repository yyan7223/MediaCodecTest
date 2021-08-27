package com.example.mediacodectest;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AsyncDecoder {
    // https://blog.csdn.net/zjfengdou30/article/details/81276154
    private final static String TAG = "AsyncDecoder";
    private MediaCodec  mMediaCodec;
    private MediaFormat mMediaFormat;
    private MediaExtractor mMediaExtractor;
    private Surface     mSurface;

    private Handler mAsyncDecoderHandler;
    private HandlerThread mAsyncDecoderHandlerThread = new HandlerThread("AsyncDecoder");
    private boolean decodeURL = false;
    private int mCount = 0;
    private final static int TIME_INTERNAL = 30;

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec mediaCodec, int id) {
            Log.d(TAG, "Has inputed one frame");
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(id);
            inputBuffer.clear();

            if (decodeURL) {
                int size = mMediaExtractor.readSampleData(inputBuffer, 0);
                Log.d(TAG, "Size is: "+size);
                if (size <= 0) {
                    stopDecoder();
                    release();
                }
                int flags = mMediaExtractor.getSampleFlags();
                long presentationTimeUs = mMediaExtractor.getSampleTime();
                mMediaExtractor.advance();
                mediaCodec.queueInputBuffer(id,0, size, presentationTimeUs, flags);
            }
            else if (!TextureRendererPlugIn.h264FrameQueue.isEmpty()){
                byte[] h264Frame = TextureRendererPlugIn.h264FrameQueue.poll();
                Log.d(TAG, "byte[] Size is: " + h264Frame.length);
                inputBuffer.put(h264Frame);
                mediaCodec.queueInputBuffer(id,0, h264Frame.length, mCount * TIME_INTERNAL, 0);
                mCount++;
            }
            else{
                Log.d(TAG, "Not available ");
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec mediaCodec, int id, MediaCodec.BufferInfo bufferInfo) {
            Log.d(TAG, "Has decoded one frame");
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(id);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(id);
            if(mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0){
                byte [] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
            }
            mMediaCodec.releaseOutputBuffer(id, true);
        }

        @Override
        public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
            Log.d(TAG, "------> onError");
        }

        @Override
        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
            Log.d(TAG, "------> onOutputFormatChanged");
        }
    };

    // decode local video or online video, requires mExtractor
    public AsyncDecoder(MediaExtractor mExtractor, MediaFormat mFormat, Surface surface){
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
            mMediaExtractor = mExtractor;
            mMediaFormat = mFormat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSurface = surface;
        mAsyncDecoderHandlerThread.start();
        mAsyncDecoderHandler = new Handler(mAsyncDecoderHandlerThread.getLooper());
        decodeURL = true;
    }

    // directly decode H264 byte stream
    public AsyncDecoder(MediaFormat mFormat, Surface surface){
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
            mMediaFormat = mFormat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSurface = surface;
        mAsyncDecoderHandlerThread.start();
        mAsyncDecoderHandler = new Handler(mAsyncDecoderHandlerThread.getLooper());
    }

    public void startDecoder(){
        if(mMediaCodec != null && mSurface != null){
            mMediaCodec.setCallback(mCallback, mAsyncDecoderHandler);
            mMediaCodec.configure(mMediaFormat, mSurface, null, 0);
            mMediaCodec.start();
        }else{
            throw new IllegalArgumentException("startDecoder failed, please check the MediaCodec is init correct");
        }
    }

    public void stopDecoder(){
        if(mMediaCodec != null){
            mMediaCodec.stop();
        }
    }

    /**
     * release all resource that used in Encoder
     */
    public void release(){
        if(mMediaCodec != null){
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }
}
