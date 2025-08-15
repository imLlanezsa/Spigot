package io.github.cspigot.utils.collection;

import lombok.RequiredArgsConstructor;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.PacketPlayOutAnimation;
import net.minecraft.server.PacketPlayOutPlayerInfo;
import org.bukkit.craftbukkit.entity.CraftPlayer;

@RequiredArgsConstructor
public class CitizensRunnable implements Runnable {

    private final EntityPlayer entityPlayer;
    private final Entity entity;

    @Override
    public void run() {
        this.entityPlayer.playerConnection.sendPacket(new PacketPlayOutAnimation(entity, 0));
        EntityPlayer player = ((CraftPlayer)entity.getBukkitEntity()).getHandle();
        this.entityPlayer.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, player));
    }
}