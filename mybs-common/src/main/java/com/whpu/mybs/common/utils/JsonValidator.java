package com.whpu.mybs.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;

/**
 * JSON 格式校验工具类
 */
public final class JsonValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonValidator() {} // 防止实例化

    /**
     * 判断字符串是否为合法的 JSON 格式。
     * 注：null、空字符串或仅空白字符串视为非法。
     * @param json 待校验的 JSON 字符串
     * @return true 表示合法，false 表示非法
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return true;          // 可根据业务需要改为 true（若允许空值）
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 校验 JSON 字符串，若不合法则抛出业务异常。
     * @param json 待校验的 JSON 字符串
     * @throws BusinessException 当格式非法时抛出
     */
    public static void validateJson(String json) throws BusinessException {
        if (!isValidJson(json)) {
            throw new BusinessException("json格式不合法");
        }
    }
}