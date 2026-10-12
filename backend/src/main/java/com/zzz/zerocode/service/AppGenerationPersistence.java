package com.zzz.zerocode.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zzz.zerocode.exception.*;
import com.zzz.zerocode.mapper.*;
import com.zzz.zerocode.model.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 短事务只包围状态和历史写入，不在模型调用期间持有数据库事务。 */
@Service
@RequiredArgsConstructor
public class AppGenerationPersistence {
    private final AppMapper appMapper;
    private final ChatHistoryMapper historyMapper;

    @Transactional
    public void start(App app, String message) {
        int updated = appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, app.getId()).eq(App::getUserId, app.getUserId())
                .ne(App::getStatus, "GENERATING")
                .set(App::getStatus, "GENERATING").set(App::getLastError, null));
        ThrowUtils.throwIf(updated != 1, ErrorCode.APP_GENERATING);
        append(app, "user", message);
    }

    @Transactional
    public void finish(App app, boolean success, String content) {
        append(app, success ? "assistant" : "error", content);
        int updated = appMapper.update(null, new LambdaUpdateWrapper<App>()
                .eq(App::getId, app.getId()).eq(App::getUserId, app.getUserId())
                .eq(App::getStatus, "GENERATING")
                .set(App::getStatus, success ? "READY" : "FAILED")
                .set(App::getLastError, success ? null : content));
        ThrowUtils.throwIf(updated != 1, ErrorCode.OPERATION_ERROR);
    }

    private void append(App app, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setAppId(app.getId()); history.setUserId(app.getUserId());
        history.setRole(role); history.setContent(content);
        ThrowUtils.throwIf(historyMapper.insert(history) != 1, ErrorCode.OPERATION_ERROR);
    }
}
