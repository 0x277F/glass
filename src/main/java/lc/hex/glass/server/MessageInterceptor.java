package lc.hex.glass.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lc.hex.glass.Commands;

import javax.inject.Inject;
import java.util.logging.Logger;

public class MessageInterceptor extends SimpleChannelInboundHandler<String> {
    private static int nextID = 0;

    private ProxyServer proxyServer;
    private Logger logger;
    private Commands commands;
    private Channel upstream;
    private Channel downstream;
    private final int downstreamId;
    boolean pinged = false;

    private String nick, user;

    @Inject
    public MessageInterceptor(ProxyServer proxyServer, Logger logger, Commands commands) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.commands = commands;
        this.downstreamId = nextID++;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        this.downstream = channel;
        logger.info("Incoming connection from " + channel.remoteAddress());
        channel.read();
    }

    public void setUpstream(Channel channel) {
        this.upstream = channel;
        logger.info("Upstream set to channel " + channel.remoteAddress());
    }

    public Channel getUpstream() {
        return upstream;
    }

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        System.out.println(">>>" + s);
        String[] split = s.split(" ");
        if (split[0].equalsIgnoreCase("NICK")) {
            nick = split[1];
            if (upstream == null) channelHandlerContext.read();
        }
        else if (split[0].equalsIgnoreCase("USER")) {
            if (split[4].contains("##")) {
                user = s.substring(5, s.indexOf("##"));
                String synLine = s.substring(s.indexOf("##") + 2);
                s = s.substring(0, s.indexOf("##"));
                split = synLine.split(" ");
            } else {
                user = s.substring(s.indexOf(" "));
            }
            if (upstream == null) channelHandlerContext.read();
        }

        if (upstream != null && upstream.isActive()) {
            upstream.writeAndFlush(s + "\r\n").addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    channelHandlerContext.channel().read();
                } else {
                    logger.warning("Closing upstream connection...");
                    logger.warning(future.cause().getMessage());
                    channelHandlerContext.channel().close();
                }
            });
        }

        if (split[0].equalsIgnoreCase("PRIVMSG")) {
            String channel = split[1];
            String msg = s.substring(s.indexOf(':', 7) + 1);
            if (msg.startsWith("@")) {
                String[] args = msg.split(" ");
                if (args.length >= 3 && args[0].equalsIgnoreCase("@syn")) {
                    synchronizeTo(args[1], Integer.parseInt(args[2].trim()), args.length >= 4 && args[3].equalsIgnoreCase("-s"), channelHandlerContext);
                } else if (commands.contains(args[0])){
                    commands.get(args[0]).handle(args[0], args, channel, channelHandlerContext, this);
                }
            }
        } else if (split.length >= 3 && split[0].equalsIgnoreCase("syn")) {
            synchronizeTo(split[1], Integer.parseInt(split[2].trim()), split.length >= 4 && split[3].equalsIgnoreCase("-s"), channelHandlerContext);
        }
    }

    public void synchronizeTo(String host, int port, boolean ssl, ChannelHandlerContext ctx) {
        logger.info("Resynchronizing " + ctx.channel().remoteAddress() + " to " + host + ":" + (ssl ? "+" : "") + port);
        if (upstream != null) {
            upstream.closeFuture().addListener((ChannelFutureListener) future -> proxyServer.createProxy(host, port, ctx.channel(), this, ssl));
            upstream.writeAndFlush("QUIT :Resynchronizing client");
        } else {
            proxyServer.createProxy(host, port, ctx.channel(), this, ssl);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        proxyServer.getActiveChannels().remove(this);
        if (upstream != null) {
            upstream.write("QUIT :Client connection lost.");
            upstream.close();
        }
        nick = null;
        user = null;
        logger.info("Lost client connection.");
    }

    public int getDownstreamId() {
        return downstreamId;
    }

    public String getNick() {
        return nick;
    }

    public String getUser() {
        return user;
    }

    public Channel getDownstream() {
        return downstream;
    }
}
