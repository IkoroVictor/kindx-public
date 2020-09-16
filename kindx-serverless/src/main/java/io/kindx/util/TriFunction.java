package io.kindx.util;

@FunctionalInterface
public interface TriFunction<S, T, U, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param s the first function argument
     * @param t the second function argument
     * @param u the third function argument
     * @return the result
     */
    R apply(S s, T t, U u);
}