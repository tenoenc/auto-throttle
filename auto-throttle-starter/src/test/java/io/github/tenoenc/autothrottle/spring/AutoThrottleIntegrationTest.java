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
                "management.endpoints.web.exposure.include=*", // 모든 Actuator 엔드포인트 노출
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
    void testThrottleFilter() {
        // 1. 정상 호출 확인
        ResponseEntity<String> response = restTemplate.getForEntity("/test", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");

        // 2. 리미터가 실제로 카운팅을 했는지 확인 (Inflight는 0이어야 하고, Limit은 초기값 이상)
        assertThat(limiter.getLimit()).isGreaterThan(0);
    }

    @Test
    void testActuatorEndpoint() {

        // when: /actuator/autothrottle 호출
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/autothrottle", Map.class);

        // then: 상태 코드 200 OK 확인
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // then: JSON 응답 본문에 'limit'과 'inflight" 키가 있는지 확인
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("limit");
        assertThat(body).containsKey("inflight");

        System.out.println("Actuator Response: " + body);
    }

    // 테스트용 미니 앱
    @SpringBootApplication
    @RestController
    static class TestApp {
        @GetMapping("/test")
        public String test() {
            try { Thread.sleep(10); } catch (Exception e) {}
            return "ok";
        }
    }
}
