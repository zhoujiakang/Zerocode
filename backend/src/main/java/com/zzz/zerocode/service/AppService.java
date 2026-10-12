package com.zzz.zerocode.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzz.zerocode.ai.CodeGenerationAgentFactory;
import com.zzz.zerocode.ai.model.*;
import com.zzz.zerocode.ai.tools.*;
import com.zzz.zerocode.common.*;
import com.zzz.zerocode.exception.*;
import com.zzz.zerocode.mapper.*;
import com.zzz.zerocode.model.dto.app.*;
import com.zzz.zerocode.model.entity.*;
import com.zzz.zerocode.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {
    private final UserService userService;
    private final AppMapper appMapper;
    private final ChatHistoryMapper historyMapper;
    private final AppGenerationPersistence persistence;
    private final CodeGenerationAgentFactory agentFactory;
    private final GeneratedFilesValidator validator;
    private final ToolManager toolManager;

    public String create(AppCreateRequest request) {
        App app = new App();
        app.setUserId(userId()); app.setAppName(request.getAppName().trim());
        app.setInitPrompt(request.getInitPrompt().trim());
        app.setCodeGenType(request.getCodeGenType().name()); app.setStatus("CREATED");
        ThrowUtils.throwIf(appMapper.insert(app) != 1, ErrorCode.OPERATION_ERROR);
        return app.getId().toString();
    }
    public AppVO get(long id) { return AppVO.from(owned(id)); }
    public PageResponse<AppVO> list(PageRequest request) {
        checkPage(request);
        var page = appMapper.selectPage(new Page<App>(request.getCurrent(), request.getPageSize()),
                new LambdaQueryWrapper<App>().eq(App::getUserId, userId()).orderByDesc(App::getId));
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords().stream().map(AppVO::from).toList());
    }
    public PageResponse<ChatHistoryVO> history(long id, PageRequest request) {
        App app = owned(id); checkPage(request);
        var page = historyMapper.selectPage(new Page<ChatHistory>(request.getCurrent(), request.getPageSize()),
                new LambdaQueryWrapper<ChatHistory>().eq(ChatHistory::getAppId, app.getId())
                        .eq(ChatHistory::getUserId, app.getUserId()).orderByAsc(ChatHistory::getId));
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords().stream().map(ChatHistoryVO::from).toList());
    }
    public AppVO generate(AppGenerateRequest request) {
        App app = owned(request.getAppId());
        ThrowUtils.throwIf("GENERATING".equals(app.getStatus()), ErrorCode.APP_GENERATING);
        String message = request.getMessage();
        if (message == null && "CREATED".equals(app.getStatus())) message = app.getInitPrompt();
        ThrowUtils.throwIf(message == null || message.isBlank(), ErrorCode.PARAMS_ERROR, "继续修改或重试时请提供描述");
        AgentContext context = context(app);
        // 配置异常不创建历史，也不将应用置为生成中。
        var agent = agentFactory.getAgent(context);
        persistence.start(app, message.trim());
        try {
            String summary = agent.generate(message.trim());
            ThrowUtils.throwIf(summary == null || summary.isBlank(), ErrorCode.OPERATION_ERROR, "模型未返回生成结果");
            validator.validate(context);
            persistence.finish(app, true, summary);
        } catch (RuntimeException e) {
            String failure = e instanceof BusinessException business ? business.getMessage() : "生成失败，请稍后重试";
            try { persistence.finish(app, false, failure); }
            catch (RuntimeException storageFailure) { e.addSuppressed(storageFailure); }
            // 服务商异常可能携带请求信息，不将其消息或响应正文写入日志。
            log.warn("应用生成失败 appId={}, exceptionType={}, persistenceFailed={}",
                    app.getId(), e.getClass().getSimpleName(), e.getSuppressed().length > 0);
            if (e instanceof BusinessException business) throw business;
            throw new BusinessException(ErrorCode.OPERATION_ERROR, failure);
        }
        return AppVO.from(appMapper.selectById(app.getId()));
    }
    public List<String> files(long id) { return validator.validate(context(owned(id))); }
    public String readFile(long id, String name) {
        App app = owned(id);
        List<String> allowed = context(app).mode() == CodeGenType.HTML ? List.of("index.html") : List.of("index.html", "style.css", "script.js");
        ThrowUtils.throwIf(!allowed.contains(name), ErrorCode.PARAMS_ERROR, "不支持的文件名");
        try { return toolManager.getWorkspace(context(app)).read(name); }
        catch (RuntimeException e) { throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在或不可读取"); }
    }
    private App owned(long id) {
        long userId = userId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appMapper.selectOne(new LambdaQueryWrapper<App>().eq(App::getId, id).eq(App::getUserId, userId));
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在或无权访问");
        return app;
    }
    private long userId() { return userService.getLoginUser().id(); }
    private AgentContext context(App app) { return new AgentContext(app.getUserId(), app.getId(), CodeGenType.valueOf(app.getCodeGenType())); }
    private void checkPage(PageRequest request) { ThrowUtils.throwIf(request.getCurrent() > 1000000, ErrorCode.PARAMS_ERROR, "页码过大"); }
}
