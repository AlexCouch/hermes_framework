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

val responsiveSidedMessageEncoder: BiConsumer<ResponsiveSidedMessage, PacketBuffer> = BiConsumer { message, packet ->
    val d = CompoundNBT()
    d.putString("name", message.name)
    d.put("data", message.dataPacket.prepareMessageData.invoke() ?: CompoundNBT())
    d.put("pos", NBTUtil.writeBlockPos(message.pos))
    packet.writeCompoundTag(d)
    CommonDataSpace.storeResponsiveDataPackets(message.name, message.dataPacket)
}

val responsiveSidedMessageDecoder: Function<PacketBuffer, ResponsiveSidedMessage?> = Function{ packet ->
    val d = packet.readCompoundTag() ?: throw IllegalStateException("Received packet has no compound tag!")
    if(d.contains("data") && d.contains("pos") && d.contains("name")){
        val name = d.getString("name")
        val data = d.getCompound("data")
        val postag = d.getCompound("pos")
        val pos = (NBTUtil.readBlockPos(postag))
        val dataPacket = CommonDataSpace.retrieveResponsiveDataPacket(name) ?: throw IllegalStateException("Packet does not exist in common data space!")
        return@Function ResponsiveSidedMessage(name, dataPacket, pos, data)
    }
    null
}

val responsiveSidedMessagehandler: BiConsumer<ResponsiveSidedMessage, Supplier<NetworkEvent.Context>> = BiConsumer{ message, context ->
    context.get().enqueueWork {
        val player = context.get().sender!!
        val world = player.serverWorld
        val data = message.data
        val dataPacket = message.dataPacket ?: return@enqueueWork
        val pos = message.pos
        dataPacket.processMessageData(data, world, pos, player)
        context.get().packetHandled = true
        MessageFactory.sendDataToClient("${message.name}:response", player, pos, dataPacket.prepareResponseData, dataPacket.processResponseData)
    }
}

class ResponsiveSidedMessage(val name: String, var dataPacket: ResponsiveDataPacket, var pos: BlockPos, val data: CompoundNBT = CompoundNBT())