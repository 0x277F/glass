package lc.hex.glass;

import com.google.common.collect.ImmutableSet;
import lc.hex.glass.server.ProxyServer;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    public Registers(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.registerValues = new HashMap<>();
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
