package org.example.keycloakdemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroupRequest {

    private String username;
    private String email;
    private String groupName;

    public UserGroupRequest() {
    }

    public UserGroupRequest(String username, String email, String groupName) {
        this.username = username;
        this.email = email;
        this.groupName = groupName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return "UserGroupRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", groupName='" + groupName + '\'' +
                '}';
    }
}
