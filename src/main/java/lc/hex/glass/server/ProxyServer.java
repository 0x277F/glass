package lc.hex.glass.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Singleton
public class ProxyServer implements Runnable {
    private final Logger logger;
    private final Provider<ServerInitializer> serverInitializerProvider;
    private final UpstreamConnector.Factory upstreamConnectorFactory;
    private NioEventLoopGroup eventLoop, childLoop;
    private Set<MessageInterceptor> activeChannels;

    @Inject
    public ProxyServer(Logger logger, Provider<ServerInitializer> serverInitializerProvider, UpstreamConnector.Factory upstreamConnectorFactory) {
        this.logger = logger;
        this.serverInitializerProvider = serverInitializerProvider;
        this.upstreamConnectorFactory = upstreamConnectorFactory;
        eventLoop = new NioEventLoopGroup(1);
        childLoop = new NioEventLoopGroup();
        activeChannels = new HashSet<>();
    }

    @Override
    public void run() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class)
                    .group(eventLoop, childLoop)
                    .childHandler(serverInitializerProvider.get())
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(8841)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            eventLoop.shutdownGracefully();
            childLoop.shutdownGracefully();
        }
    }

    public ChannelFuture createProxy(String host, int port, Channel inbound, MessageInterceptor interceptor, boolean ssl) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(childLoop)
                .channel(NioSocketChannel.class)
                .handler(upstreamConnectorFactory.create(ssl))
                .option(ChannelOption.AUTO_READ, false);
        return bootstrap.connect(host, port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                activeChannels.add(interceptor);
                Channel c = f.channel();
                UpstreamConnector upstream = c.pipeline().get(UpstreamConnector.class);
                upstream.setClientChannel(interceptor);
                interceptor.setUpstream(c);
                upstream.setDownstreamId(interceptor.getDownstreamId());
                inbound.read();
            } else {
                logger.warning(f.cause().getMessage());
                inbound.close();
            }
        });
    }

    public Set<MessageInterceptor> getActiveChannels() {
        return activeChannels;
    }
}
