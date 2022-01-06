package com.example.application.MCU;

import android.content.Context;

import com.example.application.base.BaseHelloWorldLogic;

import fm.liveswitch.AudioStream;
import fm.liveswitch.Channel;
import fm.liveswitch.ConnectionState;
import fm.liveswitch.LayoutUtility;
import fm.liveswitch.Log;
import fm.liveswitch.McuConnection;
import fm.liveswitch.VideoLayout;
import fm.liveswitch.VideoStream;

public class MCUHelloWorldLogic extends BaseHelloWorldLogic {
    private String mcuViewId;
    private McuConnection mcuConnection;
    private VideoLayout videoLayout = null;

    public MCUHelloWorldLogic(Context context) {
        super(context);
    }

    public static synchronized MCUHelloWorldLogic getInstance(Context context){
        if(app == null){
            app = new MCUHelloWorldLogic(context);
        }
        return (MCUHelloWorldLogic) app;
    }

    public McuConnection openMcuConnection() {
        // Create remote media.
        final MCURemoteMedia remoteMedia = new MCURemoteMedia(context, false, false, aecContext);
        mcuViewId = remoteMedia.getId();

        handler.post(() -> {
            // Add remote view to the layout.
            layoutManager.addRemoteView(mcuViewId, remoteMedia.getView());
        });

        // Create audio and video streams with local media and remote media.
        AudioStream audioStream = (localMedia.getAudioTrack() != null) ? new AudioStream(localMedia, remoteMedia) : null;
        VideoStream videoStream = (localMedia.getVideoTrack() != null) ? new VideoStream(localMedia, remoteMedia) : null;

        // Create a MCU connection with audio and video stream.
        McuConnection connection = channel.createMcuConnection(audioStream, videoStream);

        connection.addOnStateChange(conn -> {
            Log.info(String.format("MCU connection %s is currently in a %s state.", conn.getId(), conn.getState().toString()));

            if (conn.getState() == ConnectionState.Closing || conn.getState() == ConnectionState.Failing) {
                if (conn.getRemoteClosed()) {
                    Log.info(String.format("Media server has closed the MCU connection %s.", conn.getId()));
                }

                handler.post(() -> {
                    // Removing remote view from UI.
                    layoutManager.removeRemoteView(remoteMedia.getId());
                    remoteMedia.destroy();
                });

            } else if (conn.getState() == ConnectionState.Failed) {
                openMcuConnection();
            }
        });

    /*
        MCU connections are bidirectional, so the local media from the client end will be received by the same client. To prevent
        duplicate streams a float local preview is needed, which "floats" over the local media presented as remote media for the
        client.
     */
        layoutManager.addOnLayout(layout -> {
            if (mcuConnection != null) {
                LayoutUtility.floatLocalPreview(layout, videoLayout, mcuConnection.getId(), mcuViewId, localMedia.getViewSink());
            }
        });

        connection.open();
        return connection;
    }

    @Override
    protected void onClientRegistered(Channel[] channels) {
        super.onClientRegistered(channels);
        // Store our channel reference.
        channel = channels[0];

        // Add callback to re-layout based on Media Server Callbacks on layout.
        channel.addOnMcuVideoLayout(vidLayout -> {
            videoLayout = vidLayout;

            if (layoutManager != null) {
                handler.post(() -> layoutManager.layout());
            }
        });

        // Open a new MCU connection.
        mcuConnection = openMcuConnection();
    }
}