import java.util.*;

public class Cache {
    /**
     * The constructor: allocates a cacheBlocks number of cache blocks, 
     * each containing blockSize-byte data, on memory 
     * @param blockSize amt of data in each block
     * @param cacheBlocks amt of cache blocks
     */
    public Cache(int blockSize, int cacheBlocks) {
        //init the pageTable with entries
        pageTable = new Entry[cacheBlocks];
        for (int i = 0; i < cacheBlocks; i++) {
            pageTable[i] =  new Entry(i, blockSize);
        }
    }


    //Entry = cache block
    private class Entry {
        byte[] buffer; //needed?
        byte dirtyBit = 0;
        //This is the reference bit for the second chance algorithm
        //Initialized to 1, set to 0 when searching for a replacement bit
        //if 0, will be replaced if searching 
        byte secondChance = 1;
        //int refBit = 0;
        /**
         * Contains the disk block number of cached data
         * if this entry does not have valid block information, the disk block number
         * should be set to -1
         */
        int blockFrameNumber = -1;
        int blockSize;
        int id;
        boolean valid = true; //if valid is false, replace
        Entry(int id, int blockSize) {
            this.id = id;
            this.blockSize = blockSize;
        }
    }

    private Entry[] pageTable = null;

    /**
     * Search for an used block where the blockFrameNumber is -1, or if none are -1
     * find the next victim and return it's id
     * @return id of freeBlock
     */
    private int findFreePage() {
        //search for an unused block sequentially
        int replaceMe = -1;
        int length = pageTable.length;
        for (int i = 0; i < length; i++) {
            if (!pageTable[i].valid) {
                replaceMe = i; //was not valid, so open for replacement
                break;
            }
            if (pageTable[i].blockFrameNumber == -1) {
                replaceMe = i;
                break;
            }
        }
        if (replaceMe != -1) {
            return replaceMe;
        }
        else {
            //try to find a 0 bit entry to return
            int j = 0;
            while (replaceMe == -1) {
                //while loop since worst case scenario is that all entries are SCB = 1 at the start
                if (pageTable[j].secondChance == 0) {
                    replaceMe = j;
                    break;
                }
                else {
                    //set the bit to 0
                    pageTable[j].secondChance = 0;
                }
                j++;
                if (j == length-1) {
                    j = 0;
                }
            }
            return replaceMe;
        }
    }

    
    /**
     * reads into the buffer[ ] parameter the contents of the cache block specified by
     * blockId from the disk cache if it is in cache (scan the page table to see if it
     * is in memory), otherwise reads the corresponding disk block from the ThreadOS disk 
     * device. Upon an error*, it should return false, otherwise return true.  
     * @param blockId block to be red
     * @param buffer where to store read contents
     * @return false on error
     */
    public synchronized boolean read(int blockId, byte buffer[]) {
        //cache.read( param, ( byte[] )args )
        int cacheID = isInCache(blockId);
        if (cacheID == -1) {
            int read = SysLib.rawread(blockId, buffer);
            if (read != 1) { // i think this is right
                return false;
            }
        }
        else {
            //in cache
            buffer = pageTable[cacheID].buffer;
            pageTable[cacheID].secondChance = 1; //used, so reset the secondchance bit
        }
        return true;
    }

    
    /**
     * writes the contents of buffer[ ]array to the cache block specified by blockId from 
     * the disk cache if it is in cache, otherwise finds a free cache block and writes the
     * buffer [ ] contents to it. No write through (doesn't touch the disk). Upon an error*, 
     * it should return false, otherwise return true.   
     * In here, when a block is not in cache and there are no free blocks, you'll have 
     * to find a victim for replacement. 
     * 
     * The ThreadOS disk contains 1000 blocks.  You should only return false in the case of 
     * an out of bounds blockId.
     * 
     * 
     * @param blockId cache block to write to (not disk)
     * @param buffer contents to be read
     * @return false if error
     */
    public synchronized boolean write(int blockId, byte buffer[]) {
        if (blockId > 1000) { //hardcoded since it says the max is 1000 in the description
            return false;
        }
        int cacheID = isInCache(blockId);
        if (cacheID == -1) {
            //blockID not associated w/ cache, so get a free cache block
            cacheID = findFreePage();
            if (pageTable[cacheID].dirtyBit == 1 && pageTable[cacheID].valid) {
                //need to write the data back to the disk
                int wrote = SysLib.rawwrite(cacheID, buffer);
                if (wrote != 1) {
                    return false; //had an error
                }
            }
        }
        //now you have a free cache block to write to
        //make sure to reset the info bits
        resetCacheBlock(cacheID);
        pageTable[cacheID].blockFrameNumber = blockId;
        //now write the buffer to that cache block
        pageTable[cacheID].buffer = buffer;
        pageTable[cacheID].dirtyBit = 1; //set dirty bit since it was wrote to
        return true;
    }
    
    private void resetCacheBlock(int cacheID) {
        pageTable[cacheID].dirtyBit = 0;
        pageTable[cacheID].secondChance = 1;
        pageTable[cacheID].valid = true;
        pageTable[cacheID].buffer = null;
        pageTable[cacheID].blockFrameNumber = -1;
    }
    
    /**
     * Checks if data associated with the blockID passed is in cache 
     * @param blockId
     * @return -1 if couldn't find, cache block id if found
     */
    private int isInCache(int blockId) {
        for (int i = 0; i <pageTable.length; i++) {
            if (pageTable[i].blockFrameNumber == blockId && pageTable[i].valid) {
                return i; //found it
            }
        }
        return -1; //didn't find it
    }

    /*writes back all dirty blocks to Disk.java and thereafter forces Diskjava to write back
    all contents to the DISK file. 
    */
    
    
    /**
     * The sync( ) method still maintains clean block copies in Cache.java
     * must be called when shutting down ThreadOS
     * 
     *  Writes back all dirty blocks to Disk.java  
     *  Forces Disk.java to write back all contents to the DISK file.
     */
    public synchronized void sync() {
        //write back any cache blocks w/ dirty bit set to disk
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].dirtyBit == 1) {
                int wrote = SysLib.rawwrite(pageTable[i].blockFrameNumber, pageTable[i].buffer);
                if (wrote == 1) { //i think supposed to be 1?
                    pageTable[i].dirtyBit = 0;
                }
            }
        }
    }

    /**
     * the flush( ) method invalidates all cached blocks.
     * should be called when you keep running a different test case without receiving any
     * caching effects incurred by the previous test.
     * 
     * Writes back all dirty blocks to Disk.java 
     * Forces Disk.java to write back all contents to the DISK file.  
     * Wipes all cached blocks.  
     */
    public synchronized void flush() {
        //write back all dirty bits to disk, and then invalidate all blocks
        for (int i = 0; i < pageTable.length; i++) {
            if (pageTable[i].dirtyBit == 1 && pageTable[i].valid) {
                int wrote = SysLib.rawwrite(pageTable[i].blockFrameNumber, pageTable[i].buffer);
                if (wrote == 1) { //i think supposed to be 1?
                    pageTable[i].dirtyBit = 0;
                }
            }
            resetCacheBlock(i);
            pageTable[i].valid = false;
        }
    }
}
