package com.zzz.zerocode.ai.tools;

import com.zzz.zerocode.ai.model.AgentContext;
import com.zzz.zerocode.ai.model.CodeGenType;
import dev.langchain4j.exception.ToolErrorVisibleToLlm;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 每个工具实例固定绑定应用目录，路径不由模型指定。 */
public final class AppWorkspace {
    private final Path root;
    private final List<String> allowedFiles;
    private final long maxFileBytes;

    public AppWorkspace(Path outputRoot, AgentContext context, long maxFileBytes) {
        if (outputRoot == null || context == null || maxFileBytes < 1 || maxFileBytes > Integer.MAX_VALUE)
            throw new IllegalArgumentException("应用目录或文件大小限制不正确");
        this.root = outputRoot.toAbsolutePath().normalize().resolve(Long.toString(context.userId()))
                .resolve(Long.toString(context.appId())).resolve(context.mode().name().toLowerCase(Locale.ROOT));
        this.allowedFiles = context.mode() == CodeGenType.HTML ? List.of("index.html")
                : List.of("index.html", "style.css", "script.js");
        this.maxFileBytes = maxFileBytes;
    }
    public Path directory() { return root; }
    public synchronized String read(String name) {
        Path path = file(name);
        try {
            verifyDirectory(false);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw error("文件不存在：" + name);
            if (Files.size(path) > maxFileBytes) throw error("文件超过读取大小限制");
            try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                long size = channel.size();
                if (size > maxFileBytes) throw error("文件超过读取大小限制");
                ByteBuffer buffer = ByteBuffer.allocate((int) size);
                while (buffer.hasRemaining() && channel.read(buffer) != -1) {}
                buffer.flip();
                return StandardCharsets.UTF_8.decode(buffer).toString();
            }
        } catch (IOException e) { throw error("读取失败，请检查文件是否存在"); }
    }
    public synchronized void write(String name, String content) {
        Path target = file(name);
        if (content == null || content.isBlank()) throw error("文件内容不能为空");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxFileBytes) throw error("文件超过大小限制，请精简源码");
        Path temporary = null;
        try {
            verifyDirectory(true);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
                throw error("目标必须是普通文件");
            temporary = Files.createTempFile(root, ".writing-", ".tmp");
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            // 临时文件与目标处于同一目录，成功替换前保留旧文件。
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { throw error("写入失败，原文件未完成替换，请重试"); }
        finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }
    public synchronized String modify(String name, String oldText, String newText) {
        if (oldText == null || oldText.isEmpty() || newText == null) throw error("原文本不能为空，新文本不能为 null");
        String content = read(name);
        int first = content.indexOf(oldText);
        if (first < 0) throw error("未找到原文本，请先读取文件确认内容");
        if (content.indexOf(oldText, first + 1) >= 0) throw error("原文本匹配多处，请提供唯一的上下文");
        write(name, content.substring(0, first) + newText + content.substring(first + oldText.length()));
        return "已修改 " + name;
    }
    public synchronized String list() {
        try {
            verifyDirectory(false);
            List<String> files = new ArrayList<>();
            for (String name : allowedFiles) {
                Path path = file(name);
                if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw error("应用目录包含非普通文件");
                    files.add(name + " (" + Files.size(path) + " bytes)");
                }
            }
            return files.isEmpty() ? "应用目录暂无文件" : String.join("\n", files);
        } catch (IOException e) { throw error("无法读取应用目录"); }
    }
    private Path file(String name) {
        if (name == null || !allowedFiles.contains(name)) throw error("文件名不允许，当前模式仅支持：" + String.join(", ", allowedFiles));
        return root.resolve(name);
    }
    private void verifyDirectory(boolean create) throws IOException {
        // 从用户/应用隔离层开始检查，拒绝通过符号链接切换到其他目录。
        Path base = root.getParent().getParent().getParent();
        if (Files.isSymbolicLink(base)) throw error("代码输出根目录不能是符号链接");
        if (create) Files.createDirectories(base);
        for (Path path : List.of(base, root.getParent().getParent(), root.getParent(), root)) {
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw error("应用目录必须是普通目录");
            } else if (create) {
                try { Files.createDirectory(path); }
                catch (FileAlreadyExistsException ignored) { /* 其他应用可能同时创建共用的用户目录。 */ }
                if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw error("应用目录必须是普通目录");
            }
        }
    }
    private RuntimeException error(String message) { return ToolErrorVisibleToLlm.from(message); }
}
