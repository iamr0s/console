package com.rosan.process.console.request

import com.rosan.process.Console

open class StringRequest : Console.Request() {
    open var charset = Charsets.UTF_8

    init {
        setSeparator(" ")
        setCommit(String.format("%n"))
    }

    open fun setPrefix(prefix: String) {
        this.prefix = prefix.toByteArray(charset)
    }

    open fun setSeparator(separator: String) {
        this.separator = separator.toByteArray(charset)
    }

    open fun setPostfix(postfix: String) {
        this.postfix = postfix.toByteArray(charset)
    }

    open fun setCommit(commit: String) {
        this.commit = commit.toByteArray(charset)
    }

    open fun setCommand(vararg command: String): StringRequest {
        return setCommand(command.asList())
    }

    open fun setCommand(command: List<String>): StringRequest {
        val letCommand = ArrayList<ByteArray>()
        command.forEach {
            letCommand.add(it.toByteArray(charset))
        }
        setCommand(letCommand)
        return this
    }

    open fun addCommand(command: String): StringRequest {
        return super.addCommand(command.toByteArray(charset)) as StringRequest
    }
}