# CSS430-File-System

Overview
For CSS 430, Operating Systems, our final project was to implement a File System. With this project, we split the amount of work as evenly as we saw fit between the two. Below is a closer look at each part of our File System. 

Assumptions
Our assumptions in design and implementation come from what we read in the assignment description on OneNote, the extended notes tab, and the outdated but informational PDF that Professor Parsons added to the notes section. We went under the assumption that the all the information given to us in the assignment were up to a reasonable standard for a user’s needs, the OS and file system control. We did not incorporate protection into our project because we went under the assumption that all access to the files and commands would be legitimate. 

Limitations
A limitation that we were able to see in this File System was there is a limited number of inodes on the disk (64 from the assignment description). If we wanted to create more, we would have trouble doing so. This is an issue with a real-time File System because it should be able to handle thousands of files dynamically. Though, it is understandable that there was a limit with the amount of inodes to simplify debugging and and help understand disk behavior. However, this causes a limitation because we are not able to replicate what happens in real-world implementations. 

Another limitation in this File System is that there is no source of protection for the disk and file system. As seen in the implementation of this project, most of our methods are declared public and any method holding a reference to any file system class can perform the functions within the class, even the ones that need elevated privileges. 

Inode Design
The purpose of Inode is to describe a file. It holds 12 pointers (11 direct and 1 indirect) in the index block. There is a total of 16 inodes that can be stored in a block. The Inode includes information such as the length of the corresponding file, the number of file table entries that point to the inode, and a flag to indicate if it is being used or not.   The direct pointers will contain the id of the block to access for data directly, whereas the indirect pointer points to a block filled exclusively with short block ids to access data directly with.  

When an inode is initialized via its default constructor, it is only allocated a single block for storing data, which is direct[0].  When a file is being written, if more storage space is needed, new blocks will be allocated to the direct blocks first, then once those fill, the indirect block will be allocated, and blocks will be allocated for each short in the indirect block.

File Table Design
The purpose of File Table is to create a new file table entry when needed and then add it to the Vector of the file table class. The file table class is suppose to represent the set of file table entries. Each entry represents one file descriptor.

Superblock Design
How the Superblock class works is that it reads the physical Superblock from the disk, validates the disk and sees if it is useable, and provides a way for identifying free blocks, adding blocks to the free list and writing the new contents of Superblock back to the disk. If the validation fails, the Superblock will perform the format method which is used to restore itself to an empty state and write a new Superblock to the disk. 

Directory Design
The purpose behind directory is to manage the “files” that the file system is dealing with. How the Directory class can handle files is through implementing two arrays, a 1D array to hold the sizes (fsizes) and a 2D array to hold each file’s name(fname). The purpose of fsizes is to hold the sizes of the files in their respected locations. One way to visualize fsizes is as a simple list of ints to represent the different sizes that are stored in the fname array. The purpose of fnames is to contain the files that the Directory is holding. 

In the constructor, an int (maxnumbers) is passed through to represent the max storage files that the fsizes array can hold. From there, fnames gets initialized to contain both the sizes and the maxchar that the file name can be. So, for example, a file could be named “FileName” and it’s size would would be 8 (i.e. fsizes = 8) since size is determined by how many chars are in a world and fnames would hold each character (F,i,l,e,N,a,m,e) of the string. 

File System Design
When reading from a file, first there is a check to ensure that the file is in read mode - if not, if will return -1 as an error.  If the file is in read mode, then as long as the buffer passed is not full and the file end has not been reached, a single byte will be read from the file into the buffer.  This is done by reading through all the direct and indirect pointers; as soon as a block has been read completely, the next pointer in the line is loaded and read into the buffer.  For example, if you are reading direct pointer 3 and finish the block, you will move onto direct pointer 4.  If you are on direct pointer 11 and finish the block, you will move on to the first indirect pointer.

When writing from a file, first there is a check to see if the file is in read or append mode; if so, the file will be written to starting where the seek pointer points to.  The correct direct or indirect block for starting to write is found, and within that block, the specific byte where to begin writing.  Once this byte has been found, the bytes from the buffer will be written from there onwards.  If the bytes from the buffer are not finished writing by the time the direct/indirect block is full, the next direct or indirect block is found (in the same fashion as read above), and writing will resume in there.  This continues until the bytes written equal the length of the buffer passed in, or until there is no room left to write.

