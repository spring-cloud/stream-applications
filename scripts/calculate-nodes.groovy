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

int divideRoundUp(int a, int b) {
    int r = a / b
    if (a % b != 0) {
        r += 1
    }
    return r
}

void usageExit(String message = null) {
    if (message != null) {
        println message
    }
    println 'Usage: [--output <outputFile>] [--verbose] [--shrink] [--machine <machineType>] [--current <currentNodeCount>] [--ram <requiredRam>] [--cpu <requiredCpu>] [--min <minimumNodes>] [--max <maximumNodes>] [--nodes <requiredNodes>] [--cpu-per-pod <cpuPerPod>] [--pods-per-job <podsPerJob>] [--pods <requiredPods>] [--podfile <podfile>]'
    println '''
\tmachine-type:      The machine used by provider for nodes.
\tcurrentNodeCount:  Total number of nodes in cluster.
\toutputFile:        JSON output { "nodes": number, "shrink": boolean } will be written to the file.
\tpodfile:           Output of "kubectl get pods --A -o json" will be used to determine used cpus and memory. 
\trequiredNodes:     Total number of nodes required. If podFile is provided the available will be considered.
\trequiredRam:       RAM required. machineType is required to calculate nodes.
\trequiredCpu:       CPU required. machineType is required to calculate nodes.
\tmaximumNodes:      Maximum number of nodes allowed in the cluster.
\tminimumNodes:      Minimum number of nodes allowed in the cluster.
\tverbose:           Provide extra output.
\tshrink:            Allow a target lower than the currentNodeCount.                
'''
    System.exit(1)
}
Integer requiredRam = 0
Integer requiredCpu = 0
int currentNodes = 0
int maxNodes = -1
int minNodes = -1
String outputFile
String machineType
String podFile
int ramUsed = 0
int cpuUsed = 0
int additionalNodes = 0
int requiredPods = 0
int podsPerJob = 1
double ramPerPod = 1.0
double cpuPerPod = 1.0
Integer requiredNodes = null
boolean shrink = false
boolean verbose = false
for (int i = 0; i < args.length; i++) {
    String arg = args[i]
    switch (arg) {
        case '--shrink':
            shrink = true
            break
        case '--verbose':
            verbose = true
            println "Arguments: $args"
            break
        case '--podfile':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --podfile')
            }
            podFile = args[i + 1]
            i += 1
            break
        case '--output':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --output')
            }
            outputFile = args[i + 1]
            i += 1
            break
        case '--add':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --add')
            }
            additionalNodes = args[i+1].toInteger()
            i += 1
            break
        case '--pods':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --pods')
            }
            requiredPods = args[i + 1].toInteger()
            if (requiredPods == 0) {
                usageExit('pods-required must not be 0')
            }
            i += 1
            break
        case '--pods-per-job':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --pods-per-job')
            }
            podsPerJob = args[i + 1].toInteger()
            if (podsPerJob <= 0) {
                usageExit('pods-required must be greater than 0')
            }
            i += 1
            break
        case '--cpu-per-pod':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --cpu-per-pod')
            }
            cpuPerPod = Double.parseDouble(args[i + 1])
            if (cpuPerPod == 0) {
                usageExit('cpu-per-pod must not be 0')
            }
            i += 1
            break
        case '--ram-per-pod':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --ram-per-pod')
            }
            ramPerPod = Double.parseDouble(args[i + 1])
            if (ramPerPod == 0) {
                usageExit('ram-per-pod must not be 0')
            }
            i += 1
            break
        case '--cpu':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --cpu')
            }
            requiredCpu = Math.ceil(Double.parseDouble(args[i + 1])).intValue()
            if (requiredCpu < 0) {
                shrink = true
            }
            i += 1
            break
        case '--ram':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --ram')
            }
            requiredRam = Math.ceil(Double.parseDouble(args[i + 1])).intValue()
            if (requiredRam < 0) {
                shrink = true
            }
            i += 1
            break
        case '--nodes':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --nodes')
            }
            requiredNodes = args[i + 1].toInteger()
            if (requiredNodes < 0) {
                shrink = true
            }
            i += 1
            break
        case '--max':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --max')
            }
            maxNodes = args[i + 1].toInteger()
            assert maxNodes >= 0
            i += 1
            break
        case '--min':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --min')
            }
            minNodes = args[i + 1].toInteger()
            assert minNodes >= 0
            i += 1
            break
        case '--machine':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --machine')
            }
            machineType = args[i + 1]
            i += 1
            break
        case '--current':
            if (args.length <= i + 1) {
                usageExit('Missing arguments after --current')
            }
            currentNodes = args[i + 1].toInteger()
            i += 1
            break
        default:
            usageExit("Invalid argument: $arg")
    }
}
if ((requiredPods > 0 || requiredRam > 0 || requiredCpu > 0) && (machineType == null)) {
    usageExit("Machine type is required to calculate nodes from Pods, RAM or CPU required")
}
if ((requiredPods > 0 || requiredRam > 0 || requiredCpu > 0) && (requiredNodes != null)) {
    usageExit("Cannot require Nodes and Pods, CPU or RAM")
}

