package gordeevbr.transactions;

import gordeevbr.transactions.exceptions.InsufficientBalanceException;
import gordeevbr.transactions.exceptions.InvalidAccountException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation #2.
 *
 * An implementation of a transaction based on using CAS twice.
 *
 * This implementation does not use locks, which is an advantage over the first implementation. The fact that we do CAS
 * more than once means that the transfer is not an atomic transaction and other transfers would be able to observe an
 * intermediate state (when currency was withdrawn from one account and not yet assigned to the other one). This also
 * means that if the JVM would be stop in between, the end state would be invalid. Nonetheless, if we put these
 * assumptions aside, this is a sufficient implementation.
 *
 * @author gordeevbr
 */
public final class DoubleAtomicTransferService implements TransferService {

    private final AtomicLong accountIdGenerator = new AtomicLong(0);

    private final Map<Long, AtomicReference<BigDecimal>> accounts = new HashMap<>();

    @Override
    public void transfer(final long fromAccount, final long toAccount, final BigDecimal amount) {
        final AtomicReference<BigDecimal> from = getAccount(fromAccount);
        final AtomicReference<BigDecimal> to = getAccount(toAccount);
        boolean committed = false;
        while (!committed) {
            final BigDecimal actualFrom = from.get();
            final BigDecimal resultFrom = actualFrom.subtract(amount);
            if (resultFrom.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(actualFrom, amount, fromAccount);
            }
            committed = from.compareAndSet(actualFrom, resultFrom);
        }

        committed = false;
        while (!committed) {
            final BigDecimal actualTo = to.get();
            final BigDecimal resultTo = actualTo.add(amount);
            committed = from.compareAndSet(actualTo, resultTo);
        }
    }

    @Override
    public BigDecimal getBalance(final long account) {
        return accounts.get(account).get();
    }

    @Override
    public long openAccount(final BigDecimal initialBalance) {
        final long id = accountIdGenerator.getAndIncrement();
        accounts.put(id, new AtomicReference<>(initialBalance));
        return id;
    }

    private AtomicReference<BigDecimal> getAccount(final long id) {
        final AtomicReference<BigDecimal> reference = accounts.get(id);
        if (reference == null) {
            throw new InvalidAccountException(id);
        }
        return reference;
    }
}
