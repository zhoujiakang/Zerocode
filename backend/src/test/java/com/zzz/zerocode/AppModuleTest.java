package com.zzz.zerocode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzz.zerocode.ai.*;
import com.zzz.zerocode.ai.config.AiProperties;
import com.zzz.zerocode.ai.model.*;
import com.zzz.zerocode.ai.tools.ToolManager;
import com.zzz.zerocode.exception.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppModuleTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired AiProperties properties;
    @Autowired ToolManager tools;
    @Autowired com.zzz.zerocode.mapper.AppMapper appMapper;
    @Autowired com.zzz.zerocode.service.AppGenerationPersistence persistence;
    @MockitoBean CodeGenerationAgentFactory factory;
    private Path output;
    private String originalRoot;
    private Cookie owner;
    private long ownerId;
    private static final String HTML = "<!doctype html><html><head><meta charset='utf-8'><style>body{margin:0}</style></head><body><h1>hello</h1><script>console.log('hello')</script></body></html>";

    @BeforeEach void setup() throws Exception {
        jdbc.update("DELETE FROM chat_history"); jdbc.update("DELETE FROM app"); jdbc.update("DELETE FROM `user`");
        originalRoot = properties.getOutputRoot();
        output = Files.createTempDirectory("zerocode-app-test-"); properties.setOutputRoot(output.toString());
        owner = registerAndLogin("owner_test");
        ownerId = jdbc.queryForObject("SELECT id FROM `user` WHERE userAccount = 'owner_test'", Long.class);
    }
    @AfterEach void cleanup() throws Exception {
        properties.setOutputRoot(originalRoot);
        try (var files = Files.walk(output)) {
            for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private Cookie registerAndLogin(String account) throws Exception {
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userAccount", account, "userPassword", "StrongPass123!", "checkPassword", "StrongPass123!")))).andExpect(status().isOk());
        var response = mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("userAccount", account, "userPassword", "StrongPass123!")))).andExpect(status().isOk()).andReturn();
        String header = response.getResponse().getHeaders("Set-Cookie").stream().filter(v -> v.startsWith("zerocode-token=")).findFirst().orElseThrow();
        return new Cookie("zerocode-token", header.substring(header.indexOf('=') + 1, header.indexOf(';')));
    }
    private long create(CodeGenType mode) throws Exception {
        var response = mvc.perform(post("/app/add").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("appName", "测试应用", "initPrompt", "创建一个个人主页", "codeGenType", mode.name(), "userId", 999)))).andExpect(status().isOk()).andReturn();
        return Long.parseLong(json.readTree(response.getResponse().getContentAsString()).get("data").asText());
    }
    private AgentContext context(long id, CodeGenType mode) { return new AgentContext(ownerId, id, mode); }
    private String body(long id) { return "{\"appId\":\"" + id + "\"}"; }
    private String appStatus(long id) { return jdbc.queryForObject("SELECT status FROM app WHERE id = ?", String.class, id); }
    private int historyCount(long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM chat_history WHERE appId = ?", Integer.class, id); }
    private void generate(long id) throws Exception {
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY"));
    }
    @Test void creationRequiresLoginAndFixesOwnership() throws Exception {
        mvc.perform(post("/app/add").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isUnauthorized());
        long id = create(CodeGenType.HTML);
        assertEquals(ownerId, jdbc.queryForObject("SELECT userId FROM app WHERE id = ?", Long.class, id));
        mvc.perform(get("/app/" + id).cookie(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(Long.toString(id)))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
        mvc.perform(post("/app/list/my").cookie(owner).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }
    @Test void otherUserCannotReadGenerateOrReadHistoryAndFiles() throws Exception {
        long id = create(CodeGenType.HTML); Cookie stranger = registerAndLogin("other_test");
        mvc.perform(get("/app/" + id).cookie(stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/app/" + id + "/file").param("fileName", "index.html").cookie(stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/app/" + id + "/files").cookie(stranger)).andExpect(status().isNotFound());
        mvc.perform(post("/app/" + id + "/history").cookie(stranger).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isNotFound());
        mvc.perform(post("/app/generate").cookie(stranger).contentType(MediaType.APPLICATION_JSON).content(body(id))).andExpect(status().isNotFound());
        mvc.perform(post("/app/list/my").cookie(stranger).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.data.total").value(0));
        verifyNoInteractions(factory);
    }
    @Test void firstGenerationAndFollowupKeepFullHistoryAndFileContents() throws Exception {
        long id = create(CodeGenType.HTML); AgentContext context = context(id, CodeGenType.HTML);
        when(factory.getAgent(context)).thenReturn(message -> {
            var workspace = tools.getWorkspace(context);
            if (message.equals("创建一个个人主页")) workspace.write("index.html", HTML);
            else workspace.modify("index.html", "<h1>hello</h1>", "<h1>你好</h1>");
            return "已完成：" + message;
        });
        generate(id);
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("appId", id, "message", "改成中文标题")))).andExpect(status().isOk());
        assertEquals(4, historyCount(id));
        mvc.perform(post("/app/" + id + "/history").cookie(owner).contentType(MediaType.APPLICATION_JSON).content("{\"pageSize\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.records[0].role").value("user"))
                .andExpect(jsonPath("$.data.records[0].content").value("创建一个个人主页"))
                .andExpect(jsonPath("$.data.records[1].role").value("assistant"));
        mvc.perform(get("/app/" + id + "/file").cookie(owner).param("fileName", "index.html"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(HTML.replace("<h1>hello</h1>", "<h1>你好</h1>")));
        mvc.perform(get("/app/" + id + "/files").cookie(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0]").value("index.html"));
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id))).andExpect(status().isBadRequest());
    }
    @Test void multiFileGenerationRequiresAllThreeFilesAndReferences() throws Exception {
        long id = create(CodeGenType.MULTI_FILE); AgentContext context = context(id, CodeGenType.MULTI_FILE);
        when(factory.getAgent(context)).thenReturn(message -> {
            var workspace = tools.getWorkspace(context);
            workspace.write("index.html", "<!doctype html><html><head><link rel='stylesheet' href='./style.css'></head><body><script src='script.js'></script></body></html>");
            workspace.write("style.css", "body { margin: 0; }"); workspace.write("script.js", "console.log('hi');"); return "已生成三文件";
        });
        generate(id);
        mvc.perform(get("/app/" + id + "/files").cookie(owner)).andExpect(jsonPath("$.data.length()").value(3));
    }
    @Test void missingFilesFailAndCanBeRetriedWithNewMessage() throws Exception {
        long id = create(CodeGenType.MULTI_FILE); AgentContext context = context(id, CodeGenType.MULTI_FILE);
        when(factory.getAgent(context)).thenReturn(message -> "已生成");
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id))).andExpect(status().isInternalServerError());
        assertEquals("FAILED", appStatus(id)); assertEquals(2, historyCount(id));
        when(factory.getAgent(context)).thenReturn(message -> {
            var workspace = tools.getWorkspace(context);
            workspace.write("index.html", "<!doctype html><html><head><link href='style.css' rel='stylesheet'></head><body><script src='./script.js'></script></body></html>");
            workspace.write("style.css", "body{}"); workspace.write("script.js", "// no interaction"); return "已补齐";
        });
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                Map.of("appId", id, "message", "重新生成完整文件")))).andExpect(status().isOk());
        assertEquals("READY", appStatus(id)); assertEquals(4, historyCount(id));
    }
    @Test void unconfiguredAiDoesNotClaimAppOrWriteHistory() throws Exception {
        long id = create(CodeGenType.HTML);
        when(factory.getAgent(any())).thenThrow(new BusinessException(ErrorCode.AI_NOT_CONFIGURED));
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id))).andExpect(status().isServiceUnavailable());
        assertEquals("CREATED", appStatus(id)); assertEquals(0, historyCount(id));
    }
    @Test void providerFailureStoresSafeErrorAndReleasesApp() throws Exception {
        long id = create(CodeGenType.HTML);
        when(factory.getAgent(any())).thenReturn(message -> { throw new IllegalStateException("secret-provider-key"); });
        mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id)))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("生成失败，请稍后重试"));
        assertEquals("FAILED", appStatus(id)); assertEquals(2, historyCount(id));
        assertEquals("生成失败，请稍后重试", jdbc.queryForObject("SELECT lastError FROM app WHERE id = ?", String.class, id));
    }
    @Test void concurrentGenerationIsRejectedWithoutDuplicateHistory() throws Exception {
        long id = create(CodeGenType.HTML); AgentContext context = context(id, CodeGenType.HTML);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(factory.getAgent(context)).thenReturn(message -> {
            entered.countDown();
            try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { throw new IllegalStateException(e); }
            tools.getWorkspace(context).write("index.html", HTML); return "已完成";
        });
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = executor.submit(() -> { try { generate(id); } catch (Exception e) { throw new RuntimeException(e); } });
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id)))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40900));
            assertEquals(1, historyCount(id)); release.countDown(); first.get(10, TimeUnit.SECONDS);
            assertEquals(2, historyCount(id)); assertEquals("READY", appStatus(id));
        } finally { release.countDown(); executor.shutdownNow(); }
    }
    @Test void atomicDatabaseClaimAllowsOnlyOneStarter() throws Exception {
        long id = create(CodeGenType.HTML);
        var app = appMapper.selectById(id);
        CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> starter = () -> {
            ready.countDown(); assertTrue(go.await(10, TimeUnit.SECONDS));
            try { persistence.start(app, "首次生成"); return true; }
            catch (BusinessException e) { assertEquals(ErrorCode.APP_GENERATING, e.getErrorCode()); return false; }
        };
        try {
            Future<Boolean> first = executor.submit(starter), second = executor.submit(starter);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); go.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals("GENERATING", appStatus(id)); assertEquals(1, historyCount(id));
            persistence.finish(app, false, "测试终止"); assertEquals("FAILED", appStatus(id));
        } finally { go.countDown(); executor.shutdownNow(); }
    }
    @Test void incompleteDocumentAndMissingReferencesCannotBecomeReady() throws Exception {
        for (String html : new String[]{"<h1>片段</h1>", HTML}) {
            long id = create(CodeGenType.MULTI_FILE); AgentContext context = context(id, CodeGenType.MULTI_FILE);
            when(factory.getAgent(context)).thenReturn(message -> {
                var workspace = tools.getWorkspace(context);
                workspace.write("index.html", html); workspace.write("style.css", "body{}"); workspace.write("script.js", "// empty");
                return "已完成";
            });
            mvc.perform(post("/app/generate").cookie(owner).contentType(MediaType.APPLICATION_JSON).content(body(id)))
                    .andExpect(status().isInternalServerError());
            assertEquals("FAILED", appStatus(id));
        }
    }
    @Test void invalidRequestsAndTraversalAreRejected() throws Exception {
        mvc.perform(post("/app/add").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"appName\":\"test\",\"initPrompt\":\"hello\",\"codeGenType\":\"VUE\"}" )).andExpect(status().isBadRequest());
        long id = create(CodeGenType.HTML);
        mvc.perform(get("/app/" + id + "/file").cookie(owner).param("fileName", "../../application-local.yml")).andExpect(status().isBadRequest());
        mvc.perform(get("/app/" + id + "/file").cookie(owner).param("fileName", "index.html")).andExpect(status().isNotFound());
        mvc.perform(post("/app/list/my").cookie(owner).contentType(MediaType.APPLICATION_JSON).content("{\"current\":1000001}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/app/" + id + "/history").cookie(owner).contentType(MediaType.APPLICATION_JSON).content("{\"pageSize\":101}" )).andExpect(status().isBadRequest());
    }
}
