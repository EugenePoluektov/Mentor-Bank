package ru.mentorbank.backoffice.services.moneytransfer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.mentorbank.backoffice.dao.exception.OperationDaoException;
import ru.mentorbank.backoffice.dao.stub.OperationDaoStub;
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
import ru.mentorbank.backoffice.services.accounts.AccountServiceBean;
import ru.mentorbank.backoffice.services.moneytransfer.exceptions.TransferException;
import ru.mentorbank.backoffice.services.stoplist.StopListService;
import ru.mentorbank.backoffice.services.stoplist.StopListServiceStub;
import ru.mentorbank.backoffice.test.AbstractSpringTest;
import static org.mockito.Mockito.*;

public class MoneyTransferServiceTest extends AbstractSpringTest {

	@Autowired
	private StopListService mockedStopListService;
	private AccountService mockedAccountService;
	private OperationDaoStub mockedOperationDaoStub;
	private MoneyTransferServiceBean moneyTransferServiceBean;
	private TransferRequest request;
	private PhysicalAccountInfo srcAccount;
	private JuridicalAccountInfo dstAccount;

	@Before
	public void setUp() {
		mockedStopListService = mock(StopListServiceStub.class);

		mockedAccountService = mock(AccountServiceBean.class);

		mockedOperationDaoStub = mock(OperationDaoStub.class);

		
		moneyTransferServiceBean = new MoneyTransferServiceBean();;

		moneyTransferServiceBean.setAccountService(mockedAccountService);

		moneyTransferServiceBean.setStopListService(mockedStopListService);

		moneyTransferServiceBean.setOperationDao(mockedOperationDaoStub);

		
		request = new TransferRequest();

		srcAccount = new PhysicalAccountInfo();

		dstAccount = new JuridicalAccountInfo();

		request.setSrcAccount(srcAccount);

		request.setDstAccount(dstAccount);

		
		StopListInfo stopListStatusOK = new StopListInfo();

		stopListStatusOK.setStatus(StopListStatus.OK);

		
		when(mockedAccountService.verifyBalance(srcAccount)).thenReturn(true);

		when(
				mockedStopListService
						.getJuridicalStopListInfo(any(JuridicalStopListRequest.class)))
				.thenReturn(stopListStatusOK);

		when(
				mockedStopListService
						.getPhysicalStopListInfo(any(PhysicalStopListRequest.class)))
				.thenReturn(stopListStatusOK);
	}

	@Test
	public void transfer() throws TransferException, OperationDaoException {
		moneyTransferServiceBean.transfer(request);

		verify(mockedStopListService).getJuridicalStopListInfo(
				any(JuridicalStopListRequest.class));
		verify(mockedAccountService).verifyBalance(any(AccountInfo.class));
		verify(mockedOperationDaoStub, times(1)).saveOperation(
				any(Operation.class));


	}
}
