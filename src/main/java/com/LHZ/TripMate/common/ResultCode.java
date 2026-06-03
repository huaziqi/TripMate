package com.LHZ.TripMate.common;

import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
public enum ResultCode {

    // ---- 通用 ----
    SUCCESS(200, "操作成功"),
    FAIL(400, "请求失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    // ---- 用户模块 ----
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户名已存在"),

    // ---- 参数校验 ----
    PARAM_ERROR(2001, "参数错误");

    private final int    code;
    private final String message;

    ResultCode(int code, String message) {
        this.code    = code;
        this.message = message;
    }
}
