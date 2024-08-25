package io.github.trueangle.knative.lambda.runtime.log

import io.github.trueangle.knative.lambda.runtime.api.Context
import io.github.trueangle.knative.lambda.runtime.api.dto.LogMessageDto
import io.ktor.util.reflect.TypeInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal class JsonLogFormatter(
    private val json: Json,
    private val clock: Clock = Clock.System,
) : LogFormatter {
    private var requestContext: Context? = null

    override fun <T> format(logLevel: LogLevel, message: T?, messageType: TypeInfo): String {
        val json = try {
            val messageSerializer = serializer(messageType.reifiedType)
            val dtoSerializer = LogMessageDto.serializer(messageSerializer)
            json.encodeToString(
                dtoSerializer,
                LogMessageDto(
                    timestamp = clock.now().toString(),
                    message = if (message is Throwable) message.prettyPrint() else message,
                    level = logLevel,
                    awsRequestId = requestContext?.awsRequestId
                )
            )
        } catch (e: SerializationException) {
            json.encodeToString(
                LogMessageDto(
                    timestamp = clock.now().toString(),
                    message = message?.toString(),
                    level = logLevel,
                    awsRequestId = requestContext?.awsRequestId
                )
            )
        }

        return json
    }

    override fun onContextAvailable(context: Context) {
        requestContext = context
    }
}