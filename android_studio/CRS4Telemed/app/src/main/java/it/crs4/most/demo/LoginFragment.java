package it.crs4.most.demo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.crs4.most.demo.models.TaskGroup;
import it.crs4.most.demo.models.User;


public class LoginFragment extends Fragment {

    public static final int LOGGED_IN_RESULT = 0;
    public static final String USER_ARG = "it.crs4.most.demo.user";
    private static final String TAG = "LoginFragment";
    private static final String USERS = "users";
    private static final String TASKGROUPS = "taskgroups";
    private static final int PASSCODE_LEN = 5;

    private String mTaskGroup;
    private Spinner mUsernameSpinner;
    private Spinner mTaskGroupSpinner;
    private RESTClient mRESTClient;
    private User mUser;
    private TextView mPasswordText;
    private ArrayList<User> mUsers;
    private ArrayList<TaskGroup> mTaskGroups;
    private CustomSpinnerAdapter<User> mUsersAdapter;
    private CustomSpinnerAdapter<TaskGroup> mTaskGroupAdapter;

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String serverIP = QuerySettings.getConfigServerAddress(getActivity());
        Integer serverPort = Integer.valueOf(QuerySettings.getConfigServerPort(getActivity()));
        mRESTClient = new RESTClient(getActivity(), serverIP, serverPort);

        View v = inflater.inflate(R.layout.login_fragment, container, false);

