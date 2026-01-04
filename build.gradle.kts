allprojects {
    version = "0.2.4"
    group = "com.expenses-tracker"
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}