allprojects {
    version = "1.1.2"
    group = "com.expenses-tracker"
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}