plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    forge()
}

dependencies {
    forge("net.minecraftforge:forge:$forge_version")
}

loom {
    accessWidenerPath.set(project(":Common").loom.accessWidenerPath)

    forge {
        convertAccessWideners.set(true)
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig("$mod_id.mixins.json")
        mixinConfig("$mod_id.forge.mixins.json")

    }

}

val common by configurations.creating
val shadowCommon by configurations.creating
val developmentForge = configurations.named("developmentForge")

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    developmentForge.get().extendsFrom(common)
}

dependencies {
    forge("net.minecraftforge:forge:$forge_version")

    common(project(path = ":Common", configuration = "namedElements")) { isTransitive = false }
    shadowCommon(project(path = ":Common", configuration = "transformProductionForge")) { isTransitive = false }

    include(mixinExtras)
    forgeRuntimeLibrary(mixinExtras)

    modImplementation("com.jozufozu.flywheel:flywheel-forge-$minecraft_version:$forge_flywheel_version")

    forgeRuntimeLibrary("icyllis.modernui:ModernUI-Core:$modernui_core_version")
    modCompileOnly("icyllis.modernui:ModernUI-Forge:${minecraft_version}-${modernui_version}")

    modImplementation("maven.modrinth:embeddium:0.3.31+mc1.20.1")
    modImplementation("maven.modrinth:oculus:1.20.1-1.7.0")

    // Embeddium's dev jar references a small subset of Fabric API interfaces (jar-in-jar at runtime),
    // but they are not visible on the compile classpath. A stub FabricBlockView interface is provided
    // under Forge/src/main/java/net/fabricmc/... to satisfy the compiler.

    // Valkyrien Skies 2 (compile-only for ship lighting compat)
    modCompileOnly("maven.modrinth:valkyrien-skies:$vs2_forge_version")
    compileOnly("org.valkyrienskies.core:api:$vs_core_version")
    compileOnly("org.valkyrienskies.core:util:$vs_core_version")

}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    exclude("net/fabricmc/**")

    configurations = listOf(shadowCommon)

    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    val shadowJarTask = tasks.shadowJar.get()
    inputFile.set(shadowJarTask.archiveFile)
    dependsOn(shadowJarTask)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
    exclude("net/fabricmc/**")
}

tasks.sourcesJar {
    val commonSources = project(":Common").tasks.sourcesJar
    dependsOn(commonSources)
    from(commonSources.get().archiveFile.map(project::zipTree))
}

components.getByName<SoftwareComponent>("java") {
    (this as AdhocComponentWithVariants).apply {
        withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
            skip()
        }
    }
}