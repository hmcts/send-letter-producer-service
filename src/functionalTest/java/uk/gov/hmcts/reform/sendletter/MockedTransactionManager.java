package uk.gov.hmcts.reform.sendletter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

@Component
public class MockedTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return null;
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        //This method is intentionally left empty
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        //This method is intentionally left empty
    }
}
