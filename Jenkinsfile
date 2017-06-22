#!/usr/bin/env groovy

node('jenkins-slave-java8-maven') {
    stage('Checkout') {
        checkout scm
        
        branchName = env.BRANCH_NAME;

        sh 'git rev-parse HEAD > commit'
        commit = readFile('commit').trim()
        
        pomVersion = sh(script: 'mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'${project.version}\' --non-recursive exec:exec', returnStdout: true).trim()
        
        echo "branchName: $branchName"
        echo "commit: $commit"
        echo "pomVersion: $pomVersion"
    }

    stage('Package') {
        sh 'mvn clean package -DskipTests=true'
    }

    stage('Nexus Deploy') {
        sh 'mvn deploy -P release -Dmaven.test.skip=true'
    }
}