val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

group = "com.ahmedhnewa"
version = "0.0.1"
application {
    mainClass.set("com.ahmedhnewa.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

val javaVersion = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
}

val sshAntTask = configurations.create("sshAntTask")

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-http-redirect-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    sshAntTask("org.apache.ant:ant-jsch:1.10.13")
}

val buildingJarFileName = "temp-server.jar"
val startingJarFileName = "server.jar"

val serverUser = "root"
val serverHost = "YOUR_IP_ADDRESS"
val serverSshKey = file("keys/id_rsa")
val deleteLog = true
val lockFileName = ".serverLock"

val serviceName = "ktor-server"
val serverFolderName = "app"

ktor {
    fatJar {
        archiveFileName.set(buildingJarFileName)
    }
}

ant.withGroovyBuilder {
    "taskdef"(
        "name" to "scp",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
        "classpath" to configurations["sshAntTask"].asPath
    )
    "taskdef"(
        "name" to "ssh",
        "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
        "classpath" to configurations["sshAntTask"].asPath
    )
}

fun sudoIfNeeded(): String {
    if (serverUser.trim() == "root") {
        return ""
    }
    return "sudo "
}

fun sshCommand(command: String, knownHosts: File) = ant.withGroovyBuilder {
    "ssh"(
        "host" to serverHost,
        "username" to serverUser,
        "keyfile" to serverSshKey,
        "trust" to true,
        "knownhosts" to knownHosts,
        "command" to command
    )
}

task("cleanAndDeploy") {
    dependsOn("clean", "deploy")
}

task("deploy") {
    dependsOn("buildFatJar")
    ant.withGroovyBuilder {
        doLast {
            val knownHosts = File.createTempFile("knownhosts", "txt")
            try {
                println("Make sure the $serverFolderName folder exists if doesn't")
                sshCommand(
                    "mkdir -p \$HOME/$serverFolderName",
                    knownHosts
                )
                println("Lock the server requests...")
                sshCommand(
                    "touch \$HOME/$serverFolderName/$lockFileName",
                    knownHosts
                )
                println("Deleting the previous building jar file if exists...")
                sshCommand(
                    "rm \$HOME/$serverFolderName/$buildingJarFileName -f",
                    knownHosts
                )
                println("Uploading the new jar file...")
                val file = file("build/libs/$buildingJarFileName")
                "scp"(
                    "file" to file,
                    "todir" to "$serverUser@$serverHost:/\$HOME/$serverFolderName",
                    "keyfile" to serverSshKey,
                    "trust" to true,
                    "knownhosts" to knownHosts
                )
                println("Upload done, attempt to stop the current ktor server...")
                sshCommand(
                    "${sudoIfNeeded()}systemctl stop $serviceName",
                    knownHosts
                )
                println("Server stopped, attempt to delete the current ktor server jar...")
                sshCommand(
                    "rm \$HOME/$serverFolderName/$startingJarFileName -f",
                    knownHosts,
                )
                println("The old ktor server jar file has been deleted, now let's rename the new jar file")
                sshCommand(
                    "mv \$HOME/$serverFolderName/$buildingJarFileName \$HOME/$serverFolderName/$startingJarFileName",
                    knownHosts
                )
                if (deleteLog) {
                    sshCommand(
                        "rm /var/log/$serviceName.log -f",
                        knownHosts
                    )
                    println("The $serviceName log at /var/log/$serviceName.log has been removed")
                }
                println("Unlock the server requests...")
                sshCommand(
                    "rm \$HOME/$serverFolderName/$lockFileName -f",
                    knownHosts
                )
                println("Now let's start the ktor server service!")
                sshCommand(
                    "${sudoIfNeeded()}systemctl start $serviceName",
                    knownHosts
                )
                println("Done!")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            } finally {
                knownHosts.delete()
            }
        }
    }
}

task("upgrade") {
    ant.withGroovyBuilder {
        doLast {
            val knownHosts = File.createTempFile("knownhosts", "txt")
            try {
                println("Update repositories...")
                sshCommand(
                    "${sudoIfNeeded()}apt update",
                    knownHosts
                )
                println("Update packages...")
                sshCommand(
                    "${sudoIfNeeded()}apt upgrade -y",
                    knownHosts
                )
                println("Done")
            } catch (e: Exception) {
                println("Error while upgrading server packages: ${e.message}")
            } finally {
                knownHosts.delete()
            }
        }
    }
}

abstract class ProjectNameTask : DefaultTask() {

    @TaskAction
    fun greet() = println("The project name is ${project.name}")
}

tasks.register<ProjectNameTask>("projectName")