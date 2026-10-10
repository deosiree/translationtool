package com.shr.translationtoolservice.entity.vo.exception;

/**
 * 异常信息VO对象，包含给用户提供的这次请求的异常信息
 */
public class ExceptionVO {
    /**
     * 异常信息, 告诉用户发生什么事
     */
    String message;

    /**
     * 如何解决这个问题的方法
     */
    String resolvedMethodMessage;

    public ExceptionVO(String message, String resolvedMethodMessage) {
        this.message = message;
        this.resolvedMethodMessage = resolvedMethodMessage;
    }

    public static ExceptionVO newInstance(String message){
        ExceptionVO exceptionVO = new ExceptionVO(message, "");
        return exceptionVO;
    }

    public static ExceptionVO newInstance(String message, String resolvedMethodMessage){
        ExceptionVO exceptionVO = new ExceptionVO(message, resolvedMethodMessage);
        return exceptionVO;
    }

    public String getResolvedMethodMessage() {
        return resolvedMethodMessage;
    }

    public void setResolvedMethodMessage(String resolvedMethodMessage) {
        this.resolvedMethodMessage = resolvedMethodMessage;
    }

    public ExceptionVO(String message) {
        this.message = message;
    }

    public ExceptionVO() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((resolvedMethodMessage == null) ? 0 : resolvedMethodMessage.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExceptionVO other = (ExceptionVO) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (resolvedMethodMessage == null) {
            if (other.resolvedMethodMessage != null)
                return false;
        } else if (!resolvedMethodMessage.equals(other.resolvedMethodMessage))
            return false;
        return true;
    }
    
}
