package com.marosirobi.workouttrackerapi.integration;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// shared context: full app, in-memory DB (test profile)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "Password1!";

    @Autowired
    protected MockMvcTester mvc;

    protected record TestUser(String username, String email, String accessToken, String refreshToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    protected static String uniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String body(MvcTestResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    protected static <T> T read(MvcTestResult result, String jsonPath) throws Exception {
        return JsonPath.read(body(result), jsonPath);
    }

    protected MvcTestResult postJson(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    protected MvcTestResult register(String username, String email, String password) {
        return postJson("/api/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(username, email, password));
    }

    protected TestUser registerUser() throws Exception {
        String id = uniqueId();
        String username = "u" + id;
        String email = "user" + id + "@test.hu";

        MvcTestResult result = register(username, email, PASSWORD);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        return new TestUser(username, email, read(result, "$.token"), read(result, "$.refreshToken"));
    }
}
