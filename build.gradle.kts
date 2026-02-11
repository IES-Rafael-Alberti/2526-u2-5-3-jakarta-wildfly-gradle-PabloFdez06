plugins {
    java
    war
}

group = "com.mycompany.myproject"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Jakarta EE 10 API (Provided porque WildFly ya lo incluye)
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    
    // JUnit 5 para tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}


tasks.withType<War> {
    archiveBaseName.set("modulename.backend")
}
