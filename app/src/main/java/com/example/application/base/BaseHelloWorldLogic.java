package com.example.application.base;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;

import com.example.application.AecContext;
import com.example.application.CameraLocalMedia;
import com.example.application.Config;

import java.util.Locale;

import fm.liveswitch.Channel;
import fm.liveswitch.ChannelClaim;
import fm.liveswitch.Client;
import fm.liveswitch.ClientState;
import fm.liveswitch.Future;
import fm.liveswitch.Log;
import fm.liveswitch.ManagedThread;
import fm.liveswitch.Promise;
import fm.liveswitch.Token;
import fm.liveswitch.android.LayoutManager;

/**
 * Created by paulbisioc on 05.01.2022
 */
public class BaseHelloWorldLogic {

    private final Context context;
    private final Handler handler;
    private static BaseHelloWorldLogic app;

    private String applicationId = Config.applicationId;
    private String channelId = Config.channelId;
    private String gatewayUrl = Config.gatewayUrl;
    private String sharedSecret = Config.sharedSecret;

    // Register / Unregister
    int reRegisterBackoff = 200;
    int maxRegisterBackoff = 60000;
    boolean unregistering = false;

    // Start / Stop Local Media
    private com.example.application.LocalMedia<View> localMedia;
    private LayoutManager layoutManager;
    private final com.example.application.AecContext aecContext = new AecContext();

    public BaseHelloWorldLogic(Context context)
    {
        this.context = context.getApplicationContext();
        this.handler = new Handler(context.getMainLooper());
    }

    public static synchronized BaseHelloWorldLogic getInstance(Context context){
        if(app == null){
            app = new BaseHelloWorldLogic(context);
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

    public Future<Object> startLocalMedia(final Activity activity, final RelativeLayout container) {
        final Promise<Object> promise = new Promise<>();

        activity.runOnUiThread(() -> {
            // Create a new local media with audio and video enabled.
            localMedia = new CameraLocalMedia(context, false, false, aecContext);

            // Set local media in the layout.
            layoutManager = new LayoutManager(container);
            layoutManager.setLocalView(localMedia.getView());

            // Start capturing local media.
            localMedia.start().then(localMedia -> {
                promise.resolve(null);

            }, promise::reject);
        });

        return promise;
    }

    public Future<Object> stopLocalMedia() {
        final Promise<Object> promise = new Promise<>();

        if (localMedia == null) {
            promise.resolve(null);
        } else {
            // Stop capturing local media.
            localMedia.stop().then(result -> {
                if (layoutManager != null) {
                    // Remove views from the layout.
                    layoutManager.removeRemoteViews();
                    layoutManager.unsetLocalView();
                    layoutManager = null;
                }

                if (localMedia != null) {
                    localMedia.destroy();
                    localMedia = null;
                }

                promise.resolve(null);

            }, promise::reject);
        }

        return promise;
    }
}
