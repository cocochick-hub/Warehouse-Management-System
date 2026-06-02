package com.example.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应格式
 * 所有控制器返回值均使用此包装，保证前后端交互格式一致
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /** 状态码：200-成功 / 401-未认证 / 403-无权限 / 500-服务器错误 */
    private int code;

    /** 提示消息 */
    private String message;

    /** 返回数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    // ==================== 快捷工厂方法 ====================

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> unauthorized(String message) {
        return new ApiResult<>(401, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> forbidden(String message) {
        return new ApiResult<>(403, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> serverError(String message) {
        return new ApiResult<>(500, message, null, System.currentTimeMillis());
    }
}
