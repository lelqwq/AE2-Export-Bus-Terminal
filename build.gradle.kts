plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

base {
    archivesName.set("ae2-export-bus-terminal")
}

tasks.test {
    useJUnit()
}