int nodeRam = -1
int nodeCpu = -1
if(machineType) {
    switch (machineType) {
        case 't2.small':
            nodeRam = 2
            nodeCpu = 1
            break
        case 't3.small': case 't3a.small':
            nodeRam = 2
            nodeCpu = 2
            break

        case 'n2-highcpu-2': case 'n2d-highcpu-2': case 'e2-highcpu-2':
            nodeCpu = 2
            nodeRam = 2
            break
        case 'c2d-highcpu-2': case 't2.medium': case 't3.medium': case 't3a.medium':
            nodeRam = 4
            nodeCpu = 2
            break
        case 'n2-highcpu-4': case 'n2d-highcpu-4': case 'e2-highcpu-4':
            nodeRam = 4
            nodeCpu = 4
            break
        case 'n2-highmem-2': case 'n2d-highmem-2': case 'c2d-highmem-2': case 'e2-highmem-2':
            nodeRam = 16
            nodeCpu = 2
            break
        case 'n2-standard-2': case 'n2d-standard-2': case 'c2d-standard-2': case 'e2-standard-2':
        case 'm5.large': case 'm5a.large': case 't2.large': case 't3a.large': case 't3.large':
            nodeRam = 8
            nodeCpu = 2
            break
        case 'c2d-highcpu-4':
            nodeRam = 8
            nodeCpu = 4
            break
        case 'n2-highcpu-8': case 'n2d-highcpu-8': case 'e2-highcpu-8':
            nodeRam = 8
            nodeCpu = 8
            break
        case 'n2-standard-4': case 'n2d-standard-4': case 'c2-standard-4': case 'c2d-standard-4': case 'e2-standard-4':
        case 'm5.xlarge': case 'm5a.xlarge': case 't2.xlarge': case 't3a.xlarge': case 't3.xlarge':
            nodeRam = 16
            nodeCpu = 4
            break
        case 'c2d-highcpu-8':
            nodeRam = 16
            nodeCpu = 8
            break
        case 'n2-highcpu-16': case 'n2d-highcpu-16': case 'e2-highcpu-16':
            nodeRam = 16
            nodeCpu = 16
            break
        case 'n2-highmem-4': case 'n2d-highmem-4': case 'c2d-highmem-4': case 'e2-highmem-4':
            nodeRam = 32
            nodeCpu = 4
            break
        case 'n2-standard-8': case 'n2d-standard-8': case 'c2-standard-8': case 'c2d-standard-8': case 'e2-standard-8':
        case 'm5.2xlarge': case 'm5a.2xlarge': case 't2.2xlarge': case 't3a.2xlarge': case 't3.2xlarge':
            nodeRam = 32
            nodeCpu = 8
            break
        case 'c2d-highcpu-16':
            nodeRam = 32
            nodeCpu = 16
            break
        case 'n2-highcpu-32': case 'n2d-highcpu-32': case 'e2-highcpu-32':
            nodeRam = 32
            nodeCpu = 32
            break
        case 'n2-highmem-8': case 'n2d-highmem-8': case 'c2d-highmem-8': case 'e2-highmem-8':
            nodeRam = 64
            nodeCpu = 8
            break
        case 'n2-standard-16': case 'n2d-standard-16': case 'c2-standard-16': case 'c2d-standard-16': case 'e2-standard-16':
        case 'm5.4xlarge': case 'm5a.4xlarge':
            nodeRam = 64
            nodeCpu = 16
            break
        case 'c2d-highcpu-32':
            nodeRam = 64
            nodeCpu = 32
            break
        case 'n2-highcpu-64': case 'n2d-highcpu-64':
            nodeRam = 64
            nodeCpu = 64
            break
        case 'n2-highmem-16': case 'n2d-highmem-16': case 'c2d-highmem-16': case 'e2-highmem-16':
            nodeRam = 128
            nodeCpu = 16
            break
        default:
            println "Unknown machine type $machineType"
            System.exit(1)
    }
    if (verbose || outputFile != null) {
        println "Machine Type:$machineType CPU=$nodeCpu, RAM=$nodeRam"
    }
}

