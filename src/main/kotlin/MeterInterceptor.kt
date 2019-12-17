package com.doordash.common.request

import com.doordash.common.monitoring.CommonMeterRegistry
import com.doordash.logging.KontextLogger
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import java.util.concurrent.TimeUnit

class MeterInterceptor(
    private val meter: CommonMeterRegistry.DefaultMeterRegistry,
    private val logger: KontextLogger? = null
) : ServerInterceptor {
    class InterceptedCaller<ReqT : Any?, RespT : Any?>(
        private val meter: CommonMeterRegistry.DefaultMeterRegistry,
        private val logger: KontextLogger?,
        private val call: ServerCall<ReqT, RespT>,
        private val startTime: Long
    ) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
        override fun close(status: Status, trailers: Metadata?) {
            val endTime = System.nanoTime()
            val elapsedMillis = (endTime - startTime) / 1_000_000
            val methodName = call.methodDescriptor.fullMethodName.replace("/", ".")
            val tags = arrayOf("method", methodName, "status", "${status.code.value()}")

            meter.timer("grpc.elapsed", *tags).apply {
                record(elapsedMillis, TimeUnit.MILLISECONDS)
            }

            if (status.isOk) {
                meter.counter("grpc.success", *tags)
            } else {
                meter.counter("grpc.failure", *tags)
            }

            logger?.withValues(
                "status" to status.code.value(),
                "elapsed_millis" to elapsedMillis,
                "method" to methodName,
                "type" to "instrumentation-log"
            )?.info(null)
            super.close(status, trailers)
        }
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val now = System.nanoTime()
        return next.startCall(InterceptedCaller(meter, logger, call, now), headers)
    }
}