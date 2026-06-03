package com.LHZ.TripMate.common;

import lombok.Data;

/**
 * 统一 API 响应体
 *
 * <p>与前端 useApi.ts 中的 ApiResponse&lt;T&gt; 结构对应：
 * <pre>
 * {
 *   "code":    200,
 *   "message": "操作成功",
 *   "data":    { ... }
 * }
 * </pre>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，其余见 {@link ResultCode} */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    // ----------------------------------------------------------------
    // 私有构造，统一通过静态工厂方法创建
    // ----------------------------------------------------------------

    private Result(int code, String message, T data) {
        this.code    = code;
        this.message = message;
        this.data    = data;
    }

    // ----------------------------------------------------------------
    // 成功响应
    // ----------------------------------------------------------------

    /** 成功，无数据 */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功，携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(), data);
    }

    /** 成功，自定义提示信息 + 数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ----------------------------------------------------------------
    // 失败响应
    // ----------------------------------------------------------------

    /** 失败，使用预定义错误码 */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(),
                resultCode.getMessage(), null);
    }

    /** 失败，自定义提示信息 */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /** 失败，自定义状态码 + 提示信息 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
