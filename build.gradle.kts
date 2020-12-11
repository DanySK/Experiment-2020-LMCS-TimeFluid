import java.io.ByteArrayOutputStream
import java.awt.GraphicsEnvironment

plugins {
    application
    kotlin("jvm")
}

val githubPackagesIssue =
    """
    This experiment relies on a pre-release version of the Alchemist simulator.
    The required dependencies are located on a GitHub Packages repository,
    which requires authentication even for public repositories.
    This has been a long standing issue for github, see:
    https://github.community/t/download-from-github-package-registry-without-authentication/14407
    Once the issue will be solved, this experiment will work without any configuration user side.
    
    This build script preconfigures the environment, but of course it requires the user's authentication.
    In order to authenticate, first create a GitHub account if you do not have one.
    Then visit: https://github.com/settings/tokens/new
    And create a new token with permission at least "read:packages".
    Save the token, you can see it only once and it's not recoverable (but you can generate another one)
    Now, you can feed your credentials to this system in two ways:
    
    Via environment variables:
    * set your username in GITHUB_USERNAME
    * set your token in GITHUB_TOKEN
    
    Via Gradle project properties, see: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
    * set your username in githubUsername
    * set your token in githubToken
    
    Environment variables take precedence.
    """.trimIndent()

repositories {
    mavenCentral()
    /* 
     * The following repositories contain beta features and should be added for experimental mode only
     * 
     * maven("https://dl.bintray.com/alchemist-simulator/Alchemist/")
     * maven("https://dl.bintray.com/protelis/Protelis/")
     */
    maven("https://maven.pkg.github.com/alchemistsimulator/alchemist") {
        credentials {
            username = System.getenv("GITHUB_USERNAME")
                ?: project.properties["githubUsername"]?.toString()
                ?: throw IllegalStateException("MISSING USERNAME\n$githubPackagesIssue")
            password = System.getenv("GITHUB_TOKEN")
                ?: project.properties["githubToken"]?.toString()
                ?: throw IllegalStateException("MISSING TOKEN\n$githubPackagesIssue")
        }
    }
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}
dependencies {
    implementation("it.unibo.alchemist:alchemist:_")
    implementation("it.unibo.alchemist:alchemist-implementationbase:_")
    implementation("it.unibo.alchemist:alchemist-incarnation-protelis:_")
    if (!GraphicsEnvironment.isHeadless()) {
        implementation("it.unibo.alchemist:alchemist-swingui:_")
    }
    implementation("org.jgrapht:jgrapht-core:_")
    implementation("org.apache.commons:commons-lang3:_")
    implementation(kotlin("stdlib-jdk8"))
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long = maxHeap ?:
    if (System.getProperty("os.name").toLowerCase().contains("linux")) {
        ByteArrayOutputStream()
            .use { output ->
                exec {
                    executable = "bash"
                    args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
                    standardOutput = output
                }
                output.toString().trim().toLong() / 1024
            }
            .also { println("Detected ${it}MB RAM available.") }  * 9 / 10
    } else {
        // Guess 16GB RAM of which 2 used by the OS
        14 * 1024L
    }
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 512 * 3
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize ))

val alchemistGroup = "Run Alchemist"
/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroup
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroup
    description = "Launches all experiments"
}
/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            group = alchemistGroup
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            main = "it.unibo.alchemist.Alchemist"
            classpath = sourceSets["main"].runtimeClasspath
            args("-y", it.absolutePath)
            if (System.getenv("CI") == "true") {
                args("-hl", "-t", "2")
            } else {
                args("-g", "effects/${it.nameWithoutExtension}.aes")
            }
            this.additionalConfiguration()
        }
        val capitalizedName = it.nameWithoutExtension.capitalize()
        val graphic by basetask("run${capitalizedName}Graphic") {
            args("-e", "sample-data")
        }
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            description = "Launches batch experiments for $capitalizedName"
            jvmArgs("-XX:+UseZGC")
//            jvmArgs("-XX:+UseTransparentHugePages") // Only works on Linux
//            jvmArgs("-XX:+AggressiveHeap")
            maxHeapSize = "${(heap / 10 * 9).toInt()}m"
            File("data").mkdirs()
            args(
                "-e", "data/${it.nameWithoutExtension}",
                "-b",
                "-var", "seed", "speed", "algorithm",
                "-p", threadCount,
                "-i", "0.2"
            )
        }
        runAllBatch.dependsOn(batch)
    }

