package lc.hex.glass;

import io.netty.channel.ChannelHandlerContext;
import lc.hex.glass.server.MessageInterceptor;

@FunctionalInterface
public interface CommandHandler {
    void handle(String cmd, String[] args, String channel, ChannelHandlerContext ctx, MessageInterceptor interceptor);

    static void send(String target, String msg, MessageInterceptor interceptor) {
        for (String line : msg.split(System.getProperty("line.separator"))) {
            interceptor.getUpstream().writeAndFlush("PRIVMSG " + target + " :" + line + "\r\n");
            interceptor.getDownstream().writeAndFlush(":" + interceptor.getNick() + "!" + interceptor.getUser().split(" ")[0] + "@glass.hex.lc PRIVMSG " + target + " :" + line + "\r\n");
        }
    }
}
