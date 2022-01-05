package com.example.application;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import fm.liveswitch.*;
import fm.liveswitch.android.LayoutManager;
import fm.liveswitch.android.LogProvider;

public class HelloWorldLogic {

    private final Context context;
    private final Handler handler;
    private static HelloWorldLogic app;

    private String applicationId = Config.applicationId;
    private String channelId = Config.channelId;
    private String gatewayUrl = Config.gatewayUrl;
    private String sharedSecret = Config.sharedSecret;

    int reRegisterBackoff = 200;
    int maxRegisterBackoff = 60000;
    boolean unregistering = false;

    private HelloWorldLogic(Context context)
    {
        this.context = context.getApplicationContext();
        this.handler = new Handler(context.getMainLooper());
    }

    public static synchronized HelloWorldLogic getInstance(Context context){
        if(app == null){
            app = new HelloWorldLogic(context);
        }
        return app;
    }

    // Client and channel
    private Client client;
    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public Client getClient() {
        return client;
    }

    // Make a registration request.
    public Future<Channel[]> joinAsync() {
        // Create a client.
        client = new Client(gatewayUrl, applicationId);

        // Create a token (do this on the server to avoid exposing your shared secret).
        String token = Token.generateClientRegisterToken(applicationId, client.getUserId(), client.getDeviceId(), client.getId(), null, new ChannelClaim[] {new ChannelClaim(channelId)}, sharedSecret);

        // Allow re-register.
        unregistering = false;

        client.addOnStateChange(client -> {
            if (client.getState() == ClientState.Unregistered) {
                Log.debug("Client has been unregistered.");

                if (!unregistering) {
                    Log.debug(String.format(Locale.US, "Registering with backoff = %d.", reRegisterBackoff));

                    // Incrementally increase register backoff to prevent runaway process.
                    ManagedThread.sleep(reRegisterBackoff);
                    if (reRegisterBackoff < maxRegisterBackoff) {
                        reRegisterBackoff += reRegisterBackoff;
                    }

                    client.register(token).then(channels -> {
                        // Reset re-register backoff after successful registration.
                        reRegisterBackoff = 200;
                        onClientRegistered(channels);
                    }, ex -> Log.error("ERROR: Client unable to register with the gateway.", ex));
                }
            }
        });

        // Register client with token.
        return client.register(token).then(this::onClientRegistered, ex -> Log.error("ERROR: Client unable to register with the gateway.", ex));
    }

    public Future<Object> leaveAsync() {
        if (this.client != null) {
            // Disable re-register.
            unregistering = true;
            return this.client.unregister().fail(ex -> {
                Log.error("ERROR: Unable to unregister client.", ex);
            });
        }
        return null;
    }

    // Register the client with token.
    private void onClientRegistered(Channel[] channels) {
        // Store our channel reference.
        channel = channels[0];

        Log.info("Client " + client.getId() + " has successfully registered to channel = " + channel.getId() + ", Hello World!");
    }
}