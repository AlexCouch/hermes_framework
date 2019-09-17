package com.couch

import com.couch.hermes.plus
import com.couch.hermes.sendDataToServer
import com.couch.hermes.sendDataToServerWithResponse
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

@Mod(modid="test-mod", version="1.0", name="Test Mod", modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter")
object TestMod{}

@Mod.EventBusSubscriber(modid="test-mod")
object EventHandler{
    private const val test1 = false
    private const val test2 = true

    @JvmStatic
    @SubscribeEvent
    fun serverMessageTest(event: TickEvent.PlayerTickEvent){
        if(test1) {
            sendDataToServer {
                playermp =
                    if (event.player is EntityPlayerMP) event.player as EntityPlayerMP else return@sendDataToServer
                prepareMessageData {
                    val someNBTString = NBTTagString("Hello you!")
                    val ret = NBTTagCompound()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processMessageData { data, _, _ ->
                    if (data.hasKey("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
            }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun responsiveServerMessageTest(event: TickEvent.PlayerTickEvent){
        if(test2) {
            sendDataToServerWithResponse {
                playermp =
                    if (event.player is EntityPlayerMP) event.player as EntityPlayerMP else return@sendDataToServerWithResponse
                prepareMessageData {
                    val someNBTString = NBTTagString("Hello server!")
                    val ret = NBTTagCompound()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processMessageData { data, _, _ ->
                    if (data.hasKey("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
                prepareResponseMessageData {
                    val someNBTString = NBTTagString("Hello client!")
                    val ret = NBTTagCompound()
                    ret + ("some_string" to someNBTString)
                    ret
                }
                processResponseMessageData { data, _, _ ->
                    if (data.hasKey("some_string")) {
                        val someString = data.getString("some_string")
                        println(someString)
                    }
                }
            }
        }
    }
}