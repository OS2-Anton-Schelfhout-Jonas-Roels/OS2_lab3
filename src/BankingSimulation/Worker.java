package BankingSimulation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Worker extends Thread {
    private Bank bank;
    private Queue<Transaction> buffer = new LinkedList<>();
    private List<Transaction> localBuffer = new ArrayList<>();

    public Worker(Bank bank) {
        this.bank = bank;
    }

    /* Called by Transferer threads during load balancing */
    public void addToBuffer(Transaction transaction) {
        synchronized (buffer) {
            buffer.add(transaction);
            buffer.notify(); // there is only one thread waiting on this
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (buffer) {
                while (buffer.isEmpty())
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                
                // During high load, there can be more than one new
                // transaction in the buffer at a time. Instead of 
                // taking just one transaction and releasing the lock,
                // we add all new transactions to a local worklist,
                // which we then processing without holding any locks.
                //
                // this drastically reduces lock contention with the Transferers.
                localBuffer.addAll(buffer);
                buffer.clear();

                // note: if the Java util lib supported fast, destructive List concatenation,
                // this `addAll` operation could be a `moveAll`, and we wouldnt
                // need to copy the transactions around here. alas.
            }

            for (var transaction : localBuffer) {
                transaction.resize();
                bank.process(transaction);
                transaction.release();
            }
            localBuffer.clear();
        }
    }
}
