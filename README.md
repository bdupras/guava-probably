Guava-Probably: Probabilistic data structures for Guava 
=======================================================

[![Build Status](https://travis-ci.org/bdupras/guava-probably.svg?branch=master)](https://travis-ci.org/bdupras/guava-probably)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.duprasville.guava.guava-probably/guava-probably/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.duprasville.guava.guava-probably/guava-probably/)
[![Stories in Ready](https://badge.waffle.io/bdupras/guava-probably.svg?label=ready&title=Ready)](http://waffle.io/bdupras/guava-probably)
[![Join the chat at https://gitter.im/bdupras/guava-probably](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/bdupras/guava-probably)

The Guava-Probably project provides several probabilistic data structures for Guava.

Requires JDK 1.6 or higher and Google Guava 19.0 or higher (as of 1.0).


Latest release
--------------
- `1.0-SNAPSHOT`: [API Docs][guava-probably-snapshot-api-docs], 30 December 2015.

To add a dependency on Guava-Probably using Maven, use the following:

```xml
<dependency>
  <groupId>com.duprasville.guava</groupId>
  <artifactId>guava-probably</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'com.duprasville.guava:guava-probably:1.0-SNAPSHOT'
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
- [Google Guava BloomFilter](https://github.com/google/guava/wiki/HashingExplained#bloomfilter)

Links
-----

- [GitHub project](https://github.com/bdupras/guava-probably)
- [Issue tracker: report a defect or feature request](https://github.com/bdupras/guava-probably/issues/new)

[guava-probably-release-api-docs]: http://bdupras.github.io/guava-probably/releases/1.0/api/docs/
[guava-probably-snapshot-api-docs]: http://bdupras.github.io/guava-probably/releases/snapshot/api/docs/
