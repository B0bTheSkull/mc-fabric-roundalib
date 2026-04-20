plugins {
  id("java")
  id("maven-publish")
  id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("group_id").get()

base {
  archivesName = extra["archives_base_name"].toString()
}

repositories {
  mavenCentral()
  maven("https://maven.fabricmc.net/")
  mavenLocal()
}

dependencies {
  minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
  implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  options.release = 25
}

java {
  withSourcesJar()
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}

tasks.processResources {
  inputs.property("version", version)
  filteringCharset = "UTF-8"
  filesMatching("fabric.mod.json") {
    expand("version" to version)
  }
}

tasks.jar {
  inputs.property("archivesName", base.archivesName)
  from("${rootDir}/LICENSE") {
    rename { "${it}_${base.archivesName.get()}" }
  }
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      artifactId = base.archivesName.get()
      from(components["java"])
    }
  }
}
