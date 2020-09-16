package io.kindx.factory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.kindx.Application;

public class InjectorFactory {
    private static Injector INJECTOR;

    public static Injector getInjector() {
        if (INJECTOR == null) {
            INJECTOR = Guice.createInjector(new Application());
        }
        return INJECTOR;
    }

    public static Injector overrideInjector(Module override) {
        INJECTOR = Guice.createInjector(Modules.override(new Application()).with(override));
        return INJECTOR;
    }

}
