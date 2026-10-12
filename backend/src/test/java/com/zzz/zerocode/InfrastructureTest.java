package com.zzz.zerocode;

import com.zzz.zerocode.common.DeleteRequest;
import com.zzz.zerocode.common.PageRequest;
import com.zzz.zerocode.config.CorsConfig;
import com.zzz.zerocode.controller.HealthController;
import com.zzz.zerocode.exception.*;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {HealthController.class, InfrastructureTest.TestController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173")
@Import({GlobalExceptionHandler.class, CorsConfig.class, InfrastructureTest.TestController.class})
class InfrastructureTest {
    @Autowired MockMvc mvc;
    @Test void healthUsesResponseEnvelope() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.status").value("UP"));
    }
    @Test void invalidDeleteRequestReturns400() throws Exception {
        mvc.perform(post("/test/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }
    @Test void oversizedPageReturns400() throws Exception {
        mvc.perform(post("/test/page").contentType(MediaType.APPLICATION_JSON).content("{\"pageSize\":101}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }
    @Test void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/test/delete").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }
    @Test void businessExceptionRetainsCodeAndStatus() throws Exception {
        mvc.perform(get("/test/business")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
    }
    @Test void unexpectedExceptionDoesNotExposeInternalMessage() throws Exception {
        mvc.perform(get("/test/unexpected")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("系统内部异常"));
    }
    @Test void corsAllowsConfiguredOrigin() throws Exception {
        mvc.perform(options("/health").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test void corsRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/health").header("Origin", "https://unknown.example")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }
    @Test void missingResourceReturnsWrapped404() throws Exception {
        mvc.perform(get("/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(40400));
    }
    @RestController
    static class TestController {
        @PostMapping("/test/delete") void delete(@Valid @RequestBody DeleteRequest request) {}
        @PostMapping("/test/page") void page(@Valid @RequestBody PageRequest request) {}
        @GetMapping("/test/business") void business() { ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR); }
        @GetMapping("/test/unexpected") void unexpected() { throw new IllegalStateException("private internal detail"); }
    }
}
