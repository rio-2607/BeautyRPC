package com.beautyboss.slogen.rpc.client.thrift;

/**
 * Author : Slogen
 * Date   : 2019/1/26
 */
public class ExceptionMsg {

    public static final String[] TAPP = {
            "UNKNOWN",
            "UNKNOWN_METHOD",
            "INVALID_MESSAGE_TYPE",
            "WRONG_METHOD_NAME",
            "BAD_SEQUENCE_ID",
            "MISSING_RESULT",
            "INTERNAL_ERROR",
            "PROTOCOL_ERROR",
            "INVALID_TRANSFORM",
            "INVALID_PROTOCOL",
            "UNSUPPORTED_CLIENT_TYPE"
    };

    public static final String[] TPRO = {
            "UNKNOWN",
            "INVALID_DATA",
            "NEGATIVE_SIZE",
            "SIZE_LIMIT",
            "BAD_VERSION",
            "NOT_IMPLEMENTED",
            "DEPTH_LIMIT"
    };


    public static final String[] TTRAS = {
            "UNKNOWN",
            "NOT_OPEN",
            "ALREADY_OPEN",
            "TIMED_OUT",
            "END_OF_FILE"
    };
}
