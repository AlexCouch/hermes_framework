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
    d.putString("name", message.name)
    d.put("data", message.dataPacket.prepareMessageData.invoke())
    d.put("pos", NBTUtil.writeBlockPos(message.pos))
    packet.writeCompoundTag(d)
    CommonDataSpace.storeDataPackets(message.name, message.dataPacket)
}

val basicSidedMessageDecoder: Function<PacketBuffer, BasicSideMessage?> = Function{ packet ->
    val d = packet.readCompoundTag() ?: throw IllegalStateException("Received packet has no compound tag!")
    if(d.contains("data") && d.contains("pos") && d.contains("name")){
        val name = d.getString("name")
        val data = d.getCompound("data")
        val postag = d.getCompound("pos")
        val pos = (NBTUtil.readBlockPos(postag))
        val dataPacket = CommonDataSpace.retrieveDataPacket(name) ?: throw IllegalStateException("Packet does not exist in common data space!")
        return@Function BasicSideMessage(name, dataPacket, pos, data)
    }
    null
}

val basicSidedMessageHandler: BiConsumer<BasicSideMessage, Supplier<NetworkEvent.Context>> = BiConsumer{ message, context ->
    context.get().enqueueWork {
        val mc = Minecraft.getInstance()
        val world = mc.world
        val player = mc.player
        message.dataPacket.processMessageData.invoke(message.data, world, message.pos, player)
        context.get().packetHandled = true
    }
}

data class BasicSideMessage(val name: String, val dataPacket: DataPacket, val pos: BlockPos, val data: CompoundNBT = CompoundNBT())