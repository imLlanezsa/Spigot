package net.minecraft.server;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.IOException;
import java.util.List;
import net.minecraft.server.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.WorldSettings.EnumGamemode;

public class PacketPlayOutPlayerInfo implements Packet<PacketListenerPlayOut> {
    private EnumPlayerInfoAction a;
    private List<PlayerInfoData> b = Lists.newArrayList();

    public PacketPlayOutPlayerInfo() {
    }

    public PacketPlayOutPlayerInfo(EnumPlayerInfoAction packetplayoutplayerinfo_enumplayerinfoaction, EntityPlayer... aentityplayer) {
        this.a = packetplayoutplayerinfo_enumplayerinfoaction;

        for(EntityPlayer entityplayer : aentityplayer) {
            this.b.add(new PlayerInfoData(entityplayer.getProfile(), entityplayer.ping, entityplayer.playerInteractManager.getGameMode(), entityplayer.getPlayerListName()));
        }

    }

    public PacketPlayOutPlayerInfo(EnumPlayerInfoAction packetplayoutplayerinfo_enumplayerinfoaction, Iterable<EntityPlayer> iterable) {
        this.a = packetplayoutplayerinfo_enumplayerinfoaction;

        for(EntityPlayer entityplayer : iterable) {
            this.b.add(new PlayerInfoData(entityplayer.getProfile(), entityplayer.ping, entityplayer.playerInteractManager.getGameMode(), entityplayer.getPlayerListName()));
        }

    }

