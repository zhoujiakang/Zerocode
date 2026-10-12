package com.zzz.zerocode.ai.tools;

import com.zzz.zerocode.ai.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class AppWorkspaceTest {
    @TempDir Path directory;
    private AppWorkspace workspace(long userId, long appId, CodeGenType mode) {
        return new AppWorkspace(directory.resolve("generated"), new AgentContext(userId, appId, mode), 1024);
    }
    @Test void nativeFilesCanBeWrittenReadListedAndPreciselyModified() throws Exception {
        var workspace = workspace(1, 1, CodeGenType.MULTI_FILE);
        workspace.write("index.html", "<h1>Hello</h1>");
        workspace.write("style.css", "h1 { color: red; }");
        workspace.write("script.js", "console.log('hello');");
        assertTrue(workspace.list().contains("index.html"));
        assertTrue(workspace.list().contains("style.css"));
        assertTrue(workspace.list().contains("script.js"));
        workspace.modify("index.html", "Hello", "你好");
        assertEquals("<h1>你好</h1>", workspace.read("index.html"));
        workspace.modify("index.html", "你好", "");
        assertEquals("<h1></h1>", workspace.read("index.html"));
        try (var files = Files.list(workspace.directory())) { assertEquals(3, files.count()); }
    }
    @Test void htmlModeRejectsOtherFilesAndPathTraversal() {
        var workspace = workspace(1, 1, CodeGenType.HTML);
        for (String file : new String[]{"style.css", "script.js", "../index.html", "/tmp/index.html", "sub/index.html", "index.html/", ".env"}) {
            assertThrows(RuntimeException.class, () -> workspace.write(file, "content"));
        }
        assertFalse(Files.exists(workspace.directory()));
    }
    @Test void filesAreIsolatedByUserApplicationAndMode() {
        var original = workspace(1, 1, CodeGenType.HTML);
        original.write("index.html", "original");
        for (var other : new AppWorkspace[]{workspace(2, 1, CodeGenType.HTML), workspace(1, 2, CodeGenType.HTML), workspace(1, 1, CodeGenType.MULTI_FILE)}) {
            assertEquals("应用目录暂无文件", other.list());
            assertThrows(RuntimeException.class, () -> other.read("index.html"));
            other.write("index.html", "other");
        }
        assertEquals("original", original.read("index.html"));
    }
    @Test void ambiguousOrMissingReplacementDoesNotChangeFile() {
        var workspace = workspace(1, 1, CodeGenType.HTML);
        workspace.write("index.html", "Hello Hello");
        assertThrows(RuntimeException.class, () -> workspace.modify("index.html", "Hello", "Goodbye"));
        assertThrows(RuntimeException.class, () -> workspace.modify("index.html", "missing", "Goodbye"));
        assertEquals("Hello Hello", workspace.read("index.html"));
    }
    @Test void utf8LimitAndBlankContentDoNotOverwriteSavedFile() {
        var workspace = new AppWorkspace(directory.resolve("generated"), new AgentContext(1, 1, CodeGenType.HTML), 5);
        workspace.write("index.html", "Hello");
        assertThrows(RuntimeException.class, () -> workspace.write("index.html", "中文"));
        assertThrows(RuntimeException.class, () -> workspace.write("index.html", " "));
        assertEquals("Hello", workspace.read("index.html"));
    }
    @Test void differentApplicationsCanCreateTheirSharedUserDirectoryConcurrently() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>();
            for (int appId = 1; appId <= 8; appId++) {
                var workspace = workspace(1, appId, CodeGenType.HTML);
                futures.add(executor.submit(() -> {
                    start.await(); workspace.write("index.html", "Hello"); return null;
                }));
            }
            start.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            for (int appId = 1; appId <= 8; appId++) assertEquals("Hello", workspace(1, appId, CodeGenType.HTML).read("index.html"));
        }
    }
    @Test void symlinkFileAndApplicationDirectoryCannotEscapeWorkspace() throws Exception {
        var workspace = workspace(1, 1, CodeGenType.HTML);
        workspace.write("index.html", "original");
        Path outside = directory.resolve("outside.html"); Files.writeString(outside, "private");
        Files.delete(workspace.directory().resolve("index.html"));
        Files.createSymbolicLink(workspace.directory().resolve("index.html"), outside);
        assertThrows(RuntimeException.class, () -> workspace.read("index.html"));
        assertThrows(RuntimeException.class, () -> workspace.write("index.html", "overwrite"));
        assertEquals("private", Files.readString(outside));
        var other = workspace(1, 2, CodeGenType.HTML);
        Path externalDirectory = directory.resolve("external"); Files.createDirectory(externalDirectory);
        Files.createSymbolicLink(other.directory().getParent(), externalDirectory);
        assertThrows(RuntimeException.class, () -> other.write("index.html", "overwrite"));
        assertFalse(Files.exists(externalDirectory.resolve("html/index.html")));
    }
}
