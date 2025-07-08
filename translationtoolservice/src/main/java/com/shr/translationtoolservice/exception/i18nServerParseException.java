package com.shr.translationtoolservice.exception;

public class i18nServerParseException extends Exception implements i18nServerException {
    
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "文件解析存在异常";
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return (this == obj) || (obj instanceof i18nServerParseException);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 2;
    }

}
