Guava-Probably: TODO List 
=======================================================

=1.0
* @throws UnsupportedOperationException - true up to java.util.Set
* Full interface tests on CuckooFilter and BloomFilter
* double-check CuckooFilter.MIN_FPP value - calculation seems wrong

==CI
* commit/push to release SNAPSHOT, major, minor, patch :: maven central && javadocs

== Features
* MultiSet interface operations (count, set counts)
* CuckooFilter impl increase max capacity (separate even/odd tables?)
* Primitive interface API (to avoid object alloc)
* Direct hash fn invocation (to avoid object alloc)
* extract filter dimensions calculation
