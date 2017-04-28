package it.crs4.most.demo.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import it.crs4.most.demo.TeleconsultationException;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mUsername = null;
    private String mFirstName = null;
    private String mLastName = null;
    private boolean mAdmin = false;

    public User(String firstName, String lastName, String username, boolean isAdmin) {
        mFirstName = firstName;
        mLastName = lastName;
        mUsername = username;
        mAdmin = isAdmin;
    }

    public String getFirstName() {
        return mFirstName;
    }

    public String getLastName() {
        return mLastName;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getUsername() {
        return mUsername;
    }

    public static User fromJSON(JSONObject userData) throws TeleconsultationException {
        try {
            String firstname = userData.getString("firstname");
            String lastname = userData.getString("lastname");
            String username = userData.getString("username");
            boolean isAdmin = userData.has("is_admin")? userData.getBoolean("is_admin"): false;
            return new User(firstname, lastname, username, isAdmin);
        }
        catch (JSONException e) {
            throw new TeleconsultationException();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        User other = (User) o;
        return
            getFirstName().equals(other.getFirstName()) &&
            getLastName().equals(other.getLastName()) &&
            getUsername().equals(other.getUsername());
    }

    @Override
    public String toString() {
        return  String.format("%s %s (%s)", getFirstName(), getLastName(), getUsername());
    }

    public boolean isAdmin() {
        return mAdmin;
    }
}