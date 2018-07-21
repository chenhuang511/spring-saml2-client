package vn.softdreams.springsaml.core;

import com.google.gson.Gson;

/**
 * Created by chen on 7/21/18.
 */
public class AccountInfo {
    private String requestId;
    private String name;
    private String email;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
