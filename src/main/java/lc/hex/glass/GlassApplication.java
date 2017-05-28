package lc.hex.glass;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lc.hex.glass.server.ProxyServer;
import lc.hex.glass.server.ServerInitializer;

import java.util.logging.Logger;

public class GlassApplication extends AbstractModule {
    private Injector injector;
    private ProxyServer proxyServer;

    public static void main(String[] args) {
        GlassApplication application = new GlassApplication();
        application.proxyServer = new ProxyServer(Logger.getGlobal(), application);
        application.injector = Guice.createInjector(application);
        application.proxyServer.run();
    }

    @Override
    protected void configure() {
        bind(GlassApplication.class).toInstance(this);
        bind(ProxyServer.class).toInstance(proxyServer);
        bind(Commands.class).toInstance(new Commands(Logger.getGlobal(), proxyServer));
        bind(ServerInitializer.class).toProvider(() -> new ServerInitializer(GlassApplication.this));
    }

    public Injector getInjector() {
        return injector;
    }
}
