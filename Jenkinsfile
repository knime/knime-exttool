#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-chemistry/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {

    buildConfigs = [
        Tycho: {
            knimetools.defaultTychoBuild('org.knime.update.exttool')
        },
        UnitTests: {
            runUnitTests() 
        }
    ]

    parallel buildConfigs

    workflowTests.runTests(
        dependencies: [ repositories: ['knime-exttool', 'knime-chemistry', 'knime-streaming', 'knime-distance', 'knime-sas', 'knime-filehandling']]
    )

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
def runUnitTests() {
    /*
     * Run integrated tests against remote filesystem
     */
    node("workflow-tests && ubuntu18.04"){
        def sidecars = dockerTools.createSideCarFactory()
        try {
            stage('Running unit tests'){
                env.lastStage = env.STAGE_NAME
                checkout scm

                def sshdImage = "knime/sshd:alpine3.10"
                def sshdhost = sidecars.createSideCar(dockerTools.ECR + "/" + sshdImage, 'ssh-test-host', [], [22]).start()

                def address =  sshdhost.getAddress(22)
                def testEnv = ["KNIME_SSHD_HOST=${address}"]
                
                knimetools.runIntegratedWorkflowTests(mvnEnv: testEnv, profile: "test")
            }
        } catch (ex) {
            currentBuild.result = 'FAILURE'
            throw ex
        } finally {
            sidecars.close()
        }
    }
}

/* vim: set shiftwidth=4 expandtab smarttab: */
