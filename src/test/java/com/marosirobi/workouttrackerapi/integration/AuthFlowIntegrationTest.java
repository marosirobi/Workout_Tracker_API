package com.marosirobi.workouttrackerapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIntegrationTest extends IntegrationTestSupport {

    private static String refreshBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private static String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    void registeredUser_canOpenProtectedEndpoint() throws Exception {
        var user = registerUser();

        assertThat(mvc.get().uri("/api/users/me").header("Authorization", user.bearer()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.email").isEqualTo(user.email());
    }

    @Test
    void protectedEndpoint_withoutToken_isRejected() {
        assertThat(mvc.get().uri("/api/users/me")).hasStatus4xxClientError();
    }

    @Test
    void refresh_rotatesToken_andOldOneStopsWorking() throws Exception {
        var user = registerUser();

        var refreshed = postJson("/api/auth/refresh", refreshBody(user.refreshToken()));

        assertThat(refreshed).hasStatusOk();
        String newRefreshToken = read(refreshed, "$.refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(user.refreshToken());
        assertThat(postJson("/api/auth/refresh", refreshBody(user.refreshToken()))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void logout_invalidatesRefreshToken() throws Exception {
        var user = registerUser();

        assertThat(postJson("/api/auth/logout", refreshBody(user.refreshToken()))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(postJson("/api/auth/refresh", refreshBody(user.refreshToken()))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void register_takenEmailInOtherCase_returns409WithCode() throws Exception {
        var user = registerUser();

        assertThat(register("v" + uniqueId(), user.email().toUpperCase(), PASSWORD))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void register_weakPassword_returns400() {
        String id = uniqueId();

        assertThat(register("u" + id, "user" + id + "@test.hu", "weak")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_isCaseInsensitiveForEmail() throws Exception {
        var user = registerUser();

        assertThat(postJson("/api/auth/login", loginBody(user.email().toUpperCase(), PASSWORD)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.token").asString().isNotBlank();
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var user = registerUser();

        assertThat(postJson("/api/auth/login", loginBody(user.email(), "Wrong1234!")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePassword_revokesOldSessions() throws Exception {
        var user = registerUser();

        assertThat(mvc.put().uri("/api/users/me/password")
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"%s","newPassword":"NewPassword1!"}
                        """.formatted(PASSWORD)))
                .hasStatusOk();

        assertThat(postJson("/api/auth/refresh", refreshBody(user.refreshToken()))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(postJson("/api/auth/login", loginBody(user.email(), PASSWORD))).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(postJson("/api/auth/login", loginBody(user.email(), "NewPassword1!"))).hasStatusOk();
    }
}
