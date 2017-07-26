package lc.hex.glass;

import com.google.common.collect.ImmutableSet;
import lc.hex.glass.server.ProxyServer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Singleton
public class Registers {
    private Map<Character, String> registerValues;
    private ProxyServer proxyServer;
    private Logger logger;
    private File saveFile;

    @Inject
    public Registers(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.registerValues = new HashMap<>();
        this.saveFile = new File("glass-registers");
        try {
            if (!saveFile.exists()) {
                saveFile.createNewFile();
                System.out.println("Created new register save file.");
            } else {
                Files.lines(saveFile.toPath()).map(s -> s.split("§")).forEach(s -> {
                    registerValues.put(s[0].charAt(0), s[1]);
                    System.out.println("Loaded register value " + s[0] + " = " + s[1]);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            saveFile.delete();
            saveFile.createNewFile();
            System.out.println("Saving registers...");
            registerValues.forEach((c, s) -> {
                try {
                    Files.write(saveFile.toPath(), (c + "§" + s).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSet(char reg) {
        return registerValues.containsKey(reg) && !registerValues.get(reg).equals("");
    }

    public void set(char reg, String value) {
        registerValues.put(reg, value);
        logger.info("Register " + reg + " set to \"" + value + "\"");
    }

    public boolean offerTo(char reg, String value) {
        if (!isSet(reg)) {
            set(reg, value);
            return true;
        }
        return false;
    }

    public String get(char reg) {
        return registerValues.getOrDefault(reg, "");
    }

    public void clear(char reg) {
        registerValues.remove(reg);
    }

    public Set<Character> inUse() {
        return ImmutableSet.copyOf(registerValues.keySet());
    }
}
