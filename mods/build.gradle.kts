import org.apache.commons.lang3.SystemUtils

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val modid: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")

// Every finished jar (core + one per mod) is copied here.
val outputDir = rootProject.file("../build/mods")

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
        if (transformerFile.exists()) {
            println("Installing access transformer")
            accessTransformer(transformerFile)
        }
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)

    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
}

// ---------------------------------------------------------------------------
// Core jar (Pegasus-Core): the shared library + all mixins + bundled Mixin runtime.
// ---------------------------------------------------------------------------

fun org.gradle.jvm.tasks.Jar.coreManifest() {
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
        if (transformerFile.exists()) this["FMLAT"] = "${modid}_at.cfg"
    }
}

tasks.jar {
    archiveBaseName.set("Pegasus-Core")
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    exclude("dev/mylauncher/mods/**")
    exclude("dev/mylauncher/devtest/**")
    coreManifest()
}

tasks.shadowJar {
    archiveBaseName.set("Pegasus-Core")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    exclude("dev/mylauncher/mods/**")
    exclude("dev/mylauncher/devtest/**")
    coreManifest()
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveBaseName.set("Pegasus-Core")
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
    destinationDirectory.set(outputDir)
}

// ---------------------------------------------------------------------------
// One jar per mod. mods.tsv lists: <package id> <TAB> <Jar name> <TAB> <Display name> <TAB> <description>
// Each mod lives in src/main/java/dev/mylauncher/mods/<package id>/ and has modid "ml_<package id>".
// ---------------------------------------------------------------------------

data class ModEntry(val id: String, val jarName: String, val name: String, val description: String)

val modEntries: List<ModEntry> = file("mods.tsv").readLines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .map { line ->
        val p = line.split("\t")
        ModEntry(p[0], p[1], p[2], p.getOrElse(3) { "" })
    }
    .filter { file("src/main/java/dev/mylauncher/mods/${it.id}").isDirectory }

fun jsonEscape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

val buildAllMods by tasks.registering {
    group = "pegasus"
    description = "Builds Pegasus-Core and every mod jar into ../build/mods"
    dependsOn(remapJar)
}

for (entry in modEntries) {
    val infoDir = layout.buildDirectory.dir("generated/mcmod/${entry.id}")

    val info = tasks.register("mcmodInfo_${entry.id}") {
        val out = infoDir
        inputs.property("entry", entry.toString())
        outputs.dir(out)
        doLast {
            val f = out.get().file("mcmod.info").asFile
            f.parentFile.mkdirs()
            f.writeText(
                """[{"modid":"ml_${entry.id}","name":"${jsonEscape(entry.name)}","description":"${jsonEscape(entry.description)}",""" +
                    """"version":"$version","mcversion":"$mcVersion","authorList":["Pegasus"],"requiredMods":["mlcore"],"dependencies":["mlcore"]}]"""
            )
        }
    }

    val devJar = tasks.register<Jar>("devJar_${entry.id}") {
        dependsOn(tasks.classes, info)
        archiveBaseName.set("Pegasus-${entry.jarName}")
        archiveClassifier.set("dev")
        destinationDirectory.set(layout.buildDirectory.dir("intermediates/mods"))
        from(sourceSets.main.get().output) {
            include("dev/mylauncher/mods/${entry.id}/**")
            include("assets/ml_${entry.id}/**")
        }
        from(infoDir)
    }

    val remap = tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapJar_${entry.id}") {
        dependsOn(devJar)
        input.set(devJar.flatMap { it.archiveFile })
        archiveBaseName.set("Pegasus-${entry.jarName}")
        archiveClassifier.set("")
        destinationDirectory.set(outputDir)
    }

    buildAllMods { dependsOn(remap) }
}

tasks.assemble { dependsOn(buildAllMods) }

// ---------------------------------------------------------------------------
// testClient: dev Minecraft 1.8.9 with every mod loaded. Replaces loom's runClient,
// which fails Gradle 8 task validation in loom 0.10.
// ---------------------------------------------------------------------------
tasks.register<JavaExec>("testClient") {
    group = "pegasus"
    description = "Starts a dev 1.8.9 client with Pegasus-Core and all mods (offline account)"
    dependsOn(tasks.classes, "downloadAssets")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.fabricmc.devlaunchinjector.Main")
    jvmArgs(
        "-Dfabric.dli.config=${file(".gradle/loom-cache/launch.cfg").absolutePath}",
        "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.minecraft.launchwrapper.Launch",
        "-Xmx2G",
        "-Dpegasus.devtest=" + (project.findProperty("devtest") ?: "false")
    )
    workingDir = file("run").also { it.mkdirs() }
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(8)) })
}
