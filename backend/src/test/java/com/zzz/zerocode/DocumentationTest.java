package com.zzz.zerocode;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DocumentationTest {
    @Autowired MockMvc mvc;
    @Test void groupedOpenApiCanBeGenerated() throws Exception {
        mvc.perform(get("/v3/api-docs/default")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/health']").exists());
    }
    @Test void requestLogAspectAddsTraceHeader() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", not(emptyString())));
    }
}
