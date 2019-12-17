import com.doordash.logging.KontextLogger
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

val ContextDataKey = Context.key<ExampleInterceptor.ContextData>(
    ExampleInterceptor.ContextData::javaClass.name
);

class ExampleInterceptor : ServerInterceptor {
    data class ContextData(
        val requestMetadata: Metadata,
        val responseMetadata: Metadata,
        val startedNano: Long
    )

    companion object {
        val logger = KontextLogger.logger {}
    }

    class InterceptedCaller<ReqT : Any?, RespT : Any?>(
        private val call: ServerCall<ReqT, RespT>,
        private val contextData: ContextData
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

        override fun sendHeaders(headers: Metadata) {
            val responseHeaders = Metadata().apply {
                this.merge(headers)
                this.merge(contextData.responseMetadata)
            }

            super.sendHeaders(responseHeaders)
        }

        override fun close(status: Status, trailers: Metadata) {
            // Record time consumed from context data, where startedNano was initialized before call started
            val elapsedMillis = (System.nanoTime() - contextData.startedNano) / 1_000_000
            val methodName = call.methodDescriptor.fullMethodName.replace("/", ".")
            logger.info("[Instrumentation:$methodName] Time recorded ${elapsedMillis}ms")

            // Record success
            if (status.isOk) {
                logger.info("[Instrumentation:$methodName] Record success...")
                super.close(status, trailers)
                return
            }

            // Inspect exception and report it if required
            val exception = status.cause
            if (exception != null) {
                println("[Instrumentation:$methodName] Logging exception $exception")
            }

            // Attach response headers as trailers to failing call
            // This could be useful providing additional metadata
            val responseTrailers = Metadata().apply {
                this.merge(trailers)
                this.merge(contextData.responseMetadata)
            }

            // Record failure to instrumentation
            println("[Instrumentation:$methodName] Recoding error ${status.code} $responseTrailers")
            super.close(status, responseTrailers)
        }
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val data = ContextData(
            headers,
            Metadata(),
            System.nanoTime()
        )

        val forwardContext = Context.current().withValue(ContextDataKey, data)
        return Contexts.interceptCall(forwardContext, InterceptedCaller(call, data), headers, next)
    }
}