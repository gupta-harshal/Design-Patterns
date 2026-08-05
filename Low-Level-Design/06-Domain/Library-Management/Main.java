import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// ============================================================
// 0. DOMAIN ERRORS
// ============================================================

class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}

// ============================================================
// 1. CATALOG ENTITIES
// ============================================================

// Book = the bibliographic record (what you search for).
class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final String subject;

    public Book(String isbn, String title, String author, String subject) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.subject = subject;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "\"" + title + "\" by " + author + " [" + isbn + "]";
    }
}

enum BookItemStatus {
    AVAILABLE,
    LOANED,
    RESERVED,
    LOST
}

// BookItem = one physical copy on a shelf (what you actually borrow).
class BookItem {
    private final String barcode;
    private final Book book;
    private BookItemStatus status;
    private String reservedForMemberId;

    public BookItem(String barcode, Book book) {
        this.barcode = barcode;
        this.book = book;
        this.status = BookItemStatus.AVAILABLE;
    }

    public String getBarcode() {
        return barcode;
    }

    public Book getBook() {
        return book;
    }

    public BookItemStatus getStatus() {
        return status;
    }

    public void setStatus(BookItemStatus status) {
        this.status = status;
    }

    public String getReservedForMemberId() {
        return reservedForMemberId;
    }

    public void setReservedForMemberId(String memberId) {
        this.reservedForMemberId = memberId;
    }

    @Override
    public String toString() {
        return barcode + " -> " + book + " (" + status + ")";
    }
}

// ============================================================
// 2. MEMBER + LOAN
// ============================================================

class Member {
    private final String id;
    private final String name;
    private final List<Loan> activeLoans = new ArrayList<>();
    private double outstandingFine;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Loan> getActiveLoans() {
        return Collections.unmodifiableList(activeLoans);
    }

    public int activeLoanCount() {
        return activeLoans.size();
    }

    public void addLoan(Loan loan) {
        activeLoans.add(loan);
    }

    public void removeLoan(Loan loan) {
        activeLoans.remove(loan);
    }

    public double getOutstandingFine() {
        return outstandingFine;
    }

    public void addFine(double amount) {
        outstandingFine += amount;
    }

    public void payFine(double amount) {
        outstandingFine = Math.max(0.0, outstandingFine - amount);
    }
}

// Loan = the association record between one copy and one member for a period.
class Loan {
    private final String barcode;
    private final String memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private double fineCharged;

    public Loan(String barcode, String memberId, LocalDate issueDate, LocalDate dueDate) {
        this.barcode = barcode;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineCharged() {
        return fineCharged;
    }

    public void setFineCharged(double fineCharged) {
        this.fineCharged = fineCharged;
    }

    public boolean isActive() {
        return returnDate == null;
    }

    @Override
    public String toString() {
        return "Loan{" + barcode + ", member=" + memberId + ", due=" + dueDate + "}";
    }
}

// ============================================================
// 3. FINE POLICY (Strategy)
// ============================================================

interface FinePolicy {
    double calculate(LocalDate dueDate, LocalDate returnDate);
}

// Simple flat per-day charge on days beyond the due date.
class PerDayFinePolicy implements FinePolicy {
    private final double ratePerDay;

    public PerDayFinePolicy(double ratePerDay) {
        this.ratePerDay = ratePerDay;
    }

    @Override
    public double calculate(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (daysLate <= 0) {
            return 0.0;
        }
        return daysLate * ratePerDay;
    }
}

// ============================================================
// 4. CATALOG (search indexes)
// ============================================================

class Catalog {
    private final Map<String, Book> booksByIsbn = new HashMap<>();
    private final Map<String, List<BookItem>> copiesByIsbn = new HashMap<>();
    private final Map<String, Set<String>> isbnByTitleWord = new HashMap<>();
    private final Map<String, Set<String>> isbnByAuthor = new HashMap<>();

    public void addBook(Book book) {
        if (booksByIsbn.containsKey(book.getIsbn())) {
            throw new LibraryException("Book already in catalog: " + book.getIsbn());
        }
        booksByIsbn.put(book.getIsbn(), book);
        copiesByIsbn.put(book.getIsbn(), new ArrayList<>());
        for (String word : normalize(book.getTitle()).split("\\s+")) {
            isbnByTitleWord.computeIfAbsent(word, k -> new LinkedHashSet<>()).add(book.getIsbn());
        }
        isbnByAuthor.computeIfAbsent(normalize(book.getAuthor()), k -> new LinkedHashSet<>())
                .add(book.getIsbn());
    }

    public BookItem addCopy(String isbn, String barcode) {
        Book book = booksByIsbn.get(isbn);
        if (book == null) {
            throw new LibraryException("Unknown ISBN: " + isbn);
        }
        BookItem item = new BookItem(barcode, book);
        copiesByIsbn.get(isbn).add(item);
        return item;
    }

