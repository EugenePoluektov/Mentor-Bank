package ru.mentorbank.backoffice.services.moneytransfer;

import java.util.GregorianCalendar;

import ru.mentorbank.backoffice.dao.OperationDao;
import ru.mentorbank.backoffice.dao.exception.OperationDaoException;
import ru.mentorbank.backoffice.model.Account;
import ru.mentorbank.backoffice.model.Operation;
import ru.mentorbank.backoffice.model.stoplist.JuridicalStopListRequest;
import ru.mentorbank.backoffice.model.stoplist.PhysicalStopListRequest;
import ru.mentorbank.backoffice.model.stoplist.StopListInfo;
import ru.mentorbank.backoffice.model.stoplist.StopListStatus;
import ru.mentorbank.backoffice.model.transfer.AccountInfo;
import ru.mentorbank.backoffice.model.transfer.JuridicalAccountInfo;
import ru.mentorbank.backoffice.model.transfer.PhysicalAccountInfo;
import ru.mentorbank.backoffice.model.transfer.TransferRequest;
import ru.mentorbank.backoffice.services.accounts.AccountService;
import ru.mentorbank.backoffice.services.moneytransfer.exceptions.TransferException;
import ru.mentorbank.backoffice.services.stoplist.StopListService;

public class MoneyTransferServiceBean implements MoneyTransferService {

	public static final String LOW_BALANCE_ERROR_MESSAGE = "Can not transfer money, because of low balance in the source account";
	private AccountService accountService;
	private StopListService stopListService;
	private OperationDao operationDao;

	public void transfer(TransferRequest request) throws TransferException {
		// Создаём новый экземпляр внутреннего класса, для того, чтобы можно
		// было хранить в состоянии объекта информацию по каждому запросу.
		// Так как MoneyTransferServiceBean конфигурируется как singleton
		// scoped, то в нём нельзя хранить состояние уровня запроса из-за
		// проблем параллельного доступа.
		new MoneyTransfer(request).transfer();
	}

	class MoneyTransfer {

		private TransferRequest request;
		private StopListInfo srcStopListInfo;
		private StopListInfo dstStopListInfo;

		public MoneyTransfer(TransferRequest request) {
			this.request = request;
		}

		public void transfer() throws TransferException {
			verifySrcBalance();
			initializeStopListInfo();
			saveOperation();
			if (isStopListInfoOK()) {
				transferDo();
				removeSuccessfulOperation();
			} else
				throw new TransferException("Невозможно сделать перевод. Необходимо ручное вмешательство.");
		}

		/**
		 * Если операция перевода прошла, то её нужно удалить из таблицы
		 * операций для ручного вмешательства
		 */
		private void removeSuccessfulOperation() {

		}

		private void initializeStopListInfo() {
			srcStopListInfo = getStopListInfo(request.getSrcAccount());
			dstStopListInfo = getStopListInfo(request.getDstAccount());
		}

		private void saveOperation() throws TransferException {
			Operation operation = new Operation();
			Account srcAccount = new Account();
			Account dstAccount = new Account();
			srcAccount.setAccountNumber(request.getSrcAccount().getAccountNumber());
			dstAccount.setAccountNumber(request.getDstAccount().getAccountNumber());
			
			operation.setCreateDate(GregorianCalendar.getInstance());
			operation.setSrcAccount(srcAccount);
			operation.setDstAccount(dstAccount);
			operation.setSrcStoplistInfo(srcStopListInfo);
			operation.setDstStoplistInfo(dstStopListInfo);
			
			try {
				operationDao.saveOperation(operation);
			} catch (OperationDaoException e) {
				// TODO Auto-generated catch block
				throw new TransferException("Failed to save operation in the database because of: " + e.getMessage());
			}
			
		}

		private void transferDo() throws TransferException {
			// Эту операцию пока не реализовавем. Она должна вызывать
			// CDCMoneyTransferServiceConsumer которого ещё нет
		}

		private boolean isStopListInfoOK() {
			if (StopListStatus.OK.equals(srcStopListInfo.getStatus())
					&& StopListStatus.OK.equals(dstStopListInfo.getStatus())) {
				return true;
			}
			return false;
		}

		private StopListInfo getStopListInfo(AccountInfo accountInfo) {
			if (accountInfo instanceof JuridicalAccountInfo) {
				JuridicalAccountInfo juridicalAccountInfo = (JuridicalAccountInfo) accountInfo;
				JuridicalStopListRequest juridicalStopListRequest = new JuridicalStopListRequest();
				juridicalStopListRequest.setInn(juridicalAccountInfo.getInn());
				StopListInfo stopListInfo = stopListService.getJuridicalStopListInfo(juridicalStopListRequest);
				return stopListInfo;
			} else if (accountInfo instanceof PhysicalAccountInfo) {

				PhysicalAccountInfo physicalAccountInfo = (PhysicalAccountInfo) accountInfo;
				PhysicalStopListRequest physicalStopListRequest = new PhysicalStopListRequest();
				physicalStopListRequest.setFirstname(physicalAccountInfo.getFirstname());
				physicalStopListRequest.setLastname(physicalAccountInfo.getLastname());
				physicalStopListRequest.setMiddlename(physicalAccountInfo.getMiddlename());
				physicalStopListRequest.setDocumentNumber(physicalAccountInfo.getDocumentNumber());
				physicalStopListRequest.setDocumentSeries(physicalAccountInfo.getDocumentSeries());
				StopListInfo stopListInfo = stopListService.getPhysicalStopListInfo(physicalStopListRequest);
				return stopListInfo;
			}
			return null;
		}

		private boolean processStopListStatus(StopListInfo stopListInfo)
				throws TransferException {
			if (StopListStatus.ASKSECURITY.equals(stopListInfo.getStatus())) {
				return false;
			}
			return true;
		}

		private void verifySrcBalance() throws TransferException {
			if (!accountService.verifyBalance(request.getSrcAccount()))
				throw new TransferException(LOW_BALANCE_ERROR_MESSAGE);
		}
	}

	public void setAccountService(AccountService accountService) {
		this.accountService = accountService;
	}

	public void setStopListService(StopListService stopListService) {
		this.stopListService = stopListService;
	}

	public void setOperationDao(OperationDao operationDao) {
		this.operationDao = operationDao;
	}
}
