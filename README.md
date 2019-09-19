Hermes Framework
======================
This simple messages wrapper allows you to customize and manage your network messages 
without needing to create and register a message and a handler. It also handles the sided programming for you.
All this requires is some functionality (lambdas) to be executed on their respective sides (see code examples).

There are two different kinds of messages: Basic and Responsive.

Basic Messages
--------------
Basic messages only require two functions: ``prepareMessageData`` and ``processMessageData``. 
These two functions will be stored in the CommonDataSpace to be accessed by both the Client and the Server.
These are stored in the form of a DataPacket with a UUID so that your unique message can find its unique data
packet without conflicts. ``prepareMessageData`` is executed before the message is sent so you can encode any
data you want to send to the server such as tile entity data. This is done in the form of an NBTTagCompound (1.12) or CompoundNBT (1.14).
A Basic Message will ``prepareMessageData`` on the sending side (the side sending the message) and ``processMessageData`` unpacks the nbt
you encoded in ``prepareMessageData`` on the receiving side (the side receiving your message).

Responsive Messages
-----------------
Responsive Messages require four functions: ``prepareMessageData``, ``processMessageData``, ``prepareResponseData``, and ``processResponseData``.
The sending side will send a responsive message and invoke ``prepareMessageData`` and ``processMessageData`` in a 
responsive handler on the receiving side that returns back to the sending side a basic message that invokes 
``prepareMessageData`` and ``processMessageData``. This operates exactly the same as a basic message it just yeilds back to
the sender another message.

Hermes 1.1
----------------
As of Hermes 1.1, a Kotlin DSL is provided for Kotlin developers. This does not break compatibility with Java.
The below examples are uses of the provided Kotlin DSL.

Hermes 2.0
------------------
As of Hermes 2.0, message names are now no longer required as they have been replaced by internal UUID's.
You no longer provide the framework with a blockpos as that can easily be encoded in NBT in the prepare functions.

Code examples
-------------------
Sending a basic message
```Kotlin
sendDataToServer {
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
```

Sending a responsive message

```Kotlin
sendDataToServerWithResponse {
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
```
