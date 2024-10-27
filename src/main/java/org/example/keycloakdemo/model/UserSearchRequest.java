package org.example.keycloakdemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSearchRequest {
    private String username;

    public UserSearchRequest() {
    }

    public UserSearchRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserSearchRequest{" +
                "username='" + username + '\'' +
                '}';
    }
}
