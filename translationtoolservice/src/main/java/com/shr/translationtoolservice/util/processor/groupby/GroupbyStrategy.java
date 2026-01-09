package com.shr.translationtoolservice.util.processor.groupby;

import java.util.function.Function;




public interface GroupbyStrategy<E,T extends com.shr.translationtoolservice.util.processor.groupby.GroupbyStrategy.ReplicatedVOType> {


    Function<E,T> newInstance();

    public static abstract class ReplicatedVOType{}

}
