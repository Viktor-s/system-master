/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package apprtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.Button;

import me.up.ctd.ImageViewRounded;
import me.up.ctd.R;
import android.widget.ImageView;
import android.graphics.drawable.AnimationDrawable;
import android.widget.FrameLayout;
import android.graphics.BitmapFactory;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.net.MalformedURLException;


import me.up.ctd.WebAppInterface;

/**
 * Main Activity of the AppRTCDemo Android app demonstrating interoperability
 * between the Android/Java implementation of PeerConnection and the
 * apprtc.appspot.com demo webapp.
 */
public class AppRTCDemoActivity extends Activity
        implements AppRTCClient.IceServersObserver {
    private static final String TAG = "AppRTCDemoActivity";
    private static boolean factoryStaticInitialized;
    private PeerConnectionFactory factory;
    private VideoSource videoSource;
    private boolean videoSourceStopped;
    private PeerConnection pc;
    private final PCObserver pcObserver = new PCObserver();
    private final SDPObserver sdpObserver = new SDPObserver();
    private final GAEChannelClient.MessageHandler gaeHandler = new GAEHandler();
    private AppRTCClient appRtcClient = new AppRTCClient(this, gaeHandler, this);
    private AppRTCGLView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private Button hangup;
    private Toast logToast;
    private final LayoutParams hudLayout =
            new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    private TextView hudView;
    private LinkedList<IceCandidate> queuedRemoteCandidates =
            new LinkedList<IceCandidate>();
    // Synchronize on quit[0] to avoid teardown-related crashes.
    private final Boolean[] quit = new Boolean[] { false };
    private MediaConstraints sdpMediaConstraints;
    public static String room_id;
    public static AppRTCDemoActivity kill_handler;
    public boolean active;
    public int collect_packages;
    public ImageView animation;
    public AnimationDrawable loop_animation;
    public static String calling_avatar;
    public ImageViewRounded avatar;
    public ImageViewRounded my_avatar_img;
    public static String my_avatar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                new UnhandledExceptionHandler(this));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        kill_handler = this;
        active = false;
        collect_packages = 0;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        vsv = new AppRTCGLView(this, displaySize);
        vsv.setBackgroundColor(getResources().getColor(R.color.dark_bg));
        VideoRendererGui.setView(vsv);
        remoteRender = VideoRendererGui.create(0, 0, 100, 100);
        localRender = VideoRendererGui.create(70, 5, 25, 25);
        setContentView(vsv);
        //create_animation();
        create_avatar();
        //create_my_avatar();
        create_hangup();

        if (!factoryStaticInitialized) {
            abortUnless(PeerConnectionFactory.initializeAndroidGlobals(
                            this, true, true),
                    "Failed to initializeAndroidGlobals");
            factoryStaticInitialized = true;
        }

        AudioManager audioManager =
                ((AudioManager) getSystemService(AUDIO_SERVICE));

        boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
        audioManager.setMode(isWiredHeadsetOn ?
                AudioManager.MODE_IN_CALL : AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(!isWiredHeadsetOn);
        audioManager.setStreamVolume (
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));

        final Intent intent = getIntent();
        if ("android.intent.action.VIEW".equals(intent.getAction())) {
            connectToRoom(intent.getData().toString());
            return;
        }
        connectToRoom("https://apprtc.appspot.com/?r=upme" + room_id);
    }

    private void connectToRoom(String roomUrl) {
        //logAndToast("Connecting to room...");
        appRtcClient.connectToRoom(roomUrl);
    }

    // Toggle visibility of the heads-up display.
    private void toggleHUD() {

    }

    public void show_screen() {
        active = true;
        vsv.setBackgroundColor(Color.TRANSPARENT);
        //loop_animation.stop();
        //animation.setVisibility(View.INVISIBLE);
        avatar.setVisibility(View.INVISIBLE);
        //my_avatar_img.setVisibility(View.INVISIBLE);
        kill_ring();
    }

    public void create_hangup() {
        hangup = new Button(this);
        addContentView(hangup, hudLayout);
        FrameLayout.LayoutParams align_hangup = new FrameLayout.LayoutParams(114,
               114, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        hangup.setBackgroundResource(R.drawable.hangup_button);
        align_hangup.setMargins(300, 0, 0, 0);
        hangup.setLayoutParams(align_hangup);
    }

    public void create_avatar() {
        avatar = new ImageViewRounded(this);
        addContentView(avatar, hudLayout);
        new Thread(new Runnable() {
            public void run() {
                loadImageFromURL(calling_avatar, avatar);
            }
        }).start();
        FrameLayout.LayoutParams align_avatar = new FrameLayout.LayoutParams(317,
                317, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        avatar.setLayoutParams(align_avatar);
    }

    public void create_my_avatar() {
        my_avatar_img = new ImageViewRounded(this);
        addContentView(my_avatar_img, hudLayout);
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadImageFromURL(my_avatar, my_avatar_img);
            }
        }).start();
        FrameLayout.LayoutParams align_my_avatar = new FrameLayout.LayoutParams(255,
                255, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        align_my_avatar.setMargins(0, 0, 0, 80);
        my_avatar_img.setLayoutParams(align_my_avatar);
    }

    public void create_animation() {
        animation = new ImageView(this);
        addContentView(animation, hudLayout);
        animation.setBackgroundResource(R.drawable.call_animation);
        FrameLayout.LayoutParams align = new FrameLayout.LayoutParams(1000,
                563, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        align.setMargins(0, 0, 0, -115);
        animation.setLayoutParams(align);
        loop_animation = (AnimationDrawable) animation.getBackground();
        loop_animation.start();
    }

    public boolean loadImageFromURL(String fileUrl, ImageView iv){
        try {
            URL myFileUrl = new URL (fileUrl);
            HttpURLConnection conn =
                    (HttpURLConnection) myFileUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();

            InputStream is = conn.getInputStream();
            iv.setImageBitmap(BitmapFactory.decodeStream(is));

            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        disconnectAndExit();

    /*vsv.onPause();
    if (videoSource != null) {
      videoSource.stop();
      videoSourceStopped = true;
    }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        vsv.onResume();
        if (videoSource != null && videoSourceStopped) {
            videoSource.restart();
        }
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        vsv.updateDisplaySize(displaySize);
        super.onConfigurationChanged(newConfig);
    }

    // Just for fun (and to regression-test bug 2302) make sure that DataChannels
    // can be created, queried, and disposed.
    private static void createDataChannelToRegressionTestBug2302(
            PeerConnection pc) {
        DataChannel dc = pc.createDataChannel("dcLabel", new DataChannel.Init());
        abortUnless("dcLabel".equals(dc.label()), "Unexpected label corruption?");
        dc.close();
        dc.dispose();
    }

    @Override
    public void onIceServers(List<PeerConnection.IceServer> iceServers) {
        factory = new PeerConnectionFactory();
        try {
            MediaConstraints pcConstraints = appRtcClient.pcConstraints();
            pcConstraints.optional.add(
                    new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
            pc = factory.createPeerConnection(iceServers, pcConstraints, pcObserver);

            createDataChannelToRegressionTestBug2302(pc);  // See method comment.
        } catch (NullPointerException e) {

        }


        // Uncomment to get ALL WebRTC tracing and SENSITIVE libjingle logging.
        // NOTE: this _must_ happen while |factory| is alive!
        // Logging.enableTracing(
        //     "logcat:",
        //     EnumSet.of(Logging.TraceLevel.TRACE_ALL),
        //     Logging.Severity.LS_SENSITIVE);

        {
            final PeerConnection finalPC = pc;
            final Runnable repeatedStatsLogger = new Runnable() {
                public void run() {
                    synchronized (quit[0]) {
                        if (quit[0]) {
                            return;
                        }
                        final Runnable runnableThis = this;
                        boolean success = finalPC.getStats(new StatsObserver() {
                            public void onComplete(final StatsReport[] reports) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        for (StatsReport report : reports) {
                                            if (report.type.toString().equals("ssrc") && !kill_handler.active) {
                                                if (kill_handler.collect_packages == 12) {
                                                    show_screen();
                                                } else {
                                                    kill_handler.collect_packages++;
                                                }
                                            }
                                        }
                                    }
                                });
                                for (StatsReport report : reports) {
                                    Log.d(TAG, "Stats: " + report.toString());
                                }
                                vsv.postDelayed(runnableThis, 1000);
                            }
                        }, null);
                        if (!success) {
                            throw new RuntimeException("getStats() return false!");
                        }
                    }
                }
            };
            vsv.postDelayed(repeatedStatsLogger, 1000);
        }

        {
            //logAndToast("Creating local video source...");
            MediaStream lMS = factory.createLocalMediaStream("ARDAMS");
            if (appRtcClient != null) {
                if (appRtcClient.videoConstraints() != null) {
                    VideoCapturer capturer = getVideoCapturer();
                    videoSource = factory.createVideoSource(
                            capturer, appRtcClient.videoConstraints());
                    VideoTrack videoTrack =
                            factory.createVideoTrack("ARDAMSv0", videoSource);
                    videoTrack.addRenderer(new VideoRenderer(localRender));
                    lMS.addTrack(videoTrack);
                }
                if (appRtcClient.audioConstraints() != null) {
                    lMS.addTrack(factory.createAudioTrack(
                            "ARDAMSa0",
                            factory.createAudioSource(appRtcClient.audioConstraints())));
                }
            } else {
                disconnectAndExit();
            }
            pc.addStream(lMS, new MediaConstraints());
        }
        //logAndToast("Waiting for ICE candidates...");
        hangup.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onPause();
            }
        });
    }

    // Cycle through likely device names for the camera and return the first
    // capturer that works, or crash if none do.
    private VideoCapturer getVideoCapturer() {
        String[] cameraFacing = { "front", "back" };
        int[] cameraIndex = { 0, 1 };
        int[] cameraOrientation = { 0, 90, 180, 270 };
        for (String facing : cameraFacing) {
            for (int index : cameraIndex) {
                for (int orientation : cameraOrientation) {
                    String name = "Camera " + index + ", Facing " + facing +
                            ", Orientation " + orientation;
                    VideoCapturer capturer = VideoCapturer.create(name);
                    if (capturer != null) {
                        //logAndToast("Using camera: " + name);
                        return capturer;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to open capturer");
    }

    @Override
    protected void onDestroy() {
        disconnectAndExit();
        super.onDestroy();
    }

    // Poor-man's assert(): die with |msg| unless |condition| is true.
    private static void abortUnless(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException(msg);
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    // Send |json| to the underlying AppEngine Channel.
    private void sendMessage(JSONObject json) {
        appRtcClient.sendMessage(json.toString());
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Mangle SDP to prefer ISAC/16000 over any other audio codec.
    private static String preferISAC(String sdpDescription) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String isac16kRtpMap = null;
        Pattern isac16kPattern =
                Pattern.compile("^a=rtpmap:(\\d+) ISAC/16000[\r]?$");
        for (int i = 0;
             (i < lines.length) && (mLineIndex == -1 || isac16kRtpMap == null);
             ++i) {
            if (lines[i].startsWith("m=audio ")) {
                mLineIndex = i;
                continue;
            }
            Matcher isac16kMatcher = isac16kPattern.matcher(lines[i]);
            if (isac16kMatcher.matches()) {
                isac16kRtpMap = isac16kMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
            Log.d(TAG, "No m=audio line, so can't prefer iSAC");
            return sdpDescription;
        }
        if (isac16kRtpMap == null) {
            Log.d(TAG, "No ISAC/16000 line, so can't prefer iSAC");
            return sdpDescription;
        }
        String[] origMLineParts = lines[mLineIndex].split(" ");
        StringBuilder newMLine = new StringBuilder();
        int origPartIndex = 0;
        // Format is: m=<media> <port> <proto> <fmt> ...
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(origMLineParts[origPartIndex++]).append(" ");
        newMLine.append(isac16kRtpMap);
        for (; origPartIndex < origMLineParts.length; ++origPartIndex) {
            if (!origMLineParts[origPartIndex].equals(isac16kRtpMap)) {
                newMLine.append(" ").append(origMLineParts[origPartIndex]);
            }
        }
        lines[mLineIndex] = newMLine.toString();
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

    // Implementation detail: observe ICE & stream changes and react accordingly.
    private class PCObserver implements PeerConnection.Observer {
        @Override public void onIceCandidate(final IceCandidate candidate){
            runOnUiThread(new Runnable() {
                public void run() {
                    JSONObject json = new JSONObject();
                    jsonPut(json, "type", "candidate");
                    jsonPut(json, "label", candidate.sdpMLineIndex);
                    jsonPut(json, "id", candidate.sdpMid);
                    jsonPut(json, "candidate", candidate.sdp);
                    sendMessage(json);
                }
            });
        }

        @Override public void onError(){
            runOnUiThread(new Runnable() {
                public void run() {
                    throw new RuntimeException("PeerConnection error!");
                }
            });
        }

        @Override public void onSignalingChange(
                PeerConnection.SignalingState newState) {
        }

        @Override public void onIceConnectionChange(
                PeerConnection.IceConnectionState newState) {
        }

        @Override public void onIceGatheringChange(
                PeerConnection.IceGatheringState newState) {
        }

        @Override public void onAddStream(final MediaStream stream){
            runOnUiThread(new Runnable() {
                public void run() {
                    abortUnless(stream.audioTracks.size() <= 1 &&
                                    stream.videoTracks.size() <= 1,
                            "Weird-looking stream: " + stream);
                    if (stream.videoTracks.size() == 1) {
                        stream.videoTracks.get(0).addRenderer(
                                new VideoRenderer(remoteRender));
                    }
                }
            });
        }

        @Override public void onRemoveStream(final MediaStream stream){
            runOnUiThread(new Runnable() {
                public void run() {
                    stream.videoTracks.get(0).dispose();
                }
            });
        }

        @Override public void onDataChannel(final DataChannel dc) {
            runOnUiThread(new Runnable() {
                public void run() {
                    throw new RuntimeException(
                            "AppRTC doesn't use data channels, but got: " + dc.label() +
                                    " anyway!");
                }
            });
        }

        @Override public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    private class SDPObserver implements SdpObserver {
        private SessionDescription localSdp;

        @Override public void onCreateSuccess(final SessionDescription origSdp) {
            abortUnless(localSdp == null, "multiple SDP create?!?");
            final SessionDescription sdp = new SessionDescription(
                    origSdp.type, preferISAC(origSdp.description));
            localSdp = sdp;
            runOnUiThread(new Runnable() {
                public void run() {
                    pc.setLocalDescription(sdpObserver, sdp);
                }
            });
        }

        // Helper for sending local SDP (offer or answer, depending on role) to the
        // other participant.  Note that it is important to send the output of
        // create{Offer,Answer} and not merely the current value of
        // getLocalDescription() because the latter may include ICE candidates that
        // we might want to filter elsewhere.
        private void sendLocalDescription() {
            //logAndToast("Sending " + localSdp.type);
            JSONObject json = new JSONObject();
            jsonPut(json, "type", localSdp.type.canonicalForm());
            jsonPut(json, "sdp", localSdp.description);
            sendMessage(json);
        }

        @Override public void onSetSuccess() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (appRtcClient.isInitiator()) {
                        if (pc.getRemoteDescription() != null) {
                            // We've set our local offer and received & set the remote
                            // answer, so drain candidates.
                            drainRemoteCandidates();
                        } else {
                            // We've just set our local description so time to send it.
                            sendLocalDescription();
                        }
                    } else {
                        if (pc.getLocalDescription() == null) {
                            // We just set the remote offer, time to create our answer.
                            //logAndToast("Creating answer");
                            pc.createAnswer(SDPObserver.this, sdpMediaConstraints);
                        } else {
                            // Answer now set as local description; send it and drain
                            // candidates.
                            sendLocalDescription();
                            drainRemoteCandidates();
                        }
                    }
                }
            });
        }

        @Override public void onCreateFailure(final String error) {
            runOnUiThread(new Runnable() {
                public void run() {
                    throw new RuntimeException("createSDP error: " + error);
                }
            });
        }

        @Override public void onSetFailure(final String error) {
            runOnUiThread(new Runnable() {
                public void run() {
                    throw new RuntimeException("setSDP error: " + error);
                }
            });
        }

        private void drainRemoteCandidates() {
            for (IceCandidate candidate : queuedRemoteCandidates) {
                pc.addIceCandidate(candidate);
            }
            queuedRemoteCandidates = null;
        }
    }

    // Implementation detail: handler for receiving GAE messages and dispatching
    // them appropriately.
    private class GAEHandler implements GAEChannelClient.MessageHandler {
        @JavascriptInterface public void onOpen() {
            if (!appRtcClient.isInitiator()) {
                return;
            }
            //logAndToast("Creating offer...");
            pc.createOffer(sdpObserver, sdpMediaConstraints);
        }

        @JavascriptInterface public void onMessage(String data) {
            try {
                JSONObject json = new JSONObject(data);
                String type = (String) json.get("type");
                if (type.equals("candidate")) {
                    IceCandidate candidate = new IceCandidate(
                            (String) json.get("id"),
                            json.getInt("label"),
                            (String) json.get("candidate"));
                    if (queuedRemoteCandidates != null) {
                        queuedRemoteCandidates.add(candidate);
                    } else {
                        pc.addIceCandidate(candidate);
                    }
                } else if (type.equals("answer") || type.equals("offer")) {
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(type),
                            preferISAC((String) json.get("sdp")));
                    pc.setRemoteDescription(sdpObserver, sdp);
                } else if (type.equals("bye")) {
                    //logAndToast("Remote end hung up; dropping PeerConnection");
                    disconnectAndExit();
                } else {
                    throw new RuntimeException("Unexpected message: " + data);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @JavascriptInterface public void onClose() {
            disconnectAndExit();
        }

        @JavascriptInterface public void onError(int code, String description) {
            disconnectAndExit();
        }
    }

    public void kill_ring() {
        WebAppInterface.wv_handler.call_dropped();
        try {
            WebAppInterface.ring_handler.stop();
            WebAppInterface.ring_handler.release();
        } catch (IllegalStateException e) {
            Log.d(TAG, e.toString());
        }
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnectAndExit() {
        synchronized (quit[0]) {
            if (quit[0]) {
                return;
            }
            quit[0] = true;
            if (pc != null) {
                pc.dispose();
                pc = null;
            }
            if (appRtcClient != null) {
                appRtcClient.sendMessage("{\"type\": \"bye\"}");
                appRtcClient.disconnect();
                appRtcClient = null;
            }
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            if (factory != null) {
                factory.dispose();
                factory = null;
            }
            kill_ring();
            finish();
        }
    }

}
