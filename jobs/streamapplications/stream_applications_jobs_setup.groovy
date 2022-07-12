package streamapplications

import org.springframework.jenkins.stream.applications.ci.StreamApplicationsPhasedBuildMaker
import javaposse.jobdsl.dsl.DslFactory

DslFactory dsl = this

String releaseType = "" // possible values are - "", milestone or ga

// Main CI
new StreamApplicationsPhasedBuildMaker(dsl).build(false, "")

// 2021.1.x CI
new StreamApplicationsPhasedBuildMaker(dsl).build(false, "", "2021.1.x")

// 2021.0.x CI
new StreamApplicationsPhasedBuildMaker(dsl).build(false, "", "2021.0.x")
