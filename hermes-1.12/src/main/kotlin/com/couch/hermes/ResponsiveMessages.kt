package com.couch.hermes

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ResponsiveServerMessage() : ResponsiveSidedMessage(){
    constructor(responsiveDataPacket: ResponsiveDataPacket) : this(){
        this.dataPacket = responsiveDataPacket
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("uuid")){
            val name = d.getString("uuid")
            val packet = d.getCompoundTag("packet")
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveResponsiveDataPacket(name) ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setTag("packet", this.dataPacket!!.prepareMessageData())
        val uuid = CommonDataSpace.storeResponsiveDataPackets(this.dataPacket!!)
        d.setString("uuid", uuid)
        ByteBufUtils.writeTag(buf, d)
    }

}

class ResponsiveServerMessageHandler : ResponsiveSidedMessageHandler<ResponsiveServerMessage, ClientSideMessage>(){
    override fun onMessage(message: ResponsiveServerMessage, ctx: MessageContext): ClientSideMessage? {
        val player = ctx.serverHandler.player
        val world = player.serverWorld
        val data = message.data
        val dataPacket = message.dataPacket!!
        world.addScheduledTask {
            dataPacket.processMessageData(data, world, player)
        }
        val respPacket = DataPacket(dataPacket.prepareResponseData, dataPacket.processResponseData)
        return ClientSideMessage(respPacket)
    }

}

class ResponsiveClientMessage() : ResponsiveSidedMessage(){
    constructor(data: ResponsiveDataPacket) : this(){
        this.dataPacket = data
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("uuid")){
            val uuid = d.getString("uuid")
            val packet = d.getCompoundTag("packet")
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveResponsiveDataPacket(uuid) ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setTag("packet", this.dataPacket!!.prepareMessageData())
        val uuid = CommonDataSpace.storeResponsiveDataPackets(this.dataPacket!!)
        d.setString("uuid", uuid)
        ByteBufUtils.writeTag(buf, d)
    }

}

class ResponsiveClientMessageHandler : ResponsiveSidedMessageHandler<ResponsiveClientMessage, ServerSideMessage>(){
    override fun onMessage(message: ResponsiveClientMessage, ctx: MessageContext): ServerSideMessage? {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
        val player = mc.player
        val data = message.data
        val dataPacket = message.dataPacket!!
        mc.addScheduledTask {
            dataPacket.processMessageData(data, world, player)
        }
        val responseDataPacket = DataPacket(
                dataPacket.prepareResponseData,
                dataPacket.processResponseData
        )
        return ServerSideMessage(responseDataPacket)
    }

}