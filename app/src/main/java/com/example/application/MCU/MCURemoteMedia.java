// - Set to the appropriate package location in your project
package com.example.application.MCU;

import android.content.Context;
import android.widget.FrameLayout;

import com.example.application.AecContext;

import fm.liveswitch.AudioConfig;
import fm.liveswitch.AudioDecoder;
import fm.liveswitch.AudioFormat;
import fm.liveswitch.AudioSink;
import fm.liveswitch.RtcRemoteMedia;
import fm.liveswitch.VideoDecoder;
import fm.liveswitch.VideoFormat;
import fm.liveswitch.VideoPipe;
import fm.liveswitch.VideoSink;
import fm.liveswitch.ViewSink;
import fm.liveswitch.android.OpenGLSink;

public class MCURemoteMedia extends RtcRemoteMedia<FrameLayout> {

    private final Context context;

    // Enable AEC
    public MCURemoteMedia(final Context context, boolean disableAudio, boolean disableVideo, AecContext aecContext) {
        super(disableAudio, disableVideo, aecContext);
        this.context = context;
        super.initialize();
    }

    // Remote Audio
    @Override
    protected AudioSink createAudioRecorder(AudioFormat audioFormat) {
        return new fm.liveswitch.matroska.AudioSink(getId() + "-remote-audio-" + audioFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected AudioSink createAudioSink(AudioConfig audioConfig) {
        return new fm.liveswitch.android.AudioTrackSink(audioConfig);
    }

    @Override
    protected AudioDecoder createOpusDecoder(AudioConfig audioConfig) {
        return new fm.liveswitch.opus.Decoder();
    }

    // Remote Video
    @Override
    protected VideoSink createVideoRecorder(VideoFormat videoFormat) {
        return new fm.liveswitch.matroska.VideoSink(getId() + "-remote-video-" + videoFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected ViewSink<FrameLayout> createViewSink() {
        return new OpenGLSink(context);
    }

    @Override
    protected VideoDecoder createVp8Decoder() {
        return new fm.liveswitch.vp8.Decoder();
    }

    @Override
    protected VideoDecoder createVp9Decoder() {
        return new fm.liveswitch.vp9.Decoder();
    }

    @Override
    protected VideoDecoder createH264Decoder() {
        return null;
    }

    @Override
    protected VideoPipe createImageConverter(VideoFormat videoFormat) {
        return new fm.liveswitch.yuv.ImageConverter(videoFormat);
    }
}


