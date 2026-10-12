package com.zzz.zerocode.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.io.Serializable;

/** 控制单页最大数量；业务层必须将 sortField 映射到字段白名单，不能直接拼接 SQL。 */
@Data
public class PageRequest implements Serializable {
    @Min(value = 1, message = "页码不能小于 1")
    private long current = 1;
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能大于 100")
    private long pageSize = 10;
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$", message = "排序字段格式不正确")
    private String sortField;
    @Pattern(regexp = "^(ascend|descend)$", message = "排序方向仅支持 ascend 或 descend")
    private String sortOrder = "descend";
}
