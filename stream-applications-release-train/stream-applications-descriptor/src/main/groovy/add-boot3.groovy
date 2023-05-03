import groovy.io.FileType

def processFile(String fileName) {
    def requiresBoot3 = new HashSet()
    def inputFile = new File(fileName)
    if (inputFile.canRead()) {
        inputFile.withInputStream { input ->
            def items = [:]
            def properties = new Properties()
            properties.load(input)

            properties.keySet().forEach { key ->
                if (!key.endsWith('.bootVersion') && !key.endsWith('.metadata')) {
                    if (properties.getProperty("${key}.bootVersion") == null) {
                        requiresBoot3.add(key)
                    }
                }
            }
        }
    } else {
        println("Cannot read $inputFile")
    }
    if (!requiresBoot3.isEmpty()) {
        println("BootVersion entries required for $inputFile")
        StringWriter sw = new StringWriter()
        sw.withPrintWriter { pw ->
            String inputString = inputFile.text
            inputString.lines().forEach { line ->
                pw.println(line)
                String key = line.takeBefore('=')
                if (requiresBoot3.contains(key)) {
                    String newLine = "${key}.bootVersion=3"
                    pw.println(newLine)
                    println("Adding $newLine")
                }
            }
        }
        println("Updating $inputFile")
        inputFile.text = sw.toString()
    } else {
        println("bootVersion not required for $inputFile")
    }
}

if (properties['dir']) {
    def dir = new File(properties['dir'])
    dir.eachFileRecurse(FileType.FILES) { file ->
        if(file.name.endsWith('.properties')) {
            processFile(file.path)
        }
    }
} else {
    if(args.length == 0) {
        println('Require filename argument')
    } else {
        args.forEach { arg ->
            processFile(arg)
        }
    }
}