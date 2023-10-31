package com.shr.translationtoolservice.util;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName ExceptionUtils
 * @USER: Cola
 * @Date 2023/9/19 0019 8:33
 **/
public class ExceptionUtils   extends RuntimeException{
    private static final Logger logger = LoggerFactory.getLogger(ExceptionUtils.class);


    private static final long serialVersionUID = 1L;

    private String code = "";

    private String msg = "";

    //使用super(msg, cause)将异常对象组装成链存储起来
    public ExceptionUtils(String code, String msg, Throwable cause) {
        super(msg, cause);
        this.setCode(code);
        this.setMsg(msg);
    }

    /**
     *
     */
    public ExceptionUtils() {
        super();
    }
    public ExceptionUtils(String message) {
        super(message);
    }

    /**
     *
     * @param message
     *          消息
     * @param cause
     *          原因
     */
    public ExceptionUtils(String message, Throwable cause) {
        super(message, cause);
        this.setMsg(msg);
    }

    /**
     *
     * @param code
     * @param msg
     */
    public ExceptionUtils(String code, String msg) {
        super(msg);
        this.setCode(code);
        this.setMsg(msg);
    }


    /**
     *
     * @param cause
     *          原因
     */
    public ExceptionUtils(Throwable cause) {
        super(cause);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
