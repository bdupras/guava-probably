Guava-Probably: Probabilistic data structures for Guava 
=======================================================

[![Build Status](https://travis-ci.org/bdupras/guava-probably.svg?branch=master)](https://travis-ci.org/bdupras/guava-probably)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.duprasville.guava.guava-probably/guava-probably/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.duprasville.guava.guava-probably/guava-probably/)

The Guava-Probably project provides several probabilistic data structures for Guava.

Requires JDK 1.6 or higher and Google Guava 16.0.1 or higher (as of 1.0).


Latest release
--------------

The most recent release is [Guava-Probably 1.0][], released December 30, 2015.

- 1.0 API Docs: [guava-probably][guava-probably-release-api-docs]

To add a dependency on Guava-Probably using Maven, use the following:

```xml
<dependency>
  <groupId>com.duprasville.guava.guava-probably</groupId>
  <artifactId>guava-probably</artifactId>
  <version>1.0</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'com.duprasville.guava.guava-probably:guava-probably:1.0'
}
```

Snapshots
---------

Snapshots of Guava-Probably built from the `master` branch are available through Maven
using version `1.0-SNAPSHOT`. API documentation is available here:

- Snapshot API Docs: [guava-probably][guava-probably-snapshot-api-docs]

Learn about data structures provided by Guava-Probably
------------------------------------------------------

- [Cuckoo Filter: Practically Better Than Bloom](https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf)
- [Bloom Filters by Example](http://billmill.org/bloomfilter-tutorial/)
- [Google Guava BloomFilter](https://github.com/bdupras/guava-probably/wiki/HashingExplained#bloomfilter)

Links
-----

- [GitHub project](https://github.com/bdupras/guava-probably)
- [Issue tracker: report a defect or feature request](https://github.com/bdupras/guava-probably/issues/new)

