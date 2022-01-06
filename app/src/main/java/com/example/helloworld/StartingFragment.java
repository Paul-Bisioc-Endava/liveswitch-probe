package com.example.helloworld;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.application.MCU.MCUHelloWorldLogic;
import com.example.application.SFU.SFUHelloWorldLogic;
import com.example.application.base.BaseHelloWorldLogic;
import com.example.application.base.HelloWorldLogicMediator;

import fm.liveswitch.Promise;
import fm.liveswitch.Future;

public class StartingFragment extends Fragment {

    private BaseHelloWorldLogic appInstance;
    private Button joinButton;
    private Button leaveButton;

    public StartingFragment() {
    }

    public static StartingFragment newInstance() {
        StartingFragment fragment = new StartingFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MainActivity mainActivity = (MainActivity) getActivity();

        // Define the appInstance
        if(mainActivity.isSFUSelected == null)
            appInstance = BaseHelloWorldLogic.getInstance(mainActivity);
        else
            appInstance = HelloWorldLogicMediator.Companion.getInstance(mainActivity.isSFUSelected, mainActivity);

        // Inflate the layout for this fragment.
        View startingView = inflater.inflate(R.layout.fragment_starting, container, false);

        joinButton = (Button) startingView.findViewById(R.id.joinButton);
        leaveButton = (Button) startingView.findViewById(R.id.leaveButton);
        joinButton.setClickable(true);
        leaveButton.setClickable(false);

        return startingView;
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpButtons();
    }

    public RelativeLayout getVideoContainer() {
        return (RelativeLayout) getActivity().findViewById(R.id.videoContainer);
    }

    public void setStatusText(String text) {
        if (getView() != null) {
            getView().post(() -> {
                TextView statusText = (TextView) getView().findViewById(R.id.appStatusText);
                statusText.setText(text);
            });
        }
    }

    public void setButtonJoinClickable(boolean clickable) {
        if (getView() != null) {
            getView().post(() -> joinButton.setClickable(clickable));
        }
    }

    public void setButtonStopClickable(boolean clickable) {
        if (getView() != null) {
            getView().post(() -> leaveButton.setClickable(clickable));
        }
    }

    public void setUpButtons() {
        joinButton.setOnClickListener(view -> start().then(result -> {
            setButtonJoinClickable(false);
            setButtonStopClickable(true);
        }));

        leaveButton.setOnClickListener(view -> stop().then(result -> {
            setButtonJoinClickable(true);
            setButtonStopClickable(false);
        }));
    }

    public Future<Object> start() {
        Promise<Object> promise = new Promise<>();
        if(appInstance instanceof MCUHelloWorldLogic) {
            appInstance.startLocalMedia(getActivity(), getVideoContainer()).then(resultStart -> {
                appInstance.joinAsync().then(resultJoin -> {
                    String message = String.format("Client %s has successfully joined channel %s.",
                            appInstance.getClient().getId(),
                            appInstance.getChannel().getId());
                    setStatusText(message);
                    promise.resolve(null);
                }).fail(ex -> {
                    setStatusText("Unable to join channel.");
                    promise.reject(ex);
                });

            }).fail(ex -> {
                setStatusText("Unable to start local media.");
                promise.reject(null);
            });
        } else if(appInstance instanceof SFUHelloWorldLogic) {
            appInstance.startLocalMedia(getActivity(), getVideoContainer()).then(resultStart -> {
                appInstance.joinAsync().then(resultJoin -> {
                    String message = String.format("Client %s has successfully joined channel %s.",
                            appInstance.getClient().getId(),
                            appInstance.getChannel().getId());
                    setStatusText(message);
                    promise.resolve(null);
                }).fail(ex -> {
                    setStatusText("Unable to join channel.");
                    promise.reject(ex);
                });

            }).fail(ex -> {
                setStatusText("Unable to start local media.");
                promise.reject(null);
            });
        }
        return promise;
    }

    public Future<Object> stop() {
        Promise<Object> promise = new Promise<>();

        if (appInstance.getClient() != null) {
            // Try to unregister the client
            appInstance.leaveAsync().then(resultLeave -> {
            // Client unregistered
                // <LocalMedia>
//                appInstance.stopLocalMedia().then(resultStop -> {
//                    setStatusText("Application successfully stopped local media.");
//                    promise.resolve(null);
//                }).fail(ex -> {
//                    setStatusText("Unable to stop local media.");
//                    promise.reject(ex);
//                });
                // </LocalMedia>
           // Client unregistering failed
             }).fail(ex -> {
                 setStatusText(String.format("Unable to leave channel %s.", appInstance.getChannel().getId()));
                 promise.reject(ex);
             });
        } else {
            promise.resolve(null);
        }

        return promise;
    }
}