    public List<BookItem> copiesOf(String isbn) {
        return copiesByIsbn.getOrDefault(isbn, Collections.emptyList());
    }

    public Optional<Book> searchByIsbn(String isbn) {
        return Optional.ofNullable(booksByIsbn.get(isbn));
    }

    // Word index gives O(1) candidate lookup instead of scanning every book.
    public List<Book> searchByTitle(String query) {
        Set<String> hits = new LinkedHashSet<>();
        for (String word : normalize(query).split("\\s+")) {
            hits.addAll(isbnByTitleWord.getOrDefault(word, Collections.emptySet()));
        }
        return toBooks(hits);
    }

    public List<Book> searchByAuthor(String author) {
        return toBooks(isbnByAuthor.getOrDefault(normalize(author), Collections.emptySet()));
    }

    private List<Book> toBooks(Set<String> isbns) {
        List<Book> result = new ArrayList<>();
        for (String isbn : isbns) {
            result.add(booksByIsbn.get(isbn));
        }
        return result;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

// ============================================================
// 5. LIBRARY SERVICE (orchestration + rules)
// ============================================================

class LibraryService {
    private static final int MAX_LOANS_PER_MEMBER = 3;
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double MAX_ALLOWED_FINE = 20.0;

    private final Catalog catalog;
    private final FinePolicy finePolicy;
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, BookItem> itemsByBarcode = new HashMap<>();
    private final Map<String, Loan> activeLoanByBarcode = new HashMap<>();
    private final Map<String, Deque<String>> holdQueueByBarcode = new HashMap<>();
    private final List<Loan> loanHistory = new ArrayList<>();

    public LibraryService(Catalog catalog, FinePolicy finePolicy) {
        this.catalog = catalog;
        this.finePolicy = finePolicy;
    }

    public void registerMember(Member member) {
        members.put(member.getId(), member);
    }

    public BookItem addCopy(String isbn, String barcode) {
        BookItem item = catalog.addCopy(isbn, barcode);
        itemsByBarcode.put(barcode, item);
        return item;
    }

    public Loan checkout(String memberId, String barcode, LocalDate today) {
        Member member = requireMember(memberId);
        BookItem item = requireItem(barcode);

        if (item.getStatus() == BookItemStatus.LOANED) {
            throw new LibraryException("Copy " + barcode + " is already loaned out.");
        }
        if (item.getStatus() == BookItemStatus.LOST) {
            throw new LibraryException("Copy " + barcode + " is marked LOST.");
        }
        // A RESERVED copy is only checkoutable by the member who holds the reservation.
        if (item.getStatus() == BookItemStatus.RESERVED
                && !memberId.equals(item.getReservedForMemberId())) {
            throw new LibraryException("Copy " + barcode + " is reserved for another member.");
        }
        if (member.activeLoanCount() >= MAX_LOANS_PER_MEMBER) {
            throw new LibraryException(member.getName() + " already holds the max "
                    + MAX_LOANS_PER_MEMBER + " books.");
        }
        if (member.getOutstandingFine() > MAX_ALLOWED_FINE) {
            throw new LibraryException(member.getName() + " has unpaid fines of "
                    + member.getOutstandingFine());
        }

        Loan loan = new Loan(barcode, memberId, today, today.plusDays(LOAN_PERIOD_DAYS));
        item.setStatus(BookItemStatus.LOANED);
        item.setReservedForMemberId(null);
        activeLoanByBarcode.put(barcode, loan);
        loanHistory.add(loan);
        member.addLoan(loan);
        return loan;
    }

    public double returnItem(String barcode, LocalDate today) {
        Loan loan = activeLoanByBarcode.remove(barcode);
        if (loan == null) {
            throw new LibraryException("Copy " + barcode + " is not currently on loan.");
        }
        Member member = requireMember(loan.getMemberId());
        BookItem item = requireItem(barcode);

        double fine = finePolicy.calculate(loan.getDueDate(), today);
        loan.setReturnDate(today);
        loan.setFineCharged(fine);
        member.removeLoan(loan);
        member.addFine(fine);

        // The returned copy goes to the next member waiting in the hold queue.
        Deque<String> queue = holdQueueByBarcode.get(barcode);
        if (queue != null && !queue.isEmpty()) {
            String nextMemberId = queue.pollFirst();
            item.setStatus(BookItemStatus.RESERVED);
            item.setReservedForMemberId(nextMemberId);
        } else {
            item.setStatus(BookItemStatus.AVAILABLE);
            item.setReservedForMemberId(null);
        }
        return fine;
    }

    public void reserve(String memberId, String barcode) {
        requireMember(memberId);
        BookItem item = requireItem(barcode);
        if (item.getStatus() == BookItemStatus.AVAILABLE) {
            item.setStatus(BookItemStatus.RESERVED);
            item.setReservedForMemberId(memberId);
            return;
        }
        Deque<String> queue = holdQueueByBarcode.computeIfAbsent(barcode, k -> new ArrayDeque<>());
        if (queue.contains(memberId)) {
            throw new LibraryException("Member " + memberId + " already holds a reservation here.");
        }
        queue.addLast(memberId);
    }

    // Convenience: pick any shelf-ready copy of a title.
    public Optional<BookItem> findAvailableCopy(String isbn) {
        for (BookItem item : catalog.copiesOf(isbn)) {
            if (item.getStatus() == BookItemStatus.AVAILABLE) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public List<Loan> getLoanHistory() {
        return Collections.unmodifiableList(loanHistory);
    }

    private Member requireMember(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new LibraryException("Unknown member: " + memberId);
        }
        return member;
    }

    private BookItem requireItem(String barcode) {
        BookItem item = itemsByBarcode.get(barcode);
        if (item == null) {
            throw new LibraryException("Unknown barcode: " + barcode);
        }
        return item;
    }
}

// ============================================================
// 6. CLIENT DEMO
// ============================================================

public class Main {
    public static void main(String[] args) {
        Catalog catalog = new Catalog();
        catalog.addBook(new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Programming"));
        catalog.addBook(new Book("978-0132350884", "Clean Code", "Robert Martin", "Programming"));
        catalog.addBook(new Book("978-0201633610", "Design Patterns", "Gang of Four", "Programming"));

        LibraryService library = new LibraryService(catalog, new PerDayFinePolicy(2.0));
        library.addCopy("978-0134685991", "EJ-COPY-1");
        library.addCopy("978-0134685991", "EJ-COPY-2");
        library.addCopy("978-0132350884", "CC-COPY-1");
        library.addCopy("978-0201633610", "DP-COPY-1");

        Member alice = new Member("M1", "Alice");
        Member bob = new Member("M2", "Bob");
        library.registerMember(alice);
        library.registerMember(bob);

        LocalDate day0 = LocalDate.of(2024, 5, 1);

        System.out.println("=== 1. SEARCH ===");
        System.out.println("by title 'java'   -> " + catalog.searchByTitle("java"));
        System.out.println("by author 'bloch' -> " + catalog.searchByAuthor("Joshua Bloch"));
        System.out.println("by isbn           -> " + catalog.searchByIsbn("978-0132350884").orElse(null));

        System.out.println();
        System.out.println("=== 2. CHECKOUT ===");
        Loan aliceLoan = library.checkout("M1", "EJ-COPY-1", day0);
        System.out.println("Alice borrowed EJ-COPY-1, due " + aliceLoan.getDueDate());

        System.out.println();
        System.out.println("=== 3. DOUBLE CHECKOUT IS REJECTED ===");
        try {
            library.checkout("M2", "EJ-COPY-1", day0);
        } catch (LibraryException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        System.out.println("Bob takes the other copy instead: "
                + library.findAvailableCopy("978-0134685991").map(BookItem::getBarcode).orElse("none"));
        library.checkout("M2", "EJ-COPY-2", day0);

        System.out.println();
        System.out.println("=== 4. RESERVATION / HOLD QUEUE ===");
        library.reserve("M2", "EJ-COPY-1");
        System.out.println("Bob joined the hold queue for EJ-COPY-1");

        System.out.println();
        System.out.println("=== 5. RETURN + FINE ===");
        LocalDate onTime = day0.plusDays(10);
        System.out.println("Bob returns EJ-COPY-2 on day 10, fine = "
                + library.returnItem("EJ-COPY-2", onTime));

        LocalDate late = day0.plusDays(18);
        double fine = library.returnItem("EJ-COPY-1", late);
        System.out.println("Alice returns EJ-COPY-1 on day 18 (4 days late), fine = " + fine);
        System.out.println("Alice outstanding fine = " + alice.getOutstandingFine());

        System.out.println();
        System.out.println("=== 6. HOLD IS HONOURED ON RETURN ===");
        try {
            library.checkout("M1", "EJ-COPY-1", late);
        } catch (LibraryException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        Loan bobLoan = library.checkout("M2", "EJ-COPY-1", late);
        System.out.println("Bob claimed his reserved copy, due " + bobLoan.getDueDate());

        System.out.println();
        System.out.println("=== 7. LOAN HISTORY ===");
        for (Loan loan : library.getLoanHistory()) {
            System.out.println("  " + loan + " returned=" + loan.getReturnDate()
                    + " fine=" + loan.getFineCharged());
        }
    }
}
