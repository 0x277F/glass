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
import lc.hex.glass.GlassApplication;

import javax.inject.Inject;
import java.util.logging.Logger;

public class ProxyServer implements Runnable {
    private Logger logger;
    private GlassApplication application;
    private NioEventLoopGroup eventLoop, childLoop;

    @Inject
    public ProxyServer(Logger logger, GlassApplication application) {
        this.logger = logger;
        this.application = application;
        eventLoop = new NioEventLoopGroup(1);
        childLoop = new NioEventLoopGroup();
    }

    public void run() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class)
                    .group(eventLoop, childLoop)
                    .childHandler(application.getInjector().getInstance(ServerInitializer.class))
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
                .handler(new UpstreamConnector(logger, this, ssl))
                .option(ChannelOption.AUTO_READ, false);
        return bootstrap.connect(host, port).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
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
}
