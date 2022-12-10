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

            int index = (int) ((address - arena.startAddress.get(block)) / size);
            arena.allocedChunks.set(index, false);
            arena.amount--;
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
        if (entry == null) return false;
        assert address >= entry.getKey();
        var entryEnd = entry.getKey() + entry.getValue();
        if (address >= entryEnd) return false;
        return true;
    }
}

