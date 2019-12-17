import com.doordash.common.monitoring.CommonMeterRegistry
import com.doordash.common.monitoring.MeterRegistryModule
import com.doordash.common.monitoring.SentryClient
import com.doordash.common.request.InstrumentationInterceptor
import com.doordash.common.request.MeterInterceptor
import com.doordash.common.request.SentryReporterInterceptor
import com.doordash.logging.KontextLogger
import com.google.inject.Inject
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import io.grpc.examples.helloworld.GreeterCoroutineGrpc
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.statsd.StatsdConfig

class ServiceModule : KotlinModule() {
    override fun configure() {
        bind<GreeterCoroutineGrpc.GreeterImplBase>().to<GreeterService>()
        install(MeterRegistryModule(CommonMeterRegistry()))
    }

    @Provides
    @Inject
    fun sentryReporter(client: SentryClient) = SentryReporterInterceptor(client)

    @Provides
    @Inject
    @Singleton
    fun instrumentationReporter(meterRegistry: CommonMeterRegistry.DefaultMeterRegistry) = InstrumentationInterceptor(
        meterRegistry,
        KontextLogger.logger("Instrumentation")
    )

    @Provides
    @Inject
    @Singleton
    fun meterReporter(meterRegistry: CommonMeterRegistry.DefaultMeterRegistry) = MeterInterceptor(
        meterRegistry,
        KontextLogger.logger("Instrumentation")
    )
}