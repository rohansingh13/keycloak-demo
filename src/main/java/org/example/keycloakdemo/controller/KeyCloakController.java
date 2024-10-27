package org.example.keycloakdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.keycloakdemo.model.UserGroupRequest;
import org.example.keycloakdemo.model.UserSearchRequest;
import org.example.keycloakdemo.model.UserSearchResponse;
import org.example.keycloakdemo.service.KeyCloakService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/keycloak/v1")
@Tag(name = "Keycloak API", description = "API for managing users and groups in Keycloak")
public class KeyCloakController {

    private static final Logger logger = Logger.getLogger(KeyCloakController.class.getName());
    private final KeyCloakService keyCloakService;
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public KeyCloakController(KeyCloakService keyCloakService) {
        this.keyCloakService = keyCloakService;
    }

    @PostMapping("/users/create-and-assign-group")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create User and Assign to Group  (v1)",
            description = "Creates a user in Keycloak and assigns them to a specified group in a single request."
    )
    public Mono<String> createUserAndAssignGroup(
            @Parameter(description = "User and group information") @RequestBody UserGroupRequest userGroupRequest) {

        validateRequest(userGroupRequest);

        String username = userGroupRequest.getUsername();
        String email = userGroupRequest.getEmail();
        String groupName = userGroupRequest.getGroupName();

        return keyCloakService.getKeycloakToken()
                .flatMap(token ->
                        // Step 1: Check if the user exists
                        keyCloakService.getUserId(token, username)
                                .flatMap(userId ->
                                        // Step 2: Check if the group exists
                                        keyCloakService.getGroupId(token, groupName)
                                                .flatMap(groupId ->
                                                        // If the group exists, assign the user to the group
                                                        keyCloakService.assignUserToGroup(userId, groupId, token)
                                                                .then(Mono.just("User with ID: " + userId + " assigned to existing group with ID: " + groupId + "."))
                                                )
                                                .switchIfEmpty(
                                                        // If the group does not exist, create it
                                                        keyCloakService.createGroup(groupName, token)
                                                                .then(
                                                                        // Step 3: Get the new group ID
                                                                        keyCloakService.getGroupId(token, groupName)
                                                                )
                                                                .flatMap(groupId ->
                                                                        // Step 4: Assign the user to the new group
                                                                        keyCloakService.assignUserToGroup(userId, groupId, token)
                                                                                .then(Mono.just("User with ID: " + userId + " created group with ID: " + groupId + "."))
                                                                )
                                                )
                                )
                                .switchIfEmpty(
                                        // If the user does not exist, create the user and group
                                        keyCloakService.createUser(username, email, token)
                                                .then(keyCloakService.createGroup(groupName, token))
                                                .then(keyCloakService.getUserId(token, username))
                                                .flatMap(userId ->
                                                        keyCloakService.getGroupId(token, groupName)
                                                                .flatMap(groupId ->
                                                                        keyCloakService.assignUserToGroup(userId, groupId, token)
                                                                                .then(Mono.just("User created with ID: " + userId + " and assigned to group with ID: " + groupId + "."))
                                                                )
                                                )
                                )
                )
                .onErrorResume(this::handleErrors);
    }

    @PostMapping("/users/search")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Search User in Test Realm (v1)",
            description = "Search for a user in the Keycloak test realm using the master realm token."
    )
    public Mono<UserSearchResponse> searchUser(
            @Parameter(description = "Username to search the user") @RequestBody UserSearchRequest userSearchRequest
    ) {

        if (userSearchRequest.getUsername() == null || userSearchRequest.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required for searching.");
        }

        return keyCloakService.getKeycloakToken()
                .flatMap(token -> keyCloakService.searchUser(token, userSearchRequest.getUsername()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in test realm")));
    }

    private void validateRequest(UserGroupRequest userGroupRequest) {
        if (userGroupRequest.getUsername() == null || userGroupRequest.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (userGroupRequest.getEmail() == null || !isValidEmail(userGroupRequest.getEmail())) {
            throw new IllegalArgumentException("Valid email is required.");
        }
        if (userGroupRequest.getGroupName() == null || userGroupRequest.getGroupName().isEmpty()) {
            throw new IllegalArgumentException("Group name is required.");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    private Mono<String> handleErrors(Throwable throwable) {
        logger.log(Level.WARNING, "Error occurred: ", throwable);
        if (throwable instanceof WebClientResponseException webClientException) {
            return Mono.error(new ResponseStatusException(webClientException.getStatusCode(), "Keycloak API error: " + webClientException.getMessage()));
        } else if (throwable instanceof IllegalArgumentException) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, throwable.getMessage()));
        } else {
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + throwable.getMessage()));
        }
    }

}
