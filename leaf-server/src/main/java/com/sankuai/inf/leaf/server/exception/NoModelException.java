package com.sankuai.inf.leaf.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 非支持模式异常
 * @author jiangyx3915
 */
@ResponseStatus(code=HttpStatus.INTERNAL_SERVER_ERROR,reason="Not Support this mode")
public class NoModelException extends RuntimeException {
}
