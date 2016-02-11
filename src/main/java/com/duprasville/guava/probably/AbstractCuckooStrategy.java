/*
 * Copyright (C) 2015 Brian Dupras
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.duprasville.guava.probably;

abstract class AbstractCuckooStrategy implements CuckooStrategy {
  AbstractCuckooStrategy(int ordinal) {
    this.ordinal = ordinal;
  }

  public abstract long index(int hash, long m);

  public abstract long altIndex(long index, int fingerprint, long m);

  protected abstract int pickEntryToKick(int numEntriesPerBucket);

  protected abstract long maxRelocationAttempts();

  private final int ordinal;

  public int ordinal() {
    return ordinal;
  }

  public boolean addAll(CuckooTable thiz, CuckooTable that) {
    for (long index = 0; index < that.numBuckets; index++) {
      for (int entry = 0; entry < that.numEntriesPerBucket; entry++) {
        int fingerprint = that.readEntry(index, entry);
        if (CuckooTable.EMPTY_ENTRY != fingerprint && !(
            putEntry(fingerprint, thiz, index) ||
                putEntry(fingerprint, thiz,
                    altIndex(index, fingerprint, thiz.numBuckets)))) {
          return false;
        }
      }
    }
    return true;
  }

  protected boolean putEntry(int fingerprint, CuckooTable table, long index) {
    return table.swapAnyEntry(fingerprint, CuckooTable.EMPTY_ENTRY, index)
        || putEntry(fingerprint, table, index, 0);
  }


  protected boolean putEntry(int fingerprint, final CuckooTable table, long index, int kick) {
    if (maxRelocationAttempts() == kick) {
      return false;
    }

    int entry = pickEntryToKick(table.numEntriesPerBucket);
    int kicked = table.swapEntry(fingerprint, index, entry);

    if ((CuckooTable.EMPTY_ENTRY == kicked)
        || putEntry(kicked, table, altIndex(index, kicked, table.numBuckets), kick + 1)) {
      return true;
    } else {
      int kickedBack = table.swapEntry(kicked, index, entry);
      assert kickedBack == fingerprint : "Uh oh - couldn't unroll failed attempts to putEntry()";
      return false;
    }
  }

  public boolean equivalent(CuckooTable thiz, CuckooTable that) {
    if (!thiz.isCompatible(that)) {
      return false;
    }

    for (long index = 0; index < that.numBuckets; index++) {
      for (int entry = 0; entry < that.numEntriesPerBucket; entry++) {
        int fingerprint = that.readEntry(index, entry);
        if (CuckooTable.EMPTY_ENTRY == fingerprint) {
          continue;
        }

        int thizCount = thiz.countEntry(fingerprint, index) +
            thiz.countEntry(fingerprint, altIndex(index, fingerprint, thiz.numBuckets));
        int thatCount = that.countEntry(fingerprint, index) +
            that.countEntry(fingerprint, altIndex(index, fingerprint, that.numBuckets));
        if (thizCount != thatCount) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean containsAll(CuckooTable thiz, CuckooTable that) {
    if (!thiz.isCompatible(that)) {
      return false;
    }

    for (long index = 0; index < that.numBuckets; index++) {
      for (int entry = 0; entry < that.numEntriesPerBucket; entry++) {
        int fingerprint = that.readEntry(index, entry);
        if (CuckooTable.EMPTY_ENTRY == fingerprint) {
          continue;
        }

        int thizCount = thiz.countEntry(fingerprint, index) +
            thiz.countEntry(fingerprint, altIndex(index, fingerprint, thiz.numBuckets));
        int thatCount = that.countEntry(fingerprint, index) +
            that.countEntry(fingerprint, altIndex(index, fingerprint, that.numBuckets));
        if (thizCount < thatCount) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean removeAll(CuckooTable thiz, CuckooTable that) {
    if (!thiz.isCompatible(that)) {
      return false;
    }

    for (long index = 0; index < that.numBuckets; index++) {
      for (int entry = 0; entry < that.numEntriesPerBucket; entry++) {
        int fingerprint = that.readEntry(index, entry);
        if (CuckooTable.EMPTY_ENTRY == fingerprint) {
          continue;
        }

        long altIndex = altIndex(index, fingerprint, thiz.numBuckets);
        int thatCount = that.countEntry(fingerprint, index) + that.countEntry(fingerprint, altIndex);

        for (int i = 0; i < thatCount; i++) {
          if (!(thiz.swapAnyEntry(CuckooTable.EMPTY_ENTRY, fingerprint, index)
              || thiz.swapAnyEntry(CuckooTable.EMPTY_ENTRY, fingerprint, altIndex))) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CuckooStrategy) {
      return ((CuckooStrategy) obj).ordinal() == this.ordinal();
    } else {
      return super.equals(obj);
    }
  }

  @Override
  public int hashCode() {
    return this.ordinal();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + '{' +
        "ordinal=" + this.ordinal() +
        '}';
  }
}
