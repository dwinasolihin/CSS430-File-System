
import java.util.Arrays;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author sacht
 */
public class blockCounter {
    private static boolean[] blocks = null;
    private static blockCounter instance = null;
    
    public static blockCounter getInstance() {
        if (instance == null) {
            instance = new blockCounter();
            SysLib.cerr("Initialized blockCounter with no parameter; probably going to error.");
        }
        return instance;
    }
    
    public static blockCounter getInstance(int diskBlocks) {
        if (instance == null) {
            instance = new blockCounter();
            //init array
            blocks = new boolean[diskBlocks];
            //init the array to true (open block) for everything
            Arrays.fill(blocks, Boolean.TRUE);
            //superblock uses block 0, so it's false
            blocks[0] = false;
        }
        return instance;
    }
    
    /**
     * Flips the state of the block passed
     * true->false
     * false->true
     * @param loc id of block to flip
     */
    public void flipBlockState(int loc) {
        blocks[loc] = !blocks[loc];
    }
    
    /**
     * Return the state of the block passed in
     * @param loc
     * @return id of block
     */
    public boolean getBlockState(int loc) {
        return blocks[loc];
    }
    
    /**
     * Use this to find a free block
     * @return free block number, -1 if no free
     */
    public short getFreeBlock() {
        for (short i = 0; i < blocks.length; i++) {
            if (blocks[i] == true) {
                return i;
            }
        }
        return -1;
    }
    
    private blockCounter() {
    }
}
