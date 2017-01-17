
import java.util.LinkedList;
import java.util.Vector;

public class FileTable {

    //assignment description says to use vectors
    private final Vector table;
   //private final LinkedList<FileTableEntry> table; // the actual entity of this file table
   private final Directory dir;        // the root directory
   
   //Added by Dwina Solihin
   public final static int UNUSED = 0; //file does not exist
   public final static int USED = 1; //file exists but not read or written by anyone
   public final static int READ = 2; //read by someone
   public final static int WRITE = 3; //written by someone

   public FileTable( Directory directory ) { // constructor
      //table = new LinkedList<>( );     // instantiate a file (structure) table
      table = new Vector();
      dir = directory;           // receive a reference to the Director
   }                             // from the file system

   // major public methods
   /**
     * Dwina: Follows the format in the CSS430FinalProject.pdf file in the notes section 
     * of the assignment description. 
     */
   public synchronized FileTableEntry falloc( String filename, String mode ) {
       short iNumber = -1; //inode number
       Inode inode = null; //Inode object
       while (true){
           //gets iNumber from the inode of give filename
           iNumber = (filename.equals("/") ? 0 : dir.namei(filename));
           
           //checks if inode for give filename is there
           if(iNumber >= 0){
               inode = new Inode(iNumber);
               
               //checks if the file is read
               if (mode.equals("r")){
                   
                   //if file's flag is read, or used, or unused (no one has read
                   //or written to file)
                   if (inode.flag == READ || inode.flag == USED || inode.flag == UNUSED){
                       inode.flag = READ; //changes flag to read then break
                       break; 
                       
                       //checks if file has already been written by someone,
                       //waits until it is finished
                   } else if (inode.flag == WRITE){
                       try {
                           wait();
                       } catch (InterruptedException e){}
                    } 
                   
                //checks if the file's flag is used, or unused
                } else {
                    if (inode.flag == USED || inode.flag == UNUSED) {
                        inode.flag = WRITE; //changes it to wirte and then breaks
                        break;
                        
                    //this wait occurs when the file's flad is read or write    
                    } else { 
                        try {
                            wait();
                        } catch (InterruptedException e){}
                    }
                }
               
               //if the mode for the given file does not exist
               
            } else if (!mode.equals("r")){
                
                //creates a new inode object for that file, uses the ialloc() 
                //method from the directory class to get the iNumber
                
                iNumber = dir.ialloc(filename);
                if (iNumber == -1) {
                    SysLib.cerr("Error occured trying to allocate iNumber within falloc for " + filename);
                    SysLib.exit();
                }
                inode = new Inode(iNumber);
                inode.flag = WRITE;
                break;
            } else {  
                return null;
            }
       }
        inode.count++; //increment number of users
        inode.toDisk(iNumber); //writes to disk
        
        //creates new FTE and adds it to the file table
        FileTableEntry newEntry = new FileTableEntry (inode, iNumber, mode);
        table.addElement(newEntry);
        return newEntry;
   }
                
       
//      short inodeNumber = dir.ialloc(filename); //allocate an inode for the file
//      
// allocate/retrieve and register the corresponding inode using dir
//      Inode inode = new Inode(inodeNumber);
//      short inumber = dir.namei(filename);
//      
// allocate a new file (structure) table entry for this file name
//      FileTableEntry newEntry = new FileTableEntry(inode, inumber, mode);
//      inode.count++;// increment this inode's count
//      
// immediately write back this inode to the disk
//      inode.toDisk(inumber);
//      
// return a reference to this file (structure) table entry
//      return newEntry;
   

   public synchronized boolean ffree( FileTableEntry e ) {
//      // receive a file table entry reference  
//      boolean found = false;
//      if (table.contains(e)) {
//          found = true;
//      }
//      // save the corresponding inode to the disk
//      e.inode.toDisk(e.iNumber);
//      // free this file table entry. 
//      //currently the FTE fields are final...not 100% sure how to actually enable free
//      //decrement inode count
//      e.inode.count--;
//      if (e.inode.count == 0) {
//          //set flag to 0, this had been the last thread using the inode
//          e.inode.flag = 0;
//      }
//      // return true if this file table entry found in my table
//      return found;

        //receive a file table entry reference
        Inode inode = new Inode(e.iNumber);
        //free this file table entry, checks if read or write
        if (table.remove(e)){
            if (inode.flag == READ){
                if (inode.count == 1){
                    notify();
                    inode.flag = USED;
                }
            } else if (inode.flag == WRITE){
                inode.flag = USED;
                notifyAll();
            }
            inode.count++;
            
            //save the corresponding inode to disk
            inode.toDisk(e.iNumber);
            return true; //return true if the FTE found in my table
        }
        return false; //not found
   }

   public synchronized boolean fempty( ) {
      return table.isEmpty( );  // return if table is empty 
   }                            // should be called before starting a format
}