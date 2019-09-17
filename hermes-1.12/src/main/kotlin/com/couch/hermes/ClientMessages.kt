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
    constructor(name: String, data: DataPacket, pos: BlockPos) : this(){
        this.dataPacket = data
        this.pos = pos
        this.name = name
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("pos") && d.hasKey("name")){
            val name = d.getString("name")
            val packet = d.getCompoundTag("packet")
            val postag = d.getCompoundTag("pos")
            this.pos = NBTUtil.getPosFromTag(postag)
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveDataPacket(name) ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setString("name", this.name)
        d.setTag("packet", this.dataPacket?.prepareMessageData?.invoke() ?: NBTTagCompound())
        d.setTag("pos", NBTUtil.createPosTag(this.pos))
        ByteBufUtils.writeTag(buf, d)
        CommonDataSpace.storeDataPackets(this.name, this.dataPacket ?: return)
    }

}

class ClientSideMessageHandler : BasicSidedMessageHandler<ClientSideMessage>(){
    override fun onMessage(message: ClientSideMessage, ctx: MessageContext): IMessage? {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
        val player = mc.player
        mc.addScheduledTask {
            message.dataPacket?.processMessageData?.invoke(message.data, world, message.pos, player)
        }
        return null
    }
}