        //Set taskgroups
        mTaskGroupSpinner = (Spinner) v.findViewById(R.id.taskgroup_spinner);
        if (savedInstanceState != null) {
            mTaskGroups = (ArrayList<TaskGroup>) savedInstanceState.getSerializable(TASKGROUPS);
        }
        else {
            mTaskGroups = new ArrayList<>();
            loadTaskgroups();
        }
        mTaskGroupAdapter = new CustomSpinnerAdapter<>(getActivity(), R.layout.spinner_dropdown, mTaskGroups);
        mTaskGroupAdapter.setHintMessage(R.string.spinner_taskgrouop_hint);
        mTaskGroupAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mTaskGroupSpinner.setAdapter(mTaskGroupAdapter);
        mTaskGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TaskGroup taskGroup = mTaskGroupAdapter.getItem(position);
                if (taskGroup != null) {
                    mUsernameSpinner.setClickable(true);
                    mTaskGroup = taskGroup.getId();
                    loadUsers();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        //Set users
        mUsernameSpinner = (Spinner) v.findViewById(R.id.username_spinner);
        if (savedInstanceState != null && savedInstanceState.containsKey(USERS)) {
            mUsers = (ArrayList<User>) savedInstanceState.getSerializable(USERS);
        }
        else {
            mUsers = new ArrayList<>();
        }
        mUsersAdapter = new CustomSpinnerAdapter<>(getActivity(), R.layout.spinner_dropdown, mUsers);
        mUsersAdapter.setHintMessage(R.string.spinner_users_hint);
        mUsersAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        mUsernameSpinner.setAdapter(mUsersAdapter);

        mPasswordText = (TextView) v.findViewById(R.id.password_text);
        Button sendPassword = (Button) v.findViewById(R.id.password_button);

        if (QuerySettings.isEcographist(getActivity())) {
            mPasswordText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            mPasswordText.setHint(R.string.lbl_enter_passcode);
            mPasswordText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String passcode = mPasswordText.getText().toString();
                    if (passcode.length() == PASSCODE_LEN) {
                        retrieveAccessToken(passcode);
                        mPasswordText.setText("");
                    }
                }
            });
            sendPassword.setVisibility(View.INVISIBLE);
        }
        else {
            mPasswordText.setHint(R.string.lbl_enter_password);
            sendPassword.setVisibility(View.VISIBLE);
            sendPassword.setEnabled(true);
            sendPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String password = mPasswordText.getText().toString();
                    retrieveAccessToken(password);
                }
            });
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(USERS, mUsers);
        outState.putSerializable(TASKGROUPS, mTaskGroups);
    }

    private void loadTaskgroups() {
        final ProgressDialog loadingConfigDialog = new ProgressDialog(getActivity());
        loadingConfigDialog.setTitle(getString(R.string.load_taskgroups_title));
        loadingConfigDialog.setMessage(getString(R.string.load_taskgroups_message));
        loadingConfigDialog.setMax(10);
        loadingConfigDialog.show();

        mRESTClient.getTaskgroups(
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject taskgroupsData) {
                        try {
                            boolean success = (taskgroupsData != null && taskgroupsData.getBoolean("success"));
                            if (!success) {
                                Log.e(TAG, "No valid taskgroups found for this device");
                                loadingConfigDialog.dismiss();

                                if (taskgroupsData.getJSONObject("error").getInt("code") == 501) {
                                    showError(R.string.device_not_registered, true);
                                }
                                else {
                                    showError(R.string.generic_taskgroup_error, true);
                                }
                                return;
                            }
                            mTaskGroupAdapter.clear();
                            JSONArray jsonTaskgroups = taskgroupsData.getJSONObject("data").getJSONArray("task_groups");
                            CharSequence taskGroupId;
                            CharSequence taskGroupName;
                            for (int i = 0; i < jsonTaskgroups.length(); i++) {
                                taskGroupId = jsonTaskgroups.getJSONObject(i).getString("uuid");
                                taskGroupName = jsonTaskgroups.getJSONObject(i).getString("name");
                                mTaskGroupAdapter.add(new TaskGroup(taskGroupId.toString(), taskGroupName.toString()));
                            }
                            mTaskGroupAdapter.notifyDataSetChanged();
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                            return;
                        }
                        loadingConfigDialog.dismiss();
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Log.e(TAG, "Error contacting the configuration server");
                        loadingConfigDialog.dismiss();
                        showError(R.string.server_connection_error, true);
                    }
                }
        );
    }

    private void loadUsers() {
        final ProgressDialog loadUserDialog = new ProgressDialog(getActivity());
        loadUserDialog.setMessage(getString(R.string.loading_users));
        loadUserDialog.setCancelable(false);
        loadUserDialog.setCanceledOnTouchOutside(false);
        loadUserDialog.setMax(10);
        loadUserDialog.show();

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject usersData) {
                final JSONArray users;
                int currentUserPos = -1;
                mUsersAdapter.clear();
                try {
                    boolean success = usersData != null && usersData.getBoolean("success");
                    if (!success) {
                        Log.e(TAG, "No valid users found for this taskgroup");
                        return;
                    }
                    users = usersData.getJSONObject("data").getJSONArray("applicants");
                }
                catch (JSONException e) {
                    Log.e(TAG, "Error loading user information");
                    e.printStackTrace();
                    return;
                }

                for (int i = 0; i < users.length(); i++) {
                    User u;
                    try {
                        u = User.fromJSON(users.getJSONObject(i));
                        mUsersAdapter.add(u);

                        if (mUser != null && u.equals(mUser)) {
                            currentUserPos = i;
                        }
                    }
                    catch (TeleconsultationException | JSONException e) {
                        Log.e(TAG, "Error loading user information");
                    }
                }
                loadUserDialog.dismiss();
                mUsersAdapter.notifyDataSetChanged();
                mPasswordText.setEnabled(true);
                if (currentUserPos != -1) {
                    mUsernameSpinner.setSelection(currentUserPos);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loadUserDialog.dismiss();
                showError(R.string.server_connection_error, true);
            }
        };

        mRESTClient.getUsersByTaskgroup(mTaskGroup, listener, errorListener);
    }

    private void retrieveAccessToken(String password) {
        final User selectedUser = (User) mUsernameSpinner.getSelectedItem();
        String username = selectedUser.getUsername();
        String grantType = QuerySettings.isEcographist(getActivity()) ?
                RESTClient.GRANT_TYPE_PINCODE :
                RESTClient.GRANT_TYPE_PASSWORD;

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonresponse = new JSONObject(response);
                    String accessToken = jsonresponse.getString("access_token");

                    if (accessToken != null) {
                        QuerySettings.setAccessToken(getActivity(), accessToken);
                        QuerySettings.setUser(getActivity(), selectedUser);
                        QuerySettings.setLoginChecked(getActivity(), true);
                        getActivity().finish();
                    }
                    else {
                        showError(R.string.login_error_details, false);
                    }
                }
                catch (JSONException e) {
                    Log.e(TAG, "error parsing json response: " + e);
                    e.printStackTrace();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                e.printStackTrace();
                showError(R.string.login_error_details, false);
            }
        };

        mRESTClient.getAccessToken(username, mTaskGroup, grantType, password, listener, errorListener);
    }

    private void showError(int errorMsgId, final boolean finishActivity) {
        new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.error)
                .setMessage(errorMsgId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (finishActivity) {
                            getActivity().finish();
                        }
                    }
                })
                .create()
                .show();
    }
}