package com.couch.hermes

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler

typealias ProcessData = (data: NBTTagCompound, world: World, pos: BlockPos, player: EntityPlayer) -> Unit

object CommonDataSpace{
    private val dataPackets = HashMap<String, DataPacket>()
    private val responsiveDataPackets = HashMap<String, ResponsiveDataPacket>()

    fun storeDataPackets(name: String, data: DataPacket){
        this.dataPackets += (name to data)
    }

    fun storeResponsiveDataPackets(name: String, data: ResponsiveDataPacket){
        this.responsiveDataPackets += (name to data)
    }

    fun retrieveDataPacket(name: String) = this.dataPackets.remove(name)

    fun retrieveResponsiveDataPacket(name: String) = this.responsiveDataPackets.remove(name)
}

data class DataPacket(val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData)

data class ResponsiveDataPacket(
        val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData,
        val prepareResponseData: () -> NBTTagCompound, val processResponseData: ProcessData
)

abstract class BasicSidedMessage() : IMessage{
    var name: String = ""
    var dataPacket: DataPacket? = null
    var pos: BlockPos = BlockPos.ORIGIN
    var data = NBTTagCompound()
}

abstract class ResponsiveSidedMessage() : IMessage{
    var name: String = ""
    var dataPacket: ResponsiveDataPacket? = null
    var pos: BlockPos = BlockPos.ORIGIN
    var data = NBTTagCompound()
}

abstract class BasicSidedMessageHandler<M : BasicSidedMessage> : IMessageHandler<M, IMessage>
abstract class ResponsiveSidedMessageHandler<M : ResponsiveSidedMessage, R : BasicSidedMessage> : IMessageHandler<M, R>