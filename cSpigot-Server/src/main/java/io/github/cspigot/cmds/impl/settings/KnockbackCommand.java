package io.github.cspigot.cmds.impl.settings;

import io.github.cspigot.api.command.annotation.*;
import io.github.cspigot.cSpigot;
import io.github.cspigot.knockback.impl.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackConverter;
import io.github.cspigot.knockback.KnockbackHandler;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.ChatPaginator;
import io.github.cspigot.utils.Clickable;
import io.github.cspigot.utils.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Register(name = "knockback", aliases = "kb")
@SuppressWarnings("unused")
public class KnockbackCommand {

    private final ChatColor color = SettingsConfig.THEME;

    private final String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "Knockback Help"),
            CC.translate(""),
            CC.translate(" &e<> &7= &eRequired") + CC.GRAY + " | " + CC.translate("&e[] &7= &eOptional"),
            CC.translate(""),
            CC.translate("&7* " + color + "/knockback list &7- View profile list"),
            CC.translate("&7* " + color + "/knockback save &7- Save knockback profiles"),
            CC.translate("&7* " + color + "/knockback reload &7- Reload all knockback profiles"),
            CC.translate("&7* " + color + "/knockback import &7<file-name.yml> &7- Migrate old knockback config from a file"),
            CC.translate(""),
            CC.translate("&7* " + color + "/knockback create &7<name> &7<type> &7- Create a profile according to the type"),
            CC.translate("&7* " + color + "/knockback edit &7<profile> &7<field> &7<value> &7- Edit a profile"),
            CC.translate("&7* " + color + "/knockback remove &7<profile> &7- Remove a profile"),
            CC.translate("&7* " + color + "/knockback set &7<profile> &7- Load a profile"),
            CC.translate("&7* " + color + "/knockback view &7<profile> &7- View a profile's knockback settings"),
            CC.translate("&7* " + color + "/knockback viewProjectile &7<profile> &7- View a profile's projectile settings"),
            CC.translate("&7* " + color + "/knockback viewDamage &7<profile> &7- View a profile's damage tick settings"),
            CC.translate("&7* " + color + "/knockback setDefault &7<profile> &7- Set the default profile"),
            CC.translate("&7* " + color + "/knockback getDefault &7<profile> &7- View the default profile"),
            CC.translate("&7* " + color + "/knockback setType &7<profile> &7<type> &7- Set a profile's type"),
            CC.translate("&7* " + color + "/knockback setProfile &7<player> &7<profile> &7- Set a player's knockback profile"),
            SettingsConfig.CHAT_BAR
    };

    @Command(name = "", aliases = "help", desc = "View knockback commands")
    @Require("cSpigot.knockback")
    public void help(@Sender CommandSender sender) {
        sender.sendMessage(HELP_MESSAGE);
    }

    @Command(name = "save", desc = "Save knockback profiles to file")
    @Require("cSpigot.knockback")
    public void save(@Sender CommandSender sender) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        knockbackHandler.save();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eSuccessfully saved all knockback profiles!"));
    }

    @Command(name = "reload", desc = "Reload knockback profiles from file")
    @Require("cSpigot.knockback")
    public void reload(@Sender CommandSender sender) {
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Reloading profiles...."));
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        knockbackHandler.init();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aReloaded all profiles!"));
    }

    @Command(name = "import", usage = "<file.yml>", desc = "Import a knockback profile from a file")
    @Require("cSpigot.knockback")
    public void importKB(@Sender CommandSender sender, String fileName) {
        KnockbackConverter converter = new KnockbackConverter();
        converter.migrate(sender, fileName);
    }

    @Command(name = "list", usage = "[pageNumber]", aliases = "profiles", desc = "View list of loaded knockback profiles")
    @Require("cSpigot.knockback")
    public void list(@Sender CommandSender sender, @OptArg("1") int pageNumber) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();

        List<String> profiles = knockbackHandler.getProfiles().stream().map(KnockbackProfile::getName).collect(Collectors.toList());
        ChatPaginator.ChatPage page = ChatPaginator.paginate(profiles, pageNumber, ChatPaginator.OPEN_CHAT_PAGE_HEIGHT);

        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(CC.translate(color + CC.BOLD + "Knockback Profiles " + (page.getTotalPages() > 1 ? "&7(Page #" + page.getPageNumber() + ")" : "")));
        sender.sendMessage("");

        for (String name : page.getLines()) {
            KnockbackProfile profile = knockbackHandler.getKnockbackProfile(name);
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Clickable knockback = new Clickable(
                        "&7 * " + color.toString() + profile.getName() + " &7[" + profile.getType().name() + "]",
                        "&7&oClick to view &e&oinfo &7&oabout this profile.",
                        "/knockback view " + profile.getName());
                knockback.sendToPlayer(player);
            } else {
                sender.sendMessage(CC.translate("&7 * " + color + name));
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (page.getTotalPages() > 1) {
                sender.sendMessage("");
                Clickable clickable;
                if (pageNumber == page.getTotalPages()) {
                    clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page", "/knockback list " + (pageNumber - 1));
                } else if (pageNumber > 1) {
                    clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page", "/knockback list " + (pageNumber - 1));
                    clickable.add(" &a[Next Page]", "&7Click here to view next page", "/knockback list " + (pageNumber + 1));
                } else {
                    clickable = new Clickable("&7 * &a[Next Page]", "&7Click here to view next page", "/knockback list " + (pageNumber + 1));
                }
                clickable.sendToPlayer(player);
            }
        }

        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }

    @Command(name = "view", usage = "<profile> [pageNumber]", aliases = {"values", "info"}, desc = "View information about knockback profile")
    @Require("cSpigot.knockback")
    public void view(@Sender CommandSender sender, KnockbackProfile profile, @OptArg("1") int pageNumber) {
        List<KnockbackValue<?>> values = profile.getProfileValues();

        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(CC.translate(color + "&l" + profile.getName() + "'s Settings"));
        sender.sendMessage(" ");

        for (KnockbackValue<?> value : values) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Clickable knockback = new Clickable(
                        "&7* &e" + value.getName() + ": " + color + value.getValue(),
                        "&7&oClick to &e&oedit &7&othis value.",
                        "/knockback edit " + profile.getName() + " " + value.getId() + " ", true);

                knockback.sendToPlayer(player);
            } else {
                sender.sendMessage(CC.translate("&7* &e" + value.getName() + ": " + color + value.getValue()));
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage("");
            Clickable otherValues = new Clickable("&7 * &e[Projectile Values]", "&7Click here to view projectile values", "/knockback viewProjectile " + profile.getName());
            otherValues.add(" &a[DamageTick Values]", "&7Click here to view damage-tick values", "/knockback viewDamage " + profile.getName());

            otherValues.sendToPlayer(player);
        }

        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }

    @Command(name = "viewprojectile", usage = "<profile> [pageNumber]", aliases = "projectile", desc = "View knockback profile's projectile values")
    @Require("cSpigot.knockback")
    public void projectile(@Sender CommandSender sender, KnockbackProfile profile, @OptArg("1") int pageNumber) {
        List<KnockbackValue<?>> values = profile.getProjectileValues();
        List<String> profiles = values.stream().map(KnockbackValue::getId).collect(Collectors.toList());
        ChatPaginator.ChatPage page = ChatPaginator.paginate(profiles, pageNumber, ChatPaginator.OPEN_CHAT_PAGE_HEIGHT);

        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(CC.translate(color + "&l" + profile.getName() + "'s Projectile Settings " + (page.getTotalPages() > 1 ? "&7(Page #" + page.getPageNumber() + ")" : "")));
        sender.sendMessage("");

        for (String id : page.getLines()) {
            KnockbackValue<?> value = values.stream().filter(v -> v.getId().equals(id)).findFirst().orElse(null);
            if (value == null) continue;

            if (sender instanceof Player) {
                Player player = (Player) sender;
                Clickable knockback = new Clickable(
                        "&7* &e" + value.getName() + ": " + color + value.getValue(),
                        "&7&oClick to &e&oedit &7&othis value.",
                        "/knockback edit " + profile.getName() + " " + value.getId() + " ", true);

                knockback.sendToPlayer(player);
            } else {
                sender.sendMessage(CC.translate("&7* &e" + value.getName() + ": " + color + value.getValue()));
            }
        }

        if (sender instanceof Player && page.getTotalPages() > 1) {
            Player player = (Player) sender;
            sender.sendMessage("");
            Clickable clickable;
            if (pageNumber == page.getTotalPages()) {
                clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page", "/knockback viewProjectile " + profile.getName() + " " + (pageNumber - 1));
            } else if (pageNumber > 1) {
                clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page", "/knockback viewProjectile " + profile.getName() + " " + (pageNumber - 1));
                clickable.add(" &a[Next Page]", "&7Click here to view next page", "/knockback viewProjectile " + profile.getName() + " " + (pageNumber + 1));
            } else {
                clickable = new Clickable("&7 * &a[Next Page]", "&7Click here to view next page", "/knockback viewProjectile " + profile.getName() + " " + (pageNumber + 1));
            }
            clickable.sendToPlayer(player);
        }

        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }

    @Command(name = "viewdamage", usage = "<profile>", aliases = {"damageTicks", "damage"}, desc = "View knockback profile's damage-tick values")
    @Require("cSpigot.knockback")
    public void damage(@Sender CommandSender sender, KnockbackProfile profile, @OptArg("1") int pageNumber) {
        List<KnockbackValue<?>> values = profile.getDamageTickValues();

        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(CC.translate(color + "&l" + profile.getName() + "'s Damage Settings"));
        sender.sendMessage("");

        for (KnockbackValue<?> value : values) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Clickable knockback = new Clickable(
                        "&7* &e" + value.getName() + ": " + color + value.getValue(),
                        "&7&oClick to &e&oedit &7&othis value.",
                        "/knockback edit " + profile.getName() + " " + value.getId() + " ", true);

                knockback.sendToPlayer(player);
            } else {
                sender.sendMessage(CC.translate("&7* &e" + value.getName() + ": " + color + value.getValue()));
            }
        }
        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }

    @Command(name = "set", usage = "<profile>", aliases = "use", desc = "Set everyone's profile that's in the same world as you")
    @Require("cSpigot.knockback")
    public void set(@Sender Player player, KnockbackProfile knockbackProfile) {
        int i = 0;
        for (Player other : player.getWorld().getPlayers()) {
            other.cSpigot().setKnockbackProfile(knockbackProfile);
            i++;
        }
        player.sendMessage(CC.translate(StringUtils.PREFIX + "&eSuccessfully updated Knockback profile to " + color + knockbackProfile.getName() + " (" + i + ")"));
    }

    @Command(name = "create", usage = "<name> <type>", aliases = "add", desc = "Create a knockback profile")
    @Require("cSpigot.knockback")
    public void create(@Sender CommandSender sender, String name, KnockbackType type) {
        if (!org.apache.commons.lang3.StringUtils.isAlphanumeric(name)) {
            sender.sendMessage(StringUtils.PREFIX + ChatColor.RED + "Name must only contain alphabets or numbers!");
            return;
        }
        if (name.length() > 16) {
            sender.sendMessage(StringUtils.PREFIX + ChatColor.RED + "Name can't have more than 16 characters!");
            return;
        }

        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        if (knockbackHandler.hasKnockbackProfile(name)) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eA knockback profile with that name already exists."));
            return;
        }

        switch (type) {
            case EASY:
                knockbackHandler.addKnockbackProfile(new EasyKnockbackProfile(name));
                break;
            case DETAILED:
                knockbackHandler.addKnockbackProfile(new DetailedKnockbackProfile(name));
                break;
            case SMOOTH:
                knockbackHandler.addKnockbackProfile(new SmoothKnockbackProfile(name));
                break;
            case EXPERT:
                knockbackHandler.addKnockbackProfile(new ExpertKnockbackProfile(name));
                break;
            case EXPLICIT:
                knockbackHandler.addKnockbackProfile(new ExplicitKnockbackProfile(name));
                break;
            case EXPERIMENTAL:
                knockbackHandler.addKnockbackProfile(new ExperimentalKnockbackProfile(name));
                break;
        }
        knockbackHandler.save();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Knockback profile created " + color + name + "&7 on type " + color + type.name()));
    }

    @Command(name = "edit", aliases = "change", usage = "<profile> <field> <value>", desc = "Edit a knockback profile's certain value")
    @Require("cSpigot.knockback")
    @SuppressWarnings("unchecked")
    public void edit(@Sender CommandSender sender, KnockbackProfile profile, KnockbackValue field, String value) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();

        if (field.getValue() instanceof Integer) {
            try {
                field.setValue(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            }
        } else if (field.getValue() instanceof Double) {
            try {
                double doubleValue = Double.parseDouble(value);
                if (Double.isNaN(doubleValue)) {
                    throw new NumberFormatException();
                }
                field.setValue(doubleValue);
            } catch (NumberFormatException e) {
                sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            }
        } else if (field.getValue() instanceof Boolean) {
            try {
                field.setValue(Boolean.parseBoolean(value));
            } catch (NumberFormatException e) {
                sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            }
        }

        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You've updated " + color + profile.getName() + "&7's " + color + field.getName() + "&7! &8[" + color + field.getValue() + "&8]"));
        knockbackHandler.save();
    }

    @Command(name = "delete", usage = "<profile>", aliases = "remove", desc = "Delete a knockback profile")
    @Require("cSpigot.knockback")
    public void delete(@Sender CommandSender sender, KnockbackProfile profile) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();

        if (profile.equals(knockbackHandler.getDefaultKnockback())) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&ennot delete the default profile."));
            return;
        }

        knockbackHandler.removeKnockbackProfile(profile.getName());
        knockbackHandler.save();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Successfully deleted " + color + profile.getName() + " &7knockback profile"));
    }

    @Command(name = "setprofile", usage = "<target> <profile>", aliases = {"setPlayer", "setKB"}, desc = "Set a player's knockback profile")
    @Require("cSpigot.knockback")
    public void setProfile(@Sender CommandSender sender, Player target, KnockbackProfile profile) {
        target.cSpigot().setKnockbackProfile(profile);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Successfully set " + color + target.getName() + "'s &7knockback profile to " + color + profile.getName()));
    }

    @Command(name = "settype", aliases = "recreate", desc = "Recreate a knockback profile with the given type")
    @Require("cSpigot.knockback")
    public void setType(@Sender CommandSender sender, KnockbackProfile profile, KnockbackType type) {
        String name = profile.getName();
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        knockbackHandler.removeKnockbackProfile(name);

        switch (type) {
            case EASY:
                knockbackHandler.addKnockbackProfile(new EasyKnockbackProfile(name.toLowerCase()));
                break;
            case DETAILED:
                knockbackHandler.addKnockbackProfile(new DetailedKnockbackProfile(name.toLowerCase()));
                break;
            case SMOOTH:
                knockbackHandler.addKnockbackProfile(new SmoothKnockbackProfile(name.toLowerCase()));
                break;
            case EXPERT:
                knockbackHandler.addKnockbackProfile(new ExpertKnockbackProfile(name.toLowerCase()));
                break;
            case EXPLICIT:
                knockbackHandler.addKnockbackProfile(new ExplicitKnockbackProfile(name.toLowerCase()));
                break;
            case EXPERIMENTAL:
                knockbackHandler.addKnockbackProfile(new ExperimentalKnockbackProfile(name));
                break;
        }
        knockbackHandler.save();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Knockback profile re-created " + color + name.toLowerCase() + " &7 on type " + color + type.name()));
    }

    @Command(name = "setdefault", usage = "<profile>", aliases = "default", desc = "Set default knockback profile")
    @Require("cSpigot.knockback")
    public void setDefault(@Sender CommandSender sender, KnockbackProfile knockbackProfile) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        knockbackHandler.setDefaultKnockback(knockbackProfile.getName());
        knockbackHandler.save();

        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Successfully updated the default knockback to " + color + knockbackProfile.getName()));
    }

    @Command(name = "getdefault", aliases = "viewDefault", desc = "Get default knockback profile")
    @Require("cSpigot.knockback")
    public void getDefault(@Sender CommandSender sender) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        this.view(sender, knockbackHandler.getDefaultKnockback(), 0);
    }
}
