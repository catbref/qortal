package org.qora.test;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.qora.account.Account;
import org.qora.asset.Asset;
import org.qora.block.Block;
import org.qora.block.GenesisBlock;
import org.qora.data.transaction.TransactionData;
import org.qora.repository.DataException;
import org.qora.repository.Repository;
import org.qora.repository.RepositoryFactory;
import org.qora.repository.RepositoryManager;
import org.qora.repository.hsqldb.HSQLDBRepositoryFactory;
import org.qora.transaction.Transaction;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

// Don't extend Common as we want an in-memory database
public class GenesisTests {

	public static final String connectionUrl = "jdbc:hsqldb:mem:db/blockchain;create=true";

	@BeforeAll
	public static void setRepository() throws DataException {
		RepositoryFactory repositoryFactory = new HSQLDBRepositoryFactory(connectionUrl);
		RepositoryManager.setRepositoryFactory(repositoryFactory);
	}

	@AfterAll
	public static void closeRepository() throws DataException {
		RepositoryManager.closeRepositoryFactory();
	}

	@Test
	public void testGenesisBlockTransactions() throws DataException {
		try (final Repository repository = RepositoryManager.getRepository()) {
			assertEquals(0, repository.getBlockRepository().getBlockchainHeight(), "Blockchain should be empty for this test");

			GenesisBlock block = GenesisBlock.getInstance(repository);

			assertNotNull(block);
			assertTrue(block.isSignatureValid());
			// Note: only true if blockchain is empty
			assertEquals(Block.ValidationResult.OK, block.isValid());

			List<Transaction> transactions = block.getTransactions();
			assertNotNull(transactions);

			for (Transaction transaction : transactions) {
				assertNotNull(transaction);

				TransactionData transactionData = transaction.getTransactionData();

				assertEquals(Transaction.TransactionType.GENESIS, transactionData.getType());
				assertTrue(transactionData.getFee().compareTo(BigDecimal.ZERO) == 0);
				assertNull(transactionData.getReference());
				assertNotNull(transactionData.getSignature());
				assertTrue(transaction.isSignatureValid());
				assertEquals(Transaction.ValidationResult.OK, transaction.isValid());
			}

			// Actually try to process genesis block onto empty blockchain
			block.process();
			repository.saveChanges();

			// Attempt to load first transaction directly from database
			TransactionData transactionData = repository.getTransactionRepository().fromSignature(transactions.get(0).getTransactionData().getSignature());
			assertNotNull(transactionData);

			assertEquals(Transaction.TransactionType.GENESIS, transactionData.getType());
			assertTrue(transactionData.getFee().compareTo(BigDecimal.ZERO) == 0);
			assertNull(transactionData.getReference());

			Transaction transaction = Transaction.fromData(repository, transactionData);
			assertNotNull(transaction);

			assertTrue(transaction.isSignatureValid());
			assertEquals(Transaction.ValidationResult.OK, transaction.isValid());

			// Check known balance
			Account testAccount = new Account(repository, "QegT2Ws5YjLQzEZ9YMzWsAZMBE8cAygHZN");
			BigDecimal testBalance = testAccount.getConfirmedBalance(Asset.QORA);
			BigDecimal expectedBalance = new BigDecimal("12606834").setScale(8);
			assertTrue(testBalance.compareTo(expectedBalance) == 0);
		}
	}

}