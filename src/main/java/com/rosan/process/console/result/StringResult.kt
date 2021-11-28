package com.rosan.process.console.result

import com.rosan.process.Console

open class StringResult : Console.Result() {
    open var charset = Charsets.UTF_8

    open var readString: List<String>
        get() {
            val messages = ArrayList<String>()
            read.forEach {
                messages.add(it.toString(charset))
            }
            return messages
        }
        set(value) {
            val bytess = ArrayList<ByteArray>()
            value.forEach {
                bytess.add(it.toByteArray(charset))
            }
            read = bytess
        }

    open var errorString: List<String>
        get() {
            val messages = ArrayList<String>()
            error.forEach {
                messages.add(it.toString(charset))
            }
            return messages
        }
        set(value) {
            val bytess = ArrayList<ByteArray>()
            value.forEach {
                bytess.add(it.toByteArray(charset))
            }
            error = bytess
        }
}