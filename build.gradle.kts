plugins {
    kotlin("jvm") version "1.9.23"
}

group = "de.felixnuesse"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("commons-io:commons-io:2.16.1")
    implementation("com.github.freva:ascii-table:1.8.0")
    // https://mvnrepository.com/artifact/org.fusesource.jansi/jansi
    implementation("org.fusesource.jansi:jansi:2.4.1")
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}

tasks.jar {
    manifest.attributes["Main-Class"] = "de.felixnuesse.MainKt"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<JavaExec>("execute") {
    mainClass.set("de.felixnuesse.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}