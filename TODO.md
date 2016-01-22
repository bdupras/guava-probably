Guava-Probably: TODO List 
=======================================================

=1.0
* removeAll(Collection)
* removeAll(Filter)
* @throws UnsupportedOperationException - true up to java.util.Set
* Full interface tests on CuckooFilter and BloomFilter
* ?? check out MultiSet interface for semantics


=Beyond 1.0

==CI
* commit/push to release SNAPSHOT, major, minor, patch :: maven central && javadocs

== Features
* CuckooFilter impl increase max capacity (separate even/odd tables?)
* Primitive interface API (to avoid object alloc)
* Direct hash fn invocation (to avoid object alloc)
* extract filter dimensions calculation
