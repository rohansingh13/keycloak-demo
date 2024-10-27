package org.example.keycloakdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String url;
    private String realm;
    private String clientId;
    private String adminUsername;
    private String adminPassword;

    public KeycloakProperties() {
    }

    public KeycloakProperties(String url, String realm, String clientId, String adminUsername, String adminPassword) {
        this.url = url;
        this.realm = realm;
        this.clientId = clientId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Override
    public String toString() {
        return "KeycloakProperties{" +
                "url='" + url + '\'' +
                ", realm='" + realm + '\'' +
                ", clientId='" + clientId + '\'' +
                ", adminUsername='" + adminUsername + '\'' +
                ", adminPassword='" + adminPassword + '\'' +
                '}';
    }
}
