package org.example.keycloakdemo.controller;

import org.example.keycloakdemo.model.UserGroupRequest;
import org.example.keycloakdemo.service.KeyCloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class KeyCloakControllerTest {

    @InjectMocks
    private KeyCloakController keyCloakController;

    @Mock
    private KeyCloakService keyCloakService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    //@Test
    void testCreateUserAndAssignGroup_Success() {
        UserGroupRequest request = new UserGroupRequest("testuser", "test@example.com", "testgroup");

        when(keyCloakService.getKeycloakToken()).thenReturn(Mono.just("token"));

        when(keyCloakService.getUserId("token", "testuser")).thenReturn(Mono.empty())
                .thenReturn(Mono.just("userId"));

        when(keyCloakService.createUser("testuser", "test@example.com", "token")).thenReturn(Mono.empty());
        when(keyCloakService.createGroup("testgroup", "token")).thenReturn(Mono.empty());
        when(keyCloakService.getGroupId("token", "testgroup")).thenReturn(Mono.just("groupId"));
        when(keyCloakService.assignUserToGroup("userId", "groupId", "token")).thenReturn(Mono.empty());

        Mono<String> response = keyCloakController.createUserAndAssignGroup(request);

        assertEquals("User created with ID: userId and assigned to group with ID: groupId.", response.block());

        verify(keyCloakService).getKeycloakToken();
        verify(keyCloakService, times(2)).getUserId(any(), eq("testuser"));
        verify(keyCloakService).createUser("testuser", "test@example.com", "token");
        verify(keyCloakService).createGroup("testgroup", "token");
        verify(keyCloakService).getGroupId("token", "testgroup");
        verify(keyCloakService).assignUserToGroup("userId", "groupId", "token");
    }

    //@Test
    void testCreateUserAndAssignGroup_UserAlreadyExists() {
        UserGroupRequest request = new UserGroupRequest("testuserrohan", "testuserrohan@example.com", "testuserrohangroup");

        when(keyCloakService.getKeycloakToken()).thenReturn(Mono.just("token"));
        when(keyCloakService.getUserId("token", "testuserrohan")).thenReturn(Mono.just("existingUserId"));
        when(keyCloakService.getGroupId("token", "testuserrohangroup")).thenReturn(Mono.just("groupId"));
        when(keyCloakService.assignUserToGroup("existingUserId", "groupId", "token")).thenReturn(Mono.empty());

        Mono<String> response = keyCloakController.createUserAndAssignGroup(request);

        assertEquals("User with ID: existingUserId assigned to existing group with ID: groupId.", response.block());

        verify(keyCloakService).getKeycloakToken();
        verify(keyCloakService).getUserId("token", "testuserrohan");
        verify(keyCloakService).getGroupId("token", "testuserrohangroup");
        verify(keyCloakService).assignUserToGroup("existingUserId", "groupId", "token");
        verify(keyCloakService, never()).createUser(anyString(), anyString(), anyString());
        verify(keyCloakService, never()).createGroup(anyString(), anyString());
    }

    @Test
    void testCreateUserAndAssignGroup_UserNameIsNull() {
        UserGroupRequest request = new UserGroupRequest(null, "test@example.com", "testgroup");

        assertThrows(IllegalArgumentException.class, () -> keyCloakController.createUserAndAssignGroup(request).block());
    }

    @Test
    void testCreateUserAndAssignGroup_InvalidEmail() {
        UserGroupRequest request = new UserGroupRequest("testuser", "invalid-email", "testgroup");

        assertThrows(IllegalArgumentException.class, () -> keyCloakController.createUserAndAssignGroup(request).block());
    }

    @Test
    void testCreateUserAndAssignGroup_GroupNameIsNull() {
        UserGroupRequest request = new UserGroupRequest("testuser", "test@example.com", null);

        assertThrows(IllegalArgumentException.class, () -> keyCloakController.createUserAndAssignGroup(request).block());
    }

    @Test
    void testCreateUserAndAssignGroup_KeycloakApiError() {
        UserGroupRequest request = new UserGroupRequest("testuser", "test@example.com", "testgroup");

        when(keyCloakService.getKeycloakToken()).thenReturn(Mono.just("token"));
        when(keyCloakService.getUserId("token", "testuser")).thenReturn(Mono.just("userId"));
        when(keyCloakService.getGroupId("token", "testgroup")).thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "Group not found", null, null, null)));

        assertThrows(ResponseStatusException.class, () -> keyCloakController.createUserAndAssignGroup(request).block());
    }
}
