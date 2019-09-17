package com.couch.hermes

import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.NBTUtil
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

val basicSidedMessageEncoder: BiConsumer<BasicSideMessage, PacketBuffer> = BiConsumer { message, packet ->
    val d = CompoundNBT()
    d.put("data", message.dataPacket.prepareMessageData.invoke())
    val uuid = CommonDataSpace.storeDataPackets(message.dataPacket)
    d.putString("uuid", uuid)
    packet.writeCompoundTag(d)
}

val basicSidedMessageDecoder: Function<PacketBuffer, BasicSideMessage?> = Function{ packet ->
    val d = packet.readCompoundTag() ?: throw IllegalStateException("Received packet has no compound tag!")
    if(d.contains("data") && d.contains("uuid")){
        val uuid = d.getString("uuid")
        val data = d.getCompound("data")
        val dataPacket = CommonDataSpace.retrieveDataPacket(uuid) ?: throw IllegalStateException("Packet does not exist in common data space!")
        return@Function BasicSideMessage(dataPacket, data)
    }
    null
}

val basicSidedMessageHandler: BiConsumer<BasicSideMessage, Supplier<NetworkEvent.Context>> = BiConsumer{ message, context ->
    context.get().enqueueWork {
        val mc = Minecraft.getInstance()
        val world = mc.world
        val player = mc.player
        message.dataPacket.processMessageData(message.data, world, player)
        context.get().packetHandled = true
    }
}

data class BasicSideMessage(val dataPacket: DataPacket, val data: CompoundNBT = CompoundNBT())