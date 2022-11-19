package BankingSimulation;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import Allocator.Allocator;
import Allocator.AllocatorException;

public class Transaction {
    private static final boolean ENABLE_ALLOCATOR = true;
    static Random random = ThreadLocalRandom.current();
    public static AtomicLong totalSize = new AtomicLong(0);
    public static AtomicLong numTransactions = new AtomicLong(0);
    public static AtomicLong numTransactionsBiggerThan2Pages = new AtomicLong(0);
    
    int from;
    int to;
    int amount;

    private Long address;
    private int size;

    Transaction(int from, int to, int amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;

        if (ENABLE_ALLOCATOR) {
            this.size = Math.max(1 + random.nextInt(48), 8);
            while (this.size < 5000 && Math.random() < 0.2)
                this.size *= 2;
            this.address = Allocator.instance.allocate(size);
            ensureAllocated(true);
            this.size *= 2;
            this.address = Allocator.instance.reAllocate(address, size);
            ensureAllocated(true);
            totalSize.getAndAdd(this.size + this.size/2);
            if (this.size >= 8192)
                numTransactionsBiggerThan2Pages.incrementAndGet();
        }
        numTransactions.incrementAndGet();
    }

    public void resize() {
        if (ENABLE_ALLOCATOR) {
            size = Math.max(16, Math.random() < 0.5 ? size/2 : size*2);
            address = Allocator.instance.reAllocate(address, size);
            ensureAllocated(true);
        }
    }

    public void release() {
        if (ENABLE_ALLOCATOR) {
            Allocator.instance.free(address);
            // ensureAllocated(false);
        }
    }

    private void ensureAllocated(boolean condition) {
        if (Allocator.instance.isAccessible(address, size) != condition)
            throw new AllocatorException("Your allocator does not show the desired behaviour. Expected '" + address + "' to be " + (condition ? "allocated." : "free."));
    }
}
