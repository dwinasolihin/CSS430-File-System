/**
 * Information about super block from Assignment on OneNote:
 * The Superblock is a block managed by the OS. No other information must be 
 * recorded in the superblock and no user threads can be able to access the 
 * superblock. 
 * The purpose of this Superblock class is that it is able to read phys
 * 
 * Created by Dwina Solihin - December 2, 2016
 */

class Superblock {
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head
    
    private final int defaultBlocks = 1000;
    
//------------------------------------------------------------------------------
/**
 * Below is the constructor for the Superblock.java that passes through the
 * diskSize which is the total number of blocks on the Disk. The constructor will
 * read the data of the Superblock from the disk and initialize the variables
 * for the total number of block, total number of inodes, and the block number
 * of the free list's head.
 */
    public Superblock(int diskSize) {
        byte[] superBlockData = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlockData);
       
        //total block location
        totalBlocks = SysLib.bytes2int(superBlockData, 0);
       
        //total inode location
        totalInodes = SysLib.bytes2int(superBlockData, 4);
       
        //block number of the free list's head
        freeList = SysLib.bytes2int(superBlockData, 8);
   
        //checks the disk contents if they are valid
        if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2){
            return; //valid disk state
        } else { //does this if the disk state is invalid and format is required
            totalBlocks = diskSize;
            format(64); //set 64 to a private, final static int on top
        }
    }
//------------------------------------------------------------------------------
/**
 * Below is a method that basically cleans the disk of all its data and resets
 * itself if the superblock detects illegal states during initialization of an
 * instance. All the instance variables of the Superblock are cleared to default
 * values and then written back to newly cleared disk.
 *
 */
    public void format(int inodeNum){
        byte[] dummyBlock = null;
        
        totalInodes = inodeNum;
        
        //dummy Inode for the object creation below
        Inode newInode = null;
        
        //creates and writes inodes to the disk
        for (int i = 0; i < totalInodes; i++){
            newInode = new Inode();
            newInode.toDisk((short)i);
        }
        
        //sets the free list depending on the first free block. In this example,
        //it should be pointing to 4
        freeList = (inodeNum / 16) + 2;
        
        //create new default number of free blocks and writes them into disk
        for (int i = freeList; i < defaultBlocks - 1; i++){
            dummyBlock = new byte[Disk.blockSize];
            
            //this is used to erase the current block
            for (int j = 0; j < Disk.blockSize; j++){
                dummyBlock[j] = 0;
            }
            //writes the next sequential free block in pointer to next free block
            SysLib.int2bytes(i + 1, dummyBlock, 0);
            
            //writes the block to disk
            SysLib.rawwrite(i, dummyBlock);
        }
        
        //writes final block
        dummyBlock = new byte[Disk.blockSize];
        
        //erases block
        for (int j = 0; j < Disk.blockSize; j++){
            dummyBlock[j] = 0;
        }
        
        //writes the next sequential free block in pointer to next free block
        SysLib.int2bytes(-1, dummyBlock, 0);
        
        //write block to disk
        SysLib.rawwrite(defaultBlocks - 1, dummyBlock);
        
        //used to create and write new Superblock to disk
        sync();
    }
//------------------------------------------------------------------------------
/**
 * Below is a sync() method that is used to bring the physical Superblock()
 * contents at block 0 on the disk together and synced with any updates that the
 * Superblock class instance. The sync() method writes back to the disk with
 * the total number of blocks, total number of Inodes, and the freeList.
 *
 */
    public void sync(){
        //creates new block to hold superblock data
        byte[] newSuperblock = new byte[Disk.blockSize];
        
        //copies total number of blocks to newSuperBlock
        SysLib.int2bytes(totalBlocks, newSuperblock, 0);
        
        //copies total number of inodes to newSuperBlock
        SysLib.int2bytes(totalInodes, newSuperblock, 4);
        
        //copies freeList to newSuperBlock
        SysLib.int2bytes(freeList, newSuperblock, 8);
        
        //writes the newly copied superBlock to disk
        SysLib.rawwrite(0, newSuperblock);
    }
//------------------------------------------------------------------------------
/**
 * Below is the getFreeBlock() method that returns the int of the first free
 * block in the freeList. The free block is the top block from the list. If
 * there is an error, then -1 is returned to show that the method failed.
 * Errors will occur from the absence of free blocks.
 */
    
    public int getFreeBlock(){
        if (freeList > 0 && freeList <= totalBlocks){
            //creates a temp block to hold first free block
            byte[] tempFreeBlock = new byte[Disk.blockSize];
            
            //reads free block form disk
            SysLib.rawread(freeList, tempFreeBlock);
            
            //hold the free block location in a temp variable
            int temp = freeList;
            
            //updates the next free block
            freeList = SysLib.bytes2int(tempFreeBlock, 0);
            
            //returns the free block location
            return temp;
        }
        
        //invalid state, retursn -1 to indicate failure
        return -1;
        
    }
//------------------------------------------------------------------------------
/**
 * Below is the returnBlock() method that is used to add a newly freed block
 * back to the free list. This newly freed block is added to the end of the list
 * (FIFO). IF the freed block does not fit to the disk parameters held in
 * Superblock, then the operation fails and returns false. 
 *
 */
    public boolean returnBlock(int blockNumber){
        
        //used to check if the blockNumber that is passed through is within range
        if (blockNumber > 0 && blockNumber <= totalBlocks){
            
            int temp = 0; //second walking pointer
            int nextFreeBlock = freeList; //next free block
            
            //temp byte[] array to hold the working block
            byte[] tempWorkingBlock = new byte[Disk.blockSize];
            
            //new block byte[] array returned to freeList
            byte[] newBlock = new byte[Disk.blockSize];
            
            
            //erases block
            for (int i = 0; i < Disk.blockSize; i++){
                tempWorkingBlock[i] = 0;
            }
            
            //sets the next block ni the newBlock byte[] arry to -1
            SysLib.int2bytes(-1, newBlock, 0);
            
            //goes through while the end of the free list has not been found.
            //This while loop will keep looking for the end of the free list
            while (nextFreeBlock != -1){
                //gets the next free block
                SysLib.rawread(nextFreeBlock, tempWorkingBlock);
                
                
                //checks the byte id of the following free block
                temp = SysLib.bytes2int(tempWorkingBlock, 0);
                
                //checks if the free block is -1, if it is -1 then it is at the
                //end of the queue
                if (temp == -1){
                    
                    //sets the next free block to the blockNumber that was passed
                    //through and write it to the disk
                    SysLib.int2bytes(blockNumber, tempWorkingBlock, 0);
                    SysLib.rawwrite(nextFreeBlock, tempWorkingBlock);
                    SysLib.rawwrite(blockNumber, newBlock);
                    
                    //method is complete
                    return true;
                    
                }
                
                //has not reached the end, keeps going through
                nextFreeBlock = temp;
            }
        }
        
        //invalid block was returned therefore returns false and does nothing
        return false;
    }
}
