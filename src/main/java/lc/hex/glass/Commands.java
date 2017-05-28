package lc.hex.glass;

import io.netty.handler.ssl.SslHandler;
import lc.hex.glass.server.ProxyServer;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Commands {
    private Map<String, CommandHandler> registry;
    private Logger logger;
    private ProxyServer proxyServer;

    @Inject
    public Commands(Logger logger, ProxyServer proxyServer) {
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
            interceptor.getUpstream().close();
            ctx.channel().close();
            System.exit(0);
        }));

        register("@iex", ((cmd, args, channel, ctx, interceptor) -> {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/bash", "-c", Arrays.stream(Arrays.copyOfRange(args, 1, args.length)).collect(Collectors.joining(" ")));
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                    output.append(System.getProperty("line.separator"));
                }
                CommandHandler.send(channel, output.toString(), interceptor);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public void register(String command, CommandHandler handler) {
        registry.put(command, handler);
        logger.info("Registered command " + command);
    }

    public CommandHandler get(String command) {
        return registry.get(command.toLowerCase());
    }

    public boolean contains(String command) {
        return registry.containsKey(command.toLowerCase());
    }
}
