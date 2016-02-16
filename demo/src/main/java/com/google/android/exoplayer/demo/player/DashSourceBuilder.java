/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.android.exoplayer.demo.player;

import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MultiSampleSource;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.FormatEvaluator.AdaptiveEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.demo.player.DemoPlayer.SourceBuilder;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;

import android.content.Context;
import android.os.Handler;

/**
 * A {@link SourceBuilder} for DASH.
 */
// TODO[REFACTOR]: Bring back DRM support.
// TODO[REFACTOR]: Bring back UTC timing element support.
public class DashSourceBuilder implements SourceBuilder {

  private static final String TAG = "DashSourceBuilder";

  private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  private static final int VIDEO_BUFFER_SEGMENTS = 200;
  private static final int AUDIO_BUFFER_SEGMENTS = 54;
  private static final int TEXT_BUFFER_SEGMENTS = 2;
  private static final int LIVE_EDGE_LATENCY_MS = 30000;

  private final Context context;
  private final String userAgent;
  private final String url;
  private final MediaDrmCallback drmCallback;

  public DashSourceBuilder(Context context, String userAgent, String url,
      MediaDrmCallback drmCallback) {
    this.context = context;
    this.userAgent = userAgent;
    this.url = url;
    this.drmCallback = drmCallback;
  }

  @Override
  public SampleSource buildRenderers(DemoPlayer player) {
    MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
    DefaultUriDataSource manifestDataSource = new DefaultUriDataSource(context, userAgent);
    ManifestFetcher<MediaPresentationDescription> manifestFetcher = new ManifestFetcher<>(url,
        manifestDataSource, parser);

    Handler mainHandler = player.getMainHandler();
    BandwidthMeter bandwidthMeter = player.getBandwidthMeter();
    LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));

    // Build the video renderer.
    DataSource videoDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
    ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher, AdaptationSet.TYPE_VIDEO,
        videoDataSource, new AdaptiveEvaluator(bandwidthMeter), LIVE_EDGE_LATENCY_MS,
        0, mainHandler, player, DemoPlayer.TYPE_VIDEO);
    ChunkSampleSource videoSampleSource = new ChunkSampleSource(videoChunkSource, loadControl,
        VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player, DemoPlayer.TYPE_VIDEO);

    // Build the audio renderer.
    DataSource audioDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
    ChunkSource audioChunkSource = new DashChunkSource(manifestFetcher, AdaptationSet.TYPE_AUDIO,
        audioDataSource, null, LIVE_EDGE_LATENCY_MS, 0, mainHandler, player, DemoPlayer.TYPE_AUDIO);
    ChunkSampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource, loadControl,
        AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
        DemoPlayer.TYPE_AUDIO);

    // Build the text renderer.
    DataSource textDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
    ChunkSource textChunkSource = new DashChunkSource(manifestFetcher, AdaptationSet.TYPE_TEXT,
        textDataSource, null, LIVE_EDGE_LATENCY_MS, 0, mainHandler, player, DemoPlayer.TYPE_TEXT);
    ChunkSampleSource textSampleSource = new ChunkSampleSource(textChunkSource, loadControl,
        TEXT_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE, mainHandler, player,
        DemoPlayer.TYPE_TEXT);

    return new MultiSampleSource(videoSampleSource, audioSampleSource, textSampleSource);
  }

}