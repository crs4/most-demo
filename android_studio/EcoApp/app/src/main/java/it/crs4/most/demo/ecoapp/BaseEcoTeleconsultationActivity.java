package it.crs4.most.demo.ecoapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import it.crs4.most.demo.ecoapp.models.EcoUser;
import it.crs4.most.demo.ecoapp.models.Teleconsultation;
import it.crs4.most.voip.Utils;
import it.crs4.most.voip.VoipEventBundle;
import it.crs4.most.voip.VoipLib;
import it.crs4.most.voip.VoipLibBackend;
import it.crs4.most.voip.enums.CallState;
import it.crs4.most.voip.enums.VoipEvent;
import it.crs4.most.voip.enums.VoipEventType;


@SuppressLint("InlinedApi")
public abstract class BaseEcoTeleconsultationActivity extends AppCompatActivity {

    private static final String TAG = "EcoTeleconsultActivity";

    protected TeleconsultationState mTcState = TeleconsultationState.IDLE;
    private String sipServerIp;
    private String sipServerPort;

    private VoipLib mVoipLib;
    private CallHandler voipHandler;
    protected HashMap<String, String> voipParams;

    protected boolean localHold = false;
    protected boolean remoteHold = false;
    protected boolean accountRegistered = false;
    protected boolean exitFromAppRequest = false;

    protected Handler handler;
    protected Teleconsultation teleconsultation;
    protected RemoteConfigReader rcr;

    protected void setTeleconsultationState(TeleconsultationState tcState) {
        mTcState = tcState;
        notifyTeleconsultationStateChanged();
    }

    protected abstract void notifyTeleconsultationStateChanged();

    protected void exitFromApp() {
        Log.d(TAG, "Called exitFromApp()");
        exitFromAppRequest = true;

        if (mVoipLib != null) {
            mVoipLib.destroyLib();
        }
        else {
            Log.d(TAG, "Voip Library deinitialized. Exiting the app");
            finish();
        }
    }

