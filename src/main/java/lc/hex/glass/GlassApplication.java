package lc.hex.glass;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import lc.hex.glass.server.ProxyServer;
import lc.hex.glass.server.UpstreamConnector;

public class GlassApplication {

    public static void main(String[] args) {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new FactoryModuleBuilder().build(UpstreamConnector.Factory.class));
            }
        });
        injector.getInstance(ProxyServer.class).run();
    }
}
