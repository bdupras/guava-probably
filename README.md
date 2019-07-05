Guava-Probably: Probabilistic Filters 
=====================================
The Guava-Probably project provides two probabilistic filters for Guava.

[![Build Status](https://travis-ci.org/bdupras/guava-probably.svg?branch=master)](https://travis-ci.org/bdupras/guava-probably)

# What is it?
A probabilistic filter is a space-efficient data structure for representing a set in order to support membership queries. [ref][BroderMitzenmacher]

# How does it work?
Check out this sweet, interactive demo: [Probabilistic Filters By Example](https://bdupras.github.io/filter-tutorial/)

# What's it good for?
Probabilistic filters are great for reducing unnecessary disk, database or network queries. Applications where the universe of possible members in a set is much larger than actual members may benefit from probabilistic filters, especially when most membership queries are expected to return false.

# No really, what's it good for?
- Google Chrome uses p-filters to make a preliminary decision whether a particular web site is malicious or safe. [ref][Yakunin]
- Exim mail transfer agent uses p-filters in its rate-limiting logic. [ref][Finch]
- Use a p-filter to reject malicious authentication attempts, protecting your cache and database from botnet queries.

# Cool, how do I get it?
Requires JDK 8 or higher and Google Guava 19.0 or higher (as of 1.0).
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

# How do I learn more?
- [Probabilistic Filters By Example](https://bdupras.github.io/filter-tutorial/)
- [Cuckoo Filter: Practically Better Than Bloom](https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf)
- [Bloom Filters by Example](http://billmill.org/bloomfilter-tutorial/)
- [Google Guava BloomFilter](https://github.com/google/guava/wiki/HashingExplained#bloomfilter)
- [Network Applications of Bloom Filters: A Survey](http://projecteuclid.org/DPubS?service=UI&version=1.0&verb=Display&handle=euclid.im/1109191032)
- [Nice Bloom filter application](http://blog.alexyakunin.com/2010/03/nice-bloom-filter-application.html)
- [What use are Bloom filters, anyway?](http://fanf.livejournal.com/82764.html)

# Links
- [GitHub project](https://github.com/bdupras/guava-probably)
- [Issue tracker: report a defect or feature request](https://github.com/bdupras/guava-probably/issues/new)

[BroderMitzenmacher]: http://projecteuclid.org/DPubS?service=UI&version=1.0&verb=Display&handle=euclid.im/1109191032  "Network Applications of Bloom Filters: A Survey; Andrei Broder and Michael Mitzenmacher"
[Yakunin]: http://blog.alexyakunin.com/2010/03/nice-bloom-filter-application.html "Nice Bloom filter application"
[Finch]: http://fanf.livejournal.com/82764.html "What use are Bloom filters, anyway?" 
[guava-probably-release-api-docs]: http://bdupras.github.io/guava-probably/releases/1.0/api/docs/
[guava-probably-snapshot-api-docs]: http://bdupras.github.io/guava-probably/releases/snapshot/api/docs/
