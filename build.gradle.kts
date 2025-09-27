plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.utsman"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    docker {
        localImageName.set("utsmannn/sheet-api")
        imageTag.set(version.toString())
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.google.api.client)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.api.services.sheets)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
