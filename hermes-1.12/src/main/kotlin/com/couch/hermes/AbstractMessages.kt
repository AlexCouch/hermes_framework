package com.couch.hermes

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import java.util.*
import kotlin.collections.HashMap

typealias ProcessData = (data: NBTTagCompound, world: World, player: EntityPlayer) -> Unit

object CommonDataSpace{
    private val dataPackets = HashMap<String, DataPacket>()
    private val responsiveDataPackets = HashMap<String, ResponsiveDataPacket>()

    fun storeDataPackets(data: DataPacket): String{
        val uuid = UUID.randomUUID().toString()
        this.dataPackets += (uuid to data)
        return uuid
    }

    fun storeResponsiveDataPackets(data: ResponsiveDataPacket): String{
        val uuid = UUID.randomUUID().toString()
        this.responsiveDataPackets += (uuid to data)
        return uuid
    }

    fun retrieveDataPacket(uuid: String) = this.dataPackets.remove(uuid)

    fun retrieveResponsiveDataPacket(uuid: String) = this.responsiveDataPackets.remove(uuid)
}

data class DataPacket(val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData)

data class ResponsiveDataPacket(
        val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData,
        val prepareResponseData: () -> NBTTagCompound, val processResponseData: ProcessData
)

abstract class BasicSidedMessage() : IMessage{
    var dataPacket: DataPacket? = null
    var data = NBTTagCompound()
}

abstract class ResponsiveSidedMessage() : IMessage{
    var dataPacket: ResponsiveDataPacket? = null
    var data = NBTTagCompound()
}

abstract class BasicSidedMessageHandler<M : BasicSidedMessage> : IMessageHandler<M, IMessage>
abstract class ResponsiveSidedMessageHandler<M : ResponsiveSidedMessage, R : BasicSidedMessage> : IMessageHandler<M, R>