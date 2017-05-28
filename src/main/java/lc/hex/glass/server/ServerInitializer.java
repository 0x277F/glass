package lc.hex.glass.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lc.hex.glass.GlassApplication;

import javax.inject.Inject;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private GlassApplication application;

    @Inject
    public ServerInitializer(GlassApplication application) {
        this.application = application;
    }

    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addFirst(new StringEncoder())
                .addLast(new LineBasedFrameDecoder(1024))
                .addLast(new StringDecoder())
                .addLast(application.getInjector().getInstance(MessageInterceptor.class));
    }
}
