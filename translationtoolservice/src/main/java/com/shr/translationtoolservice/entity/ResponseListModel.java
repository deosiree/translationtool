package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.Collection;


@Data
public class ResponseListModel<T> {
    private Collection<T> list;

    private int totalNum;

}
