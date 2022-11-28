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
    private Map<Long, Arena> arenas = new HashMap<>();
    private NavigableMap<Long, Long> alloccedBlocks = new TreeMap<>();

    @Override
    public Long allocate(int size) {

            long smallestSize = 8L;
            while (smallestSize < size) smallestSize *= 2;
        synchronized (arenas) {
            if (!arenas.containsKey(smallestSize)) {
                arenas.put(smallestSize, new Arena(smallestSize, BackingStore.getInstance().mmap(8388608)));
            }

            int index = arenas.get(smallestSize).allocedChunks.nextClearBit(0);
            arenas.get(smallestSize).allocedChunks.flip(index);
            int block = index / (8388608 / size);

            if (arenas.get(smallestSize).startAddress.size() <= block) {
                arenas.get(smallestSize).startAddress.add(block, BackingStore.getInstance().mmap(8388608));
            }
            Long address = (index * smallestSize) + arenas.get(smallestSize).startAddress.get(block);

            arenas.get(smallestSize).amount++;
            synchronized (alloccedBlocks) {
                if(alloccedBlocks.containsKey(address))
                    System.out.println("Block already exists " + index + ", " + smallestSize);
                alloccedBlocks.put(address, smallestSize);
                return address;
            }
        }
    }

    @Override
    public void free(Long address) {
        long size;
        int block = 0;
        synchronized(alloccedBlocks) {
            size = alloccedBlocks.remove(address);
        }
        for(long startAddress : arenas.get(size).startAddress) {
            if(address >= startAddress && address <= (startAddress + 8388608)) {
                block = arenas.get(size).startAddress.indexOf(startAddress);
                break;
            }
        }

        int index = (int) ((address - arenas.get(size).startAddress.get(block)) / size);
//            if (!arenas.get(size).allocedChunks.get(index))
//                throw new AllocatorException("Fout bij free, chunk was niet gealloceerd");
        synchronized (arenas) {
            arenas.get(size).allocedChunks.set(index, false);
            arenas.get(size).amount--;

            // Indien er geen elementen meer in de arena zijn dealloceren we hem
            if (arenas.get(size).amount == 0) {
                for (long startAddress : arenas.get(size).startAddress)
                    BackingStore.getInstance().munmap(startAddress, 8388608);
                arenas.remove(size);
            }
        }

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