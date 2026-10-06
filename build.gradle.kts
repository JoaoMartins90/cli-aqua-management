plugins {
    kotlin("jvm") version "2.4.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.13")
}

kotlin {
    jvmToolchain(21)

    // o codigo mora direto em src/, sem o src/main/kotlin do layout padrao
    sourceSets {
        main {
            kotlin.setSrcDirs(listOf("src"))
        }
    }
}

application {
    mainClass = "MainKt"
}

// sem isso o "gradlew run" nao repassa o teclado para o menu
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
