package ca.wacos.nametagedit;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.wacos.nametagedit.NametagChangeEvent.NametagChangeReason;
import ca.wacos.nametagedit.NametagChangeEvent.NametagChangeType;
import ca.wacos.nametagedit.core.NametagManager;
import ca.wacos.nametagedit.tasks.ClearPlayerTask;
import ca.wacos.nametagedit.tasks.DeleteTypeTask;
import ca.wacos.nametagedit.tasks.ModifyTagTask;
import ca.wacos.nametagedit.tasks.UpdateGroupTask;
import ca.wacos.nametagedit.tasks.UpdatePlayerTask;

/**
 * This class is responsible for handling the /ne command.
 * 
 * @author Levi Webb Heavily edited by @sgtcaze
 * 
 */
@SuppressWarnings("deprecation")
public class NametagCommand implements CommandExecutor {

    private NametagEdit plugin = NametagEdit.getInstance();

    public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
        if (!sender.hasPermission("nametagedit.use")) {
            Messages.NO_PERMISSION.send(sender);
            return false;
        }

        if (args.length < 1) {
            Messages.COMMAND_USAGE.send(sender);
        } else if (args.length >= 1) {
            switch (args[0]) {
            case "clear":
                cmdClear(sender, args);
                break;
            case "reload":
                cmdReload(sender, args);
                break;
            case "prefix":
                cmdEdit(sender, args);
                break;
            case "suffix":
                cmdEdit(sender, args);
                break;
            case "groups":
                cmdGroups(sender, args);
                break;
            default:
                Messages.UNRECOGNIZED_VALUE.send(sender, args[0]);
            }
        }

