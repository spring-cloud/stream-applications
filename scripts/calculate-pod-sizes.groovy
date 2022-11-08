// Collect all pods.
// find container that are not completed and determine their size to calculate the total memory used by all the pods.

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.util.stream.Collectors

double stringToGb(boolean verbose, String memory) {
    double result = 0.5
    if (memory.endsWith('Mi')) {
        result = Double.parseDouble(memory - 'Mi') / 1024.0
    } else if (memory.endsWith('Mb')) {
        result = Double.parseDouble(memory - 'Mb') / 1000.0
    } else if (memory.endsWith('Gi')) {
        result = Double.parseDouble(memory - 'Gi')
    } else if (memory.endsWith('Gb')) {
        result = Double.parseDouble(memory - 'Gb')
    } else if (memory.endsWith('Ki')) {
        result = Double.parseDouble(memory - 'Ki') / (1024.0 * 1024.0)
    } else if (memory.endsWith('Kb')) {
        result = Double.parseDouble(memory - 'Kb') / 1000000.0
    }
    if (verbose) {
        println "$memory=$result G"
    }
    return result
}

double stringToCpu(boolean verbose, String cpu) {
    double result = 0.1
    if (cpu.endsWith('m')) {
        result = Double.parseDouble(cpu - 'm') / 1000.0
    } else {
        result = Double.parseDouble(cpu)
    }
    if (verbose) {
        println "$cpu=$result"
    }
    return result
}

double memoryCalc(boolean verbose, String limit, String request) {
    if (request != null) {
        return stringToGb(verbose, request)
    }
    if (limit != null) {
        return stringToGb(verbose, limit)
    }
    return 0.5
}

double cpuCalc(boolean verbose, String limit, String request) {
    if (request != null) {
        return stringToCpu(verbose, request)
    }
    if (limit != null) {
        return stringToCpu(verbose, limit)
    }
    return 0.1
}

double calculateUsing(boolean verbose, Map matrix, Closure<Double> calc) {
    return matrix.items.stream().filter { pod ->
        if (verbose) {
            println "Pod:$pod.metadata"
        }
        pod.status.containerStatuses.stream().anyMatch { containerStatus ->
            if (verbose) {
                println "\tcontainerStatus:$containerStatus"
            }
            boolean running = containerStatus.state?.running != null
            if (verbose) {
                println "Pod:${pod.metadata.name}:$running"
            }
            return running
        }
    }.map { pod ->
        double m = pod.spec.containers.stream().map { container ->
            double result = calc.call(pod, container)
            if (verbose) {
                println "Pod:${pod.metadata.name}:Container:${container.name}=$result"
            }
            return result
        }.collect(Collectors.summingDouble(Double::doubleValue))
        if (verbose) {
            println "Pod:${pod.metadata.name}=$m"
        }
        return m
    }.collect(Collectors.summingDouble(Double::doubleValue))
}

Map<String, Double> calculatePodSizes(boolean verbose, String fileName) {
    def file = new File(fileName)
    if (!file.exists()) {
        System.err.println "File not found: $file.absoluteFile"
        System.exit(2)
    }
    JsonSlurper jsonSlurper = new JsonSlurper()
    if (verbose) {
        println "Loading: $file.absoluteFile"
    }
    def matrix = jsonSlurper.parse(file)
    double memory = calculateUsing(verbose, matrix) { pod, container ->
        double result = memoryCalc(verbose, container.resources.limits?.memory, container.resources.requests?.memory)
        if (verbose) {
            println "Pod:${pod.metadata.name}:Container:${container.name}=$result"
        }
        return result
    }
    double cpu = calculateUsing(verbose, matrix) { pod, container ->
        double result = cpuCalc(verbose, container.resources.limits?.cpu, container.resources.requests?.cpu)
        if (verbose) {
            println "Pod:${pod.metadata.name}:Container:${container.name}=$result"
        }
        return result
    }
    return ['memory': memory, 'cpu': cpu]
}

if (args.length == 0) {
    println 'Usage: [--verbose] <json-pods-file> [<json-pods-files>]'
    System.exit(1)
}
boolean verbose = false
double totalMemory = 0.0
double totalCpu = 0.0
for (arg in args) {
    if (arg == '--verbose') {
        verbose = true
    } else {
        Map<String, Double> sizes = calculatePodSizes(verbose, arg)
        if (verbose) {
            println "File:$arg:sizes=$sizes"
        }
        if (sizes['memory'] != null) {
            totalMemory += sizes['memory']
        }
        if (sizes['cpu'] != null) {
            totalCpu += sizes['cpu']
        }
    }
}
long memory = (long) Math.ceil(totalMemory)
long cpu = (long) Math.ceil(totalCpu)
Map<String, Long> values = ['memory': memory, 'cpu': cpu]
def result = JsonOutput.toJson(values)
println "$result"

