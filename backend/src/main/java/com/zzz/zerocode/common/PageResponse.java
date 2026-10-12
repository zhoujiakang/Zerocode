package com.zzz.zerocode.common;

import java.util.List;

public record PageResponse<T>(long current, long pageSize, long total, List<T> records) {}
