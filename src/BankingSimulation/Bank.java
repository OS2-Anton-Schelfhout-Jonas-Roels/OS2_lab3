package BankingSimulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

class Account {
    int accountNumber;
    int saldo;

    public Account(int accountNumber) {
        this.accountNumber = accountNumber;
        this.saldo = 10000;
    }
}

public class Bank {
    final private List<Account> accounts;
    final private List<Worker> workers;
    private AtomicLong currentWorkerIdx = new AtomicLong(0);
    private AtomicLong numTransactionsProcessed = new AtomicLong(0);
    private AtomicLong numTransactionsInserted = new AtomicLong(0);
    final private Logger logger = new Logger(this);

    public Bank () {
        accounts = Collections.unmodifiableList(new ArrayList<>() {{
            for (int i = 0; i < 1000; i++)
                add(new Account(i));
        }});
        workers = Collections.unmodifiableList(new ArrayList<>() {{
            for (int i = 0; i < 100; i++)
                add(new Worker(Bank.this));
        }});
        for (var worker : workers)
            worker.start();
        logger.start();
    }

    public int numAccounts() {
        // doesnt need to be synchronized: accounts.size() wont
        // change over the duration of the program
        // this is enforced by using Collections.unmodifiableList
        return accounts.size();
    }

    public long getNumTransactionsProcessed() {
        // this is racy (bcs unsynchronized), but that's okay:
        // it's called by Logger to get a rough pinpoint of the 
        // number of transactions currently processed
        return numTransactionsProcessed.get();
    }

    public long getNumTransactionsInserted() {
        // same as above
        return numTransactionsInserted.get();
    }

    /* Called by Transferer threads to insert Transactions in the buffer */
    public boolean transfer(int from, int to, int amount) {
        var transaction = new Transaction(from, to, amount);
        // random transaction failure!
        if (Math.random() < 0.05) {
            transaction.release();
            return false;
        }

        // we implement load balancing here, with an atomic index
        // we treat the array of workers like a ring buffer, and 
        // iteratively distribute incoming transactions over their
        // respective internal, private buffers.
        //
        // this massively reduces lock contention: 
        //  * Only every `workers.size()`-th transfer can contend
        //  * contention will basically only happen with a worker
        //      holding the lock. 
        var idxLong = currentWorkerIdx.getAndIncrement() % workers.size();
        int idx = (int) idxLong;
        assert idxLong == idx;
        var worker = workers.get(idx);
        worker.addToBuffer(transaction);
        numTransactionsInserted.incrementAndGet();
        return true;
    }

    /* Called by Worker threads to actually process transactions */
    public void process(Transaction transaction) {
        if (transaction.from == transaction.to)
            throw new RuntimeException("stop sending money to yourself");

        // we lock the involved accounts in an _absolute_ ordering in every thread
        // this bypasses the entire deadlock problem, as the locks are always 
        // acquired in the same order in every thread
        int lowest = Math.min(transaction.from, transaction.to);
        int highest = Math.max(transaction.from, transaction.to);
        synchronized (accounts.get(lowest)) {
            synchronized (accounts.get(highest)) {
                var from = accounts.get(transaction.from);
                var to = accounts.get(transaction.to);
                if (from.saldo - transaction.amount > 0) {
                    from.saldo -= transaction.amount;
                    to.saldo += transaction.amount;
                }
            }
        }

        numTransactionsProcessed.incrementAndGet();
    }
}
