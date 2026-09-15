plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    named("main") {
        java.srcDir("../../src/main/java")
        java.include("com/example/daifugo/game/**")
        java.exclude("com/example/daifugo/game/cpu/CpuGameService.java")
    }
}
