package com.bankflow.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class BankFlowApiIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("bankflow")
        .withUsername("bankflow")
        .withPassword("bankflow");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("bankflow.jwt.secret", () -> "integration-secret-integration-secret-integration-secret");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void loginCreateDepositTransferReplayAndAuditFlow() throws Exception {
        String token = login("customer@bankflow.dev", "Customer123!");
        UUID newAccount = createAccount(token);
        mockMvc.perform(post("/api/accounts/{id}/deposit", newAccount)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": \"200.00\", \"description\": \"payroll\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.balance").value(200.00));

        String payload = "{\"sourceAccountId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"destinationAccountId\":\"" + newAccount + "\",\"amount\":\"50.00\",\"description\":\"test transfer\"}";
        MvcResult first = mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "it-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.amount").value(50.00))
            .andReturn();

        MvcResult replay = mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "it-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(objectMapper.readTree(first.getResponse().getContentAsString()).at("/data/id").asText())
            .isEqualTo(objectMapper.readTree(replay.getResponse().getContentAsString()).at("/data/id").asText());

        String admin = login("admin@bankflow.dev", "Admin123!");
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").isNotEmpty());
    }

    @Test
    void refreshTokenRotatesAndLogoutRevokesSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"customer@bankflow.dev\",\"password\":\"Customer123!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();
        String refreshToken = objectMapper.readTree(login.getResponse().getContentAsString()).at("/data/refreshToken").asText();

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();
        String rotatedRefreshToken = objectMapper.readTree(refresh.getResponse().getContentAsString()).at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void adminCanRunLedgerReconciliation() throws Exception {
        String admin = login("admin@bankflow.dev", "Admin123!");

        mockMvc.perform(get("/api/admin/reconciliation").header("Authorization", "Bearer " + admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.balanced").value(true))
            .andExpect(jsonPath("$.data.accountIssues.length()").value(0))
            .andExpect(jsonPath("$.data.transferIssues.length()").value(0));
    }

    @Test
    void unauthorizedAccessAndInsufficientBalanceReturnConsistentErrors() throws Exception {
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        String token = login("customer@bankflow.dev", "Customer123!");
        mockMvc.perform(post("/api/accounts/{id}/withdraw", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"999999.00\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void concurrentWithdrawalsDoNotOverdrawAccount() throws Exception {
        String token = login("customer@bankflow.dev", "Customer123!");
        UUID accountId = createAccount(token);
        mockMvc.perform(post("/api/accounts/{id}/deposit", accountId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":\"100.00\"}")).andExpect(status().isOk());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var futures = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return mockMvc.perform(post("/api/accounts/{id}/withdraw", accountId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"80.00\"}")).andReturn().getResponse().getStatus();
        })).toList();
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        long okCount = 0;
        long rejectedCount = 0;
        for (var future : futures) {
            int status = future.get(10, TimeUnit.SECONDS);
            if (status == 200) okCount++;
            if (status == 400) rejectedCount++;
        }
        executor.shutdownNow();
        assertThat(okCount).isEqualTo(1);
        assertThat(rejectedCount).isEqualTo(1);

        MvcResult balance = mockMvc.perform(get("/api/accounts/{id}", accountId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode node = objectMapper.readTree(balance.getResponse().getContentAsString());
        assertThat(new BigDecimal(node.at("/data/balance").asText())).isEqualByComparingTo("20.00");
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private UUID createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"CHECKING\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText());
    }
}
