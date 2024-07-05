package io.github.trueangle.knative.lambda.runtime

import io.github.trueangle.knative.lambda.runtime.api.LambdaClient
import io.github.trueangle.knative.lambda.runtime.api.asHandlerError
import io.github.trueangle.knative.lambda.runtime.api.asInitError
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

object LambdaRuntime {
    inline fun run(crossinline initHandler: () -> LambdaHandler) = runBlocking {
        val httpClient = HttpClient(CIO) { install(HttpTimeout) }
        val client = LambdaClient(httpClient)

        try {
            val handler = try {
                initHandler()
            } catch (e: Throwable) {
                throw HandlerInitException()
            }

            while (true) {
                val event = client.retrieveNextEvent()
                try {
                    // todo payload to actual types
                    val result = handler.handleRequest(event.payload, event.context)
                    client.sendResponse(event.context, result)
                } catch (e: Exception) {
                    client.sendError(e.asHandlerError(event.context))

                    break
                }
            }
        } catch (e: HandlerInitException) {
            client.sendError(e.asInitError())

            exitProcess(1)
        } catch (e: Exception) {
            client.sendError(e.asInitError()) // todo

            exitProcess(1)
        } finally {
            httpClient.close()
        }
    }
}

class HandlerInitException : IllegalStateException()