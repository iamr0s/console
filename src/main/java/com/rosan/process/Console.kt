package com.rosan.process

import com.rosan.process.console.request.StringRequest
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.concurrent.thread

/*
* by ros
* on rosan
* on 2021/8/31
*/

open class Console {
    open class Builder {
        open var terminal = emptyList<String>()

        open var separator: String? = null

        open var prefix: String? = null

        open var postfix: String? = null

        open var autoWrap = true

        open var initRequests: List<Request>? = null

        open var listener = object : Listener() {}

        open fun getProcess(): Process? {
            return kotlin.runCatching { ProcessBuilder(getTerminalFormatted()).start() }.getOrNull()
        }

        open fun setTerminal(vararg terminal: String): Builder {
            this.terminal = terminal.asList()
            return this
        }

        open fun setTerminal(terminal: Set<String>): Builder {
            this.terminal = terminal.toList()
            return this
        }

        open fun getTerminalFormatted(): ArrayList<String> {
            val letTerminal = ArrayList<String>()
            if (autoWrap)
                prefix?.let { letTerminal.add(it) }
            terminal.forEachIndexed { index, bytes ->
                if (autoWrap)
                    if (index != 0)
                        separator?.let { letTerminal.add(it) }
                letTerminal.add(bytes)
            }
            if (autoWrap)
                postfix?.let { letTerminal.add(it) }
            return letTerminal
        }

        open fun addInitRequest(request: Request): Builder {
            val initRequests =
                if (initRequests != null) ArrayList(initRequests) else ArrayList()
            initRequests.add(request)
            this.initRequests = initRequests
            return this
        }

        open fun getFinishRequest(): Request {
            return StringRequest().addCommand("exit")
        }

        open fun <T : Result> openWait(result: T): T {
            val resultRead = ArrayList<ByteArray>()
            val resultError = ArrayList<ByteArray>()
            val console = open(object : Listener() {
                override var read = fun(bytes: ByteArray) {
                    this@Builder.listener.read(bytes)
                    resultRead.add(bytes)
                }

                override var error = fun(bytes: ByteArray) {
                    this@Builder.listener.error(bytes)
                    resultError.add(bytes)
                }
            })
            result.read = resultRead
            result.error = resultError
            console?.let {
                result.code = it.closeWait()
            }
            return result
        }

        open fun open(listener: Listener? = null): Console? {
            val listener = listener ?: this.listener
            kotlin.runCatching {
                val console = Console()
                val process = getProcess() ?: return null
                console.process = process
                console.listener = listener
                console.readThread = thread(false) {
                    kotlin.runCatching {
                        // val input = process.inputStream
                        val steam = process.inputStream
                        while (!Thread.currentThread().isInterrupted) {
                            val length = steam.available()
                            if (length > 0) {
                                val bytes = ByteArray(length)
                                if (steam.read(bytes) > 0)
                                    console.listener.read(bytes)
                            }
                        }
                    }
                }
                console.errorThread = thread(false) {
                    kotlin.runCatching {
                        // val input = process.errorStream
                        val steam = process.errorStream
                        while (!Thread.currentThread().isInterrupted) {
                            val length = steam.available()
                            if (length > 0) {
                                val bytes = ByteArray(length)
                                if (steam.read(bytes) > 0)
                                    console.listener.error(bytes)
                            }
                        }
                    }
                }
                console.aliveThread = thread {
                    console.listener.start()
                    console.readThread?.start()
                    console.errorThread?.start()
                    initRequests?.forEach {
                        kotlin.runCatching {
                            console.request(it)
                        }
                    }
                    val exitValue = console.closeWait()
                    console.listener.close(exitValue)
                }
                return console
            }.getOrElse {
                listener.error(it.localizedMessage.toByteArray(Charset.defaultCharset()))
            }
            return null
        }
    }

    open class Listener {
        open var start = fun() {}

        open var read = { _: ByteArray -> }

        open var error = { _: ByteArray -> }

        open var write = { _: List<ByteArray> -> }

        open var close = { _: Int -> }
    }

    open class Result {
        companion object {
            const val CODE_OK = 0

            const val CODE_DEFAULT = -1
        }

        open var code = CODE_DEFAULT

        open var read = listOf<ByteArray>()

        open var error = listOf<ByteArray>()
    }

    class RequestData(var inputStream: InputStream, var chunkSize: Long = 1024) {
        constructor(bytes: ByteArray) : this(ByteArrayInputStream(bytes), bytes.size.toLong())
    }

    open class Request {
        open var command = arrayListOf<RequestData>()

        open var separator = ByteArray(0)

        open var prefix: ByteArray? = null

        open var postfix: ByteArray? = null

        open var commit: ByteArray? = null

        open var autoWrap = true

        open var autoCommit = true
        open fun setCommand(vararg command: ByteArray): Request {
            return setCommand(command.asList())
        }

        open fun setCommand(command: List<ByteArray>): Request {
            val letCommand = arrayListOf<RequestData>()
            command.forEach {
                letCommand.add(RequestData(it))
            }
            this.command = letCommand
            return this
        }

        open fun getCommandFormatted(): List<RequestData> {
            val letCommand = ArrayList<RequestData>()
            if (autoWrap)
                prefix?.let { letCommand.add(RequestData(it)) }
            command.forEachIndexed { index, bytes ->
                if (autoWrap)
                    if (index != 0)
                        separator.let { letCommand.add(RequestData(it)) }
                letCommand.add(bytes)
            }
            if (autoWrap)
                postfix?.let { letCommand.add(RequestData(it)) }
            if (autoCommit)
                commit?.let { letCommand.add(RequestData(it)) }
            return letCommand
        }

        open fun addCommand(bytes: ByteArray): Request {
            this.command.add(RequestData(bytes))
            return this
        }

        open fun addCommand(data: RequestData): Request {
            this.command.add(data)
            return this
        }

        open fun open(console: Console) {
            console.request(this)
        }
    }

    internal var process: Process? = null

    open lateinit var listener: Listener

    internal var readThread: Thread? = null

    internal var errorThread: Thread? = null

    internal var aliveThread: Thread? = null

    @Synchronized
    open fun request(request: Request): Console {
        val output = process?.outputStream ?: return this
        val command = request.getCommandFormatted()
        command.forEach {
            val inputStream = it.inputStream
            val chunSize = it.chunkSize
            val bytes = ByteArray(chunSize.toInt())
            while (true) {
                val length = inputStream.read(bytes)
                if (length <= 0) break
                val newBytes = bytes.copyOf(length)
                listener.write(listOf(newBytes))
                output.write(newBytes)
                output.flush()
            }
        }
        return this
    }

    open fun closeWait(): Int {
        process?.waitFor()
        readThread?.interrupt()
        errorThread?.interrupt()
        while (
            readThread?.isAlive == true ||
            errorThread?.isAlive == true
        ) {
        }
        return close()
    }

    open fun close(): Int {
        synchronized(this) {
            kotlin.runCatching {
                process?.apply {
                    destroy()
                    return waitFor()
                }
            }
            return -1
        }
    }
}
