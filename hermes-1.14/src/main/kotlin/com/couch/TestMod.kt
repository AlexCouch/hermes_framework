@file:Suppress("INACCESSIBLE_TYPE", "UNCHECKED_CAST")

package com.couch

import com.couch.hermes.*
import net.alexwells.kottle.FMLKotlinModLoadingContext
import net.alexwells.kottle.KotlinEventBusSubscriber
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.StringNBT
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent

@Mod("test-mod")
object TestMod{
    init{
        FMLKotlinModLoadingContext.get().modEventBus.addListener{ _: FMLCommonSetupEvent ->
            MessageFactory
            var id = 0
            //Register the basic sided message
            MessageFactory.stream.messageBuilder(BasicSideMessage::class.java, id++)
                .encoder(basicSidedMessageEncoder)
                .decoder(basicSidedMessageDecoder)
                .consumer(basicSidedMessageHandler)
                .add()
            MessageFactory.stream.messageBuilder(ResponsiveSidedMessage::class.java, id++)
                .encoder(responsiveSidedMessageEncoder)
                .decoder(responsiveSidedMessageDecoder)
                .consumer(responsiveSidedMessagehandler)
                .add()
        }
    }
}

@KotlinEventBusSubscriber(modid="test-mod", bus = KotlinEventBusSubscriber.Bus.FORGE)
object EventHandler{
    private const val test1 = false
    private const val test2 = true

    @SubscribeEvent
    fun serverMessageTest(event: TickEvent.PlayerTickEvent){
        if(event.side == LogicalSide.SERVER) return
        if(test1) {
            sendDataToServer {
                prepareMessageData {
                    val someNBTString = StringNBT("Hello you!")
                    val ret = CompoundNBT()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processMessageData { data, _, _ ->
                    if (data.contains("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun responsiveServerMessageTest(event: TickEvent.PlayerTickEvent){
        if(event.side == LogicalSide.SERVER) return
        if(test2) {
            sendDataToServerWithResponse {
                prepareMessageData {
                    val someNBTString = StringNBT("Hello server!")
                    val ret = CompoundNBT()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processMessageData { data, _, _ ->
                    if (data.contains("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
                prepareResponseMessageData {
                    val someNBTString = StringNBT("Hello client!")
                    val ret = CompoundNBT()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processResponseMessageData { data, _, _ ->
                    if (data.contains("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
            }
        }
    }
}