int usedMemory = 0
int usedCpu = 0

if (podFile != null && currentNodes > 0) {
    Map<String, Double> sizes = calculatePodSizes(verbose, podFile)
    if (verbose || outputFile != null) {
        println "Pods sizes=$sizes"
    }
    if (sizes.memory != null) {
        usedMemory = (int) Math.ceil(sizes.memory)
    }
    if (sizes.cpu != null) {
        usedCpu = (int) Math.ceil(sizes.cpu)
    }
}
int podsNodes = 0
int ramNodes = 0
int cpuNodes = 0
if(requiredPods != 0) {
    int nodesFromRam = Math.ceil(requiredPods.doubleValue() / Math.floor(nodeRam.doubleValue() / ramPerPod) * podsPerJob.doubleValue()).intValue()
    int nodesFromCpu = Math.ceil(requiredPods.doubleValue() / Math.floor(nodeCpu.doubleValue() / cpuPerPod) * podsPerJob.doubleValue()).intValue()
    requiredNodes = Math.max(nodesFromCpu, nodesFromRam)
    println "PODS: Nodes from RAM:$nodesFromRam, CPU:$nodesFromCpu, Additional Nodes: $additionalNodes, Required Nodes: $requiredNodes"
} else {

    if (requiredRam != 0) {
        int totalRam = currentNodes * nodeRam
        int availableRam = totalRam - usedMemory
        if (requiredRam > availableRam) {
            ramNodes = divideRoundUp(requiredRam - availableRam, nodeRam)
        } else if (requiredRam < 0 && Math.abs(requiredRam) < availableRam) {
            ramNodes = divideRoundUp(availableRam - Math.abs(requiredRam), nodeRam) * -1
        }
        if (verbose || outputFile != null) {
            println "RAM: total=$totalRam, used=$usedMemory, available=$availableRam, required=$requiredRam. Nodes=$ramNodes"
        }
    }

    if (requiredCpu != 0) {
        int totalCpu = currentNodes * nodeCpu
        int availableCpu = totalCpu - usedCpu
        if (requiredCpu > availableCpu) {
            cpuNodes = divideRoundUp(requiredCpu - availableCpu, nodeRam)
        } else if (requiredCpu < 0 && Math.abs(requiredCpu) < availableCpu) {
            cpuNodes = divideRoundUp(availableCpu - Math.abs(requiredCpu), nodeRam) * -1
        }
        if (verbose || outputFile != null) {
            println "CPU: total=$totalCpu, used=$usedCpu, available=$availableCpu, required=$requiredCpu. Nodes=$cpuNodes"
        }
    }
    if (cpuNodes != 0 && ramNodes != 0) {
        requiredNodes = Math.max(cpuNodes, ramNodes)
    } else if (cpuNodes != 0) {
        requiredNodes = cpuNodes
    } else if (ramNodes != 0) {
        requiredNodes = ramNodes
    }
}
if (requiredNodes == null) {
    requiredNodes = 0
}
int targetNodes = currentNodes + requiredNodes + additionalNodes
if (minNodes >= 0 && targetNodes < minNodes) {
    if (verbose || outputFile != null) {
        println "Min:$minNodes"
    }
    targetNodes = minNodes
}
if (maxNodes >= 0 && targetNodes > maxNodes) {
    if (verbose || outputFile != null) {
        println "Max:$maxNodes"
    }
    targetNodes = maxNodes
    shrink = true
}
if (!shrink && targetNodes < currentNodes) {
    targetNodes = currentNodes
}

if (verbose || outputFile != null) {
    println "Nodes: current=$currentNodes, required=$requiredNodes, target=$targetNodes, shrink=$shrink"
}
def result = [nodes: targetNodes, shrink: shrink]
String output = JsonOutput.toJson(result)
if (outputFile != null) {
    def file = new File(outputFile)
    file.text = output
    if (verbose) {
        println "Created: $outputFile"
    }
} else {
    println output
}
