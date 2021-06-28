package es.weso.rdfshape.cli

/**
 * Data class encapsulating the values read from the arguments passed to the application
 * @param port Port number read from CLI
 * @param https HTTPS value read from CLI (true or false)
 * @param verbose Verbosity level read from CLI
 */
case class ArgumentsData(port: Int, https: Boolean, verbose: Int) {}
