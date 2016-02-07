package im.actor.core.js.providers.webrtc;

public interface JsPeerConnectionListener {
    void onIceCandidate(JsRTCIceCandidate candidate);

    void onStreamAdded(JsMediaStream stream);
}