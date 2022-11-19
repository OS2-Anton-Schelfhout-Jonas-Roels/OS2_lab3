package BankingSimulation;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Transferer extends Thread {
    private static Random random = ThreadLocalRandom.current();
    private Bank bank;

    Transferer(Bank bank) {
        this.bank = bank;
    }

    @Override
    public void run() {
        while (true) {
            int from = 0;
            int to = 0;
            while (from == to) {
                from = random.nextInt(bank.numAccounts());
                to = random.nextInt(bank.numAccounts());
            }
            while (bank.transfer(from, to, random.nextInt(200)) == false);

            // network I/O generally has some delay, sleep a bit
            try {
                // sleeping for 1µs simulates a 1,000,000 transactions/s connection
                Thread.sleep(0, 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}