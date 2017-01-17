
import java.util.Arrays;

/**
 * Below is an implementation for a File System designed to run in ThreadOS.
 * 
 * @author Steffan Achtmann
 * @author Dwina Solihin
 */
public class FileSystem {
    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;
    public blockCounter counter;

    
    /**
     * Below is the File System constructor that is given. 
     * @param diskBlocks 
     */
    public FileSystem (int diskBlocks) {
        //Init block counter stuff for Inode
        counter = blockCounter.getInstance(diskBlocks);
        //create superblock and format disk with 64 inodes in default
        superblock = new Superblock(diskBlocks);
        
        //create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);
        
        //file table is created, and store directory in the file table
        filetable = new FileTable(directory);
        
        //directory reconstruction
        //reads the "/" file from disk
        FileTableEntry dirEnt = open( "/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            //the directory has some data
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    }
    
    
    /**
     * Below is the sync() method that is used to sync the file system to the 
     * physical disk. 
     * This method writes the directory information in the root directory to the
     * disk. This also makes sure that the superblock is in sync as well. 
     * 
     * @author Dwina Solihin
     * 
     */
    void sync() {
        //opens root directory with write access
        FileTableEntry openRoot = open("/", "w");
        
        //write directory to root
        write(openRoot, directory.directory2bytes());
        
        //close root directory
        close(openRoot);
        
        //sync superblock
        superblock.sync();
    }
    
    /**
     * Below is a format() method that is used to format the disk. How this 
     * method works is that it erases all the contents of the disk and recreates
     * the superblock, directory and file tables. This method passes through the 
     * number of files (AKA inodes) that will be created by the superblock. 
     * @param files how many files (inodes) the superblock will create
     * @return if the disk has completed formating
     * 
     * @author Dwina Solihin
     * 
     */
    boolean format(int files) {
        //uses the format method in superblock and passes through the number of 
        //files from parameter
        superblock.format(files);
        
        //creates a new directory which will set "/" in directory as entry 0
        directory = new Directory(superblock.totalInodes);
        
        //creates a new file table and puts the directory in the file table
        filetable = new FileTable(directory);
        
        //returns true once done
        return true; 
    }
    
    /**
     * Below is the open() method used to indicate which file needs to be opened 
     * based on the filename being passed through and which mode the filename 
     * object will have once it is created. 
     * First this function will create a new entry in the FileTableEntry using 
     * the falloc() method and passing through filename and mode parameters. 
     * When that object is created, it checks if the mode is "w" (write). If it 
     * is write, then it will delete all the blocks and start writing a new block. 
     * After the mode is checked, it returns the new FileTableEntry object created
     * at the beginning. 
     * Follows the format in the CSS430FinalProject.pdf file in the notes section 
     * of the assignment description. 
     * @param filename the name of the new file
     * @param mode what mode the new file will be in
     * @return the new FileTable entry
     * 
     * @author Dwina Solihin
     * 
     */
    FileTableEntry open(String filename, String mode) {
        //creates a new FileTableEntry object that holds the file name and mode 
        //parameters, allocates the new file being passed throughin
        FileTableEntry newTableEntry = filetable.falloc(filename, mode);
        
        if (mode == "w"){ //checks if it is writing mode
            //deletes all blocks first if writing mode
            if (deallocAllBlocks(newTableEntry) == false){
                return null;
            }
            
            //deletes all blocks first if writing mode
            //deallocAllBlocks(newTableEntry); 
        }
        return newTableEntry; //returns new filetableentry
    }
    
    /**
     * Below is the close() method used to close the corresponding file entry 
     * based on the FileTableEntry that is being passed through. 
     * @param ftEnt the file that needs to be closed
     * @return false upon an error
     * 
     * @author Steffan Achtmann & Dwina Solihin
     * 
     */
    boolean close(FileTableEntry ftEnt) {
        synchronized (ftEnt){
        //when closing a file, we need to.. close the iNode? <-- nope, since we're not closing the file itself, just the FTE
        ftEnt.inode.count--; //when closing FTE, reduce it's inode count
        if (ftEnt.count == 0){
            return filetable.ffree(ftEnt);
        }
        //if ftEnt.count isn't 0, then it's in use still... then what?
        return true;
        }
    }
    
    /**
     * Below is the fsize() method that is used to return the size of a file in 
     * bytes based on the entry that is passed through. 
     * @param ftEnt File used to determine file size
     * @return the length of Inode object
     * 
     * @author Dwina Solihin
     */
    int fsize(FileTableEntry ftEnt) {
        synchronized(ftEnt){ //cast the parameter as synchronized
            Inode inode = ftEnt.inode; //new inode object to entries of Inode
            return inode.length; //returns length of Inode object
        }
    }
    
    /**
     * Below is the read() method that is used to read the block and calculate 
     * the buffer based on data size. 
     * @param ftEnt
     * @param buffer read block into the buffer
     * @return -1 on error
     * 
     * @author Steffan Achtmann (modified by Dwina Solihin)
     * 
     */
    int read(FileTableEntry ftEnt, byte[] buffer) {
        //checks if mode is not read
        if ((ftEnt.mode.equals("w")) || (ftEnt.mode.equals("a")) || ftEnt.mode.equals("w+")){
            return -1;
        }
        
        int bytesRead = 0;
        int fileSize = ftEnt.inode.length;
        int curDirect = 0;
        int curIndirect = 0;
        int curBufferPtr = 0;
        boolean doneReading = false;
        
        synchronized(ftEnt){
        while (!doneReading) {
            if (bytesRead >= buffer.length) {
                doneReading = true;
                break;
            }
            if (curDirect <= 11) {
                //read from direct pointers
                byte[] readBuf = new byte[Disk.blockSize];
                if (SysLib.rawread(ftEnt.inode.direct[curDirect], readBuf) != 0) {
                        SysLib.cerr("Could not read within the read function.");
                        return -1;
                }
                
                for (int i = 0; i < Disk.blockSize; i++) {
                    if (bytesRead >= buffer.length || bytesRead >= fileSize) {
                        doneReading = true;
                        break;
                    }
                    buffer[curBufferPtr] = readBuf[i];
                    curBufferPtr++;
                    bytesRead++;
                }
                curDirect++;
            }
            else {
                //read from indirect pointers
                byte[] readBuf = new byte[Disk.blockSize];
                //read the indirect block
                if (SysLib.rawread(ftEnt.inode.indirect, readBuf) != 0) {
                        SysLib.cerr("Could not read within the read function.");
                        return -1;
                }
                //grab the right block id
                short correctLocationToReadIndirectBlockLocation = SysLib.bytes2short(readBuf, curIndirect*2);
                //Now read from the block id
                if (SysLib.rawread(correctLocationToReadIndirectBlockLocation, readBuf) != 0) {
                        SysLib.cerr("Could not read within the read function.");
                        return -1;
                }
                
                for (int i = 0; i < Disk.blockSize; i++) {
                    if (bytesRead >= buffer.length || bytesRead >= fileSize) {
                        doneReading = true;
                        break;
                    }
                    buffer[curBufferPtr] = readBuf[i];
                    curBufferPtr++;
                    bytesRead++;
                }
                curIndirect++;
            }
        }
             return buffer.length;
        }
    }
    
    /**
     * Below is the write() method that is used to write the contents of buffer 
     * (byte[] array passed through) to the file at the indicated position of 
     * the seek pointer. 
     * @param ftEnt
     * @param buffer
     * @return -1 on error, size of file (in bytes) on success
     * 
     * @author Steffan Achtmann (modified by Dwina Solihin
     */
    int write(FileTableEntry ftEnt, byte[] buffer) {
        //Write by following the iNode links
        //seek ptr is the # of bytes into file - find that first before writing
        //the length bit in inode doesn't matter cuz seek ptr is all that matters.
        //11*512 is max 
        if ((!ftEnt.mode.equals("w")) && (!ftEnt.mode.equals("a")) && (!ftEnt.mode.equals("w+"))) {
            //not in write mode, reject
            return -1;
        }
        if (ftEnt.mode.equals("a")) {
            //it's in append mode, so ensure that seek pointer is equal to length
            ftEnt.seekPtr = ftEnt.inode.length;
        }
        if (ftEnt.seekPtr < (11*Disk.blockSize) ) {
            //seek ptr is in a direct block
            short rightDirect = (short) (ftEnt.seekPtr/Disk.blockSize);  //currently this points to the actual block
            
            //the byte within the block to start writing at
            int correctStartingByte = ftEnt.seekPtr-(Disk.blockSize*rightDirect);
            //Now write into the block
            int bytesWritten = 0;
            short curDirect = rightDirect;  //on first run, this points to the actual block
            short curIndirect = 0; //there's a max of 256 indirect pointers, this is sued to keep track of which you're on
            short blockLocationFromIndirect = 0;
           
            while(curIndirect < 256 && bytesWritten < buffer.length) { //keep writing to the next blocks while there's stuff to write & space
                byte[] readBuf = new byte[Disk.blockSize];
                //read from direct if still @ direct pointers
                if (curDirect <= 11) {
                    if (SysLib.rawread(ftEnt.inode.direct[curDirect], readBuf) != 0) {
                        SysLib.cerr("Could not read within the write function.");
                        return -1;
                    }
                }
                //else read from indirect
                else {
                    //get the indirect block location
                    //curIndirect tells you where within the indirect block to check
                    
                    if (SysLib.rawread(ftEnt.inode.indirect, readBuf) != 0) {
                        SysLib.cerr("Could not read indirect's block + " + ftEnt.inode.indirect + "within the write function.");
                        return -1;
                    }
                    
                    //Now you have the indirect block, but you need to look within it to find the actual block to write into
                    blockLocationFromIndirect = readBuf[(curIndirect*2)]; //double the curIndirect to get the start of it's short byte within the block
                    if (blockLocationFromIndirect == 0) {
                        //you need to assign a real block to the inode indirect's current redirection
                        short free = counter.getFreeBlock();
                        //Write the free block id over the previous 0 block
                        SysLib.short2bytes(free, readBuf, (curIndirect*2));
                        //make the free block no longer free
                        counter.flipBlockState(free);
                        //make the block location equal to whatever free block you found
                        //write back the change to the indirect block
                        if(SysLib.rawwrite(ftEnt.inode.indirect, readBuf) !=0) {
                            SysLib.cerr("Raw write failed when writing back to indirect after change");
                            SysLib.exit();
                        }
                        //make the block location equal to whatever free block you found
                        blockLocationFromIndirect = free;
                    }
                    //Now read the actual block with data into readBuf
                    if (SysLib.rawread(blockLocationFromIndirect, readBuf) != 0) {
                        SysLib.cerr("Could not read indirect block + " + blockLocationFromIndirect + "within the write function.");
                        SysLib.exit();
                    }
                }
                for (int i = correctStartingByte; i < Disk.blockSize; i++) {
                    if (bytesWritten >= buffer.length) {
                        //completed write, exit the for loop
                        break;
                    }
                    readBuf[i] = buffer[bytesWritten];
                    bytesWritten++;
                }
                
                //Now actually write the data back to the file
                
                
                if (curDirect < 11) {
                    //Write back to the curDirect
                    if (SysLib.rawwrite(ftEnt.inode.direct[curDirect], readBuf) != 0 ) {
                        SysLib.cerr("Hit an error trying to write back to direct block in write function");
                        SysLib.exit();
                    }
                    
                    curDirect++;
                    if (ftEnt.inode.direct[curDirect] == -1) {
                        //Allocate a new block to the inode
                        short free = counter.getFreeBlock();
                        ftEnt.inode.direct[curDirect] = free;
                        counter.flipBlockState(free);
                    }
                }
                else {
                    if (curDirect == 11) {
                        //Write back to the curDirect
                        if (SysLib.rawwrite(ftEnt.inode.direct[curDirect], readBuf) != 0 ) {
                            SysLib.cerr("Hit an error trying to write back to direct block in write function");
                            SysLib.exit();
                        }
                        curDirect++;
                        //Assign an indirect block for the inode if it doesn't exist
                        if (ftEnt.inode.indirect == -1) {
                            short free = counter.getFreeBlock();
                            ftEnt.inode.indirect = free;
                            counter.flipBlockState(free);
                        }
                    }
                    else {
                        //Write back to the current indirect
                        if (SysLib.rawwrite(blockLocationFromIndirect, readBuf) != 0 ) {
                            SysLib.cerr("Hit an error trying to write back to direct block in write function");
                            SysLib.exit();
                        }
                        curIndirect++;
                        //Don't worry about direct anymore, worry about indirects
                    }
                }
                //after the first block, correctStartingBlock doesn't matter, so set to 0 for the for loop
                correctStartingByte = 0;
            }
        }
        else {
            //it's in an indirect pointer
            short rightIndirect = (short) ((ftEnt.seekPtr / Disk.blockSize) - 11);
            short curIndirect = rightIndirect;
            int bytesWritten = 0;
            int correctStartingByte = ftEnt.seekPtr-(Disk.blockSize*rightIndirect);
            while(curIndirect < 256 && bytesWritten < buffer.length) { //keep writing to the next blocks while there's stuff to write & space
                byte[] readBuf = new byte[Disk.blockSize];
            
                if (SysLib.rawread(ftEnt.inode.indirect, readBuf) != 0) {
                    SysLib.cerr("Could not read indirect's block + " + ftEnt.inode.indirect + "within the write function.");
                    return -1;
                }

                //Now you have the indirect block, but you need to look within it to find the actual block to write into
                short blockLocationFromIndirect = readBuf[(curIndirect*2)]; //double the curIndirect to get the start of it's short byte within the block
                if (blockLocationFromIndirect == 0) {
                    //you need to assign a real block to the inode indirect's current redirection
                    short free = counter.getFreeBlock();
                    //Write the free block id over the previous 0 block
                    SysLib.short2bytes(free, readBuf, (curIndirect*2));
                    //make the free block no longer free
                    counter.flipBlockState(free);
                    //make the block location equal to whatever free block you found
                    //write back the change to the indirect block
                    if(SysLib.rawwrite(ftEnt.inode.indirect, readBuf) !=0) {
                        SysLib.cerr("Raw write failed when writing back to indirect after change");
                        SysLib.exit();
                    }
                    //make the block location equal to whatever free block you found
                    blockLocationFromIndirect = free;
                }
                //Now read the actual block with data into readBuf
                if (SysLib.rawread(blockLocationFromIndirect, readBuf) != 0) {
                    SysLib.cerr("Could not read indirect block + " + blockLocationFromIndirect + "within the write function.");
                    SysLib.exit();
                }
                
                
                for (int i = correctStartingByte; i < Disk.blockSize; i++) {
                    if (bytesWritten >= buffer.length) {
                        //completed write, exit the for loop
                        break;
                    }
                    readBuf[i] = buffer[bytesWritten];
                    bytesWritten++;
                }
                
                //Finally, write back to the block
                if (SysLib.rawwrite(blockLocationFromIndirect, readBuf) != 0 ) {
                    SysLib.cerr("Hit an error trying to write back to direct block in write function");
                    SysLib.exit();
                }
                curIndirect++;
                
                //after the first block, correctStartingBlock doesn't matter, so set to 0 for the for loop
                correctStartingByte = 0;
            }
        }
        ftEnt.inode.length = ftEnt.inode.length +  ftEnt.seekPtr - buffer.length;
        return ftEnt.inode.length;       
    }
    
    /**
     * This method will deallocate all blocks for the inode associated
     * with a FTE, except for the first direct block, which is wiped, but
     * otherwise kept allocated to the inode.
     * If not possible
     * @param ftEnt
     * @return false if not all inode blocks are valid
     * 
     * @author Steffan Achtmann (modified by Dwina Solihin)
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
      Inode node = ftEnt.inode;
      if (node.count != 0) {
          //don't deallocate since something is using
          SysLib.cerr("A process is using the file, cannot deallocate");
          return false;
      }
      blockCounter counter = blockCounter.getInstance();
      //Start with the direct blocks
      for (int i =1; i < 11; i++) {
          //-1 means it's already not in use/invalid
          if (node.direct[i] != -1) {
              int id = node.direct[i];
              node.direct[i] = -1;
              if (clearBlock(id) == false) {
                  return false; //hit an error
              } //clear out the block
              counter.flipBlockState(id); //make it free to pick up
          } 
      }
      
      int indirectid = node.indirect;
      if (indirectid != -1) {
          node.indirect = -1;
          //run through all the indirects pointers and then clear the indirect block
          byte[] block = new byte[Disk.blockSize];
          if (SysLib.rawread(indirectid, block) !=0 ) {
              SysLib.cerr("Could not read indirect id when deallocating blocks");
              SysLib.exit();
          }
          for (int i = 0; i < Disk.blockSize/2; i +=2) {
              short blockid = SysLib.bytes2short(block, i);
              if (blockid == 0 || blockid == -1) {
                  break; //finished clearing, no blocks will have these ids
              }
              if (clearBlock(blockid) == false) {
                  return false;
              }
              counter.flipBlockState(blockid);
          }
      }
      counter.flipBlockState(indirectid);
      
      //and finally, clear out the block for the first direct block, even though
      //it's still attatched to the inode
      if (clearBlock(node.direct[0]) == false) {
        return false; //hit an error
      }
      return true;
    }
    
    private boolean clearBlock(int blockid) {
        //set the block's contents to complete 0s
        byte[] block = new byte[Disk.blockSize];
        byte fillItem = 0;
        Arrays.fill(block, fillItem);
        if (SysLib.rawwrite(blockid, block) != 0) {
            SysLib.cerr("Could not write the block for clearing");
            return false;
        }
        return true;
    }
    
    /**
     * Below is the delete() method that is used for deleting a specific file 
     * based on the filename that is passed through as a parameter. 
     * First, this creates a temp FileTableEntry object to hold the inode object. 
     * This lets us have access to all private members of the filename entry we 
     * are trying to delete. With this inode, we can use the iNumber to free it 
     * from the directory and deallocate the file.  
     * 
     * @param filename that is used to find the file to delete
     * @return true or false based on if deletion happened or not
     * 
     * @author Dwina Solihin
     */
    boolean delete (String filename) {
        int iNumber;
        
        //check for blank file name
        if (filename == ""){
            return false;
        } 
        
        //gets the iNumber of the file name
        iNumber = directory.namei(filename);
       
        //checks if the iNumber is -1, which means it does not exist
        if(iNumber == -1){
            return false;
        }
        
        //deallocates file
        return directory.ifree((short)iNumber);
    }
    
    //The start positions of the file pointer
    private final int SEEK_SET = 0; //beginning of the file
    private final int SEEK_CUR = 1; //current position of the file pointer
    private final int SEEK_END = 2; //end of the file
    
    /**
     * Below is the seek() method where it updates the pointer that corresponds 
     * to the given file table entry. 
     * If it is successful, it will return 0. If it fails, it returns -1. 
     * @param ftEnt File entry
     * @param offset 
     * @param whence
     * @return 0 for success and -1 for failure
     * 
     * @author Dwina Solihin
     */
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        synchronized(ftEnt){
            //uses the whence because it is what is changed to determine where 
            //to start reading the file
            switch(whence){
                
                //Position from beginning of the file
                case SEEK_SET:
                    //Sets the seek pointer to the offset from the begining of 
                    //the file
                    ftEnt.seekPtr = offset;
                    break;
                
                //Position from the current position of the file pointer
                case SEEK_CUR:
                    //sets the seek pointer to its current value plus the offset
                    ftEnt.seekPtr += offset;
                    break;
                
                //Position from the end of the file
                case SEEK_END:
                    //sets the seek pointer to the size of the file (inode length)
                    //plus the offset
                    ftEnt.seekPtr = ftEnt.inode.length + offset;
                    break;
                
                //Return failure (-1)
                default:
                    return -1;
       
            }
            
            //If the user tries to set the seek pointer to a negative number, 
            //this sets it to 0
            if (ftEnt.seekPtr < 0){
                ftEnt.seekPtr = 0;
            }
            
            //If the user tries to set the pointer to beyond the file size, then 
            //this sets the seen pointer to the end of the file
            if (ftEnt.seekPtr > ftEnt.inode.length){
                ftEnt.seekPtr = ftEnt.inode.length;
            }
            
            //return success (0)
            return ftEnt.seekPtr;
        }
    }
}