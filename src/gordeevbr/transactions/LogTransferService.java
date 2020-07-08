package gordeevbr.transactions;

import gordeevbr.transactions.exceptions.InsufficientBalanceException;
import gordeevbr.transactions.exceptions.InvalidAccountException;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation #3.
 *
 * An implementation of a transaction using a log.
 *
 * This implementation is inspired by WAL used in pretty much any RDBMS there is. This allows for using a single CAS
 * operation (that is, append a transaction to log). This removes all the drawbacks of previous implementations,
 * enabling valid ACID transactions as far as they can be implemented in a scope of this example. The only and biggest
 * drawback so far would
 *
 * @author gordeevbr
 */
public class LogTransferService implements TransferService {

    private final AtomicReference<AppliedTransaction> logHead = new AtomicReference<>();

    private final Map<Long, AtomicReference<AccountInformation>> accounts = new HashMap<>();

    private final AtomicLong accountIdGenerator = new AtomicLong(0);

    private final AtomicLong transactionIdGenerator = new AtomicLong(0);

    @Override
    public void transfer(final long fromAccount, final long toAccount, final BigDecimal amount) {
        AccountInformation fromAccountInformation = getAccount(fromAccount);
        AccountInformation toAccountInformation = getAccount(toAccount);

        boolean committed = false;
        while (!committed) {
            final AppliedTransaction localLogHead = logHead.get();
            fromAccountInformation = rewind(fromAccountInformation, localLogHead);
            toAccountInformation = rewind(toAccountInformation, localLogHead);
            final BigDecimal fromBalance = fromAccountInformation.getBalance();
            final BigDecimal resultFromBalance = fromBalance.subtract(amount);
            if (resultFromBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(fromBalance, amount, fromAccount);
            }
            final AppliedTransaction newLogHead = new AppliedTransaction(
                    transactionIdGenerator.getAndIncrement(),
                    fromAccount,
                    toAccount,
                    amount,
                    localLogHead
            );
            committed = logHead.compareAndSet(localLogHead, newLogHead);
        }
    }

    @Override
    public BigDecimal getBalance(final long account) {
        return getAccount(account).getBalance();
    }

    @Override
    public long openAccount(final BigDecimal initialBalance) {
        final long id = accountIdGenerator.getAndIncrement();
        final AccountInformation newAccount = new AccountInformation(null, initialBalance, id);
        accounts.put(id, new AtomicReference<>(newAccount));
        return id;
    }

    private AccountInformation getAccount(final long id) {
        final AtomicReference<AccountInformation> reference = accounts.get(id);
        if (reference == null) {
            throw new InvalidAccountException(id);
        }

        return rewind(reference.get(), logHead.get());
    }

    private AccountInformation rewind(
            final AccountInformation accountInformation,
            final AppliedTransaction localLogHead
    ) {
        final Deque<BigDecimal> accountLog = new ArrayDeque<>();
        AppliedTransaction localHead = logHead.get();
        if (localHead == null) {
            return accountInformation;
        }
        final long finalTransaction = localHead.getTransactionId();
        while (localHead != null && localHead.getTransactionId() != accountInformation.getLastTransactionId()) {
            if (localHead.getFrom() == accountInformation.getAccountId()) {
                accountLog.push(localHead.getAmount().negate());
            }
            if (localHead.getTo() == accountInformation.getAccountId()) {
                accountLog.push(localHead.getAmount());
            }
            localHead = localHead.getPrevious();
        }
        BigDecimal latestBalance = accountInformation.getBalance();
        while (!accountLog.isEmpty()) {
            latestBalance = latestBalance.add(accountLog.pop());
        }
        return new AccountInformation(finalTransaction, latestBalance, accountInformation.getAccountId());
    }

    private static final class AccountInformation {

        private final Long lastTransactionId;

        private final BigDecimal balance;

        private final long accountId;

        public AccountInformation(final Long lastTransactionId, final BigDecimal balance, final long accountId) {
            this.lastTransactionId = lastTransactionId;
            this.balance = balance;
            this.accountId = accountId;
        }

        public Long getLastTransactionId() {
            return lastTransactionId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public long getAccountId() {
            return accountId;
        }
    }

    private static final class AppliedTransaction {

        private final long transactionId;

        private final long from;

        private final long to;

        private final BigDecimal amount;

        private final AppliedTransaction previous;

        public AppliedTransaction(
                final long transactionId,
                final long from,
                final long to,
                final BigDecimal amount,
                final AppliedTransaction previous
        ) {
            this.transactionId = transactionId;
            this.from = from;
            this.to = to;
            this.amount = amount;
            this.previous = previous;
        }

        public long getTransactionId() {
            return transactionId;
        }

        public long getFrom() {
            return from;
        }

        public long getTo() {
            return to;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public AppliedTransaction getPrevious() {
            return previous;
        }
    }

}
