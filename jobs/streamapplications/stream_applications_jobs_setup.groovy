package streamapplications

import org.springframework.jenkins.stream.applications.ci.StreamApplicationsPhasedBuildMaker
import javaposse.jobdsl.dsl.DslFactory

DslFactory dsl = this

String releaseType = "" // possible values are - "", milestone or ga

// Main CI
new StreamApplicationsPhasedBuildMaker(dsl).build(true, "ga")

// 2021.0.x CI
new StreamApplicationsPhasedBuildMaker(dsl).build(false, "", "2021.0.x")

// 2020.0.x CI
new StreamApplicationsPhasedBuildMaker(dsl).build(false, "", "2020.0.x")
