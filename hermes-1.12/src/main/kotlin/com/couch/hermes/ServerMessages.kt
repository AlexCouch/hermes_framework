package com.couch.hermes

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ServerSideMessage() : BasicSidedMessage(){

    constructor(dataPacket: DataPacket) : this(){
        this.dataPacket = dataPacket
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("uuid")){
            val uuid = d.getString("uuid")
            val packet = d.getCompoundTag("packet")
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveDataPacket(uuid) ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setTag("packet", this.dataPacket!!.prepareMessageData())
        val uuid = CommonDataSpace.storeDataPackets(this.dataPacket!!)
        d.setString("uuid", uuid)
        ByteBufUtils.writeTag(buf, d)
    }

}

class ServerSideMessageHandler : BasicSidedMessageHandler<ServerSideMessage>(){
    override fun onMessage(message: ServerSideMessage, ctx: MessageContext): IMessage? {
        val player = ctx.serverHandler.player
        val world = player.serverWorld
        world.addScheduledTask {
            message.dataPacket!!.processMessageData(message.data, world, player)
        }
        return null
    }

}