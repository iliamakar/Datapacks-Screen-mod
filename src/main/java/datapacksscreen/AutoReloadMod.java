package datapacksscreen;

import datapacksscreen.interfaces.ICommandFunctionManagerMixin;
import datapacksscreen.interfaces.IFunctionLoaderMixin;
import datapacksscreen.gui.DatapackScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.WorldSavePath;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AutoReloadMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Datapacks Screen");
    private static MinecraftClient client;
    private static MinecraftServer server;
    private static KeyBinding keyBinding;
    private static File configFile;
    private static List<String> autoreloadDatapacks;
    private static Path datapacksPath;
    private static DirectoryWatcher watcher;

    @Override
    public void onInitialize() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "iliamakar.datapacksscreen.open_gui_keybind",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "iliamakar.datapacksscreen.mod_name"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.isPressed() && client.getServer() != null) {
                client.setScreen(new DatapackScreen());
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(eventServer -> {
            client = MinecraftClient.getInstance();
            server = client.getServer();
            stopWatcher();
            datapacksPath = eventServer.getSavePath(WorldSavePath.DATAPACKS);
            String path = eventServer.getSavePath(WorldSavePath.ROOT).toString();
            configFile = Path.of(path.substring(0, path.length() - 1) + "\\data\\autoreload").toFile();
            try {
                loadConfig();
                if (autoreloadDatapacks.size() > 0) {
                    startWatcher();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(eventServer -> {
            stopWatcher();
        });
    }

    public static void startWatcher() throws IOException {
        if (watcher == null) {
            watcher = new DirectoryWatcher(datapacksPath);
            watcher.start();
        }
    }

    public static void stopWatcher() {
        if (watcher != null) {
            watcher.stop();
        }
    }

    public static void updateConfig(List<String> list) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(configFile.toString()));
            for (String str : list) {
                writer.write(str);
                writer.newLine();
            }
            writer.flush();
            writer.close();
            autoreloadDatapacks = list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadConfig() throws IOException {
        if (configFile.createNewFile()) {
            autoreloadDatapacks = new ArrayList<>();
        }
        else {
            autoreloadDatapacks = getListFromPath(configFile.toPath());
        }
    }

    private static Map<Identifier, CommandFunction> getModifiedFunctions() {
        Map<Identifier, CommandFunction> commands = new HashMap<>();

        if (watcher.isDirDeleted()) {
            for (String pack : autoreloadDatapacks) {
                try (Stream<Path> walk = Files.walk(datapacksPath.resolve(pack.substring(pack.indexOf("/") + 1)))) {
                    walk.forEach(path -> {
                        if (path.toString().endsWith(".mcfunction") && isRightDirectory(path, "functions")) {
                            Identifier functionID = null;
                            try {
                                functionID = getIdentifierFromPath(path);
                                commands.put(functionID, CommandFunction.create(functionID, server.getCommandFunctionManager().getDispatcher(), server.getCommandSource(), getListFromPath(path)));
                            } catch (IllegalArgumentException e) {
                                client.player.sendMessage(Text.literal("An error occurred in " + functionID + " --> " + e.getMessage()));
                            } catch (InvalidIdentifierException e) {
                                client.player.sendMessage(Text.literal(e.getMessage()));
                            }
                        }
                    });
                } catch (IOException e) {
                    LOGGER.warn("Unable to open " + pack);
                }
            }
        }
        else {
            for (Path path : watcher.getChanged()) {
                if (isAutoreloadable(path) && path.toString().endsWith(".mcfunction") && isRightDirectory(path, "functions")) {
                    Identifier functionID = null;
                    try {
                        functionID = getIdentifierFromPath(path);
                        commands.put(functionID, CommandFunction.create(functionID, server.getCommandFunctionManager().getDispatcher(), server.getCommandSource(), getListFromPath(path)));
                    } catch (IllegalArgumentException e) {
                        client.player.sendMessage(Text.literal("An error occurred in " + functionID + " --> " + e.getMessage()));
                    } catch (InvalidIdentifierException e) {
                        client.player.sendMessage(Text.literal(e.getMessage()));
                    }
                }
            }
        }
        return commands;
    }

    public static boolean isRightDirectory(Path path, String dir) {
        Path relPath = path.subpath(datapacksPath.getNameCount(), path.getNameCount());
        return relPath.getNameCount() >= 3 && relPath.getName(3).toString().equals(dir);

    }

    public static boolean isAutoreloadable(Path path) {
        Path relPath = path.subpath(datapacksPath.getNameCount(), path.getNameCount());
        return autoreloadDatapacks.contains("file/" + relPath.getName(0));
    }

    public static Identifier getIdentifierFromPath(Path path) {
        Path relPath = path.subpath(datapacksPath.getNameCount(), path.getNameCount());
        String namespace = relPath.getName(2).toString();
        String path2 = relPath.subpath(4, relPath.getNameCount()).toString();
        path2 = path2.substring(0, path2.lastIndexOf(".")).replace("\\", "/");

        return new Identifier(namespace, path2);
    }

    public static List<String> getListFromPath(Path path) {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reloadDatapacks() {
        if (watcher != null && server != null && client.player != null) {
            CompletableFuture.runAsync(AutoReloadMod::reloadResources).handle((r, ex) -> {
                throw new RuntimeException(ex);
            });

        }
    }

    private static void reloadResources() {
        //long start = System.currentTimeMillis();
        CommandFunctionManager manager = server.getCommandFunctionManager();
        FunctionLoader loader = ((ICommandFunctionManagerMixin) manager).getFunctionLoader();

        Map<Identifier, CommandFunction> commands = AutoReloadMod.getModifiedFunctions();
        ((IFunctionLoaderMixin) loader).addFunctions(commands, server.getResourceManager());
        ((ICommandFunctionManagerMixin) manager).updateTickFunctions();

        watcher.reset();
        if (commands.size() > 0) {
            //long delta = System.currentTimeMillis() - start;
            client.player.sendMessage(Text.translatable("iliamakar.datapacksscreen.reloaded", commands.size()));
        }
    }

    public static List<String> getAutoreloadDatapacks() {
        return autoreloadDatapacks;
    }

    public static DirectoryWatcher getWatcher() {
        return watcher;
    }
}


