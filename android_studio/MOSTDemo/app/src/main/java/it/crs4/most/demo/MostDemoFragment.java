package it.crs4.most.demo;


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
    private String mServerIP;
    private String mTaskGroup;
    private String mRole;
    private TextView mMsgText;
    private LinearLayout mNewTeleFrame;
    private LinearLayout mContinueTeleFrame;
    private LinearLayout mSearchTeleframe;
    private MenuItem mLoginMenuItem;

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

        mServerIP = QuerySettings.getConfigServerAddress(getActivity());
        int serverPort = Integer.valueOf(QuerySettings.getConfigServerPort(getActivity()));
        mTaskGroup = QuerySettings.getTaskGroup(getActivity());
        String accessToken = QuerySettings.getAccessToken(getActivity());
        mRole = QuerySettings.getRole(getActivity());
        RESTClient restClient = new RESTClient(getActivity(), mServerIP, serverPort);
        restClient.getOpenedTeleconsultationsByTaskgroup(mTaskGroup, accessToken, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "OPENED: " + response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkSettings()) {
            String accessToken = QuerySettings.getAccessToken(getActivity());
            Log.d(TAG, "Access token is: " + accessToken);
            updateLoginState();
        }
        else {
            String msgParts = "";
            int nullCounter = 0;
            if (mServerIP == null) {
                msgParts += getString(R.string.set_server) + ", ";
                nullCounter += 2; // Server and port
            }
            if (mTaskGroup == null) {
                msgParts += getString(R.string.select_taskgroup) + ", ";
                nullCounter++;
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
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(loginIntent);
                }
                else {
                    logout();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
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
            mSearchTeleframe.setVisibility(View.VISIBLE);
            mNewTeleFrame.setVisibility(View.GONE);
            mContinueTeleFrame.setVisibility(View.GONE);
            setTextMessage(getString(R.string.login_instructions));
        }
        setLoginButton();
    }

    private void logout() {
        QuerySettings.setAccessToken(getActivity(), null);
        QuerySettings.setUser(getActivity(), null);
        updateLoginState();
    }

    public boolean checkSettings() {
        return !(mServerIP == null || mTaskGroup == null || mRole == null);
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

}
