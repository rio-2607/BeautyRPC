package com.beautyboss.slogen.rpc.exceptions;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = -5733726019795660634L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable e) {
        super(message, e);
    }
}
