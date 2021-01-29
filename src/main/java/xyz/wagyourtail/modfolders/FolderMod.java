package xyz.wagyourtail.modfolders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.discovery.*;
import net.fabricmc.loader.gui.FabricGuiEntry;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.metadata.EntrypointMetadata;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FolderMod implements Runnable {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final FabricLoader instance = FabricLoader.getInstance();
    private static final  Class<?> loader = net.fabricmc.loader.FabricLoader.class;
    public static final Logger LOGGER = ((net.fabricmc.loader.FabricLoader) instance).getLogger();
    private static final EnvType environment = instance.getEnvironmentType();
    private static final Set<String> modIds = new LinkedHashSet<>();
    private static Method adder = null;
    private static Path modsDir;
    private static String mcVersion;
    public static Config config;
    
    public static File configFile;


    // have to do this as an early riser in order to add the mixins "properly"
    @Override
    public void run() {
        LOGGER.info("[" + getClass().getSimpleName() + "] Adding version folders to modlist.");
        
        try {
            File configDir = instance.getConfigDir().toFile();
            configFile = new File(configDir, "folderizedfabricmods.json");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            if (!configFile.exists()) {
                FolderMod.config = new Config();
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(gson.toJson(FolderMod.config));
                }
            } else {
                try (FileReader reader = new FileReader(configFile)) {
                    FolderMod.config = gson.fromJson(reader, Config.class);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.", e);
        }
        modsDir = ((net.fabricmc.loader.FabricLoader)instance).getModsDir();
        mcVersion = ((net.fabricmc.loader.FabricLoader) instance).getGameProvider().getRawGameVersion();
        
        for (Map.Entry<String, Config.AutoUpdate[]> entry : config.updaters.entrySet()) {
            File folder = new File(modsDir.toFile(), entry.getKey());
            for (Config.AutoUpdate update : entry.getValue()) {
                ModUpdater updater = null;
                try {
                    updater = ModUpdater.getUpdater(folder, update);
                    updater.updateMod();
                } catch (IOException | URISyntaxException | InterruptedException | NullPointerException e) {
                    if (updater != null) updater.failed(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        unfreezeFabricLoader();

        try {
            getNewMods();
        } catch (ModResolutionException e) {
            FabricGuiEntry.displayCriticalError(e, true);
        }

        refreezeFabricLoader();

        ((net.fabricmc.loader.FabricLoader) instance).loadAccessWideners();
        addMixins();
        runEarlyRisers();
    }
    private void unfreezeFabricLoader() {
        try {
            Field frozen = loader.getDeclaredField("frozen");
            frozen.setAccessible(true);
            frozen.set(instance, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private void refreezeFabricLoader() {
        try {
            Field frozen = loader.getDeclaredField("frozen");
            frozen.setAccessible(true);
            frozen.set(instance, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        finishModLoading();
    }

    private void finishModLoading() {
        for (ModContainer mod : instance.getAllMods()) {
            if (!((net.fabricmc.loader.ModContainer)mod).getInfo().getId().equals("fabricloader")) {
                FabricLauncherBase.getLauncher().propose(((net.fabricmc.loader.ModContainer)mod).getOriginUrl());
            }
        }

        try {
            Method post1 = loader.getDeclaredMethod("postprocessModMetadata");
            post1.setAccessible(true);
            Method post2 = loader.getDeclaredMethod("setupLanguageAdapters");
            post2.setAccessible(true);
            post1.invoke(instance);
            post2.invoke(instance);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        try {
            setupMods();
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup mods", e);
        }
    }

    // have to copy setup mod logic to filter to just the new mods.
    private void setupMods() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        Method setupRootPath = net.fabricmc.loader.ModContainer.class.getDeclaredMethod("setupRootPath");
        setupRootPath.setAccessible(true);
        Field entryPointStorageField = loader.getDeclaredField("entrypointStorage");
        entryPointStorageField.setAccessible(true);
        Class<?> entryPointStorageClass = Class.forName("net.fabricmc.loader.EntrypointStorage");
        Method addDeprecated = entryPointStorageClass.getDeclaredMethod("addDeprecated", net.fabricmc.loader.ModContainer.class, String.class, String.class);
        addDeprecated.setAccessible(true);
        Method add = entryPointStorageClass.getDeclaredMethod("add", net.fabricmc.loader.ModContainer.class, String.class, EntrypointMetadata.class, Map.class);
        add.setAccessible(true);
        Field adapterMapField = loader.getDeclaredField("adapterMap");
        adapterMapField.setAccessible(true);

        Map<String, LanguageAdapter> adapterMap = (Map<String, LanguageAdapter>) adapterMapField.get(instance);
        Object entrypointStorage = entryPointStorageField.get(instance);

        for (ModContainer mod : instance.getAllMods().stream().filter(e -> modIds.contains(e.getMetadata().getId())).collect(Collectors.toList())) {
            try {
                setupRootPath.invoke(mod);

                for (String in : ((net.fabricmc.loader.ModContainer)mod).getInfo().getOldInitializers()) {
                    String adapter = ((net.fabricmc.loader.ModContainer)mod).getInfo().getOldStyleLanguageAdapter();
                    addDeprecated.invoke(entrypointStorage, mod, adapter, in);
                }

                for (String key : ((net.fabricmc.loader.ModContainer)mod).getInfo().getEntrypointKeys()) {
                    for (EntrypointMetadata in : ((net.fabricmc.loader.ModContainer)mod).getInfo().getEntrypoints(key)) {
                        add.invoke(entrypointStorage, mod, key, in, adapterMap);
                    }
                }
            } catch (InvocationTargetException e) {
                 throw new RuntimeException(String.format("Failed to setup mod %s (%s)", ((net.fabricmc.loader.ModContainer)mod).getInfo().getName(), ((net.fabricmc.loader.ModContainer)mod).getOriginUrl().getFile()), e.getTargetException());
            }
        }
    }

    private void addModDirectories(ModResolver resolver) {

        File defaultFolder = new File(modsDir.toFile(), mcVersion);
        if (!defaultFolder.exists()) {
            defaultFolder.mkdir();
        }

        String xVersion;
        if (mcVersion.split("\\.").length == 3) {
            String[] varr = mcVersion.split("\\.");
            varr[2] = "x";
            xVersion = String.join(".", varr);
        } else {
            xVersion = mcVersion + ".x";
        }

        Arrays.stream(modsDir.toFile().listFiles())
            .filter(File::isDirectory)
            .forEach(folder -> {
                List<String> folderVersions = Arrays.stream(folder.getName().split(",")).map(String::toLowerCase).collect(Collectors.toList());
                if (folderVersions.contains(mcVersion) || folderVersions.contains(xVersion)) {
                    resolver.addCandidateFinder(new DirectoryModCandidateFinder(folder.toPath(), instance.isDevelopmentEnvironment()));
                }
            });
    }

    private void getNewMods() throws ModResolutionException {
        ModResolver resolver = new ModResolver();
        resolver.addCandidateFinder(new ClasspathModCandidateFinder());
        resolver.addCandidateFinder(new DirectoryModCandidateFinder(modsDir, instance.isDevelopmentEnvironment()));
        addModDirectories(resolver);
        Map<String, ModCandidate> candidateMap = resolver.resolve((net.fabricmc.loader.FabricLoader)instance);
        String modText;

        switch (candidateMap.values().size()) {
            case 0:
                modText = "Loading %d mods";
                break;
            case 1:
                modText = "Loading %d mod: %s";
                break;
            default:
                modText = "Loading %d mods: %s";
                break;
        }

        LOGGER.info("[" + getClass().getSimpleName() + "] " + modText, candidateMap.values().size(), candidateMap.values().stream()
            .map(info -> String.format("%s@%s", info.getInfo().getId(), info.getInfo().getVersion().getFriendlyString()))
            .collect(Collectors.joining(", ")));

        boolean runtimeModRemapping = instance.isDevelopmentEnvironment();

        if (runtimeModRemapping && System.getProperty("fabric.remapClasspathFile") == null) {
            LOGGER.warn("Runtime mod remapping disabled due to no fabric.remapClasspathFile being specified. You may need to update loom.");
            runtimeModRemapping = false;
        }

        if (runtimeModRemapping) {
            for (ModCandidate candidate : RuntimeModRemapper.remap(candidateMap.values(), ModResolver.getInMemoryFs())) {
                addMod(candidate);
            }
        } else {
            for (ModCandidate candidate : candidateMap.values()) {
                addMod(candidate);
            }
        }
    }

    private void addMod(ModCandidate candidate) {
        try {
            if (adder == null) {
                adder = loader.getDeclaredMethod("addMod", ModCandidate.class);
                adder.setAccessible(true);
            }
            try {
                adder.invoke(instance, candidate);
                // if this doesn't throw, it means the mod is new and we can add it to the modIds list.
                modIds.add(candidate.getInfo().getId());
            } catch (IllegalAccessException | InvocationTargetException ignored) {}
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private void addMixins() {
        instance.getAllMods().stream()
            .map(ModContainer::getMetadata)
            .filter(m -> m instanceof LoaderModMetadata && modIds.contains(m.getId()))
            .flatMap(m -> ((LoaderModMetadata) m).getMixinConfigs(environment).stream())
            .filter(s -> s != null && !s.isEmpty())
            .collect(Collectors.toSet())
            .forEach(Mixins::addConfiguration); //FabricMixinBootstrap::addConfiguration
    }


    //traditional early risers are after this exits so it's fine.
    private void runEarlyRisers() {
        instance.getEntrypointContainers("mm:early_risers", Runnable.class).stream()
            .filter(container -> container.getProvider().getMetadata() instanceof LoaderModMetadata && modIds.contains(container.getProvider().getMetadata().getId()))
            .collect(Collectors.toList())
            .forEach((container) -> container.getEntrypoint().run());
    }
}
