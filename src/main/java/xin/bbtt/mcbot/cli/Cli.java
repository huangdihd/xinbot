/*
 *   Copyright (C) 2024-2026 huangdihd
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xin.bbtt.mcbot.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.Xinbot;
import xin.bbtt.mcbot.config.BotConfig;
import xin.bbtt.mcbot.modpack.ModpackConfig;
import xin.bbtt.mcbot.modpack.ModpackConfigSetup;
import xin.bbtt.mcbot.modpack.ModpackExporter;
import xin.bbtt.mcbot.modpack.ModpackInstaller;
import xin.bbtt.mcbot.modpack.ModpackInstaller.InstallResult;
import xin.bbtt.mcbot.modpack.ModpackManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The command-line front end: a tiny registry that maps flags such as
 * {@code --install} to {@link Subcommand} handlers and dispatches them.
 *
 * <p>The dispatch mechanism is generic; all sub-commands are wired up in
 * {@link #create()}. To add a new one, register it there and add a handler
 * method — nothing else needs to change.
 */
public final class Cli {
    private static final Logger log = LoggerFactory.getLogger(Cli.class.getSimpleName());

    private static final String LANG_DIR = "lang";

    private final Map<String, Subcommand> registry = new LinkedHashMap<>();

    private Cli() {}

    /** Builds a dispatcher wired with all of Xinbot's sub-commands. */
    public static Cli create() {
        Cli cli = new Cli();
        cli.register(0, "xinbot.help.usage", a -> cli.version(), "--version", "-v");
        cli.register(0, "xinbot.help.usage", a -> cli.license(), "--license", "-l");
        cli.register(0, "xinbot.help.usage", a -> cli.help(), "--help", "-h");
        cli.register(1, "xinbot.modpack.install.usage", Cli::install, "--install", "-i");
        cli.register(1, "xinbot.modpack.export.usage", Cli::export, "--export", "-e");
        cli.register(1, "xinbot.modpack.export_config.usage", Cli::exportConfig, "--export-config");
        cli.register(1, "xinbot.modpack.info.usage", Cli::modpackInfo, "--modpack-info");
        return cli;
    }

    private void register(int arity, String usageKey, Subcommand.Handler handler, String... aliases) {
        Subcommand cmd = new Subcommand(List.of(aliases), arity, usageKey, handler);
        for (String alias : aliases) registry.put(alias, cmd);
    }

    /** @return true if {@code args} looks like a sub-command invocation (a leading flag). */
    public boolean isSubcommand(String[] args) {
        return args.length >= 1 && args[0].startsWith("-");
    }

    /**
     * Looks up and runs the sub-command named by {@code args[0]}.
     * @return true on success, false on unknown command, bad argument count, or handler failure
     */
    public boolean dispatch(String[] args) {
        Subcommand cmd = registry.get(args[0]);
        if (cmd == null) {
            log.error(LangManager.get("xinbot.args.unknown", args[0]));
            help();
            return false;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (rest.length != cmd.arity()) {
            log.error(LangManager.get(cmd.usageKey()));
            return false;
        }
        return cmd.handler().run(rest);
    }

    // ---- handlers --------------------------------------------------------

    private boolean version() {
        log.info(LangManager.get("xinbot.version", Xinbot.version));
        return true;
    }

    private boolean license() {
        Arrays.stream(Xinbot.license.split("\n")).forEach(log::info);
        return true;
    }

    private boolean help() {
        Arrays.stream(LangManager.get("xinbot.help.usage").split("\n")).forEach(log::info);
        return true;
    }

    private static boolean install(String[] args) {
        try {
            Path configFile = Paths.get(Xinbot.defaultConfigPath);
            InstallResult result = ModpackInstaller.install(
                    Paths.get(args[0]), Paths.get(resolvePluginDirectory()), Paths.get(LANG_DIR), configFile);
            if (result.hasConfig()) {
                log.info(LangManager.get("xinbot.modpack.setup.start"));
                ModpackConfigSetup.complete(result.configFile(), new ConsoleConfigInput());
                log.info(LangManager.get("xinbot.modpack.setup.done", result.configFile().getFileName().toString()));
            }
            return true;
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.modpack.install.failed", e.getMessage()), e);
            return false;
        }
    }

    private static boolean export(String[] args) {
        return runExport(args[0], false);
    }

    // Like --export, but also bundles the current config.conf with all credentials stripped.
    private static boolean exportConfig(String[] args) {
        return runExport(args[0], true);
    }

    private static boolean runExport(String out, boolean withConfig) {
        Path outZip = Paths.get(out);
        String name = outZip.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
        ModpackManifest manifest = new ModpackManifest(name, "1.0.0", null, null, Xinbot.version, null);
        try {
            String configHocon = withConfig ? sanitizedConfig() : null;
            if (withConfig && configHocon == null) return false;
            ModpackExporter.export(outZip, Paths.get(resolvePluginDirectory()), Paths.get(LANG_DIR), manifest, configHocon);
            return true;
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.modpack.export.failed", e.getMessage()), e);
            return false;
        }
    }

    /** Loads the current config, strips every secret, and renders it for bundling. */
    private static String sanitizedConfig() throws java.io.IOException {
        Path cfg = Paths.get(Xinbot.defaultConfigPath);
        if (!Files.exists(cfg)) {
            log.error(LangManager.get("xinbot.modpack.export.no_config", Xinbot.defaultConfigPath));
            return null;
        }
        BotConfig config = new BotConfig(cfg.toString());
        ModpackConfig.sanitize(config.getConfigData());
        return BotConfig.render(config.getConfigData());
    }

    private static boolean modpackInfo(String[] args) {
        try {
            ModpackManifest m = ModpackManifest.readFromZip(Paths.get(args[0]));
            log.info(LangManager.get("xinbot.modpack.info.name", m.getName(), m.getVersion()));
            if (m.getAuthor() != null) log.info(LangManager.get("xinbot.modpack.info.author", m.getAuthor()));
            if (m.getDescription() != null) log.info(LangManager.get("xinbot.modpack.info.description", m.getDescription()));
            if (m.getXinbotVersion() != null) log.info(LangManager.get("xinbot.modpack.info.xinbot_version", m.getXinbotVersion()));
            if (!m.getPlugins().isEmpty()) log.info(LangManager.get("xinbot.modpack.info.plugins", String.join(", ", m.getPlugins())));
            if (ModpackConfig.isPresentIn(Paths.get(args[0]))) log.info(LangManager.get("xinbot.modpack.info.config"));
            return true;
        } catch (Exception e) {
            log.error(LangManager.get("xinbot.modpack.info.failed", e.getMessage()), e);
            return false;
        }
    }

    /**
     * Resolves the plugin directory from {@code config.conf} when present,
     * falling back to the default {@code plugin} directory. Modpack sub-commands
     * must work even without a fully configured account, so config errors are tolerated.
     */
    private static String resolvePluginDirectory() {
        Path cfg = Paths.get(Xinbot.defaultConfigPath);
        if (Files.exists(cfg)) {
            try {
                String dir = new BotConfig(Xinbot.defaultConfigPath).getConfigData().getPlugin().getDirectory();
                if (dir != null && !dir.isBlank()) return dir;
            } catch (Exception ignored) {
                // fall through to the default directory
            }
        }
        return "plugin";
    }
}
