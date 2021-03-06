package lc.hex.glass;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import lc.hex.glass.server.MessageInterceptor;
import lc.hex.glass.server.ProxyServer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class Commands {
    public static final String PASTEBIN_URL;
    public static final int PASTEBIN_PORT;

    private Map<String, CommandHandler> registry;
    private final Logger logger;
    private final ProxyServer proxyServer;
    private Registers registers;

    static {
        PASTEBIN_URL = System.getProperty("lc.hex.glass.pastebin", "termbin.com");
        PASTEBIN_PORT = Integer.parseInt(System.getProperty("lc.hex.glass.pastebinport", "9999"));
    }

    @Inject
    public Commands(Logger logger, ProxyServer proxyServer, Registers registers) {
        this.registers = registers;
        registry = new HashMap<>();
        this.logger = logger;
        this.proxyServer = proxyServer;
        registerCommands();
    }

    public void registerCommands() {
        register("@rsy", (cmd, args, channel, ctx, interceptor) -> {
            String[] host = interceptor.getUpstream().remoteAddress().toString().split(":");
            interceptor.synchronizeTo(host[0].split("/")[0], Integer.parseInt(host[1]), interceptor.getUpstream().pipeline().get(SslHandler.class) != null, ctx);
        });

        register("@die", ((cmd, args, channel, ctx, interceptor) -> {
            registers.save();
            for (MessageInterceptor i : proxyServer.getActiveChannels()) {
                i.getDownstream().close();
            }
            System.exit(0);
        }));

        register("@iex", ((cmd, args, channel, ctx, interceptor) -> {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", Arrays.stream(Arrays.copyOfRange(args, 1, args.length)).collect(Collectors.joining(" ")));
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder output = new StringBuilder();
                int lines =  0;
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                    output.append(System.getProperty("line.separator"));
                    lines++;
                }
                if (lines > 3) {
                    pastebin(output.toString(), s -> CommandHandler.send(channel, s, interceptor));
                } else {
                    CommandHandler.send(channel, output.toString(), interceptor);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        register("@swp", (((cmd, args, channel, ctx, interceptor) -> {
            if (args.length >= 3) {
                char to = args[1].charAt(0);
                char from = args[2].charAt(0);
                String val = registers.get(to);
                registers.set(to, registers.get(from));
                registers.set(from, val);
            }
        })));

        register("@clr", (((cmd, args, channel, ctx, interceptor) -> {
            if (args.length >= 2) {
                for (int i = 1; i < args.length; i++) {
                    registers.clear(args[i].charAt(0));
                }
            }
        })));

        register("@acy", (((cmd, args, channel, ctx, interceptor) -> {
            if (registers.isSet('A')) {
                for (String user : registers.get('A').split(",")) {
                    String[] split = user.split(":");
                    mode(interceptor, channel, true, split[1].charAt(0), split[0]);
                }
            }
        })));
    }

    private void mode(MessageInterceptor interceptor, String channel, boolean adding, char mode, String args) {
        interceptor.getUpstream().writeAndFlush("MODE " + channel + (adding ? "+" : "-") + mode + " " + args + "\r\n");
    }

    public void register(String command, CommandHandler handler) {
        registry.put(command, handler);
        logger.info("Registered command " + command);
    }

    public void pastebin(String message, Consumer<String> callback) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .channel(NioSocketChannel.class)
                .group(new NioEventLoopGroup(1))
                .handler(new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        callback.accept(msg);
                        ctx.channel().close();
                    }
                })
                .connect(PASTEBIN_URL, PASTEBIN_PORT).addListener((ChannelFutureListener) future1 -> {
                    if (future1.isSuccess()) {
                        future1.channel().pipeline().addFirst(new StringEncoder(), new LineEncoder(), new LineBasedFrameDecoder(128), new StringDecoder());
                        future1.channel().writeAndFlush(message);
                    } else {
                        callback.accept("I/O Error");
                    }
                });
    }

    public CommandHandler get(String command) {
        return registry.get(command.toLowerCase());
    }

    public boolean contains(String command) {
        return registry.containsKey(command.toLowerCase());
    }
}
