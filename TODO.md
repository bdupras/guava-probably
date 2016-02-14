Guava-Probably: TODO List 
=======================================================

=1.0
* double-check CuckooFilter.MIN_FPP value - calculation seems wrong
* bump to most recent version of Guava and true-up Bloom filter API for longs?

==CI
* commit/push to release SNAPSHOT, major, minor, patch :: maven central && javadocs

== Features
* MultiSet interface operations (count, set counts)
* CuckooFilter impl increase max capacity (separate even/odd tables? array of tables?)
* Primitive interface API (to avoid object alloc)
* Direct hash fn invocation (to avoid object alloc)
* extract filter dimensions calculation
