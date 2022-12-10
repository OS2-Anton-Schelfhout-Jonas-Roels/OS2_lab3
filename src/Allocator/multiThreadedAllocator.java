package Allocator;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class multiThreadedAllocator implements Allocator{
    private static final Map<Long, OurAllocatorImpl> allocators = new HashMap<>();
    private static final Map<Long, Long> alloccedBlocks = new HashMap<>();

    @Override
    public Long allocate(int size) {
        long id = Thread.currentThread().getId();
        // synchroniseer op volledige map, kijk of allocator aanwezig, voegtoe indien afwezig
        OurAllocatorImpl alloc;
        synchronized (allocators) {
            if (!allocators.containsKey(id)) {
                allocators.put(id, new OurAllocatorImpl());
            }
            alloc = allocators.get(id);
        }

        // gebruik de allocator van de huidige thread om het blok te alloceren
        long address;
        synchronized (allocators.get(id)) {
            address = alloc.allocate(size);
        }

        // sla het id van de thread dat dit blok heeft gealloceerd op in de map
        synchronized (alloccedBlocks) {
            alloccedBlocks.put(address, id);
        }

        return address;
    }

    @Override
    public void free(Long address) {
        // haal het id van de thread die het blok heeft gealloceerd op
        Long id;
        synchronized (alloccedBlocks) {
            id = alloccedBlocks.get(address);
        }
//        if(id != Thread.currentThread().getId()) System.out.println("Trying to free other threads memory");
        OurAllocatorImpl alloc;
        synchronized (allocators) {
            alloc = allocators.get(id);
        }
        if(id == null) throw new AllocatorException("No block found at that address (free)");
        // gebruik de allocator met dat id om het blok te freeen
        synchronized (allocators.get(id)) {
            alloc.free(address);
        }
    }

    @Override
    public Long reAllocate(Long oldAddress, int newSize) {
        // haal het id van de thread die het blok heeft gealloceerd op
        Long id;
        synchronized (alloccedBlocks) {
            id = alloccedBlocks.remove(oldAddress);
        }
//        if(id != Thread.currentThread().getId()) System.out.println("Trying to reallocate other threads memory");
        if(id == null) throw new AllocatorException("No block found at that address (realloc)");
        // gebruik de allocator met dat id om het blok te reallocaten
        OurAllocatorImpl alloc;
        synchronized (allocators) {
            alloc = allocators.get(id);
        }
        long address;
        synchronized (allocators.get(id)) {
            address = alloc.reAllocate(oldAddress, newSize);
        }
        synchronized (alloccedBlocks) {
            alloccedBlocks.put(address, id);
        }
        return address;
    }

    @Override
    public boolean isAccessible(Long address) {
        var id = alloccedBlocks.get(address);
        OurAllocatorImpl alloc;
        synchronized (allocators) {
            alloc = allocators.get(id);
        }
        synchronized (allocators.get(id)) {
            return alloc.isAccessible(address);
        }
    }

    @Override
    public boolean isAccessible(Long address, int size) {
        var id = alloccedBlocks.get(address);
        if(id == null) return false;
        OurAllocatorImpl alloc;
        synchronized (allocators) {
            alloc = allocators.get(id);
        }
        synchronized (allocators.get(id)) {
            return alloc.isAccessible(address, size);
        }
    }
}
