import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement

def seeds = new File('/usr/share/jenkins/seeds/')
def jobManagement = new JenkinsJobManagement(System.out, [:], new File('.'))

new DslScriptLoader(jobManagement).with {
	seeds.eachFileRecurse (groovy.io.FileType.FILES) { file ->
  		runScript(file.text)
	}
}

println "Downloading the custom JMH Plugin"
def url = 'https://bintray.com/marcingrzejszczak/jenkins/download_file?file_path=jmh-jenkins%2F0.0.1%2Fjmhbenchmark.hpi'
def file = new File("${System.getenv('JENKINS_HOME')}/plugins/jmhbenchmark.hpi")
def stream = file.newOutputStream()
stream << new URL(url).openStream()
stream.close()
