# Guava-Probably: TODO List 

## CI
* commit/push to release SNAPSHOT, major, minor, patch :: maven central && javadocs
* simplify travis scripts

## Features
* MultiSet interface operations (count, set counts)
* CuckooFilter impl increase max capacity (separate even/odd tables? array of tables?)
* Primitive interface API (to avoid object alloc)
* Direct hash fn invocation (to avoid object alloc)
* extract filter dimensions calculation
* NOTE: knowing if an insertion modified a bloom filter is useful
** e.g. loop detection in routing algos
** question: what should the semantic be for returning inserted/not-inserted vs changed/not-changed?
* make deletability optional? when off, colliding insertions do not mutate the filter
