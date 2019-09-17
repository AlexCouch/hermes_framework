package com.couch.hermes

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ClientSideMessage() : BasicSidedMessage(){
    constructor(data: DataPacket) : this(){
        this.dataPacket = data
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

class ClientSideMessageHandler : BasicSidedMessageHandler<ClientSideMessage>(){
    override fun onMessage(message: ClientSideMessage, ctx: MessageContext): IMessage? {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
        val player = mc.player
        mc.addScheduledTask {
            message.dataPacket!!.processMessageData(message.data, world, player)
        }
        return null
    }
}