package org.springframework.jenkins.stream.applications.common

import org.springframework.jenkins.common.job.BuildAndDeploy

/**
 * @author Marcin Grzejszczak
 */
trait StreamApplicaitonsUtilsTrait extends BuildAndDeploy {

	@Override
	String projectSuffix() {
		return 'stream-applications'
	}

	/**
	 * Dirty hack cause Jenkins is not inserting Maven to path...
	 * Requires using Maven3 installation before calling
	 *
	 */
	String mavenBin() {
		return "/opt/jenkins/data/tools/hudson.tasks.Maven_MavenInstallation/maven33/apache-maven-3.3.9/bin/"
	}

	String setupGitCredentials() {
		return """
					set +x
					git config user.name "${githubUserName()}"
					git config user.email "${githubEmail()}"
					git config credential.helper "store --file=/tmp/gitcredentials"
					echo "https://\$${githubRepoUserNameEnvVar()}:\$${githubRepoPasswordEnvVar()}@github.com" > /tmp/gitcredentials
					set -x
				"""
	}

	String githubUserName() {
		return 'spring-buildmaster'
	}

	String githubEmail() {
		return 'buildmaster@springframework.org'
	}

	String githubRepoUserNameEnvVar() {
		return 'GITHUB_REPO_USERNAME'
	}

	String githubRepoPasswordEnvVar() {
		return 'GITHUB_REPO_PASSWORD'
	}

	String dockerHubUserNameEnvVar() {
		return 'DOCKER_HUB_USERNAME'
	}

	String dockerHubPasswordEnvVar() {
		return 'DOCKER_HUB_PASSWORD'
	}

	String cleanGitCredentials() {
		return "rm -rf /tmp/gitcredentials"
	}

	String cleanAndDeployCommonParent(boolean isRelease, String releaseType) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw stream-applications-build
					cp -R .mvn stream-applications-build
					cd stream-applications-build

                    lines=\$(find . -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                        set +x
                        ./mvnw clean deploy -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        set -x
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                    else
                        echo "Non release versions found. Exiting build"
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                        exit 1
                    fi
                """
		}
		if (isRelease && releaseType != null && releaseType.equals("milestone")) {
			return """
					#!/bin/bash -x

					cp mvnw stream-applications-build
					cp -R .mvn stream-applications-build
					cd stream-applications-build

			   		lines=\$(find . -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
					if [ \$lines -eq 0 ]; then
						./mvnw clean deploy -U -Pspring
						rm mvnw
                        rm -rf .mvn
                        cd ..
					else
						echo "Snapshots found. Exiting the release build."
						rm mvnw
                        rm -rf .mvn
                        cd ..
						exit 1
					fi
			   """
		}
	}

	String cleanAndDeployFunctions(boolean isRelease, String releaseType) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw functions
					cp -R .mvn functions
					cd functions

                    lines=\$(find . -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                        set +x
                        ./mvnw clean deploy -Pspring -Pintegration -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        set -x
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                    else
                        echo "Non release versions found. Exiting build"
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                        exit 1
                    fi
                """
		}
		if (isRelease && releaseType != null && releaseType.equals("milestone")) {
			return """
					#!/bin/bash -x

					cp mvnw functions
					cp -R .mvn functions
					cd functions

			   		lines=\$(find . -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
					if [ \$lines -eq 0 ]; then
						./mvnw clean deploy -U -Pintegration -Pspring
						rm mvnw
                        rm -rf .mvn
                        cd ..
					else
						echo "Snapshots found. Exiting the release build."
						rm mvnw
                        rm -rf .mvn
                        cd ..
						exit 1
					fi
			   """
		}
	}

	String cleanAndDeployCore(boolean isRelease, String releaseType) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw applications/stream-applications-core
					cp -R .mvn applications/stream-applications-core
					cd applications/stream-applications-core

                    lines=\$(find . -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                        set +x
                        ./mvnw clean deploy -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        set -x
                        rm mvnw
                        rm -rf .mvn
                        cd ../..
                    else
                        echo "Non release versions found. Exiting build"
                        rm mvnw
                        rm -rf .mvn
                        cd ../..
                        exit 1
                    fi
                """
		}
		if (isRelease && releaseType != null && releaseType.equals("milestone")) {
			return """
					#!/bin/bash -x

					cp mvnw applications/stream-applications-core
					cp -R .mvn applications/stream-applications-core
					cd applications/stream-applications-core

			   		lines=\$(find . -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
					if [ \$lines -eq 0 ]; then
						./mvnw clean deploy -U -Pspring

						rm mvnw
                        rm -rf .mvn
                        cd ../..
					else
						echo "Snapshots found. Exiting the release build."
						rm mvnw
                        rm -rf .mvn
                        cd ../..
						exit 1
					fi
			   """
		}
	}

	String bulkAppsGaRelease(boolean isRelease, String releaseType, String appsType) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw applications/${appsType}
					cp -R .mvn applications/${appsType}

                    cd applications/${appsType}
                    find . -name "apps" -type d -exec rm -r "{}" \\; || true

                    lines=\$(find . -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                 
						export MAVEN_PATH=${mavenBin()}
						${setupGitCredentials()}
						for dir in */ ; do
    						echo "Now processing: \${dir}"
							cd \${dir}

							if [ -d "src/main/java" ]
							then
								echo "Source folder found."
								set +x
								../mvnw clean deploy -Pintegration -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
								set -x
							else
								../mvnw clean package -Pintegration -U
							fi
							
                        	echo "Building apps"
                        	cd apps
                        	set +x
                        	./mvnw clean deploy -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
					gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        	set -x
                        	
							echo "Pushing to Docker Hub"
							set +x
                    		./mvnw -U clean package jib:build -DskipTests -Djib.httpTimeout=1800000 -Djib.to.auth.username="\$${dockerHubUserNameEnvVar()}" -Djib.to.auth.password="\$${dockerHubPasswordEnvVar()}"
							if [[ "\\\$?" -ne 0 ]] ; then
								set -e
								echo "Apps Docker Build failed: Rerunning again"
								./mvnw -U clean package jib:build -DskipTests -Djib.httpTimeout=1800000 -Djib.to.auth.username="\$${dockerHubUserNameEnvVar()}" -Djib.to.auth.password="\$${dockerHubPasswordEnvVar()}"
                        	fi
							set -x
							cd ../..
						done
						
						${cleanGitCredentials()}

                        echo "Now in: "pwd
						rm mvnw
                        rm -rf .mvn
                    else
                        echo "Non release versions found. Exiting build"
						rm mvnw
                        rm -rf .mvn
						exit 1
                    fi
                """
		}
	}

