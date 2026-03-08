allprojects {
    version = "0.3.0"
    group = "com.expenses-tracker"
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}