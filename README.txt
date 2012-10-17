The default build runs without extended tests. To enable these tests in your
environment, run the following command:

  mvn -Prun-its install

Alternatively, you can run them directly from the snorocket-tests directory
without the profile if the other modules have been built:

  mvn -f snorocket-tests/pom.xml test

These will be run automatically in the continuous integration environment.
