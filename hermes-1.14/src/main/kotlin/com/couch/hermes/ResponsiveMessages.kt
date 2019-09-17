package com.couch.hermes

import net.minecraft.nbt.CompoundNBT
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

val responsiveSidedMessageEncoder: BiConsumer<ResponsiveSidedMessage, PacketBuffer> = BiConsumer { message, packet ->
    val d = CompoundNBT()
    d.put("data", message.dataPacket.prepareMessageData.invoke() ?: CompoundNBT())
    val uuid = CommonDataSpace.storeResponsiveDataPackets(message.dataPacket)
    d.putString("uuid", uuid)
    packet.writeCompoundTag(d)
}

val responsiveSidedMessageDecoder: Function<PacketBuffer, ResponsiveSidedMessage?> = Function{ packet ->
    val d = packet.readCompoundTag() ?: throw IllegalStateException("Received packet has no compound tag!")
    if(d.contains("data") && d.contains("uuid")){
        val uuid = d.getString("uuid")
        val data = d.getCompound("data")
        val dataPacket = CommonDataSpace.retrieveResponsiveDataPacket(uuid) ?: throw IllegalStateException("Packet does not exist in common data space!")
        return@Function ResponsiveSidedMessage(dataPacket, data)
    }
    null
}

val responsiveSidedMessagehandler: BiConsumer<ResponsiveSidedMessage, Supplier<NetworkEvent.Context>> = BiConsumer{ message, context ->
    context.get().enqueueWork {
        val player = context.get().sender!!
        val world = player.serverWorld
        val data = message.data
        val dataPacket = message.dataPacket ?: return@enqueueWork
        dataPacket.processMessageData(data, world, player)
        context.get().packetHandled = true
        MessageFactory.sendDataToClient(player, dataPacket.prepareResponseData, dataPacket.processResponseData)
    }
}

class ResponsiveSidedMessage(var dataPacket: ResponsiveDataPacket, val data: CompoundNBT = CompoundNBT())