        return false;
    }

    // Clears prefixes and suffixes
    private void cmdClear(final CommandSender sender, String[] args) {
        if (!(sender.hasPermission("nametagedit.clear.self") || sender.hasPermission("nametagedit.clear.others"))) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (args.length != 2) {
            Messages.USAGE_CLEAR.send(sender);
        } else if (args.length == 2) {
            final String targetName = args[1];

            if (!sender.hasPermission("nametagedit.clear.others") && !targetName.equalsIgnoreCase(sender.getName())) {
                Messages.MODIFY_OWN_TAG.send(sender);
                return;
            }

            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                new ClearPlayerTask(sender, targetName).runTaskAsynchronously(plugin);     
            } else {
                final String uuid = target.getUniqueId().toString();

                NametagManager.clear(target.getName());

                if (plugin.getNTEHandler().getPlayerData().containsKey(uuid)) {
                    plugin.getNTEHandler().getPlayerData().remove(uuid);
                }

                if (plugin.getNTEHandler().usingDatabase()) {
                    new DeleteTypeTask("players", "uuid", uuid).runTaskAsynchronously(plugin);
                }
            }
        }
    }

    // Reloads from file or memory
    private void cmdReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nametagedit.reload")) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (args.length != 2) {
            // TODO: Send FILE/MEMORY Usage
        } else if (args[1].equalsIgnoreCase("file")) {
            plugin.getNTEHandler().reload(sender, true);
        } else if (args[1].equalsIgnoreCase("memory")) {
            plugin.getNTEHandler().reload(sender, false);
        }
    }

    // Sets prefix or suffix
    private void cmdEdit(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("nametagedit.edit.self") || sender.hasPermission("nametagedit.edit.others"))) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (args.length <= 2) {
            // TODO: Send usage
        } else if (args.length > 2) {
            String targetName = args[1];

            if (!sender.hasPermission("nametagedit.edit.others") && !targetName.equalsIgnoreCase(sender.getName())) {
                Messages.MODIFY_OWN_TAG.send(sender);
                return;
            }
            
            String type = args[0].toLowerCase();

            Player target = Bukkit.getPlayer(args[1]);

            String oper = format(args, 2, args.length);

            setType(sender, targetName, type, NametagAPI.trim(oper));

            if (plugin.getNTEHandler().usingDatabase() && target != null) {
                new UpdatePlayerTask(target.getUniqueId().toString(), target.getName(), NametagManager.getPrefix(target.getName()), NametagManager.getSuffix(target.getName())).runTaskAsynchronously(plugin);
            }
        }
    }

    // Groups subcommand
    private void cmdGroups(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nametagedit.groups")) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder();

                for (String s : plugin.getNTEHandler().getAllGroups()) {
                    sb.append("§6" + s + "§f,");
                }

                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 2);
                }
                
                Messages.LOADED_GROUPS.send(sender, sb.toString());
            } else if (args[1].equalsIgnoreCase("remove")) {
                if (args.length == 3) {
                    String group = args[2];

                    if (plugin.getNTEHandler().getAllGroups().contains(group)) {
                        plugin.getNTEHandler().getAllGroups().remove(group);
                        plugin.getNTEHandler().getGroupData().remove(group);
                    }

                    if (plugin.getNTEHandler().usingDatabase()) {
                        new DeleteTypeTask("groups", "name", group).runTaskAsynchronously(plugin);
                    }

                    Messages.GROUP_REMOVED.send(sender, group);
                }
            } else if (args[1].equalsIgnoreCase("add")) {
                if (args.length == 3) {
                    String group = args[2];
                    if (!plugin.getNTEHandler().getGroupData().containsKey(group)) {
                        plugin.getNTEHandler().getGroupData().put(group, Arrays.asList("", "", ""));
                        // sender.sendMessage(prefix + "§fThe group §c" + group + " §fhas been added!");

                        // TODO: Update mysql data
                    } else {
                        Messages.GROUP_EXISTS.send(sender, group);
                    }
                }
            } else if (args[1].equalsIgnoreCase("set")) {
                if (args.length >= 5) {
                    String group = args[3];

                    if (!plugin.getNTEHandler().getGroupData().containsKey(group)) {
                        Messages.GROUP_EXISTS_NOT.send(sender, group);
                        return;
                    }

                    if (args[2].equalsIgnoreCase("perm")) {
                        plugin.getNTEHandler().getGroupData().get(group).set(0, args[4]);
                        plugin.getNTEHandler().getPermissions().remove(plugin.getNTEHandler().getGroupData().get(group).get(2));
                        plugin.getNTEHandler().getPermissions().put(args[4], group);
                        
                        Messages.GROUP_VALUE.sendMulti(sender, group, "permission", args[4]);
                        
                        if (plugin.getNTEHandler().usingDatabase()) {
                            new UpdateGroupTask("groups", "permission", group, args[4]).runTaskAsynchronously(plugin);
                        }
                    } else if (args[2].equalsIgnoreCase("prefix")) {
                        String oper = format(args, 4, args.length);

                        plugin.getNTEHandler().getGroupData().get(group).set(1, NametagAPI.trim(oper));

                        Messages.GROUP_VALUE.sendMulti(sender, group, "prefix", NametagAPI.trim(oper));
                        
                        if (plugin.getNTEHandler().usingDatabase()) {
                            new UpdateGroupTask("groups", "prefix", group, NametagAPI.trim(oper)).runTaskAsynchronously(plugin);
                        }
                    } else if (args[2].equalsIgnoreCase("suffix")) {
                        String oper = format(args, 4, args.length);

                        plugin.getNTEHandler().getGroupData().get(group).set(2, NametagAPI.trim(oper));
                        
                        Messages.GROUP_VALUE.sendMulti(sender, group, "suffix", NametagAPI.trim(oper));

                        if (plugin.getNTEHandler().usingDatabase()) {
                            new UpdateGroupTask("groups", "suffix", group, NametagAPI.trim(oper)).runTaskAsynchronously(plugin);
                        }
                    }
                } else {
                    Messages.GROUP_USAGE.send(sender);
                }
            }
        }
    }

    /**
     * Updates the playerData hashmap and reloads the content async if the
     * player is offline (to get their UUID)
     */
    public void setType(CommandSender sender, String targetName, String type, String args) {
        NametagChangeReason reason = null;

        int id = 0;
        
        if(type.equals("prefix")) {
            reason = NametagChangeReason.SET_PREFIX;
            id = 1;
        } else {
            reason = NametagChangeReason.SET_SUFFIX;
            id = 2;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            new ModifyTagTask(sender, targetName, args, id).runTaskAsynchronously(plugin);
        } else {
            String uuid = target.getUniqueId().toString();

            if (!plugin.getNTEHandler().getPlayerData().containsKey(uuid)) {
                plugin.getNTEHandler().getPlayerData().put(uuid, Arrays.asList(targetName, "", ""));
            }

            plugin.getNTEHandler().getPlayerData().get(uuid).set(id, args);

            if (reason == NametagChangeReason.SET_PREFIX) {
                setNametagSoft(target.getName(), args, "", reason);
            } else {
                setNametagSoft(target.getName(), "", args, reason);
            }
        }
    }
    
    private String format(String[] text, int to, int from) {
        return ChatColor.translateAlternateColorCodes('&', StringUtils.join(text, ' ', to, from)).replace("'", "");
    }

    /**
     * Sets a player's nametag with the given information and additional reason.
     * 
     * @param player
     *            the player whose nametag to set
     * @param prefix
     *            the prefix to set
     * @param suffix
     *            the suffix to set
     * @param reason
     *            the reason for setting the nametag
     */
    public static void setNametagSoft(String player, String prefix, String suffix, NametagChangeReason reason) {
        NametagChangeEvent e = new NametagChangeEvent(player, NametagAPI.getPrefix(player), NametagAPI.getSuffix(player), prefix, suffix, NametagChangeType.SOFT, reason);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            NametagManager.update(player, prefix, suffix);
        }
    }
}