package gordeevbr.transactions.exceptions;

public class InvalidAccountException extends RuntimeException {

    private final long id;

    public InvalidAccountException(final long id) {
        super();
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Invalid account id: " + id + ".";
    }
}
