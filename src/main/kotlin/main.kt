import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.getInstance
import io.grpc.Channel
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.StatusRuntimeException
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main() {
    val injector = Guice.createInjector(ServiceModule())

    val greeterService = ServerInterceptors.intercept(
        injector.getInstance<GreeterCoroutineGrpc.GreeterImplBase>(),
        injector.getInstance<InstrumentationInterceptor>()
    )
    val server: Server = InProcessServerBuilder
        .forName("helloworld")
        .addService(greeterService)
        .directExecutor()
        .build()
        .start()

    runBlocking {
        callClient()
    }

    server.shutdown()
}

suspend fun callClient() {
    val channel: Channel = InProcessChannelBuilder
        .forName("helloworld")
        .directExecutor()
        .build()

    // Optional coroutineContext. Default is Dispatchers.Unconfined
    val stub = GreeterCoroutineGrpc
        .newStub(channel)
        .withCoroutineContext(Dispatchers.Default)

    repeat(10) {
        try {
            performUnaryCall(stub)
        } catch (e: StatusRuntimeException) {
            println("Unable to process request ${e.message}")
            println("Error trailers ${e.trailers}")
        }
        Thread.sleep(100)
    }
}

suspend fun performUnaryCall(stub: GreeterCoroutineGrpc.GreeterCoroutineStub) {
    val unaryResponse = stub.sayHello { name = "dead" }
    println("Unary Response: ${unaryResponse.message}")
}