package com.shr.translationtoolservice.exception;

public class i18nServerConnectException extends Exception implements i18nServerException,SocketException {

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "i18n服务连接异常";
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return (this == obj) || (obj instanceof i18nServerConnectException);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 1;
    }

}
