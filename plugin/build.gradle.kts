plugins {
    java
}

group = "dev.yeetle.classicskyblock"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

val paperApi = "io.papermc.paper:paper-api:26.3.build.+"

dependencies {
    compileOnly(paperApi)

    // The server provides the Paper API at runtime, the tests need it on their own classpath.
    testImplementation(paperApi)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.test {
    useJUnitPlatform()
}
