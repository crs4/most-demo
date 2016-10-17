package it.crs4.most.demo;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MostDemoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MostDemoFragment extends Fragment {

    private static final String TAG = "MostDemoFragment";
    private static final String LOGIN_VALID = "it.crs4.most.demo.login_valid";
    private String mServerIP;
    private String mRole;
    private TextView mMsgText;
    private LinearLayout mNewTeleFrame;
    private LinearLayout mContinueTeleFrame;
    private LinearLayout mSearchTeleframe;
    private MenuItem mLoginMenuItem;
    private RESTClient mRestClient;
    private String mAccessToken;
    private ProgressDialog mProgress;

    public MostDemoFragment() {
        // Required empty public constructor
    }

    public static MostDemoFragment newInstance() {
        return new MostDemoFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mServerIP = QuerySettings.getConfigServerAddress(getActivity());
        int serverPort = Integer.valueOf(QuerySettings.getConfigServerPort(getActivity()));
        mAccessToken = QuerySettings.getAccessToken(getActivity());
        mRole = QuerySettings.getRole(getActivity());
        mRestClient = new RESTClient(getActivity(), mServerIP, serverPort);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.most_demo_fragment, container, false);
        mMsgText = (TextView) v.findViewById(R.id.msg_text);

        mNewTeleFrame = (LinearLayout) v.findViewById(R.id.new_teleconsultation_frame);
        ImageButton newTeleButton = (ImageButton) v.findViewById(R.id.new_teleconsultation_button);
        newTeleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTeleconsultationSetupActivity(TeleconsultationSetupActivity.ACTION_NEW_TELE);
            }
        });

        mContinueTeleFrame = (LinearLayout) v.findViewById(R.id.continue_teleconsultation_frame);
        ImageButton continueTeleButton = (ImageButton) v.findViewById(R.id.continue_teleconsultation_button);
        continueTeleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                launchTeleconsultationSetupActivity(TeleconsultationSetupActivity.ACTION_CONTINUE_TELE);
            }
        });

        mSearchTeleframe = (LinearLayout) v.findViewById(R.id.search_teleconsultation_frame);
        ImageButton mSearchTeleButton = (ImageButton) v.findViewById(R.id.search_teleconsultation_button);
        mSearchTeleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchTeleconsultationSetupActivity(TeleconsultationSetupActivity.ACTION_NEW_TELE);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProgress != null) {
            mProgress.dismiss();
        }
        if (checkSettings()) {
            if (isLoggedIn() && !QuerySettings.isLoginChecked(getActivity())) {
                checkLogin();
            }
            else {
                updateLoginState();
            }
        }
        else {
            String msgParts = "";
            int nullCounter = 0;
            if (mServerIP == null) {
                msgParts += getString(R.string.set_server) + ", ";
                nullCounter += 2; // Server and port
            }
            if (mRole == null) {
                msgParts += getString(R.string.select_role) + ", ";
                nullCounter++;
            }
            msgParts = msgParts.substring(0, msgParts.length() - 2);
            if (nullCounter > 1) {
                int pos = msgParts.lastIndexOf(",");
                msgParts = new StringBuilder(msgParts).replace(pos, pos + 1, " and").toString();
            }
            String msg = String.format(getString(R.string.most_demo_fragment_instructions), msgParts);
            setTextMessage(msg);

            setLoginButton();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.most_demo_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        mLoginMenuItem = menu.findItem(R.id.login_menu_item);
        setLoginButton();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_menu_item:
                if (!isLoggedIn()) {
                    launchLoginActivity();
                }
                else {
                    logout();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkLogin() {
        mProgress = new ProgressDialog(getActivity());
        mProgress.setTitle(getString(R.string.check_login_title));
        mProgress.setMessage(getString(R.string.check_login_message));
        mProgress.show();

        ResponseHandlerDecorator<JSONObject> listener = new ResponseHandlerDecorator<>(getActivity(),
            new Response.Listener() {
                @Override
                public void onResponse(Object response) {
                    mProgress.dismiss();
                    QuerySettings.setLoginChecked(getActivity(), true);
                    updateLoginState();
                }
            });

        mRestClient.checkLogin(mAccessToken, listener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mProgress.dismiss();
                setTextMessage(getString(R.string.server_connection_error));
            }
        });
    }

    private void updateLoginState() {
        if (isLoggedIn()) {
            if (QuerySettings.isEcographist(getActivity())) {  //
                mNewTeleFrame.setVisibility(View.VISIBLE);
                mContinueTeleFrame.setVisibility(View.VISIBLE);
            }
            else {
                mSearchTeleframe.setVisibility(View.VISIBLE);
            }
            setTextMessage(null);
        }
        else {
            mSearchTeleframe.setVisibility(View.GONE);
            mNewTeleFrame.setVisibility(View.GONE);
            mContinueTeleFrame.setVisibility(View.GONE);
            setTextMessage(getString(R.string.login_instructions));
        }
        setLoginButton();
    }

    private void deleteAuthenticationData() {
        QuerySettings.setAccessToken(getActivity(), null);
        QuerySettings.setUser(getActivity(), null);
    }

    private void logout() {
        deleteAuthenticationData();
        updateLoginState();
    }

    public boolean checkSettings() {
        return !(mServerIP == null || mRole == null);
    }

    private void setTextMessage(@Nullable String message) {
        mMsgText.setText(message);
        if (message == null) {
            mMsgText.setVisibility(View.GONE);
        }
        else {
            mMsgText.setVisibility(View.VISIBLE);
        }
    }

    private void setLoginButton() {
        if (mLoginMenuItem != null) {
            Drawable icon;
            if (isLoggedIn()) {
                icon = getResources().getDrawable(R.drawable.logout);
            }
            else {
                icon = getResources().getDrawable(R.drawable.login);
            }
            mLoginMenuItem.setIcon(icon);

            if (checkSettings()) {
                mLoginMenuItem.setEnabled(true);
            }
            else {
                mLoginMenuItem.setEnabled(false);
            }
        }
    }

    private boolean isLoggedIn() {
        return QuerySettings.getAccessToken(getActivity()) != null;
    }

    private void launchTeleconsultationSetupActivity(String action) {
        Intent i = new Intent(getActivity(), TeleconsultationSetupActivity.class);
        Bundle args = new Bundle();
        args.putString(TeleconsultationSetupActivity.ACTION_ARG, action);
        i.putExtras(args);
        startActivity(i);
    }

    private void launchLoginActivity() {
        Intent i = new Intent(getActivity(), LoginActivity.class);
        startActivity(i);
    }
}
