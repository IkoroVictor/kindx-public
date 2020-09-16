package io.kindx.test;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;

import java.util.function.Consumer;

public class TestModule extends AbstractModule {

    private Consumer<Binder> binderConsumer;

    public TestModule(Consumer<Binder> binderConsumer) {
        this.binderConsumer = binderConsumer;
    }

    @Override
    protected void configure() {
        super.configure();
        binderConsumer.accept(binder());
    }
}
