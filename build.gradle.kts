allprojects {
    version = "1.4.0"
    group = "com.expenses-tracker"
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
