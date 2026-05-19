package club.someoneice.callablehorse.core;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static final String CAN_RESPAWN_HORSE = "CanRespawnHorse";
    private static final String CAN_CALL_FROM_OTHER_WORLD = "CanCallHorseFromOtherWorld";
    private static final String RANGE_CAN_CALL = "CallHorseRange";

    private final Path path;
    private final Properties properties = new Properties();

    public Config() {
        this.path = FabricLoader.getInstance().getConfigDir().resolve(CallableHorseFabric.MODID + ".properties");
        this.init();
    }

    public void init() {
        this.load();
        CallableHorseFabric.canRespawnHorse = this.getBoolean(CAN_RESPAWN_HORSE, CallableHorseFabric.canRespawnHorse);
        CallableHorseFabric.canCallFromOtherWorld = this.getBoolean(CAN_CALL_FROM_OTHER_WORLD, CallableHorseFabric.canCallFromOtherWorld);
        CallableHorseFabric.rangeCanCall = this.getInteger(RANGE_CAN_CALL, CallableHorseFabric.rangeCanCall);
        this.saveDefaults();
    }

    public Config reload() {
        this.properties.clear();
        init();
        return this;
    }

    private void load() {
        if (!Files.exists(this.path)) return;

        try (Reader reader = Files.newBufferedReader(this.path)) {
            this.properties.load(reader);
        } catch (IOException exception) {
            CallableHorseFabric.LOG.warn("Failed to read config file {}", this.path, exception);
        }
    }

    private void saveDefaults() {
        this.properties.putIfAbsent(CAN_RESPAWN_HORSE, Boolean.toString(CallableHorseFabric.canRespawnHorse));
        this.properties.putIfAbsent(CAN_CALL_FROM_OTHER_WORLD, Boolean.toString(CallableHorseFabric.canCallFromOtherWorld));
        this.properties.putIfAbsent(RANGE_CAN_CALL, Integer.toString(CallableHorseFabric.rangeCanCall));

        try {
            Files.createDirectories(this.path.getParent());
            try (Writer writer = Files.newBufferedWriter(this.path)) {
                this.properties.store(writer, "CallableHorseFabric config");
            }
        } catch (IOException exception) {
            CallableHorseFabric.LOG.warn("Failed to write config file {}", this.path, exception);
        }
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        String value = this.properties.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private int getInteger(String key, int defaultValue) {
        String value = this.properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            CallableHorseFabric.LOG.warn("Invalid integer value for {} in {}", key, this.path, exception);
            return defaultValue;
        }
    }
}
