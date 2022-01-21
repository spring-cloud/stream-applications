package org.springframework.jenkins.stream.applications.ci

import javaposse.jobdsl.dsl.DslFactory
import org.springframework.jenkins.stream.applications.common.StreamApplicationsCommons
import org.springframework.jenkins.stream.applications.common.StreamApplicaitonsUtilsTrait

/**
 * @author Soby Chacko
 */
class StreamApplicationsPhasedBuildMaker implements StreamApplicaitonsUtilsTrait {

    private final DslFactory dsl

    StreamApplicationsPhasedBuildMaker(DslFactory dsl) {
        this.dsl = dsl
    }

    void build(boolean isRelease, String releaseType, String branchToBuild = "main") {
        buildAllRelatedJobs(isRelease, releaseType, branchToBuild)
        dsl.multiJob("stream-application-builds" + "-" + branchToBuild) {
            steps {
                if (!isRelease) {
                    triggers {
                        cron "H */12 * * *"
                    }
                    phase('stream-applications-build', 'COMPLETED') {
                        scm {
                            git {
                                remote {
                                    url "https://github.com/spring-cloud/stream-applications"
                                    branch branchToBuild
                                }
                            }
                        }
                        String prefixedProjectName = prefixJob("stream-applications-build")
                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                            currentJobParameters()
                        }
                    }
                    phase('java-functions-phase', 'COMPLETED') {
                        scm {
                            git {
                                remote {
                                    url "https://github.com/spring-cloud/stream-applications"
                                    branch branchToBuild
                                }
                            }
                        }
                        String prefixedProjectName = prefixJob("java-functions")
                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                            currentJobParameters()
                        }
                    }
                    phase('stream-applications-core-phase', 'COMPLETED') {
                        scm {
                            git {
                                remote {
                                    url "https://github.com/spring-cloud/stream-applications"
                                    branch branchToBuild
                                }
                            }
                        }
                        String prefixedProjectName = prefixJob("stream-applications-core")
                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                            currentJobParameters()
                        }
                    }
                }

                int counter = 1

                if (branchToBuild.equals("2020.0.x")) {
                    (StreamApplicationsCommons.ALL_JOBS_2020).each { List<String> ph ->
                        phase("applications-ci-group-${counter}", 'COMPLETED') {
                            ph.each {
                                String projectName ->
                                    String prefixedProjectName = prefixJob(projectName)
                                    phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                                        currentJobParameters()
                                    }
                            }
                        }
                        counter++;
                    }
                }
                else {
                    (StreamApplicationsCommons.ALL_JOBS).each { List<String> ph ->
                        phase("applications-ci-group-${counter}", 'COMPLETED') {
                            ph.each {
                                String projectName ->
                                    String prefixedProjectName = prefixJob(projectName)
                                    phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                                        currentJobParameters()
                                    }
                            }
                        }
                        counter++;
                    }
                }

                if (!isRelease) {
                    phase('stream-applications-release-train') {
                        String prefixedProjectName = prefixJob("stream-applications-release-train")
                        phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                            currentJobParameters()
                        }
                    }
                    if (!branchToBuild.equals("2020.0.x")) {
                        phase('stream-applications-integration-tests') {
                            String prefixedProjectName = prefixJob("stream-applications-integration-tests")
                            phaseJob("${prefixedProjectName}-${branchToBuild}-ci".toString()) {
                                currentJobParameters()
                            }
                        }
                    }
                }
            }
        }
    }

    void buildAllRelatedJobs(boolean isRelease, String releaseType, String branchToBuild) {
        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "stream-applications-build", branchToBuild)
                .deploy(true, false, false, false, false, false, isRelease, releaseType)
        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "java-functions", branchToBuild)
                .deploy(false, true, false, false, false, false, isRelease, releaseType)
        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "stream-applications-core", branchToBuild)
                .deploy(false, false, true, false, false, false, isRelease, releaseType)

        if (branchToBuild.equals("2020.0.x")) {
            StreamApplicationsCommons.PHASED_JOBS_2020.each { k, v ->
                new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "${k}", branchToBuild)
                        .deploy(false, false, false,
                                true, false, true, isRelease, releaseType, "${v}")
            }
        }
        else {
            StreamApplicationsCommons.PHASED_JOBS.each { k, v ->
                new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "${k}", branchToBuild)
                        .deploy(false, false, false,
                                true, false, true, isRelease, releaseType, "${v}")
            }
        }

        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "source-apps-release", branchToBuild)
                .deploy(false, false, false,
                        false, false, false, isRelease, releaseType, "source", true)

        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "sink-apps-release", branchToBuild)
                .deploy(false, false, false,
                        false, false, false, isRelease, releaseType, "sink", true)

        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "processor-apps-release", branchToBuild)
                .deploy(false, false, false,
                        false, false, false, isRelease, releaseType, "processor", true)

        new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "stream-applications-release-train", branchToBuild)
                .deploy(false, false, false, false, true, false, isRelease, releaseType)

        if (!branchToBuild.equals("2020.0.x")) {
            new StreamApplicationsBuildMaker(dsl, "spring-cloud", "stream-applications", "stream-applications-integration-tests", branchToBuild)
                    .deploy(false, false, false, false, false, false, isRelease, releaseType,
                            "", false, true)
        }
    }
}
