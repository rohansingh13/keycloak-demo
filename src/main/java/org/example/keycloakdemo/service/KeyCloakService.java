package org.example.keycloakdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.keycloakdemo.config.KeycloakProperties;
import org.example.keycloakdemo.handler.KeycloakException;
import org.example.keycloakdemo.handler.UserNotFoundException;
import org.example.keycloakdemo.model.UserSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class KeyCloakService {

    private static final Logger logger = LoggerFactory.getLogger(KeyCloakService.class);

    private final WebClient webClient;
    private final KeycloakProperties keycloakProperties;

    public KeyCloakService(WebClient.Builder webClientBuilder, KeycloakProperties keycloakProperties) {
        this.webClient = webClientBuilder.build();
        this.keycloakProperties = keycloakProperties;
    }

    public Mono<String> getKeycloakToken() {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakProperties.getUrl(), keycloakProperties.getRealm());

        logger.debug("Requesting token from Keycloak at: {}", tokenUrl);

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("client_id", keycloakProperties.getClientId())
                        .with("username", keycloakProperties.getAdminUsername())
                        .with("password", keycloakProperties.getAdminPassword())
                        .with("grant_type", "password"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"))
                .doOnSuccess(token -> logger.info("Successfully obtained Access Token."))
                .doOnError(error -> logger.error("Error obtaining token: {}", error.getMessage()));
    }

    public Mono<Void> createUser(String username, String email, String token) {
        String userUrl = String.format("%s/admin/realms/%s/users", keycloakProperties.getUrl(), keycloakProperties.getRealm());

        return webClient.post()
                .uri(userUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of(
                        "username", username,
                        "email", email,
                        "enabled", true
                )))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("Client error occurred while creating user '%s': %s", username, body);
                                    logger.error(errorMessage);
                                    return Mono.error(new KeycloakException(errorMessage, null));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("Server error occurred while creating user '%s': %s", username, body);
                                    logger.error(errorMessage);
                                    return Mono.error(new KeycloakException(errorMessage, null));
                                })
                )
                .bodyToMono(Void.class)
                //.then()
                .doOnSuccess(v -> logger.info("User created successfully with username: {}", username))
                .doOnError(error -> logger.error("Error creating user: {}", error.getMessage()));
    }

    public Mono<Void> createGroup(String groupName, String token) {
        String createGroupUrl = String.format("%s/admin/realms/%s/groups", keycloakProperties.getUrl(), keycloakProperties.getRealm());

        return webClient.post()
                .uri(createGroupUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of("name", groupName)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("Error creating group '%s': %s", groupName, body);
                                    logger.error(errorMessage);
                                    return Mono.error(new KeycloakException(errorMessage, null));
                                })
                )
                .bodyToMono(Void.class)
                //.then()
                .doOnSuccess(v -> logger.info("Group created successfully with groupName: {}", groupName))
                .doOnError(error -> logger.error("Error creating group: {}", error.getMessage()));
    }

    public Mono<String> getUserId(String token, String username) {

        String searchUserUrl = String.format("%s/admin/realms/%s/users?username=%s", keycloakProperties.getUrl(), keycloakProperties.getRealm(), username);

        return webClient.get()
                .uri(searchUserUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .flatMap(users -> {
                    if (!users.isEmpty()) {
                        return Mono.just((String) users.getFirst().get("id"));
                    } else {
                        return Mono.empty();
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnError(error -> logger.error("Error retrieving user ID for username: {}. Error: {}", username, error.getMessage()));
    }

    public Mono<String> getGroupId(String token, String groupName) {

        String searchGroupUrl = String.format("%s/admin/realms/%s/groups?search=%s", keycloakProperties.getUrl(), keycloakProperties.getRealm(), groupName);

        return webClient.get()
                .uri(searchGroupUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .flatMap(groups -> {
                    if (!groups.isEmpty()) {
                        return Mono.just((String) groups.getFirst().get("id"));
                    } else {
                        return Mono.empty();
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnError(error -> logger.error("Error retrieving group ID for groupName: {}. Error: {}", groupName, error.getMessage()));
    }

    public Mono<Void> assignUserToGroup(String userId, String groupId, String token) {
        String assignUserToGroupUrl = String.format("%s/admin/realms/%s/users/%s/groups/%s", keycloakProperties.getUrl(), keycloakProperties.getRealm(), userId, groupId);

        return webClient.put()
                .uri(assignUserToGroupUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                //.contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("Error assigning User ID '%s' to Group ID '%s': %s", userId, groupId, body);
                                    logger.error(errorMessage);
                                    return Mono.error(new KeycloakException(errorMessage, null));
                                })
                )
                .bodyToMono(Void.class)
                .doOnSuccess(v -> logger.info("User with ID: {} assigned to group with ID: {}", userId, groupId))
                .doOnError(error -> logger.error("Error assigning user to group: {}", error.getMessage()));
    }

    public Mono<UserSearchResponse> searchUser(String token, String username) {
        String searchUserUrl = String.format("%s/admin/realms/test/users?username=%s", keycloakProperties.getUrl(), username);

        logger.debug("Searching for user: {}", username);

        return webClient.get()
                .uri(searchUserUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    String errorMessage = String.format("Error retrieving user for username '%s': %s", username, body);
                                    logger.error(errorMessage);
                                    return Mono.error(new UserNotFoundException(errorMessage));
                                })
                )
                .bodyToFlux(JsonNode.class)
                .filter(user -> user.get("username").asText().equalsIgnoreCase(username))
                .singleOrEmpty()
                .map(user -> new UserSearchResponse(
                        user.get("id").asText(),
                        user.get("username").asText(),
                        user.get("firstName").asText(),
                        user.get("lastName").asText(),
                        user.get("email").asText()))
                .doOnSuccess(userResponse -> logger.info("User search completed successfully for username: {}", username))
                .doOnError(error -> logger.error("Error searching user: {}", error.getMessage()))
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + username)));
    }
}
