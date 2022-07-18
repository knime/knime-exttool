#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2022-09'

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-chemistry/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    parameters(workflowTests.getConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    buildConfigs = [
        Tycho: {
            knimetools.defaultTychoBuild('org.knime.update.exttool')
        },
        UnitTests: {
            workflowTests.runIntegratedWorkflowTests(configurations: workflowTests.DEFAULT_FEATURE_BRANCH_CONFIGURATIONS,
                profile: "test", sidecarContainers: [
                    [ image: "${dockerTools.ECR}/knime/sshd:alpine3.11", namePrefix: "SSHD", port: 22 ]
                ])
        }
    ]

    parallel buildConfigs

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-exttool',
                'knime-chemistry',
                'knime-python',
                'knime-conda',
                'knime-streaming',
                'knime-distance',
                'knime-sas',
                'knime-filehandling',
                'knime-kerberos'
            ]
        ]
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

/* vim: set shiftwidth=4 expandtab smarttab: */
