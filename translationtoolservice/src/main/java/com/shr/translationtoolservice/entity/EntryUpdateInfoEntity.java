package com.shr.translationtoolservice.entity;

public class EntryUpdateInfoEntity {
    String id;

    boolean isSuccess = false;

    String message;

    public EntryUpdateInfoEntity(String id, boolean isSuccess, String message) {
        this.id = id;
        this.isSuccess = isSuccess;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }
}
