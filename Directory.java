import java.util.*;
import java.io.*;
import java.lang.*; //used to import the Java System class

public class Directory {
    private static int maxChars = 30; // max characters of each file name
    private static int maxJavaBytes = 60; //max Java bytes
    
    private final static int BYTE_ALLOC = 64; //maxJava + 4 short
    private final static int NEXT_BLOCK = 4; //used with offset
    private final static int ERROR = -1; //sets the error to -1
    
    private int dirSize; // directory size
   
    // Directory entries
   private int fsize[];        // each element stores a different file size.
   private char fnames[][];    // each element stores a different file name.


//--------------------------- Constructor --------------------------------------
/**
 * Below is a constructor that was given to us to base our Directory class off 
 * of. 
 * It passing in the size of the directory and initializes all the file sizes to 
 * 0, creates the fileName array, and sets the "/" root directory in the first
 * location.
 */
   public Directory( int maxInumber ) { // directory constructor
      fsize = new int[maxInumber];     // maxInumber = max files
      for ( int i = 0; i < maxInumber; i++ ) 
         fsize[i] = 0;                 // all file size initialized to 0
      fnames = new char[maxInumber][maxChars];
      String root = "/";                // entry(inode) 0 is "/"
      fsize[0] = root.length( );        // fsize[0] is the size of "/".
      root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
       
       dirSize = maxInumber; //initializes dirSize
   }

//--------------------------- bytes2directory ----------------------------------
/**
 * Below is a method that is used to initalize the Directory instance with
 * this.data[]. There is an assumption that data[] is receiving directory 
 * information from the disk. 
 * The way this happens is with the use of the SysLib.bytes2int() method that is
 * used to hold the size of the file. After this, it goes through the directory
 * and reads the content of data[].
 */
   public void bytes2directory( byte data[] ) {
      // assumes data[] received directory information from disk
      // initializes the Directory instance with this data[]
       
       int offset = 0; //initializes offset
       
       //for loop to hold size of file
       for (int i = 0; i < dirSize; i++){ //goes through directory
           
           //saves file size with information of data[] being passed through
           fsize[i] = SysLib.bytes2int(data, offset);
           //increments offset
           offset += NEXT_BLOCK;
       }
       
       //this for loop goes through the directory and places a temp String into
       //the fnames array
       for (int i = 0; i < dirSize; i++){ //goes through directory
           //sets temp string object
           String temp = new String(data, offset, maxJavaBytes);
           
           //puts that string object in fnames
           temp.getChars(0, fsize[i], fnames[i], 0);
           
           offset += maxJavaBytes; //increment offset
       }
   }

//--------------------------- directory2bytes ----------------------------------
/**
 * Below is a method that is used to convert the Directory information into a 
 * byte[] array and then returns that byte[] array. In the end, this byte[] array
 * will be written back into the disk. How it goes about doing this is by 
 * creating a byte[] array (containDirInfo) of the correct size. It then loops 
 * through the directory to get the file sizes first. Then it goes through the
 * directory again to get the actual data in the directory. 
 * containDirInfo[] is returned in the end. 
 */
   public byte[] directory2bytes( ) {
      // converts and return Directory information into a plain byte array
      // this byte array will be written back to disk
      // note: only meaningfull directory information should be converted
      // into bytes.
       byte[] containDirInfo = new byte [BYTE_ALLOC * dirSize];
       int offset = 0;
       
       for (int i = 0; i < dirSize; i++){
           SysLib.int2bytes(fsize[i], containDirInfo, offset);
           offset += NEXT_BLOCK;
       }
       
       for (int i = 0; i < dirSize; i++){
           String temp = new String(fnames[i], 0, fsize[i]);
           byte[] tempByte = temp.getBytes();
           
           System.arraycopy(tempByte, 0, containDirInfo, offset, tempByte.length);
           
           offset += maxJavaBytes;
       }
       
       return containDirInfo;
       
   }
//--------------------------------- ialloc() -----------------------------------
/**
 * The ialloc method below is used to allocate a new inode number for the filename
 * string that is passed through. The use of a ternary operator is used when 
 * comparing filename.length() and maxChars to help condense code. It is used to
 * find the smaller of the two ints. 
 * An inode number is returned.
 */
   public short ialloc( String filename ) {
      // filename is the one of a file to be created.
       
       for (short i = 0; i < dirSize; i++){ //goes through directory
           if (fsize[i] == 0){ //checks for empty file
               
               //uses a ternary operator
               int fizeSize = (filename.length() > maxChars)
                                                ? maxChars:filename.length();
               fsize[i] = fizeSize; //saves the file size
               
               //copies from the string
               filename.getChars(0, fsize[i], fnames[i], 0);
               return i; //returns the inode number
           }
       }
       
       return ERROR;
   }

//--------------------------------- ifree() -----------------------------------
/**
 * The ifree() method below is used by passing through an inode number and then 
 * using that inode number to mark the size of a iNumber location to 0, this meaning
 * that it can be overwritten.
 */
   public boolean ifree( short iNumber ) {
       //checks to see if the inode number is valid
       if (iNumber < maxChars && fsize[iNumber] > 0){
           fsize[iNumber] = 0; //deallocates the inode number
           
           return true; //after deallocation
       } else { //false when the inode number is invalid
           return false;
       }
   }

    
//--------------------------------- namei() -----------------------------------
/**
 * The namei() method below is used to return the inode number corresponding to
 * the filename that is being passed through. The method goes through the diectory,
 * then it checks the length of both the filename and the current inode it is on
 * matches. From them, a temp string is assigned the name of the current inode
 * it is on and checks if they are equal. If the names match, then the current
 * inode number is returned and if they do not match, then it returns an 
 * ERROR (-1).
 */
   public short namei( String filename ) {
      // returns the inumber corresponding to this filename
       
       for (short i = 0; i < dirSize; i++){ //goes through directory
           //checks if the size of the strings match
           if (filename.length() == fsize[i]){
               // creates a temp string used for the comparison
               String temp = new String (fnames[i], 0, fsize[i]);
               
               //when inode number's filename corresponds to the temp string created
               if (filename.equals(temp)){
                   return i; //returns the inode number
               }
           }
       }
       
       return ERROR; //returns error if the inode number does not match the filename
   }
}
