import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Card {
    final String number;
    final String accountId;

    Card(String number, String accountId) {
        this.number = number;
        this.accountId = accountId;
    }
}

interface BankService {
    boolean authenticate(Card card, String pin);
    long getBalanceCents(Card card);
    boolean withdraw(Card card, long amountCents);
}

class InMemoryBank implements BankService {
    private final Map<String, String> pinByAccount = new LinkedHashMap<>();
    private final Map<String, Long> balanceByAccount = new LinkedHashMap<>();

    void enroll(String accountId, String pin, long balanceCents) {
        pinByAccount.put(accountId, pin);
        balanceByAccount.put(accountId, balanceCents);
    }

    @Override
    public boolean authenticate(Card card, String pin) {
        return pin.equals(pinByAccount.get(card.accountId));
    }

    @Override
    public long getBalanceCents(Card card) {
        return balanceByAccount.getOrDefault(card.accountId, 0L);
    }

    @Override
    public boolean withdraw(Card card, long amountCents) {
        long bal = getBalanceCents(card);
        if (bal < amountCents) {
            return false;
        }
        balanceByAccount.put(card.accountId, bal - amountCents);
        return true;
    }
}

class CashDispenser {
    // denomination rupees → count of notes in cassette
    private final NavigableInventory inventory;

    CashDispenser(NavigableInventory inventory) {
        this.inventory = inventory;
    }

    /** Plan first. If exact amount impossible, return null and mutate nothing. */
    Map<Integer, Integer> plan(int amountRupees) {
        Map<Integer, Integer> plan = new LinkedHashMap<>();
        int remaining = amountRupees;
        for (int denom : inventory.denomsDescending()) {
            int available = inventory.count(denom);
            int need = remaining / denom;
            int use = Math.min(need, available);
            if (use > 0) {
                plan.put(denom, use);
                remaining -= use * denom;
            }
        }
        if (remaining != 0) {
            return null;
        }
        return plan;
    }

    boolean dispense(int amountRupees) {
        Map<Integer, Integer> plan = plan(amountRupees);
        if (plan == null) {
            return false;
        }
        inventory.commit(plan);
        System.out.println("Dispensed notes: " + plan);
        return true;
    }
}

class NavigableInventory {
    private final Map<Integer, Integer> notes = new LinkedHashMap<>();

    NavigableInventory add(int denom, int count) {
        notes.put(denom, count);
        return this;
    }

    List<Integer> denomsDescending() {
        List<Integer> keys = new ArrayList<>(notes.keySet());
        keys.sort((a, b) -> Integer.compare(b, a));
        return keys;
    }

    int count(int denom) {
        return notes.getOrDefault(denom, 0);
    }

    void commit(Map<Integer, Integer> plan) {
        for (Map.Entry<Integer, Integer> e : plan.entrySet()) {
            notes.put(e.getKey(), count(e.getKey()) - e.getValue());
        }
    }
}

enum AtmSessionState {
    READY, CARD_IN, AUTHENTICATED
}

class ATM {
    private final BankService bank;
    private final CashDispenser dispenser;
    private AtmSessionState state = AtmSessionState.READY;
    private Card card;
    private int pinAttempts;

    ATM(BankService bank, CashDispenser dispenser) {
        this.bank = bank;
        this.dispenser = dispenser;
    }

    void insertCard(Card card) {
        if (state != AtmSessionState.READY) {
            System.out.println("ATM busy");
            return;
        }
        this.card = card;
        this.pinAttempts = 0;
        state = AtmSessionState.CARD_IN;
        System.out.println("Card inserted: " + card.number);
    }

    void enterPin(String pin) {
        if (state != AtmSessionState.CARD_IN) {
            System.out.println("Insert card first");
            return;
        }
        if (bank.authenticate(card, pin)) {
            state = AtmSessionState.AUTHENTICATED;
            pinAttempts = 0;
            System.out.println("PIN OK");
        } else {
            pinAttempts++;
            System.out.println("Wrong PIN (" + pinAttempts + "/3)");
            if (pinAttempts >= 3) {
                System.out.println("Card blocked — ejecting");
                eject();
            }
        }
    }

    void balance() {
        if (state != AtmSessionState.AUTHENTICATED) {
            System.out.println("Authenticate first");
            return;
        }
        System.out.println("Balance: " + bank.getBalanceCents(card) + " cents");
    }

    void withdrawRupees(int amountRupees) {
        if (state != AtmSessionState.AUTHENTICATED) {
            System.out.println("Authenticate first");
            return;
        }
        long amountCents = amountRupees * 100L;
        Map<Integer, Integer> plan = dispenser.plan(amountRupees);
        if (plan == null) {
            System.out.println("ATM cannot assemble notes for " + amountRupees);
            return;
        }
        if (!bank.withdraw(card, amountCents)) {
            System.out.println("Bank declined (insufficient funds)");
            return;
        }
        if (!dispenser.dispense(amountRupees)) {
            // Should not happen after a successful plan; real systems reverse the bank debit.
            System.out.println("Dispense failed after bank withdraw — needs reversal");
            return;
        }
        System.out.println("Withdraw OK. New balance: " + bank.getBalanceCents(card) + " cents");
    }

    void eject() {
        System.out.println("Card ejected");
        card = null;
        state = AtmSessionState.READY;
        pinAttempts = 0;
    }
}

public class Main {
    public static void main(String[] args) {
        InMemoryBank bank = new InMemoryBank();
        bank.enroll("ACC1", "1234", 50_000_00); // 50,000.00 rupees in cents

        NavigableInventory inv = new NavigableInventory()
                .add(2000, 10)
                .add(500, 10)
                .add(200, 10)
                .add(100, 10);
        ATM atm = new ATM(bank, new CashDispenser(inv));

        System.out.println("-- Wrong PIN then success --");
        atm.insertCard(new Card("4111", "ACC1"));
        atm.enterPin("0000");
        atm.enterPin("1234");
        atm.balance();

        System.out.println("\n-- Withdraw 3800 --");
        atm.withdrawRupees(3800);

        System.out.println("\n-- Impossible mix (e.g. 30 if min note 100) --");
        atm.withdrawRupees(30);

        atm.eject();
    }
}
