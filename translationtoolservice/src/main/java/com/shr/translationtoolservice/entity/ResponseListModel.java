package com.shr.translationtoolservice.entity;

import lombok.Data;

import java.util.List;


@Data
public class ResponseListModel<T> {
    private List<T> list;

    private int totalNum;

}
