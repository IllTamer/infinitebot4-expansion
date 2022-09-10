package com.illtamer.infinite.bot.expansion.message.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaFilter<E> {

    private final E e;
    private boolean _continue = true;

    private LambdaFilter(E e) {
        this.e = e;
    }

    public LambdaFilter<?> is(Supplier<Boolean> supplier) {
        if (_continue) _continue = supplier.get();
        return this;
    }

    public LambdaFilter<E> is(Function<E, Boolean> function) {
        if (_continue) _continue = function.apply(e);
        return this;
    }

    public boolean result() {
        return _continue;
    }

    public <R> R result(R r) {
        return _continue ? r : null;
    }

    public static LambdaFilter<?> of() {
        return new LambdaFilter<>(null);
    }

    public static <E> LambdaFilter<E> of(E e) {
        return new LambdaFilter<>(e);
    }

}
