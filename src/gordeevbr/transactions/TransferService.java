package gordeevbr.transactions;

import java.math.BigDecimal;

/**
 * An interface for a service which allows transferring currency from one account to another in a transactional manner.
 * Our goal is to demonstrate on this example how we can handle transactions in real databases.
 *
 * @author gordeevbr
 */
public interface TransferService {

    public void transfer(final long fromAccount, final long toAccount, final BigDecimal amount);

    // This is mostly for debugging and testing.
    public BigDecimal getBalance(final long account);

    // This is mostly for debugging and testing.
    public long openAccount(final BigDecimal initialBalance);

}
