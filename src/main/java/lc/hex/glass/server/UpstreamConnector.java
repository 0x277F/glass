package lc.hex.glass.server;

import io.netty.channel.*;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.util.logging.Logger;

public class UpstreamConnector extends SimpleChannelInboundHandler<String> {

    private Logger logger;
    private ProxyServer proxyServer;
    private Channel clientConnection;
    private Channel channel;
    private SslContext sslContext;
    private MessageInterceptor downstream;
    private String nick;
    private String user;


    private int downstreamId;
    private boolean ssl;

    public UpstreamConnector(Logger logger, ProxyServer proxyServer, boolean ssl) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        try {
            sslContext = SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        this.ssl = ssl;
    }

    public UpstreamConnector setClientChannel(MessageInterceptor downstream) {
        this.clientConnection = downstream.getDownstream();
        this.downstream = downstream;
        logger.info("UpstreamConnector registered to channel " + channel.remoteAddress());
        return this;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
        channel.pipeline().addFirst("lineDecoder", new LineBasedFrameDecoder(1024))
            .addAfter("lineDecoder", "stringDecoder", new StringDecoder())
            .addLast(new StringEncoder());
        if (ssl) {
            channel.pipeline().addFirst("ssl", sslContext.newHandler(ctx.channel().alloc()));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        nick = downstream.getNick();
        user = downstream.getUser();
        ctx.channel().read();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith("PING")) {
            downstream.pinged = true;
        }
        clientConnection.writeAndFlush(msg + "\r\n").addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("<<<" + msg);
                ctx.channel().read();
            } else {
                logger.warning("Closing client connection...");
                logger.warning(future.cause().getMessage());
                future.channel().close();
            }
        });
        if (!downstream.pinged && nick != null && user != null) {
            channel.writeAndFlush("NICK " + downstream.getNick() + "\r\n");
            channel.writeAndFlush("USER " + downstream.getUser() + "\r\n");
            downstream.pinged = true;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Lost server connection.");
        downstream.pinged = false;
    }

    public int getDownstreamId() {
        return downstreamId;
    }

    public void setDownstreamId(int downstreamId) {
        this.downstreamId = downstreamId;
    }
}
