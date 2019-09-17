package com.couch.hermes

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

object MessageFactory{
    private val stream = NetworkRegistry.INSTANCE.newSimpleChannel("hermes")

    init{
        var id = 0
        stream.registerMessage(ClientSideMessageHandler(), ClientSideMessage::class.java, ++id, Side.CLIENT)
        stream.registerMessage(ResponsiveClientMessageHandler(), ResponsiveClientMessage::class.java, ++id, Side.CLIENT)
        stream.registerMessage(ServerSideMessageHandler(), ServerSideMessage::class.java, ++id, Side.SERVER)
        stream.registerMessage(ResponsiveServerMessageHandler(), ResponsiveServerMessage::class.java, ++id, Side.SERVER)
    }

    fun sendDataToClient(messageName: String, player: EntityPlayerMP, pos: BlockPos, prepareData: () -> NBTTagCompound, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val clientMessage = ClientSideMessage(messageName, dataPacket, pos)
        stream.sendTo(clientMessage, player)
    }

    fun sendDataToServer(messageName: String, pos: BlockPos, prepareData: () -> NBTTagCompound, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val serverMessage = ServerSideMessage(messageName, dataPacket, pos)
        stream.sendToServer(serverMessage)
    }

    fun sendDataToServerWithResponse(
            messageName: String,
            pos: BlockPos,
            prepareMessageData: () -> NBTTagCompound,
            processMessageData: ProcessData,
            prepareResponseData: () -> NBTTagCompound,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveServerMessage = ResponsiveServerMessage(messageName, responsiveDataPacket, pos)
        stream.sendToServer(responsiveServerMessage)
    }

    fun sendDataToClientWithResponse(
            messageName: String,
            pos: BlockPos,
            player: EntityPlayerMP,
            prepareMessageData: () -> NBTTagCompound,
            processMessageData: ProcessData,
            prepareResponseData: () -> NBTTagCompound,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveClientMessage = ResponsiveClientMessage(messageName, responsiveDataPacket, pos)
        stream.sendTo(responsiveClientMessage, player)
    }
}

object MessageBuilder{
    var messageName: String = ""
    var blockpos: BlockPos? = null
    var playermp: EntityPlayerMP? = null
    private var prepareDataFunc: DataBuilder? = null
    private var processDataFunc: ProcessData? = null
    private var prepareResponseDataFunc: DataBuilder? = null
    private var processResponseDataFunc: ProcessData? = null

    fun build(builder: MessageBuilder.()->Unit): MessageBuildResults{
        this.builder()
        check(!messageName.isBlank()) { "Message name cannot be blank! Please set one." }
        val bpos = blockpos
        val player = playermp
        val prepareData = prepareDataFunc ?: throw IllegalStateException("prepareDataFunc cannot be null! Please set one!")
        val processData = processDataFunc ?: throw IllegalStateException("processDataFunc cannot be null! Please set one!")
        return MessageBuildResults(messageName, bpos, player, prepareData, processData, prepareResponseDataFunc, processResponseDataFunc)
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

typealias DataBuilder = ()->NBTTagCompound

data class MessageBuildResults(
        val messageName: String,
        val blockpos: BlockPos?,
        val player: EntityPlayerMP?,
        val prepareData: DataBuilder,
        val processData: ProcessData,
        val prepareResponseData: DataBuilder?,
        val processResponseData: ProcessData?
)

fun sendDataToClient(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (messageName, blockpos, player, prepareData, processData) = buildResults
    MessageFactory.sendDataToClient(
        messageName,
        player ?: throw IllegalStateException("Blockpos cannot be null! Please set one!"),
        blockpos ?: throw IllegalStateException("Blockpos cannot be null! Please set one!"),
        prepareData,
        processData
    )
}

fun sendDataToServer(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (messageName, blockpos, _, prepareData, processData, _, _) = buildResults
    MessageFactory.sendDataToServer(
        messageName,
        blockpos ?: throw IllegalStateException("BlockPos cannot be null! Please set one!"),
        prepareData,
        processData
    )
}

fun sendDataToClientWithResponse(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (messageName, blockpos, player, prepareData, processData, prepareResponseData, processResponseData) = buildResults
    MessageFactory.sendDataToClientWithResponse(
            messageName,
            blockpos ?: throw IllegalStateException("BlockPos cannot be null! Please set one!"),
            player ?: throw IllegalStateException("Player cannot be null! Please set one!"),
            prepareData,
            processData,
            prepareResponseData ?: throw IllegalStateException("You must provide a prepare response data function."),
            processResponseData ?: throw IllegalStateException("You must provide a process response data function.")
    )
}

fun sendDataToServerWithResponse(messageBuilder: MessageBuilder.()->Unit){
    val buildResults = MessageBuilder.build(messageBuilder)
    val (messageName, blockpos, _, prepareData, processData, prepareResponseData, processResponseData) = buildResults
    MessageFactory.sendDataToServerWithResponse(
            messageName,
            blockpos ?: throw IllegalStateException("BlockPos cannot be null! Please set one!"),
            prepareData,
            processData,
            prepareResponseData ?: throw IllegalStateException("You must provide a prepare response data function."),
            processResponseData ?: throw IllegalStateException("You must provide a process response data function.")
    )
}

operator fun NBTTagCompound.plus(other: Pair<String, NBTBase>){
    this.setTag(other.first, other.second)
}