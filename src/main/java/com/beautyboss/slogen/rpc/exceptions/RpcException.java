package com.beautyboss.slogen.rpc.exceptions;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class RpcException extends RuntimeException {
    private static final long serialVersionUID = -2275296727467192665L;
    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable e) {
        super(message, e);
    }
}
