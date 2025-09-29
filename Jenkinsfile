pipeline {
    agent any

    stages {
        stage("build") {
            steps {
                echo 'building the application...'
                withGradle {
                    sh './gradlew clean build --stacktrace -i'
                }
            }
        }
    }
}