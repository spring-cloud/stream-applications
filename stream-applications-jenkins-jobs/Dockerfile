# Useful:
#   http://www.catosplace.net/blog/2015/02/11/running-jenkins-in-docker-containers/
#   https://github.com/jenkinsci/docker#preinstalling-plugins
#   https://engineering.riotgames.com/news/jenkins-docker-proxies-and-compose

FROM jenkins:2.0
MAINTAINER Marcin Grzejszczak <mgrzejszczak@pivotal.io>

COPY seed/init/init.groovy /usr/share/jenkins/ref/init.groovy
COPY seed/init/meta_seed.groovy /usr/share/jenkins/seeds/meta_seed.groovy

# To print the list of plugins from a server
#
# GOTO:
# 1) https://jenkins.address/pluginManager/api/xml?depth=1
# 2) Store the XML
# 3) Run this Groovy DSL
# def root = new groovy.util.XmlSlurper().parseText(xml)
#        println root.plugin.collect {
#            "${it.shortName}:$it.version"
#        }.join('\n')
#
# To get the list of plugins from an existing server:
#  JENKINS_HOST=myhost.com:port
#  curl -sSL "http://$JENKINS_HOST/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins" | perl -pe 's/.*?<shortName>([\w-]+).*?<version>([^<]+)()(<\/\w+>)+/\1:\2\n/g'

COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
