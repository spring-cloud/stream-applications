package org.springframework.jenkins.stream.applications.ci

import javaposse.jobdsl.dsl.DslFactory
import org.springframework.jenkins.common.job.Cron
import org.springframework.jenkins.common.job.JdkConfig
import org.springframework.jenkins.common.job.Maven
import org.springframework.jenkins.common.job.TestPublisher
import org.springframework.jenkins.stream.applications.common.StreamApplicaitonsUtilsTrait

import static org.springframework.jenkins.common.job.Artifactory.artifactoryMaven3Configurator
import static org.springframework.jenkins.common.job.Artifactory.artifactoryMavenBuild
/**
 * @author Marcin Grzejszczak
 * @author Soby Chacko
 */
class StreamApplicationsBuildMaker implements JdkConfig, TestPublisher,
        Cron, StreamApplicaitonsUtilsTrait, Maven {

    private final DslFactory dsl
    final String organization
    final String project
    final String repository

    String branchToBuild = "main"

    String jdkVersion = jdk8()

    StreamApplicationsBuildMaker(DslFactory dsl, String organization, String repository,
                                 String project, String branchToBuild) {
        this.dsl = dsl
        this.organization = organization
        this.repository = repository
        this.project = project
        this.branchToBuild = branchToBuild
    }

    void deploy(
            boolean commonParentBuild = false,
            boolean functionsBuild = false, boolean coreBuild = false,
            boolean appsBuild = false, boolean appsAggregateBuild = false,
            boolean dockerHubPush = false, boolean isRelease = false,
            String releaseType = "", String cdToApps = "", boolean appsAlternateBuild = false, boolean integTestsBuild = false) {

        dsl.job("${prefixJob(project)}-${branchToBuild}-ci") {
            scm {
                git {
                    remote {
                        url "https://github.com/${organization}/${repository}"
                        branch branchToBuild
                    }
                }
            }
            jdk jdk8()
            wrappers {
                colorizeOutput()
                maskPasswords()
                credentialsBinding {
                    usernamePassword('DOCKER_HUB_USERNAME', 'DOCKER_HUB_PASSWORD', "hub.docker.com-springbuildmaster")
                }
                if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
                    credentialsBinding {
                        file('FOO_SEC', "spring-signing-secring.gpg")
                        file('FOO_PUB', "spring-signing-pubring.gpg")
                        string('FOO_PASSPHRASE', "spring-gpg-passphrase")
                        usernamePassword('SONATYPE_USER', 'SONATYPE_PASSWORD', "oss-token")
                        usernamePassword('DOCKER_HUB_USERNAME', 'DOCKER_HUB_PASSWORD', "hub.docker.com-springbuildmaster")
                    }
                }
            }

            steps {
                if (isRelease) {
                    if (commonParentBuild) {
                        shell(cleanAndDeployCommonParent(isRelease, releaseType))
                    }
                    else if (functionsBuild) {
                        shell(cleanAndDeployFunctions(isRelease, releaseType))
                    }
                    else if (coreBuild) {
                        shell(cleanAndDeployCore(isRelease, releaseType))
                    }
                    else if (appsBuild) {
                        shell(cleanAndDeployWithGenerateApps(isRelease, releaseType, cdToApps))
                    }
                    else if (appsAlternateBuild) {
                        shell(bulkAppsGaRelease(isRelease, releaseType, cdToApps))
                    }
                    else if (appsAggregateBuild) {
                        shell(cleanAndInstallAggregate(isRelease, releaseType))
                    }
                }
                else {
                    if (commonParentBuild) {
                        maven {
                            mavenInstallation(maven35())
                            goals('clean deploy -U -Pspring -f stream-applications-build')
                        }
                    }
                    else if (functionsBuild) {
                        maven {
                            mavenInstallation(maven35())
                            goals('clean deploy -U -Pspring -Pintegration -f functions')
                        }
                    }
                    else if (coreBuild) {
                        maven {
                            mavenInstallation(maven35())
                            goals('clean deploy -U -Pspring -f applications/stream-applications-core')
                        }
                    }
                    else if (appsBuild) {
                        def (appType, app) = cdToApps.tokenize( '/' )

                        shell("""set -e
                        #!/bin/bash -x
                        export MAVEN_PATH=${mavenBin()}
                        ${setupGitCredentials()}
                        echo "Building app generator"
                        cd applications/${cdToApps}
                        rm -rf apps
                        if [ -d "src/main/java" ]
                        then
                            echo "Source folder found."
                            cd -
                            ./mvnw clean deploy -U -Pintegration -pl :${app}
                        else
                            cd -
                            ./mvnw clean package -U -Pintegration -pl :${app}
                        fi
                        ${cleanGitCredentials()}
                        """)
                    }
                    else if (appsAggregateBuild) {
                        maven {
                            mavenInstallation(maven35())
                            goals('clean install -U -Pspring -f stream-applications-release-train')
                        }
                    }
                }

                if (appsBuild) {
                    if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
                        shell("""set -e
                        #!/bin/bash -x
                        export MAVEN_PATH=${mavenBin()}
                        ${setupGitCredentials()}
                        echo "Building apps"
                        cd applications/${cdToApps}
                        cd apps
                        set +x
                        ./mvnw clean deploy -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
                            gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        set -x
                        ${cleanGitCredentials()}
                        """)
                    }
                    else {
                        shell("""
                        #!/bin/bash -x
                        export MAVEN_PATH=${mavenBin()}
                        ${setupGitCredentials()}
                        echo "Building apps"
                        cd applications/${cdToApps}
                        cd apps
                        ./mvnw clean deploy -U
                        if [[ "\$?" -ne 0 ]] ; then
                            set -e
                            echo "Apps maven Build failed: Rerunning again"
                            ./mvnw clean deploy -U
                        fi
                        ${cleanGitCredentials()}
                        """)
                    }
                }
				if (dockerHubPush) {
					shell("""
					#!/bin/bash -x
					
					export MAVEN_PATH=${mavenBin()}
					${setupGitCredentials()}
				
					clean_tmp_files() {
					  echo "Cleaning tmp files"
					  ${cleanGitCredentials()}
					}
				
					trap "clean_tmp_files" EXIT
				
					run_jib_build() {
					  echo "Running JIB build"
					  mvn_out=\$(./mvnw -U clean package jib:build -DskipTests -Djib.to.tags=${branchToBuild} -Djib.httpTimeout=1800000 -Djib.to.auth.username="\$${dockerHubUserNameEnvVar()}" -Djib.to.auth.password="\$${dockerHubPasswordEnvVar()}")
					  echo \$mvn_out
					  fail_count=\$(echo \$mvn_out | grep -c "BUILD FAILURE")
					  return \$fail_count
					}
				
					echo "Pushing to Docker Hub"
					cd applications/${cdToApps}
					cd apps
					set +x
				
					# handle the exiting ourselves
					set +e
				
					run_jib_build || ( \
					  echo "Apps Docker build failed - trying again in 5 seconds..."
					  sleep 5
					  run_jib_build || ( \
						echo "Apps Docker Build failed on retry"
						exit 1
					  )
					)
				
					exit_code=\$?
					if [ \$exit_code -eq 0 ]
					then
					  echo "Apps Docker build success"
					fi
				
					exit \$exit_code
					""")
				}
                if (integTestsBuild) {
                    maven {
                        mavenInstallation(maven35())
                        goals("-U clean test -pl :stream-applications-integration-tests -Pintegration -Dspring.cloud.stream.applications.version=${branchToBuild}")
                    }
                }
            }
            configure {
                if (appsAggregateBuild) {
                    artifactoryMavenBuild(it as Node) {
                        mavenVersion(maven35())
                        goals('clean install -U -Pfull -Pspring -f stream-applications-release-train')
                    }
                    artifactoryMaven3Configurator(it as Node) {
                        if (isRelease && releaseType != null && releaseType.equals("milestone")) {
                            deployReleaseRepository("libs-milestone-local")
                        }
                        else if (isRelease) {
                            deployReleaseRepository("libs-release-local")
                        }
                    }
                }
            }
            publishers {
                mailer('abilan@vmware.com chackos@vmware.com dturanski@vmware.com ctzolov@vmware.com cbono@vmware.com', true, true)
            }

        }
    }
}
