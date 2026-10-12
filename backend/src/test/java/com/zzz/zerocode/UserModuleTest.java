package com.zzz.zerocode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzz.zerocode.mapper.UserMapper;
import com.zzz.zerocode.model.entity.User;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserModuleTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserMapper userMapper;
    private static final String PASSWORD = "StrongPass123!";
    @BeforeEach void resetUsers() { jdbc.update("DELETE FROM `user`"); }
    private String registration(String account, String password, String confirmation) throws Exception {
        return mapper.writeValueAsString(java.util.Map.of("userAccount", account, "userPassword", password, "checkPassword", confirmation));
    }
    private long register(String account) throws Exception {
        var result = mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(registration(account, PASSWORD, PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        return Long.parseLong(mapper.readTree(result.getResponse().getContentAsString()).get("data").asText());
    }
    private Cookie login(String account) throws Exception {
        var result = mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of("userAccount", account, "userPassword", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userPassword").doesNotExist()).andReturn();
        String header = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith("zerocode-token=")).findFirst().orElseThrow();
        assertTrue(header.toLowerCase().contains("httponly"));
        assertTrue(header.contains("SameSite=Lax"));
        return new Cookie("zerocode-token", header.substring(header.indexOf('=') + 1, header.indexOf(';')));
    }
    private Cookie admin() throws Exception {
        long id = register("administrator");
        jdbc.update("UPDATE `user` SET userRole = 'admin' WHERE id = ?", id);
        return login("administrator");
    }
    @Test void databaseMaintainsTimestampsWithoutJavaFill() {
        LocalDateTime oldTime = LocalDateTime.of(2000, 1, 1, 0, 0);
        User user = new User();
        user.setUserAccount("time_check");
        user.setUserPassword(passwordEncoder.encode(PASSWORD));
        user.setUserRole("user");
        user.setIsDelete(0);
        user.setCreateTime(oldTime);
        user.setEditTime(oldTime);
        user.setUpdateTime(oldTime);
        userMapper.insert(user);
        User saved = userMapper.selectById(user.getId());
        assertTrue(saved.getCreateTime().isAfter(oldTime));
        assertTrue(saved.getEditTime().isAfter(oldTime));
        jdbc.update("UPDATE `user` SET editTime = ?, updateTime = ? WHERE id = ?",
                Timestamp.valueOf(oldTime), Timestamp.valueOf(oldTime), user.getId());
        assertEquals(1, userMapper.deleteById(user.getId()));
        assertTrue(jdbc.queryForObject("SELECT updateTime FROM `user` WHERE id = ?", Timestamp.class, user.getId()).toLocalDateTime().isAfter(oldTime));
        assertTrue(jdbc.queryForObject("SELECT editTime FROM `user` WHERE id = ?", Timestamp.class, user.getId()).toLocalDateTime().isAfter(oldTime));
        assertEquals(saved.getCreateTime(), jdbc.queryForObject("SELECT createTime FROM `user` WHERE id = ?", Timestamp.class, user.getId()).toLocalDateTime());
    }
    @Test void registrationHashesPasswordAndAlwaysUsesUserRole() throws Exception {
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"Alice\",\"userPassword\":\"StrongPass123!\",\"checkPassword\":\"StrongPass123!\",\"userRole\":\"admin\"}"))
                .andExpect(status().isOk());
        var row = jdbc.queryForMap("SELECT * FROM `user` WHERE userAccount = 'alice'");
        assertEquals("user", row.get("userrole"));
        assertNotEquals(PASSWORD, row.get("userpassword"));
        assertTrue(passwordEncoder.matches(PASSWORD, row.get("userpassword").toString()));
    }
    @Test void duplicateAccountIsRejectedCaseInsensitively() throws Exception {
        register("alice");
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(registration("ALICE", PASSWORD, PASSWORD)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40001));
    }
    @Test void passwordMismatchIsRejected() throws Exception {
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(registration("alice", PASSWORD, "Different123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }
    @Test void invalidAccountAndShortPasswordAreRejected() throws Exception {
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(registration("a b", "short", "short")))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class));
    }
    @Test void bcryptOversizedUtf8PasswordIsRejected() throws Exception {
        String password = "中".repeat(25);
        mvc.perform(post("/user/register").contentType(MediaType.APPLICATION_JSON).content(registration("alice", password, password)))
                .andExpect(status().isBadRequest());
    }
    @Test void loginCookieRestoresCurrentUserWithoutPassword() throws Exception {
        long id = register("alice"); Cookie cookie = login("ALICE");
        mvc.perform(get("/user/get/login").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(Long.toString(id)))
                .andExpect(jsonPath("$.data.userAccount").value("alice"))
                .andExpect(jsonPath("$.data.userPassword").doesNotExist());
    }
    @Test void wrongPasswordIsRejected() throws Exception {
        register("alice");
        mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"alice\",\"userPassword\":\"WrongPass123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40002));
    }
    @Test void logoutRevokesCookieAndIsIdempotent() throws Exception {
        register("alice"); Cookie cookie = login("alice");
        mvc.perform(post("/user/logout").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/user/get/login").cookie(cookie)).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/logout")).andExpect(status().isOk());
    }
    @Test void guestCannotQueryCurrentUserOrManageUsers() throws Exception {
        mvc.perform(get("/user/get/login")).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/list/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void normalUserCannotSearchOrDeleteUsers() throws Exception {
        long id = register("alice"); Cookie cookie = login("alice");
        mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40300));
        mvc.perform(post("/user/delete").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + id + "}"))
                .andExpect(status().isForbidden());
    }
    @Test void adminCanSearchWithPaginationAndRoleFilter() throws Exception {
        Cookie cookie = admin(); register("alice"); register("bobby");
        mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"ali\",\"userRole\":\"user\",\"current\":1,\"pageSize\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("alice"))
                .andExpect(jsonPath("$.data.records[0].userPassword").doesNotExist());
    }
    @Test void searchTreatsWildcardCharactersLiterally() throws Exception {
        Cookie cookie = admin();
        long id = register("wild_card");
        register("wildXcard");
        jdbc.update("UPDATE `user` SET userName = ? WHERE id = ?", "100% ready", id);
        mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userAccount\":\"wild_card\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userAccount").value("wild_card"));
        mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userName\":\"%\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }
    @Test void deletingUserRevokesEverySessionAndPreventsRelogin() throws Exception {
        Cookie admin = admin(); long id = register("alice"); Cookie first = login("alice"); Cookie second = login("alice");
        mvc.perform(post("/user/delete").cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + id + "}"))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT isDelete FROM `user` WHERE id = ?", Integer.class, id));
        mvc.perform(get("/user/get/login").cookie(first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/user/get/login").cookie(second)).andExpect(status().isUnauthorized());
        mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("userAccount", "alice", "userPassword", PASSWORD))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/user/list/page").cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{\"userAccount\":\"alice\"}"))
                .andExpect(jsonPath("$.data.total").value(0));
    }
    @Test void adminCannotDeleteSelfAndMissingUserReturns404() throws Exception {
        Cookie cookie = admin(); long id = jdbc.queryForObject("SELECT id FROM `user` WHERE userAccount = 'administrator'", Long.class);
        mvc.perform(post("/user/delete").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"id\":" + id + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/user/delete").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"id\":999999}"))
                .andExpect(status().isNotFound());
    }
    @Test void changedAdminRoleTakesEffectOnExistingSession() throws Exception {
        Cookie cookie = admin(); jdbc.update("UPDATE `user` SET userRole = 'user' WHERE userAccount = 'administrator'");
        mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
    @Test void oversizedPageIsRejected() throws Exception {
        Cookie cookie = admin();
        for (String body : new String[]{"{\"pageSize\":101}", "{\"current\":9223372036854775807}"}) {
            mvc.perform(post("/user/list/page").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

}
