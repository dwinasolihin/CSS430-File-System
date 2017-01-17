/*
The inode blocks start after the superblock. Each inode describes one file. 
The inode in this project is a simplified version of the Linux inode.  
The inode includes 12 pointers of the index block. 
The first 11 of these pointers point to direct blocks.
The last pointer points to an indirect block. In addition, each inode must include  
(1) the length of the corresponding file 
(2) the number of file (structure) table entries that point to this inode 
(3) the flag to indicate if it is unused (= 0), used(= 1), or in some other status
    (= 2, 3, 4, ..., i.e., what/how the file is currently being used for).  
Note that 16 inodes can be stored in one block.  
One inode represents an individual file
There is a block with inode data that points to the blocks with raw data
*/
public class Inode {
   private final static int iNodeSize = 32;       // fix to 32 bytes
   private final static int directSize = 11;      // # direct pointers

   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers,11
   public short indirect;                         // a indirect pointer
   
   /*
   You will need a constructor that retrieves an existing inode from the disk into the memory.
   */
   Inode( ) {                                     // a default constructor
      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < directSize; i++ ) {
          if (i == 0) {
              //give the inode a block for its first direct
              direct[i] = blockCounter.getInstance().getFreeBlock();
          }
         direct[i] = -1;
      }
      indirect = -1;
   }
   
   /*
   Given an inode number, termed inumber, this constructor reads the corresponding disk block,
   locates the corresponding inode information in that block, and initializes a new inode with
   this information. 
   
   iNumber represents the iNode number
   */
   Inode( short iNumber ) {                       // retrieving inode from disk

    //given from CSS430FinalProject.pdf
    int blockNumber = 1 + iNumber / 16; //block 0 is reserved for superblock
    
    byte[] block = new byte[Disk.blockSize]; //store the info in here
    if (SysLib.rawread(blockNumber, block) != 0) {
        //error
        System.out.println("Could not read block number " + blockNumber + " when creating INode");
        SysLib.exit();
    }
    
    //then locate inode info in this block
    //the offset is used to find the byte locations. 32 bytes/inode
    //additionally, subtract 1 b/c of the 0-based indexing? <-- nope, you want to start on 32 etc
    //int offset = (iNumber * iNodeSize);
    int offset = ((iNumber % 16) * iNodeSize);
    
    //iNode info will be at offset
    //int length, short count, short flag, short[11] direct, short indirect
    this.length = SysLib.bytes2int(block, offset);
    if (this.length < 0) {
        //some kind of error
        this.length = 0;
    }
    offset += 4;
    this.count = SysLib.bytes2short(block, offset);
    /*
    if (this.count < 0) {
        //some kind of error
        this.count = 0;
    }
    */
    offset += 2;
    
    this.flag = SysLib.bytes2short(block, offset);
    /*
    if (this.count < 0) {
        //some kind of error
        this.count = 0;
    }
    */
    offset += 2;
    
    //take care of direct links
    for (int i = 0; i < directSize; i++) {
        this.direct[i] = SysLib.bytes2short(block, offset);
        offset += 2;
    }
    //indirect link
    this.indirect = SysLib.bytes2short(block, offset);
    //offset += 2;  <-- not needed since it's the end of the function
   }

   /*
    Before an inode in memory is updated, check the corresponding inode on disk, read it from
   the disk if the disk has been updated by another thread. Then, write back its contents to 
   disk immediately. Note that the inode data to be written back include int length, short count,
   short flag, short direct[11], and short indirect, thus requiring a space of 32 bytes in total.
   For this write-back operation, you will need the toDisk method that saves this inode 
   information to the iNumber-th inode in the disk, where iNumber is given as an argument.  
   */
   void toDisk( short iNumber ) {                  
      // save to disk as the i-th inode
      // design it by yourself.
      //this method just has to write inode data back to disk I'm pretty sure
      
      int blockNumber = 1 + iNumber/16; //block 0 is reseved for superblock
      
      byte[] block = new byte[Disk.blockSize];
      
      if (SysLib.rawread(blockNumber, block) != 0) {
        //error
        System.out.println("Could not read block number " + blockNumber + " when writing INode to disk");
        SysLib.exit();
       }
      
      int offset = 0;
      //int offset = ((iNumber-(16*(blockNumber-1))) * 32);
      //Use the offset values to read the bytes 
      //int length, short count, short flag, short[11] direct, short indirect
      
      SysLib.int2bytes(length, block, offset);
      offset += 4;
      
      SysLib.int2bytes(count, block, offset);
      offset += 2;
      
      SysLib.int2bytes(flag, block, offset);
      offset+=2;
      
      for (int i = 0; i < directSize; i++) {
          SysLib.int2bytes(direct[i], block, offset);
          offset += 2;
      }
      SysLib.int2bytes(indirect, block, offset);
      offset += 2;
      //Have now written all new values to block
      
      /*  Why create a new block? just overwrite the old one
      //--------------------------------- added by Dwina -----------------------
      byte[] newBlock = new byte[512];
      SysLib.rawread(blockNumber, newBlock);
      
      offset = (iNumber % 16) * 32;
      
      System.arraycopy(block, 0, newBlock, offset, 32);
      
      SysLib.rawwrite(blockNumber, newBlock);
      //------------------------------------------------------------------------
      */
      //write block back to disk w/ new iNode values 
      if (SysLib.rawwrite(blockNumber, block) != 0) {
            //error
            System.out.println("Could not write block number " + blockNumber + " when writing INode to disk");
            SysLib.exit();       
        }
   }
}