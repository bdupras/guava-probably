Guava-Probably: TODO List 
=======================================================

=1.0
* Double-check use of generics in interface definitions. It should be possible to add() a subclass 
  instance to a filter defined for a super-class of the instance.

==CI
* commit/push to release SNAPSHOT, major, minor, patch :: maven central && javadocs

== Features
* MultiSet interface operations (count, set counts)
* CuckooFilter impl increase max capacity (separate even/odd tables? array of tables?)
* Primitive interface API (to avoid object alloc)
* Direct hash fn invocation (to avoid object alloc)
* extract filter dimensions calculation
