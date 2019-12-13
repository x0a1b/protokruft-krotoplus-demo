import io.grpc.Metadata

fun String.toMetadataKey(): Metadata.Key<String> {
    return Metadata.Key.of(this, Metadata.ASCII_STRING_MARSHALLER)
}