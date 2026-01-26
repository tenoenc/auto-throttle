package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoints.web.exposure.include=*", // Expose all Actuator endpoints for testing
                "management.endpoint.health.show-details=always"
        }
)
public class AutoThrottleIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    AtomicLimiter limiter;

    @Test
    void should_ProcessRequest_And_ReflectMetrics_When_Called() {
        // 1. Verify normal request processing (Green path)
        ResponseEntity<String> response = restTemplate.getForEntity("/test", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");

        // 2. Verify that the Limiter state is active
        // Ideally, 'inflight' should be back to 0 after the request, and 'limit' should be initialized.
        assertThat(limiter.getLimit()).isGreaterThan(0);
    }

    @Test
    void should_ExposeLimiterMetrics_ViaActuatorEndpoint() {
        // when: GET /actuator/autothrottle
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/autothrottle", Map.class);

        // then: HTTP 200 OK
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // then: Verify JSON body contains key metrics
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("limit");
        assertThat(body).containsKey("inflight");

        System.out.println("Actuator Response: " + body);
    }

    // Minimal Spring Boot Application for Integration Testing
    @SpringBootApplication
    @RestController
    static class TestApp {
        @GetMapping("/test")
        public String test() {
            // Simulate processing time (10ms)
            try { Thread.sleep(10); } catch (Exception e) {}
            return "ok";
        }
    }
}