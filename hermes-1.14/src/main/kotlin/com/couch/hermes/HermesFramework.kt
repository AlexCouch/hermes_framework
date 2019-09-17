@file:Suppress("INACCESSIBLE_TYPE", "UNCHECKED_CAST")

package com.couch.hermes

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.INBT
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.network.NetworkDirection
import net.minecraftforge.fml.network.NetworkRegistry
import java.util.*
import kotlin.collections.HashMap

object MessageFactory{
    internal val stream = NetworkRegistry.ChannelBuilder.named(ResourceLocation("hermes","hermes"))
        .clientAcceptedVersions { true }
        .serverAcceptedVersions { true }
        .networkProtocolVersion { "1.0" }
        .simpleChannel()

    fun sendDataToClient(player: PlayerEntity, prepareData: () -> CompoundNBT, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val clientMessage = BasicSideMessage(dataPacket)
        if(player is ServerPlayerEntity) stream.sendTo(clientMessage, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT)
        else throw IllegalStateException("Wrong side!")

    }

    fun sendDataToServer(prepareData: () -> CompoundNBT, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val serverMessage = BasicSideMessage(dataPacket)
        stream.sendToServer(serverMessage)
    }

    fun sendDataToServerWithResponse(
            prepareMessageData: () -> CompoundNBT,
            processMessageData: ProcessData,
            prepareResponseData: () -> CompoundNBT,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveServerMessage = ResponsiveSidedMessage(responsiveDataPacket)
        stream.sendToServer(responsiveServerMessage)
    }

    fun sendDataToClientWithResponse(
            player: PlayerEntity,
            prepareMessageData: DataBuilder,
            processMessageData: ProcessData,
            prepareResponseData: DataBuilder,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveClientMessage = ResponsiveSidedMessage(responsiveDataPacket)
        if(player is ServerPlayerEntity) stream.sendTo(responsiveClientMessage, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT)
        else throw IllegalStateException("Wrong side!")
    }
}

typealias DataBuilder = ()->CompoundNBT
typealias ProcessData = (data: CompoundNBT, world: World, player: PlayerEntity) -> Unit

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

data class DataPacket(val prepareMessageData: () -> CompoundNBT, val processMessageData: ProcessData)

data class ResponsiveDataPacket(
    val prepareMessageData: () -> CompoundNBT, val processMessageData: ProcessData,
    val prepareResponseData: () -> CompoundNBT, val processResponseData: ProcessData
)
object MessageBuilder{
    var player: PlayerEntity? = null
    private var prepareDataFunc: DataBuilder? = null
    private var processDataFunc: ProcessData? = null
    private var prepareResponseDataFunc: DataBuilder? = null

    private var processResponseDataFunc: ProcessData? = null

    fun build(builder: MessageBuilder.()->Unit): MessageBuildResults{
        this.builder()
        val player = player
        val prepareData = prepareDataFunc ?: throw IllegalStateException("prepareDataFunc cannot be null! Please set one!")
        val processData = processDataFunc ?: throw IllegalStateException("processDataFunc cannot be null! Please set one!")
        return MessageBuildResults(player, prepareData, processData, prepareResponseDataFunc, processResponseDataFunc)
    }

    fun prepareMessageData(dataBuilder: DataBuilder){
        prepareDataFunc = dataBuilder
    }

    fun processMessageData(dataBuilder: ProcessData){
        processDataFunc = dataBuilder
    }

    fun prepareResponseMessageData(dataBuilder: DataBuilder){
        prepareResponseDataFunc = dataBuilder
    }
    fun processResponseMessageData(dataBuilder: ProcessData){
        processResponseDataFunc = dataBuilder
    }

}

data class MessageBuildResults(
        val player: PlayerEntity?,
        val prepareData: DataBuilder,
        val processData: ProcessData,
        val prepareResponseData: DataBuilder?,
        val processResponseData: ProcessData?
)

fun sendDataToClient(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (player, prepareData, processData) = buildResults
    MessageFactory.sendDataToClient(
        player ?: throw IllegalStateException("Blockpos cannot be null! Please set one!"),
        prepareData,
        processData
    )
}

fun sendDataToServer(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (_, prepareData, processData, _, _) = buildResults
    MessageFactory.sendDataToServer(
        prepareData,
        processData
    )
}

fun sendDataToClientWithResponse(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (player, prepareData, processData, prepareResponseData, processResponseData) = buildResults
    MessageFactory.sendDataToClientWithResponse(
            player ?: throw IllegalStateException("Player cannot be null! Please set one!"),
            prepareData,
            processData,
            prepareResponseData ?: throw IllegalStateException("You must provide a prepare response data function."),
            processResponseData ?: throw IllegalStateException("You must provide a process response data function.")
    )
}

fun sendDataToServerWithResponse(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (_, prepareData, processData, prepareResponseData, processResponseData) = buildResults
    MessageFactory.sendDataToServerWithResponse(
            prepareData,
            processData,
            prepareResponseData ?: throw IllegalStateException("You must provide a prepare response data function."),
            processResponseData ?: throw IllegalStateException("You must provide a process response data function.")
    )
}

operator fun CompoundNBT.plus(other: Pair<String, INBT>){
    this.put(other.first, other.second)
}