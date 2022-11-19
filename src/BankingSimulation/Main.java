package BankingSimulation;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        var bank = new Bank();
        ArrayList<Transferer> transferers = new ArrayList<>();
        for (int i = 0; i < 1000; i++)
            transferers.add(new Transferer(bank));
        for (var transferer : transferers)
            transferer.start();
    }
}
