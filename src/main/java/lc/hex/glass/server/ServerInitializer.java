package lc.hex.glass.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import javax.inject.Inject;
import javax.inject.Provider;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final Provider<MessageInterceptor> messageInterceptorProvider;

    @Inject
    public ServerInitializer(Provider<MessageInterceptor> messageInterceptorProvider) {
        this.messageInterceptorProvider = messageInterceptorProvider;
    }

    @Override
    public void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addFirst(new StringEncoder())
                .addLast(new LineBasedFrameDecoder(1024))
                .addLast(new StringDecoder())
                .addLast(messageInterceptorProvider.get());
    }
}
