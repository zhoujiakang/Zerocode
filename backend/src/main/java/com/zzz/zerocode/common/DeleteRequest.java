package com.zzz.zerocode.common;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    @NotNull(message = "id 不能为空")
    @Positive(message = "id 必须大于 0")
    private Long id;
}