    protected void closeSession() {
        EcoUser ecoUser = teleconsultation.getApplicant();
        rcr.closeSession(teleconsultation.getLastSession().getId(),
                ecoUser.getAccessToken(),
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject sessionData) {
                        Log.d(TAG, "Session closed: " + sessionData);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError err) {
                        Log.e(TAG, "Error closing the session: " + err);

                    }
                });
    }
    // VOIP METHODS AND LOGIC
    protected static class CallHandler extends Handler {

        private final WeakReference<BaseEcoTeleconsultationActivity> mOuterRef;
        public boolean mReinitRequest = false;

        private CallHandler(BaseEcoTeleconsultationActivity outerRef) {
            mOuterRef = new WeakReference<>(outerRef);
        }

        protected VoipEventBundle getEventBundle(Message voipMessage) {
            //int msg_type = voipMessage.what;
            VoipEventBundle myState = (VoipEventBundle) voipMessage.obj;
            String infoMsg = "Event:" + myState.getEvent() + ": Type:" + myState.getEventType() + " : " + myState.getInfo();
            Log.d(TAG, "Called handleMessage with event info:" + infoMsg);
            return myState;
        }

        @Override
        public void handleMessage(Message voipMessage) {
            BaseEcoTeleconsultationActivity mainActivity = mOuterRef.get();
            VoipEventBundle myEventBundle = getEventBundle(voipMessage);
            Log.d(TAG, "HANDLE EVENT TYPE:" + myEventBundle.getEventType() + " EVENT:" + myEventBundle.getEvent());

            VoipEvent event = myEventBundle.getEvent();
            // Register the account after the Lib Initialization
            if (event == VoipEvent.LIB_INITIALIZED) {
                mainActivity.registerAccount();
            }
            else if (event == VoipEvent.ACCOUNT_REGISTERED) {
                if (!mainActivity.accountRegistered) {
                    mainActivity.subscribeBuddies();
                }
                else {
                    mainActivity.accountRegistered = true;
                }
            }
            else if (event == VoipEvent.ACCOUNT_UNREGISTERED) {
                mainActivity.setTeleconsultationState(TeleconsultationState.IDLE);
            }
            else if (myEventBundle.getEventType() == VoipEventType.BUDDY_EVENT) {
                Log.d(TAG, "In handle Message for BUDDY EVENT");
                // There is only one subscribed buddy in this mMainActivity, so we don't need to get IBuddy informations
                if (event == VoipEvent.BUDDY_CONNECTED) {
                    // the remote buddy is no longer on Hold State
                    mainActivity.remoteHold = false;
                    if (mainActivity.mTcState == TeleconsultationState.REMOTE_HOLDING ||
                            mainActivity.mTcState == TeleconsultationState.HOLDING) {
                        if (mainActivity.localHold) {
                            mainActivity.setTeleconsultationState(TeleconsultationState.HOLDING);
                        }
                        else {
                            mainActivity.setTeleconsultationState(TeleconsultationState.CALLING);
                        }
                    }
                    else if (mainActivity.mTcState == TeleconsultationState.IDLE) {
                        mainActivity.setTeleconsultationState(TeleconsultationState.READY);
                    }
                }
                else if (event == VoipEvent.BUDDY_HOLDING) {
                    CallState callState = mainActivity.getCallState();
                    if (callState == CallState.ACTIVE || callState == CallState.HOLDING) {
                        mainActivity.setTeleconsultationState(TeleconsultationState.REMOTE_HOLDING);
                    }
                }
                else if (event == VoipEvent.BUDDY_DISCONNECTED) {
                    mainActivity.setTeleconsultationState(TeleconsultationState.IDLE);
                }
            }
            else if (event == VoipEvent.CALL_INCOMING) {
                mainActivity.answerCall(); //handleIncomingCallRequest();
            }
            /*
            else if (event==VoipEvent.CALL_READY)
            {
                    if (incoming_call_request)

                    {
                            answerCall();
                    }
            }
            */
            else if (event == VoipEvent.CALL_ACTIVE) {
                if (mainActivity.remoteHold) {
                    mainActivity.setTeleconsultationState(TeleconsultationState.REMOTE_HOLDING);
                }
                else {
                    mainActivity.setTeleconsultationState(TeleconsultationState.CALLING);
                }
            }
            else if (event == VoipEvent.CALL_HOLDING) {
                mainActivity.setTeleconsultationState(TeleconsultationState.HOLDING);
            }
            else if (event == VoipEvent.CALL_HANGUP || event == VoipEvent.CALL_REMOTE_HANGUP) {
                if (mainActivity.mTcState != TeleconsultationState.IDLE) {
                    mainActivity.setTeleconsultationState(TeleconsultationState.READY);
                }
            }
            // Deinitialize the Voip Lib and release all allocated resources
            else if (event == VoipEvent.LIB_DEINITIALIZED || event == VoipEvent.LIB_DEINITIALIZATION_FAILED) {
                Log.d(TAG, "Setting to null MyVoipLib");
                mainActivity.mVoipLib = null;
                mainActivity.setTeleconsultationState(TeleconsultationState.IDLE);

                if (mReinitRequest) {
                    mReinitRequest = false;
                    mainActivity.setupVoipLib();
                }
                else if (mainActivity.exitFromAppRequest) {
                    mainActivity.exitFromApp();
                }
            }
            else if (event == VoipEvent.LIB_INITIALIZATION_FAILED ||
                    event == VoipEvent.ACCOUNT_REGISTRATION_FAILED ||
                    event == VoipEvent.LIB_CONNECTION_FAILED ||
                    event == VoipEvent.BUDDY_SUBSCRIPTION_FAILED)
                showErrorEventAlert(myEventBundle);
        }

        private void showErrorEventAlert(VoipEventBundle myEventBundle) {
            BaseEcoTeleconsultationActivity mainActivity = mOuterRef.get();
            AlertDialog.Builder miaAlert = new AlertDialog.Builder(mainActivity);
            miaAlert.setTitle(myEventBundle.getEventType() + ":" + myEventBundle.getEvent());
            miaAlert.setMessage(myEventBundle.getInfo());
            AlertDialog alert = miaAlert.create();
            alert.show();
        }
    }
    protected void answerCall() {
        mVoipLib.answerCall();
    }

    protected void registerAccount() {
        mVoipLib.registerAccount();
    }

    protected CallState getCallState() {
        return mVoipLib.getCall().getState();
    }

    protected void toggleHoldCall(boolean holding) {
        if (holding) {
            mVoipLib.holdCall();
        }
        else {
            mVoipLib.unholdCall();
        }
    }

    protected void hangupCall() {
        mVoipLib.hangupCall();
    }

    protected void setupVoipLib() {
        // Voip Lib Initialization Params
        voipParams = getVoipSetupParams();

        Log.d(TAG, "Initializing the lib...");
        if (mVoipLib == null) {
            Log.d(TAG, "Voip null... Initialization.....");
            mVoipLib = new VoipLibBackend();
            voipHandler = new CallHandler(this);

            // Initialize the library providing custom initialization params and an handler where
            // to receive event notifications. Following Voip methods are called from the handleMassage() callback method
            mVoipLib.initLib(getApplicationContext(), voipParams, voipHandler);
        }
        else {
            Log.d(TAG, "Voip is not null... Destroying the lib before reinitializing.....");
            // Reinitialization will be done after deinitialization event callback
            voipHandler.mReinitRequest = true;
            mVoipLib.destroyLib();
        }
    }

    protected HashMap<String, String> getVoipSetupParams() {
        HashMap<String, String> params = teleconsultation.getLastSession().getVoipParams();

        sipServerIp = params.get("sipServerIp");
        sipServerPort = params.get("sipServerPort");

        /**
         sipServerIp = "192.168.1.100";
         sipServerPort="5060";
         HashMap<String,String> params = new HashMap<String,String>();
         params.put("sipServerIp",sipServerIp);
         params.put("sipServerPort",sipServerPort); // default 5060
         params.put("turnServerIp",  sipServerIp);
         params.put("sipServerTransport","tcp");

         // used by the mMainActivity for calling the specified extension, not used directly by the VoipLib
         params.put("specExtension","MOST0001");

         */

		/* ecografista 	*/
        //accountName = params.get("sipUserName");

        String onHoldSoundPath = Utils.getResourcePathByAssetCopy(getApplicationContext(), "", "test_hold.wav");
        String onIncomingCallRingTonePath = Utils.getResourcePathByAssetCopy(getApplicationContext(), "", "ring_in_call.wav");
        String onOutcomingCallRingTonePath = Utils.getResourcePathByAssetCopy(getApplicationContext(), "", "ring_out_call.wav");

        params.put("onHoldSound", onHoldSoundPath);
        params.put("onIncomingCallSound", onIncomingCallRingTonePath); // onIncomingCallRingTonePath
        params.put("onOutcomingCallSound", onOutcomingCallRingTonePath); // onOutcomingCallRingTonePath
        return params;
    }

    protected void subscribeBuddies() {
        String buddyExtension = voipParams.get("specExtension");
        Log.d(TAG, "adding buddies...");
        mVoipLib.getAccount().addBuddy(getBuddyUri(buddyExtension));
    }

    protected String getBuddyUri(String extension) {
        return "sip:" + extension + "@" + sipServerIp + ":" + sipServerPort;
    }
}
