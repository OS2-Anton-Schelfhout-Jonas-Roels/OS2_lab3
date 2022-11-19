import Allocator.Allocator;
import Allocator.*;
import java.util.Random;

public class SingleThreadedApplication {
    private static Random random = new Random();
    public static void main(String[] args) {
        int count = 0;
        while(true) {
            int amount  = random.nextInt(5000);
            long[] addressbuffer = new long[amount];
            for(int i = 0; i < amount; i++) {
                int size = random.nextInt(2048) + 1;
                Long address = Allocator.instance.allocate(size);
                ensureAllocated(true, address, size);
                size += random.nextInt(2048);
                address = Allocator.instance.reAllocate(address, size);
                addressbuffer[i] = address;
                ensureAllocated(true, address, size);
                count++;
                if (count % 5000 == 0) {
                    System.out.println("Allocated, resized and deallocated " + count + " blocks without issues");
                }
            }
            for(int i = 0; i < amount; i++) {
                Allocator.instance.free(addressbuffer[i]);
            }
        }
    }


    private static void ensureAllocated(boolean condition, long address, int size) {
        if (Allocator.instance.isAccessible(address, size) != condition)
            throw new AllocatorException("Your allocator does not show the desired behaviour. Expected '" + address + "' to be " + (condition ? "allocated." : "free."));
    }
}
