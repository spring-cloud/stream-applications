if ('foo' == headers.route) {
    return "$foo" // mapped to baz in 'variables'
}
else {
    return "$bar" // mapped to qux in properties file
}
