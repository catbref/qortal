package org.qortal.repository.hsqldb.transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.qortal.data.transaction.BaseTransactionData;
import org.qortal.data.transaction.MessageTransactionData;
import org.qortal.data.transaction.TransactionData;
import org.qortal.repository.DataException;
import org.qortal.repository.hsqldb.HSQLDBRepository;
import org.qortal.repository.hsqldb.HSQLDBSaver;

public class HSQLDBMessageTransactionRepository extends HSQLDBTransactionRepository {

	public HSQLDBMessageTransactionRepository(HSQLDBRepository repository) {
		this.repository = repository;
	}

	TransactionData fromBase(BaseTransactionData baseTransactionData) throws DataException {
		String sql = "SELECT version, recipient, is_text, is_encrypted, amount, asset_id, data FROM MessageTransactions WHERE signature = ?";

		try (ResultSet resultSet = this.repository.checkedExecute(sql, baseTransactionData.getSignature())) {
			if (resultSet == null)
				return null;

			int version = resultSet.getInt(1);
			String recipient = resultSet.getString(2);
			boolean isText = resultSet.getBoolean(3);
			boolean isEncrypted = resultSet.getBoolean(4);
			long amount = resultSet.getLong(5);

			// Special null-checking for asset ID
			Long assetId = resultSet.getLong(6);
			if (assetId == 0 && resultSet.wasNull())
				assetId = null;

			byte[] data = resultSet.getBytes(7);

			return new MessageTransactionData(baseTransactionData, version, recipient, amount, assetId, data, isText, isEncrypted);
		} catch (SQLException e) {
			throw new DataException("Unable to fetch message transaction from repository", e);
		}
	}

	@Override
	public void save(TransactionData transactionData) throws DataException {
		MessageTransactionData messageTransactionData = (MessageTransactionData) transactionData;

		HSQLDBSaver saveHelper = new HSQLDBSaver("MessageTransactions");

		saveHelper.bind("signature", messageTransactionData.getSignature()).bind("version", messageTransactionData.getVersion())
				.bind("sender", messageTransactionData.getSenderPublicKey()).bind("recipient", messageTransactionData.getRecipient())
				.bind("is_text", messageTransactionData.isText()).bind("is_encrypted", messageTransactionData.isEncrypted())
				.bind("amount", messageTransactionData.getAmount()).bind("asset_id", messageTransactionData.getAssetId())
				.bind("data", messageTransactionData.getData());

		try {
			saveHelper.execute(this.repository);
		} catch (SQLException e) {
			throw new DataException("Unable to save message transaction into repository", e);
		}
	}

}
