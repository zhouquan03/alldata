package com.platform.dts.admin.service;

import com.platform.dts.admin.dto.FlinkXJsonBuildDto;

/**
 * com.guoliang.flinkx json构建服务层接口
 *
 * @author AllDataDC
 * @version 1.0
 * @since 2022/11/1
 */
public interface FlinkxJsonService {

    /**
     * build flinkx json
     *
     * @param dto
     * @return
     */
    String buildJobJson(FlinkXJsonBuildDto dto);
}