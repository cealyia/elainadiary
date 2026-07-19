package com.cealyia.elainadiary;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ElainaDiaryMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        CHANNEL.registerMessage(
                nextId(),
                DiaryPacket.class,
                DiaryPacket::encode,
                DiaryPacket::decode,
                DiaryPacket::handle
        );
    }
}