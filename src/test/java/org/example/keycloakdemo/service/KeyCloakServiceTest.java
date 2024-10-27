package org.example.keycloakdemo.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTest
public class KeyCloakServiceTest {

    @Autowired
    private KeyCloakService keyCloakService;

    private MockWebServer mockWebServer;

    @BeforeEach
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(mockWebServer.url("/").toString());
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.url", () -> "http://localhost:8080");
        registry.add("keycloak.realm", () -> "master");
        registry.add("keycloak.client-id", () -> "admin-cli");
        registry.add("keycloak.admin-username", () -> "admin");
        registry.add("keycloak.admin-password", () -> "admin");
    }

   // @Test
    public void testGetKeycloakTokenSuccess() {

        String mockResponse = "{\"access_token\":\"mock-access-token\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        Mono<String> tokenMono = keyCloakService.getKeycloakToken();

        StepVerifier.create(tokenMono)
                .expectNextMatches(token -> token.startsWith("eyJ"))
                .verifyComplete();
    }

    //@Test
    public void testGetKeycloakTokenFailure() {

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request"));

        Mono<String> tokenMono = keyCloakService.getKeycloakToken();

        StepVerifier.create(tokenMono)
                .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException &&
                        throwable.getMessage().contains("400 Bad Request"))
                .verify();
    }
}