	String cleanAndDeployWithGenerateApps(boolean isRelease, String releaseType, String cdToApps) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw applications/${cdToApps}
					cp -R .mvn applications/${cdToApps}

                    cd applications/${cdToApps}
                    rm -rf apps

                    lines=\$(find . -type f -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                        set +x
						if [ -d "src/main/java" ]
						then
							echo "Source folder found."
							./mvnw clean deploy -Pintegration -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
						else
							./mvnw clean package -Pintegration -U
						fi
                        set -x
						rm mvnw
                        rm -rf .mvn
						cd -
                    else
                        echo "Non release versions found. Exiting build"
						rm mvnw
                        rm -rf .mvn
						cd -
						exit 1
                    fi
                """
		}
		if (isRelease && releaseType != null && releaseType.equals("milestone")) {
			return """
                #!/bin/bash -x

				cp mvnw applications/${cdToApps}
				cp -R .mvn applications/${cdToApps}
                cd applications/${cdToApps}
                rm -rf apps

                lines=\$(find . -type f -name pom.xml | xargs grep SNAPSHOT | wc -l)
                if [ \$lines -eq 0 ]; then
                     set +x
					 if [ -d "src/main/java" ]
					 then
						echo "Source folder found."
						./mvnw clean deploy -Pintegration -U
					else
						./mvnw clean package -Pintegration -U
					fi
                    set -x
					rm mvnw
                    rm -rf .mvn
					cd -
                else
                    echo "Snapshots found. Exiting the release build."
					rm mvnw
                    rm -rf .mvn
					cd -
					exit 1
                fi
           """
		}
	}

	String cleanAndInstallAggregate(boolean isRelease, String releaseType) {
		if (isRelease && releaseType != null && !releaseType.equals("milestone")) {
			return """
                    #!/bin/bash -x

					cp mvnw stream-applications-release-train
					cp -R .mvn stream-applications-release-train
					cd stream-applications-release-train
					./mvnw clean
                    lines=\$(find . -name pom.xml | xargs egrep "SNAPSHOT|M[0-9]|RC[0-9]" | grep -v ".contains(" | grep -v regex | wc -l)
                    if [ \$lines -eq 0 ]; then
                        set +x
                        ./mvnw clean deploy -Pspring -Dgpg.secretKeyring="\$${gpgSecRing()}" -Dgpg.publicKeyring="\$${
				gpgPubRing()}" -Dgpg.passphrase="\$${gpgPassphrase()}" -DSONATYPE_USER="\$${sonatypeUser()}" -DSONATYPE_PASSWORD="\$${sonatypePassword()}" -Pcentral -U
                        set -x
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                    else
                        echo "Non release versions found. Exiting build."
                        rm mvnw
                        rm -rf .mvn
                        cd ..
                        exit 1
                    fi
                """
		}
		if (isRelease && releaseType != null && releaseType.equals("milestone")) {
			return """
					#!/bin/bash -x

					cp mvnw stream-applications-release-train
					cp -R .mvn stream-applications-release-train
					cd stream-applications-release-train
					./mvnw clean
			   		lines=\$(find . -type f -name pom.xml | xargs grep SNAPSHOT | grep -v ".contains(" | grep -v regex | wc -l)
					if [ \$lines -eq 0 ]; then
						./mvnw clean install -U -Pspring
						rm mvnw
                        rm -rf .mvn
                        cd ..
					else
						echo "Snapshots found. Exiting the release build."
						rm mvnw
                        rm -rf .mvn
                        cd ..x`
						exit 1
					fi
			   """
		}
	}

	String gpgSecRing() {
		return 'FOO_SEC'
	}

	String gpgPubRing() {
		return 'FOO_PUB'
	}

	String gpgPassphrase() {
		return 'FOO_PASSPHRASE'
	}

	String sonatypeUser() {
		return 'SONATYPE_USER'
	}

	String sonatypePassword() {
		return 'SONATYPE_PASSWORD'
	}
}
