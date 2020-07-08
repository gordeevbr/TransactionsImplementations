package gordeevbr.transactions.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal currentBalance;

    private final BigDecimal withdrawal;

    private final long id;

    public InsufficientBalanceException(final BigDecimal currentBalance, final BigDecimal withdrawal, final long id) {
        super();
        this.currentBalance = currentBalance;
        this.withdrawal = withdrawal;
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Insufficient funds on account " + id + ": current balance is " + currentBalance.toPlainString()
                + ", withdrawal amount is " + withdrawal.toPlainString() + ".";
    }
}
