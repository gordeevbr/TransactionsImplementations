package gordeevbr.transactions;

import gordeevbr.transactions.exceptions.InsufficientBalanceException;
import gordeevbr.transactions.exceptions.InvalidAccountException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation #1.
 *
 * This is the most simplistic and naive implementation out of all others.
 *
 * This implementation uses locks for every account (potential row in the database) to ensure atomicity of transactions.
 * Hypothetically, it does not ensure consistency of thereof, because if the JVM would be to stop in the middle of a
 * transfer, the account balance registers would be in an inconsistent state.
 *
 * @author gordeevbr
 */
public final class PessimisticLockTransferService implements TransferService {

    private final AtomicLong accountIdGenerator = new AtomicLong(0);

    private final Map<Long, AccountInformation> accounts = new HashMap<>();

    @Override
    public void transfer(final long fromAccount, final long toAccount, final BigDecimal amount) {
        final AccountInformation fromAccountInformation = getAccount(fromAccount);
        final AccountInformation toAccountInformation = getAccount(toAccount);

        final AccountInformation lesser = fromAccount > toAccount ? toAccountInformation : fromAccountInformation;
        final AccountInformation greater = fromAccount > toAccount ? fromAccountInformation : toAccountInformation;

        synchronized (lesser) {
            synchronized (greater) {
                final BigDecimal currentBalance = fromAccountInformation.getBalance();
                final BigDecimal fromAccountBalance = currentBalance.subtract(amount);
                if (fromAccountBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InsufficientBalanceException(currentBalance, amount, fromAccount);
                }
                fromAccountInformation.setBalance(fromAccountBalance);
                toAccountInformation.setBalance(toAccountInformation.getBalance().add(amount));
            }
        }
    }

    @Override
    public BigDecimal getBalance(final long account) {
        return getAccount(account).getBalance();
    }

    @Override
    public long openAccount(final BigDecimal initialBalance) {
        final AccountInformation newAccount =
                new AccountInformation(accountIdGenerator.getAndIncrement(), initialBalance);
        accounts.put(newAccount.getId(), newAccount);
        return newAccount.getId();
    }

    private AccountInformation getAccount(final long id) {
        final AccountInformation accountInformation = accounts.get(id);
        if (accountInformation == null) {
            throw new InvalidAccountException(id);
        }
        return accountInformation;
    }

    private static final class AccountInformation {

        private final long id;

        private BigDecimal balance;

        public AccountInformation(final long id, final BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }

        public long getId() {
            return id;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }
}
