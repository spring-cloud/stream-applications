package org.springframework.jenkins.cloud

import groovy.io.FileType
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.ScriptRequest
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {

    @Unroll
    def 'test script #file.name'() {
        given:

        MemoryJobManagement jm = new MemoryJobManagement()
        jm.parameters << [
                SCRIPTS_DIR: 'foo'
        ]
        DslScriptLoader loader = new DslScriptLoader(jm)

        when:
            loader.runScripts([new ScriptRequest(file.text)])

        then:
            noExceptionThrown()

        where:
            file << jobFiles
    }

    static List<File> getJobFiles() {
        List<File> files = []
        new File('jobs').eachFileRecurse(FileType.FILES) {
            files << it
        }
        return files
    }

}

