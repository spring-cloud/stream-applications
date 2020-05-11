if (headers.route.equals('foo')) {
    return "$foo" // mapped to baz in 'variables'
} else {
    return "$bar" // mapped to qux in properties file
}
