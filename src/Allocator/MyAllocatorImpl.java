package Allocator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class MyAllocatorImpl implements Allocator {
    private NavigableMap<Long, Long> alloccedBlocks = new TreeMap<>();

    @Override
    public Long allocate(int size) {
        synchronized (this) {
            var ret = BackingStore.getInstance().mmap(size);
            Long lsize = (long) size;
            alloccedBlocks.put(ret, lsize);
            // System.out.println("Allocating " + size + " bytes at " + ret);
            return ret;
        }
    }

    @Override
    public void free(Long address) {
        synchronized (this) {
            var size = alloccedBlocks.get(address);
            if (size == null)
                throw new AllocatorException("huh??");
            alloccedBlocks.remove(address);
            BackingStore.getInstance().munmap(address, size);
            // System.out.println("Freeing " + size + " bytes at " + address);
        }
    }

    @Override
    public Long reAllocate(Long oldAddress, int newSize) {
        synchronized (this) {
            free(oldAddress);
            return allocate(newSize);
        }
    }

    @Override
    public boolean isAccessible(Long address) {
        synchronized (this) {
            return isAccessible(address, 1);
        }
    }

    @Override
    public boolean isAccessible(Long address, int size) {
        Map.Entry<Long,Long> entry = null;
        synchronized (this) { entry = alloccedBlocks.floorEntry(address); }
        if (entry == null)
            return false;
        assert address >= entry.getKey();
        var entryEnd = entry.getKey() + entry.getValue();
        if (address >= entryEnd)
            return false;
        var end = address + size - 1;
        if (end < entry.getKey() || end >= entryEnd)
            return false;
        return true;
    }
}
