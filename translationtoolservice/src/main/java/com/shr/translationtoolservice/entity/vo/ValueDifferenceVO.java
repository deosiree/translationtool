package com.shr.translationtoolservice.entity.vo;

import java.util.ArrayList;
import java.util.List;

public class ValueDifferenceVO<T>{

    public List<T> valueOnlyInFirst = new ArrayList<>();

    public List<T> valueOnlyInSecond = new ArrayList<>();;

    // 键存在但值不同的键值对（key: 共同键, value: 第一个Map的List vs 第二个Map的List）
    private List<ListDiff<T>> differentBetweenEachOther = new ArrayList<>();;

    // 内部类：存储两个List的差异
    public static class ListDiff<E> {
        private List<E> firstList;  // 第一个Map中该键对应的List
        private List<E> secondList; // 第二个Map中该键对应的List

        public ListDiff(){

        }

        public ListDiff(List<E> firstList, List<E> secondList) {
            this.firstList = firstList;
            this.secondList = secondList;
        }

        // getter & setter
        public List<E> getFirstList() { return firstList; }
        public void setFirstList(List<E> firstList) { this.firstList = firstList; }
        public List<E> getSecondList() { return secondList; }
        public void setSecondList(List<E> secondList) { this.secondList = secondList; }

        @Override
        public String toString() {
            return "ListDiff{" +
                    "firstList=" + firstList +
                    ", secondList=" + secondList +
                    '}';
        }
    }

    public List<T> getValueOnlyInFirst() {
        return valueOnlyInFirst;
    }

    public void setValuesOnlyInFirst(List<T> valueOnlyInFirst) {
        this.valueOnlyInFirst = valueOnlyInFirst;
    }

    public void addValuesOnlyInFirst(List<T> valueOnlyInFirst) {
        this.valueOnlyInFirst.addAll(valueOnlyInFirst);
    }


    public List<T> getValueOnlyInSecond() {
        return valueOnlyInSecond;
    }

    public void setValueOnlyInSecond(List<T> valueOnlyInSecond) {
        this.valueOnlyInSecond = valueOnlyInSecond;
    }

    public void addValueOnlyInSecond(List<T> valueOnlyInSecond) {
        this.valueOnlyInSecond.addAll(valueOnlyInSecond);
    }

    public List<ListDiff<T>> getDifferentBetweenEachOther() {
        return differentBetweenEachOther;
    }

    public void setDifferentBetweenEachOther(List<ListDiff<T>> differentBetweenEachOther) {
        this.differentBetweenEachOther = differentBetweenEachOther;
    }

    public void addDifferentBetweenEachOther(ListDiff<T> differentBetweenEachOther) {
        this.differentBetweenEachOther.add(differentBetweenEachOther);
    }
}
