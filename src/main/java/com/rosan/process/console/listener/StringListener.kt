package com.rosan.process.console.listener

import com.rosan.process.Console

open class StringListener : Console.Listener() {
    open var charset = Charsets.UTF_8

    open var readString = { _: String -> }

    open var errorString = { _: String -> }

    open var writeString = { _: List<String> -> }

    override var read = fun(bytes: ByteArray) {
        super.read(bytes)
        kotlin.runCatching {
            readString(bytes.toString(charset))
        }
    }

    override var error = fun(bytes: ByteArray) {
        super.error(bytes)
        kotlin.runCatching {
            errorString(bytes.toString(charset))
        }
    }

    override var write = fun(command: List<ByteArray>) {
        super.write(command)
        kotlin.runCatching {
            val letCommand = ArrayList<String>()
            command.forEach {
                letCommand.add(it.toString(charset))
            }
            writeString(letCommand)
        }
    }
}