When deallocating all blocks, the method checks through all the indirect and direct blocks, and overwrites the information within them to 0.  It will additionally mark the blocks as free and set the direct/indirect pointers to -1 so the program knows not to use them.  The sole exception is direct[0], which is kept allocated to the inode, but it has it’s entire block overwritten as any other block would.  This is so every Inode will at least be able to hold some data, and ensure it stays useful through the life of the program.

BlockCounter
This is a helper class that is used to keep track of which blocks on the disk are currently open for use.  As blocks are allocated, they are no longer available for use, and when they are deallocated, they once again become open for use.  This is implemented as a singleton such that throughout the program, only instance of it ever exists at a time.















Results
Below are the results from our File System project using the Test5.java class given. In order to show that our project was a success, all 18 test cases should successfully compile and run.

threadOS ver 1.0:
threadOS: DISK created
Type ? for help
threadOS: a new thread (thread=Thread[Thread-3,2,main] tid=0 pid=-1)
-->l Test5
l Test5
threadOS: a new thread (thread=Thread[Thread-5,2,main] tid=1 pid=0)
1: format( 48 )...................successfully completed
Correct behavior of format......................2
2: fd = open( "css430", "w+" )....successfully completed
Correct behavior of open........................2
3: size = write( fd, buf[16] )....size = 0 (wrong)
4: close( fd )....................successfully completed
Correct behavior of close.......................2
5: reopen and read from "css430"..size = -1 (wrong)
6: append buf[32] to "css430".....size = -1 (wrong)
7: seek and read from "css430"....seek(fd,10,0)=0 (wrong)
8: open "css430" with w+..........tmpBuf[1]=0 (wrong)
9: fd = open( "bothell", "w" )....successfully completed
10: size = write( fd, buf[6656] ).size = 0 (wrong)
11: close( fd )....................successfully completed
12: reopen and read from "bothell"size = -1 (wrong)
13: append buf[32] to "bothell"...size = -1 (wrong)
14: seek and read from "bothell"...seek(fd,512 * 11,0)=0 (wrong)
15: open "bothell" with w+.........tmpBuf[1]=0 (wrong)
16: delete("css430")..............successfully completed
Correct behavior of delete....................0.5
17: create uwb0-29 of 512*13......fd[3] failed in writing
18: uwb0 read b/w Test5 & Test6...
threadOS: a new thread (thread=Thread[Thread-7,2,main] tid=2 pid=1)
Test6.java: fd = -1Test6.java: size = -1(wrong)fail
Test6.java terminated
tmpBuf[0]=0 should be 100
Test completed
-->
Performance Estimation
Performance for our file system should be adequate, however it is far from the quickest file system available.  There are multiple reasons for this, such as the fact that it will overwrite all of the blocks on block deallocation.  This serves two purposes, one, it protects privacy of whomever uses the file system, and two, it allows the code to guarantee a certain outcome in the case of over-reading a file.
Performance could be increased by increasing the level of multi-threading in the program itself; for instance, clearing multiple blocks simultaneously by creating a new thread for each block to be deallocated.  However, we found that for debugging and general sanity purposes, implementing this kind of functionality would have been too difficult, and was put on the backburner as an improvement to consider once the file system worked as intended.

Current Functionality
At the moment, our current implementation of a file system is broken. It is able to format files, open files, and close files, but it cannot successfully read or write files and update the stored length of these files. This is likely due to a mix-match of where to read/write in the program compounded by writing data to incorrect parts of the blocks, making it impossible for the program to recreate inodes based on passed in iNumbers. One thing that we noticed while testing out our file system with Test5.java is that with the call to the write method in Kernel.java was not actually calling the write() in the FileSystem.java class. It looks that the breakpoints that were set in FileSystem.java’s write() method were never hit so it sees like something is going on with either the Kernel.java or how write() was implemented in FileSystem.java. 

Possible Extended Functionality
The code is written with minimal hard-coding set in place.  This means simple changes to the kernel like adjusting the disk block size will allow the file system to work just as it did before, except with the ability to hold even more (or less) data.  Some parts of the code would have to be adjusted manually though, such as the size of the File System itself, or the number of direct/indirect pointers for the Inodes.  In the case of adding more indirect pointers, or indirection on the indirect pointers, code would have to change, however more direct pointers will prove to be no issue.

How the final project compares to a real-world application
The current functionality of our File System is simpler than real-world operating system’s file systems. For the current functionality, due to the time constraints and just general difficulty of this topic, its functionality had to be simplified in order for CSSE students to practice implementing a UNIX-like file system without the complexity of what an actual file system is like. The simplicity of this file system can be see with the limited number of inodes on the disk and how the direct and indirect pointers interact. 
