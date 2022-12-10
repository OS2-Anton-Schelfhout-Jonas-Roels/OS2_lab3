package Allocator;
import java.util.*;

class Arena {
    Long size;
    BitSet allocedChunks = new BitSet();
    List<Long> startAddress = new LinkedList<>();
    int amount = 0;

    public Arena(Long size, Long startAddress) {
        this.size = size;
        this.startAddress.add(startAddress);
    }
}


public class OurAllocatorImpl implements Allocator {
    private static final int ARENA_SIZE = 8388608;
    private final Map<Long, Arena> arenas = new HashMap<>();
    private final NavigableMap<Long, Long> alloccedBlocks = new TreeMap<>();

//    public OurAllocatorImpl (){
//        long[] sizes = {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
//        for(long s : sizes) {
//            arenas.put(s, new Arena(s, BackingStore.getInstance().mmap(8388608)));
//        }
//    }

    @Override
    public Long allocate(int size) {

        long smallestSize = 8L;
        while (smallestSize < size) smallestSize *= 2;
        Long address;
        int index;
        Arena arena;
        synchronized (arenas) {
            if (!arenas.containsKey(smallestSize)) {
                arenas.put(smallestSize, new Arena(smallestSize, BackingStore.getInstance().mmap(ARENA_SIZE)));
            }
            arena = arenas.get(smallestSize);
        }
        synchronized (arenas.get(smallestSize)) {
            index = arena.allocedChunks.nextClearBit(0);
            arena.allocedChunks.set(index, true);
            if(!arena.allocedChunks.get(index)) System.out.println("ERROR SETTING BIT FOR");
            int block = index / (ARENA_SIZE / size);

            if (arena.startAddress.size() <= block) {
                arena.startAddress.add(block, BackingStore.getInstance().mmap(ARENA_SIZE));
            }
            address = (index * smallestSize) + arena.startAddress.get(block);

            arena.amount++;
        }
        synchronized (alloccedBlocks) {
            if(alloccedBlocks.containsKey(address))
                System.out.println("Block already exists " + index + ", " + smallestSize);
            alloccedBlocks.put(address, smallestSize);
            return address;
        }
    }

    @Override
    public void free(Long address) {
        long size;
        int block = 0;
        synchronized(alloccedBlocks) {
            size = alloccedBlocks.remove(address);
        }
        Arena arena;
        synchronized (arenas) {
            arena = arenas.get(size);
        }

        synchronized (arenas.get(size)) {
            for (long startAddress : arena.startAddress) {
                if (address >= startAddress && address <= (startAddress + ARENA_SIZE)) {
                    block = arena.startAddress.indexOf(startAddress);
                    break;
                }
            }


//            if (!arenas.get(size).allocedChunks.get(index))
//                throw new AllocatorException("Fout bij free, chunk was niet gealloceerd");

            int index = (int) ((address - arena.startAddress.get(block)) / size);
            arena.allocedChunks.set(index, false);
            arena.amount--;

            // Indien er geen elementen meer in de arena zijn dealloceren we hem
        }
//        synchronized (arenas) {
//            if (arenas.get(size).amount == 0) {
//                for (long startAddress : arenas.get(size).startAddress)
//                    BackingStore.getInstance().munmap(startAddress, ARENA_SIZE);
//                arenas.remove(size);
//            }
//        }


    }

    @Override
    public Long reAllocate(Long oldAddress, int newSize) {
        free(oldAddress);
        return allocate(newSize);
    }

    @Override
    public boolean isAccessible(Long address) {
        return isAccessible(address, 1);
    }

    @Override
    public boolean isAccessible(Long address, int size) {
        Map.Entry<Long,Long> entry = null;
        synchronized (alloccedBlocks) { entry = alloccedBlocks.floorEntry(address); }
        if (entry == null)
            return false;
        assert address >= entry.getKey();
        var entryEnd = entry.getKey() + entry.getValue();
        if (address >= entryEnd)
            return false;
//        var end = address + size - 1;
//        if (end < entry.getKey() || end >= entryEnd)
//            return false;
        return true;
    }
}



//package Allocator;
//import java.util.Map;
//import java.util.NavigableMap;
//import java.util.TreeMap;
//import java.util.concurrent.locks.*;
////import java.util.concurrent.locks.ReadWriteLock;
////import java.util.concurrent.locks.ReentrantLock;
////import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//
//public class OurAllocatorImpl implements Allocator {
//    private static NavigableMap<Long, Long> alloccedBlocks = new TreeMap<>();
//    private static BackingStore backingStore = BackingStore.getInstance();
////    private Lock backintStoreLock = new ReentrantLock();
//    private static ReadWriteLock alloccedBlocksLock = new ReentrantReadWriteLock();
//
//
//    @Override
//    public Long allocate(int size) {
//        Long lsize = (long) size;
//        long ret;
//        ret = backingStore.mmap(size);
//        alloccedBlocksLock.writeLock().lock();
//        alloccedBlocks.put(ret, lsize);
//        alloccedBlocksLock.writeLock().unlock();
//
//
////        synchronized (this) {
////            alloccedBlocks.put(ret, lsize);
////        }
//        // System.out.println("Allocating " + size + " bytes at " + ret);
//        return ret;
//    }
//
//    @Override
//    public void free(Long address) {
//        alloccedBlocksLock.readLock().lock();
//        var size = alloccedBlocks.get(address);
//        alloccedBlocksLock.readLock().unlock();
//        if (size == null)
//            throw new AllocatorException("huh??");
//        alloccedBlocksLock.writeLock().lock();
//        alloccedBlocks.remove(address);
//        alloccedBlocksLock.writeLock().unlock();
//
//        backingStore.munmap(address, size);
////        synchronized (this) {
////            var size = alloccedBlocks.get(address);
////            if (size == null)
////                throw new AllocatorException("huh??");
////            alloccedBlocks.remove(address);
////            BackingStore.getInstance().munmap(address, size);
////            // System.out.println("Freeing " + size + " bytes at " + address);
////        }
//    }
//
//    @Override
//    public Long reAllocate(Long oldAddress, int newSize) {
////        free(oldAddress);
////        return allocate(newSize);
//
//        synchronized (this) {
//            free(oldAddress);
//            return allocate(newSize);
//        }
//    }
//
//    @Override
//    public boolean isAccessible(Long address) {
//        synchronized (this) {
//            return isAccessible(address, 1);
//        }
//    }
//
//    @Override
//    public boolean isAccessible(Long address, int size) {
//        Map.Entry<Long,Long> entry = null;
//        synchronized (this) { entry = alloccedBlocks.floorEntry(address); }
//        if (entry == null)
//            return false;
//        assert address >= entry.getKey();
//        var entryEnd = entry.getKey() + entry.getValue();
//        if (address >= entryEnd)
//            return false;
//        var end = address + size - 1;
//        if (end < entry.getKey() || end >= entryEnd)
//            return false;
//        return true;
//    }
//}