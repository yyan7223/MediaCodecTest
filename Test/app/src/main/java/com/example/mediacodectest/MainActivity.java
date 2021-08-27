/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mediacodectest;


import android.animation.TimeAnimator;
import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.graphics.SurfaceTexture;

import java.io.IOException;
import java.io.File;

/**
 * This activity uses a {@link android.view.TextureView} to render the frames of a video decoded using
 * {@link android.media.MediaCodec} API.
 */
public class MainActivity extends Activity {

    private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private AsyncDecoder mAsyncDecoder;
    private SyncDecoder mSyncDecoder;
    private MediaExtractor mExtractor = new MediaExtractor();
    private MediaFormat mFormat;
    TextView mAttribView = null;

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        mPlaybackView = (TextureView) findViewById(R.id.PlaybackView);
        mAttribView =  (TextView)findViewById(R.id.AttribView);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_play) {
            mAttribView.setVisibility(View.VISIBLE);
//            startAsyncPlayback();
            startSyncPlayback();
            item.setEnabled(false);
        }
        return true;
    }


    public void startAsyncPlayback() {

        // Construct a URI that points to the video resource that we want to play
        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                + R.raw.vid_bigbuckbunny);

        try {
//            mExtractor.setDataSource(this, videoUri, null);
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
                    mAsyncDecoder = new AsyncDecoder(mExtractor, mFormat, new Surface(mPlaybackView.getSurfaceTexture()));
                    mAsyncDecoder.startDecoder();
                    break;
                }
            }
            // END_INCLUDE(initialize_extractor)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startSyncPlayback() {

        // Construct a URI that points to the video resource that we want to play
        Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                + R.raw.vid_bigbuckbunny);

        try {

            // BEGIN_INCLUDE(initialize_extractor)
//            mExtractor.setDataSource(this, videoUri, null);
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
                    mSyncDecoder = new SyncDecoder(mExtractor, mFormat, new Surface(mPlaybackView.getSurfaceTexture()));
                    mSyncDecoder.startDecoder();
                    break;
                }
            }
            // END_INCLUDE(initialize_extractor)




            // By using a {@link TimeAnimator}, we can sync our media rendering commands with
            // the system display frame rendering. The animator ticks as the {@link Choreographer}
            // receives VSYNC events.
            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(final TimeAnimator animation,
                                         final long totalTime,
                                         final long deltaTime) {

                    mSyncDecoder.writeSample();

                    mSyncDecoder.popSample();

                }
            });

            // We're all set. Kick off the animator to process buffers and render video frames as
            // they become available
            mTimeAnimator.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


