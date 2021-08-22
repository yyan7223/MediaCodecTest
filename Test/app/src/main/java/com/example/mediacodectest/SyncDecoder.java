package com.example.mediacodectest;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SyncDecoder {
    private final static String TAG = "SyncDecoder";
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private MediaExtractor mMediaExtractor;
    private Surface mSurface;


    public SyncDecoder(MediaExtractor mExtractor, MediaFormat mFormat, Surface surface){
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
            mMediaExtractor = mExtractor;
            mMediaFormat = mFormat;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSurface = surface;
    }

    public void startDecoder(){
        if(mMediaCodec != null && mSurface != null){
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

    public void writeSample(){
        int inputBufferId = mMediaCodec.dequeueInputBuffer(0);
        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferId);
        inputBuffer.clear();

        int size = mMediaExtractor.readSampleData(inputBuffer, 0);
        if (size <= 0) {
            stopDecoder();
            release();
        }

        int flags = mMediaExtractor.getSampleFlags();
        long presentationTimeUs = mMediaExtractor.getSampleTime();
        mMediaExtractor.advance();

        mMediaCodec.queueInputBuffer(inputBufferId, 0, size, presentationTimeUs, flags);
    }

    public void popSample(){
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        if(outputBufferId >= 0){
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId);
            MediaFormat outputFormat = mMediaCodec.getOutputFormat(outputBufferId);
            if(mMediaFormat == outputFormat && outputBuffer != null && bufferInfo.size > 0){
                byte [] buffer = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
            }
            mMediaCodec.releaseOutputBuffer(outputBufferId, true);
        }
    }


}
