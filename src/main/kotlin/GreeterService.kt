import io.grpc.Status
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.newHelloReply
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class GreeterService : GreeterCoroutineGrpc.GreeterImplBase() {
    override val initialContext: CoroutineContext
        get() = Dispatchers.Default

    private val validNameRegex = Regex("[^0-9]*")

    override suspend fun sayHello(request: HelloRequest): HelloReply {
        val contextData = ContextDataKey.get()

        if (request.name.matches(validNameRegex)) {
            return newHelloReply { }
        } else {
            contextData.responseMetadata.put("invalid-params".toMetadataKey(), request.name)
            throw Status.INVALID_ARGUMENT
                .withDescription("Bad argument ${request.name}")
                .withCause(IllegalArgumentException())
                .asException()
        }
    }
}