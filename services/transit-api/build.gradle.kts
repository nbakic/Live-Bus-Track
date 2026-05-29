// transit-api — Kotlin/Ktor backend. Pass-through cache nad ZET GTFS feedovima
// (sekcija 5 plana — D1). Jedina ZET-okrenuta točka u cijelom sustavu.

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

group = "hr.zet.transit"
version = "0.1.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor klijent — backend dohvaća ZET feedove, OSRM i GraphHopper
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // GTFS-RT protobuf — backend parsira protobuf, klijent dobiva JSON
    implementation("org.mobilitydata:gtfs-realtime-bindings:0.0.8")

    implementation("ch.qos.logback:logback-classic:1.5.7")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

application {
    mainClass.set("hr.zet.transit.api.ApplicationKt")
    // Explicit heap cap for the forked run JVM. GtfsStaticFeedService streams
    // the ~120 MB stop_times.txt rather than buffering it, so peak usage is
    // modest; this just keeps a predictable ceiling on low-RAM dev machines.
    applicationDefaultJvmArgs = listOf("-Xmx1536m")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
