package com.example.application.SFU;

import android.content.Context;

import com.example.application.base.BaseHelloWorldLogic;

import java.util.HashMap;

import fm.liveswitch.AudioStream;
import fm.liveswitch.Channel;
import fm.liveswitch.ConnectionInfo;
import fm.liveswitch.ConnectionState;
import fm.liveswitch.LocalMedia;
import fm.liveswitch.Log;
import fm.liveswitch.ManagedConnection;
import fm.liveswitch.RemoteMedia;
import fm.liveswitch.SfuDownstreamConnection;
import fm.liveswitch.SfuUpstreamConnection;
import fm.liveswitch.VideoStream;

public class SFUHelloWorldLogic extends BaseHelloWorldLogic {
    private SfuUpstreamConnection upstreamConnection;
    private final HashMap<String, SfuDownstreamConnection> downstreamConnections = new HashMap<>();


    public SFUHelloWorldLogic(Context context) {
        super(context);
    }

    public static SfuUpstreamConnection openSfuUpstreamConnection(LocalMedia localMedia) {
        // Create audio and video streams from local media.
        AudioStream audioStream = (localMedia.getAudioTrack() != null) ? new AudioStream(localMedia) : null;
        VideoStream videoStream = (localMedia.getVideoTrack() != null) ? new VideoStream(localMedia) : null;

        // Create a SFU upstream connection with local audio and video.
        SfuUpstreamConnection connection = channel.createSfuUpstreamConnection(audioStream, videoStream);

        connection.addOnStateChange((ManagedConnection conn) -> {
            Log.info(String.format("Upstream connection %s is in a %s state.", conn.getId(), conn.getState().toString()));

            if (conn.getState() == ConnectionState.Closing || conn.getState() == ConnectionState.Failing) {
                if (conn.getRemoteClosed()) {
                    Log.info(String.format("Media server has closed the upstream connection %s.", conn.getId()));
                }
            } else if (connection.getState() == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuUpstreamConnection(localMedia);
            }
        });

        connection.open();
        return connection;
    }

    public SfuDownstreamConnection openSfuDownstreamConnection(final ConnectionInfo remoteConnectionInfo) {
        // Create remote media.
        final SFURemoteMedia remoteMedia = new SFURemoteMedia(context, false, false, aecContext);

        // Adding remote view to UI.
        handler.post(() -> layoutManager.addRemoteView(remoteMedia.getId(), remoteMedia.getView()));

        // Create audio and video streams from remote media.
        AudioStream audioStream = (remoteConnectionInfo.getHasAudio()) ? new AudioStream(remoteMedia) : null;
        VideoStream videoStream = (remoteConnectionInfo.getHasVideo()) ? new VideoStream(remoteMedia) : null;

        // Create a SFU downstream connection with remote audio and video and data streams.
        SfuDownstreamConnection connection = channel.createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream);

        // Store the downstream connection.
        downstreamConnections.put(remoteMedia.getId(), connection);

        connection.addOnStateChange((ManagedConnection conn) -> {
            Log.info(String.format("Downstream connection %s is currently in a %s state.", conn.getId(), conn.getState().toString()));

            if (conn.getState() == ConnectionState.Closing || conn.getState() == ConnectionState.Failing) {
                if (conn.getRemoteClosed()) {
                    Log.info(String.format("Media server has closed the downstream connection %s.", conn.getId()));
                }

                // Removing remote view from UI.
                handler.post(() -> {
                    layoutManager.removeRemoteView(remoteMedia.getId());
                    remoteMedia.destroy();
                });

                downstreamConnections.remove(remoteMedia.getId());

            } else if (conn.getState() == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuDownstreamConnection(remoteConnectionInfo);
            }
        });

        connection.open();
        return connection;
    }

    // Register the client with token.
    @Override
    protected void onClientRegistered(Channel[] channels) {
        super.onClientRegistered(channels);
        // Store our channel reference.
        channel = channels[0];

        // Open a new SFU downstream connection when a new remote upstream connection is opened.
        channel.addOnRemoteUpstreamConnectionOpen(connectionInfo -> {
            Log.info("A remote upstream connection has opened.");
            openSfuDownstreamConnection(connectionInfo);
        });

        // Open a new SFU upstream connection.
        upstreamConnection = openSfuUpstreamConnection(localMedia);

        // Check for existing remote upstream connections and open a downstream connection for
        // each of them.
        for (ConnectionInfo connectionInfo : channel.getRemoteUpstreamConnectionInfos()) {
            openSfuDownstreamConnection(connectionInfo);
        }
    }
}