    public PacketPlayOutPlayerInfo(EnumPlayerInfoAction packetplayoutplayerinfo_enumplayerinfoaction, List<PlayerInfoData> b) {
        this.a = packetplayoutplayerinfo_enumplayerinfoaction;
        this.b = b;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = (EnumPlayerInfoAction)packetdataserializer.a(EnumPlayerInfoAction.class);
        int i = packetdataserializer.e();

        for(int j = 0; j < i; ++j) {
            GameProfile gameprofile = null;
            int k = 0;
            WorldSettings.EnumGamemode worldsettings_enumgamemode = null;
            IChatBaseComponent ichatbasecomponent = null;
            switch (this.a) {
                case ADD_PLAYER:
                    gameprofile = new GameProfile(packetdataserializer.g(), packetdataserializer.c(16));
                    int l = packetdataserializer.e();
                    int i1 = 0;

                    for(; i1 < l; ++i1) {
                        String s = packetdataserializer.c(32767);
                        String s1 = packetdataserializer.c(32767);
                        if (packetdataserializer.readBoolean()) {
                            gameprofile.getProperties().put(s, new Property(s, s1, packetdataserializer.c(32767)));
                        } else {
                            gameprofile.getProperties().put(s, new Property(s, s1));
                        }
                    }

                    worldsettings_enumgamemode = EnumGamemode.getById(packetdataserializer.e());
                    k = packetdataserializer.e();
                    if (packetdataserializer.readBoolean()) {
                        ichatbasecomponent = packetdataserializer.d();
                    }
                    break;
                case UPDATE_GAME_MODE:
                    gameprofile = new GameProfile(packetdataserializer.g(), (String)null);
                    worldsettings_enumgamemode = EnumGamemode.getById(packetdataserializer.e());
                    break;
                case UPDATE_LATENCY:
                    gameprofile = new GameProfile(packetdataserializer.g(), (String)null);
                    k = packetdataserializer.e();
                    break;
                case UPDATE_DISPLAY_NAME:
                    gameprofile = new GameProfile(packetdataserializer.g(), (String)null);
                    if (packetdataserializer.readBoolean()) {
                        ichatbasecomponent = packetdataserializer.d();
                    }
                    break;
                case REMOVE_PLAYER:
                    gameprofile = new GameProfile(packetdataserializer.g(), (String)null);
            }

            this.b.add(new PlayerInfoData(gameprofile, k, worldsettings_enumgamemode, ichatbasecomponent));
        }

    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.a(this.a);
        packetdataserializer.b(this.b.size());

        for(PlayerInfoData packetplayoutplayerinfo_playerinfodata : this.b) {
            switch (this.a) {
                case ADD_PLAYER:
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getId());
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getName());
                    packetdataserializer.b(packetplayoutplayerinfo_playerinfodata.a().getProperties().size());

                    for(Property property : packetplayoutplayerinfo_playerinfodata.a().getProperties().values()) {
                        packetdataserializer.a(property.getName());
                        packetdataserializer.a(property.getValue());
                        if (property.hasSignature()) {
                            packetdataserializer.writeBoolean(true);
                            packetdataserializer.a(property.getSignature());
                        } else {
                            packetdataserializer.writeBoolean(false);
                        }
                    }

                    packetdataserializer.b(packetplayoutplayerinfo_playerinfodata.c().getId());
                    packetdataserializer.b(packetplayoutplayerinfo_playerinfodata.b());
                    if (packetplayoutplayerinfo_playerinfodata.d() == null) {
                        packetdataserializer.writeBoolean(false);
                    } else {
                        packetdataserializer.writeBoolean(true);
                        packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.d());
                    }
                    break;
                case UPDATE_GAME_MODE:
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getId());
                    packetdataserializer.b(packetplayoutplayerinfo_playerinfodata.c().getId());
                    break;
                case UPDATE_LATENCY:
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getId());
                    packetdataserializer.b(packetplayoutplayerinfo_playerinfodata.b());
                    break;
                case UPDATE_DISPLAY_NAME:
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getId());
                    if (packetplayoutplayerinfo_playerinfodata.d() == null) {
                        packetdataserializer.writeBoolean(false);
                    } else {
                        packetdataserializer.writeBoolean(true);
                        packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.d());
                    }
                    break;
                case REMOVE_PLAYER:
                    packetdataserializer.a(packetplayoutplayerinfo_playerinfodata.a().getId());
            }
        }

    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public String toString() {
        return Objects.toStringHelper(this).add("action", this.a).add("entries", this.b).toString();
    }

    public EnumPlayerInfoAction getA() {
        return this.a;
    }

    public List<PlayerInfoData> getB() {
        return this.b;
    }

    public void setA(EnumPlayerInfoAction a) {
        this.a = a;
    }

    public void setB(List<PlayerInfoData> b) {
        this.b = b;
    }

    public static class PlayerInfoData {
        private final int b;
        private final WorldSettings.EnumGamemode c;
        private final GameProfile d;
        private final IChatBaseComponent e;

        public PlayerInfoData(GameProfile gameprofile, int i, WorldSettings.EnumGamemode worldsettings_enumgamemode, IChatBaseComponent ichatbasecomponent) {
            this.d = gameprofile;
            this.b = i;
            this.c = worldsettings_enumgamemode;
            this.e = ichatbasecomponent;
        }

        public PlayerInfoData(PacketPlayOutPlayerInfo playerInfo, GameProfile gameprofile, int i, WorldSettings.EnumGamemode worldsettings_enumgamemode, IChatBaseComponent ichatbasecomponent) {
            this(gameprofile, i, worldsettings_enumgamemode, ichatbasecomponent);
        }

        public GameProfile a() {
            return this.d;
        }

        public int b() {
            return this.b;
        }

        public WorldSettings.EnumGamemode c() {
            return this.c;
        }

        public IChatBaseComponent d() {
            return this.e;
        }

        public String toString() {
            return Objects.toStringHelper(this).add("latency", this.b).add("gameMode", this.c).add("profile", this.d).add("displayName", this.e == null ? null : ChatSerializer.a(this.e)).toString();
        }
    }

    public static enum EnumPlayerInfoAction {
        ADD_PLAYER,
        UPDATE_GAME_MODE,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME,
        REMOVE_PLAYER;

        private EnumPlayerInfoAction() {
        }
    }
}
