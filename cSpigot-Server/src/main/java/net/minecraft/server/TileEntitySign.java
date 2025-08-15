package net.minecraft.server;

import com.google.gson.JsonParseException;
import net.minecraft.server.ChatClickable.EnumClickAction;
import net.minecraft.server.IChatBaseComponent.ChatSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class TileEntitySign extends TileEntity {

    public final IChatBaseComponent[] lines = new IChatBaseComponent[]{new ChatComponentText(""), new ChatComponentText(""), new ChatComponentText(""), new ChatComponentText("")};


    public int f = -1;
    public boolean isEditable = true;
    private EntityHuman h;
    private final CommandObjectiveExecutor i = new CommandObjectiveExecutor();

    public static final int MAX_SIGN_LINE_LENGTH = Integer.getInteger("Paper.maxSignLength", 80); // Paper


    public TileEntitySign() {
    }

    public void b(NBTTagCompound var1) {
        super.b(var1);

        for(int var2 = 0; var2 < 4; ++var2) {
            String var3 = ChatSerializer.a(this.lines[var2]);
            var1.setString("Text" + (var2 + 1), var3);
        }

        this.i.b(var1);
    }

    public void a(NBTTagCompound var1) {
        this.isEditable = false;
        super.a(var1);
        ICommandListener var2 = new ICommandListener() {
            public String getName() {
                return "Sign";
            }

            public IChatBaseComponent getScoreboardDisplayName() {
                return new ChatComponentText(this.getName());
            }

            public void sendMessage(IChatBaseComponent var1) {
            }

            public boolean a(int var1, String var2) {
                return true;
            }

            public BlockPosition getChunkCoordinates() {
                return TileEntitySign.this.position;
            }

            public Vec3D d() {
                return new Vec3D((double)TileEntitySign.this.position.getX() + 0.5, (double)TileEntitySign.this.position.getY() + 0.5, (double)TileEntitySign.this.position.getZ() + 0.5);
            }

            public World getWorld() {
                return TileEntitySign.this.world;
            }

            public Entity f() {
                return null;
            }

            public boolean getSendCommandFeedback() {
                return false;
            }

            public void a(CommandObjectiveExecutor.EnumCommandResult var1, int var2) {
            }
        };

        for(int var3 = 0; var3 < 4; ++var3) {
            String var4 = var1.getString("Text" + (var3 + 1));

            try {
                IChatBaseComponent var5 = ChatSerializer.a(var4);

                try {
                    this.lines[var3] = ChatComponentUtils.filterForDisplay(var2, var5, (Entity)null);
                } catch (CommandException var7) {
                    this.lines[var3] = var5;
                }
            } catch (JsonParseException var8) {
                this.lines[var3] = new ChatComponentText(var4);
            }
        }

        this.i.a(var1);
    }

    public Packet getUpdatePacket() {
        IChatBaseComponent[] var1 = new IChatBaseComponent[4];
        System.arraycopy(this.lines, 0, var1, 0, 4);
        return new PacketPlayOutUpdateSign(this.world, this.position, var1);
    }

    public boolean F() {
        return true;
    }

    public boolean b() {
        return this.isEditable;
    }

    public void a(EntityHuman var1) {
        this.h = var1;
    }

    public EntityHuman c() {
        return this.h;
    }

    public boolean b(final EntityHuman var1) {
        // cSpigot start - Patch command exploits
        for ( IChatBaseComponent line : this.lines ) {
            ChatModifier var4 = line == null ? null : line.getChatModifier();
            if (var4 != null && var4.h() != null) {
                ChatClickable var5 = var4.h();
                if (var5.a() == EnumClickAction.RUN_COMMAND) {
                    CraftPlayer craftPlayer = (CraftPlayer) var1.getBukkitEntity();
                    String command = var5.b();

                    command = StringUtils.normalizeSpace(command);

                    for (int i = 0; i < command.length(); ++i) {
                        if (!SharedConstants.isAllowedChatCharacter(command.charAt(i))) {
                            return false;
                        }
                    }

                    if (command.startsWith("/")) {
                        command = command.substring(1);
                    }

                    Bukkit.getServer().dispatchCommand(craftPlayer, command);
                }
            }
        }
        // cSpigot end

        return true;
    }

    public CommandObjectiveExecutor d() {
        return this.i;
    }
}
