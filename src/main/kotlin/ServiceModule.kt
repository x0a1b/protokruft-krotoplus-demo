import dev.misfitlabs.kotlinguice4.KotlinModule
import io.grpc.examples.helloworld.GreeterCoroutineGrpc

class ServiceModule : KotlinModule() {
    override fun configure() {
        bind<GreeterCoroutineGrpc.GreeterImplBase>().to<GreeterService>()
    }
}