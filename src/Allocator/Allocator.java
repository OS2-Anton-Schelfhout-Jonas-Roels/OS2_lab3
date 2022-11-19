package Allocator;
/*
 * This is the interface that your Allocator implementation must adhere to.
 */

public interface Allocator {
    /* Modify this static var to return an instantiated version of your allocator  */
    static Allocator instance = new OurAllocatorImpl();

    /* Allocates a new region of memory with the specified size */
    public Long allocate(int size);

    /* 
     * Releases the region of memory pointed to by `address`.
     * This memory can be reused to serve future `allocate` requests.
     */
    public void free(Long address);

    /*
     * Releases the memory associated with `oldAddress` and immediately
     * returns a new region of memory with size `newSize`
     * 
     * This method is commonly supported by allocators because it
     * provides the possibility for optimization: when there is unused 
     * memory available next to the allocation, the original allocation
     * can be resized in-place, yielding performance gains.
     * 
     * It is allowed for this method to return new memory every time, 
     * but a more optimized implementation is encouraged!
     */
    public Long reAllocate(Long oldAddress, int newSize);

    /*
     * Returns `true` when `address` refers to a currently allocated block.
     * Returns `false` otherwise.
     * 
     * This method is used by us to test your implementation. 
     * You still have to implement it yourselves, specifically for your
     * type of allocator.
     */
    public boolean isAccessible(Long address);

    /*
     * Same as above, except it allows to check a range of 
     * addresses more efficiently.
     * 
     * In addition, this method should verify that all addresses 
     * in the range belong to the same block of memory. 
     */
    public boolean isAccessible(Long address, int size);
}
