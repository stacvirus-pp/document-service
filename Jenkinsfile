pipeline {
    agent any
    environment {
        GITHUB_CREDENTIALS = credentials('github-pat')
    }

    stages {
        stage("build") {
            steps {
                echo 'building the application...'
                withGradle {
                    sh './gradlew clean build --stacktrace -i'
                }
            }
            post {
                success {
                    githubCommitStatus(context: 'jenkins/build', state: 'SUCCESS', message: 'Build succeeded!')
                }
                failure {
                    githubCommitStatus(context: 'jenkins/build', state: 'FAILURE', message: 'Build failed!')
                }
            }
        }
    }
    post {
        success {
            githubCommitStatus(context: 'jenkins/build', state: 'SUCCESS', message: 'Pipeline succeeded!')
        }
        failure {
            githubCommitStatus(context: 'jenkins/build', state: 'FAILURE', message: 'Pipeline failed!')
        }
    }
}