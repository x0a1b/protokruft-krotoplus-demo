import com.doordash.common.request.InstrumentationInterceptor
import com.doordash.common.request.SentryReporterInterceptor
import com.doordash.common.service.ServiceConfig
import com.doordash.logging.KontextLogger
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.getInstance
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.StatusRuntimeException
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.newHelloRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

val logger = KontextLogger.logger { }

fun main() {
    val injector = Guice.createInjector(ServiceModule())
    val executor = Executors.newFixedThreadPool(ServiceConfig.getGRPCThreadPoolSize())

    val server: Server = ServerBuilder
        .forPort(50051)
        .intercept(injector.getInstance<SentryReporterInterceptor>())
        .intercept(injector.getInstance<InstrumentationInterceptor>())
        .addService(
            injector.getInstance<GreeterCoroutineGrpc.GreeterImplBase>()
        )
        .executor(executor)
        .build()
        .start()

    runBlocking {
        callClient()
    }

    logger.info("Bye cruel world....")
    server.shutdown()
    server.awaitTermination()
}

suspend fun callClient() {
    val channel: Channel = ManagedChannelBuilder.forAddress("localhost", 50051)
        .directExecutor()
        .usePlaintext()
        .build()

    // Optional coroutineContext. Default is Dispatchers.Unconfined
    val stub = GreeterCoroutineGrpc
        .newStubWithContext(channel)

    repeat(10) {
        try {
            performUnaryCall(it, stub)
        } catch (e: StatusRuntimeException) {
            logger.error("Unable to process request ${e.message}")
            logger.error("Error trailers ${e.trailers}")
        }
    }

    delay(1000)
}

suspend fun performUnaryCall(i: Int, stub: GreeterCoroutineGrpc.GreeterCoroutineStub) {
    val name = if (i % 3 == 0) "deadp00l" else "dead"
    val unaryResponse = stub.sayHello(newHelloRequest {
        this.name = name
    })

    logger.info("Unary Response: ${unaryResponse.message}")
}