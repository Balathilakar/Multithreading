package com.amfam.billing.acquirer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;
import com.amfam.receipt.finacct.CreditCardNumber;

public class MockProtocolService implements ProtocolService {
	private static final Log LOG = LogFactory.getLog(MockProtocolService.class);
	static SimpleDateFormat sd = new SimpleDateFormat("yyMMdd HHmmss", Locale.US);
	static String dateTime = sd.format(new java.util.Date());
	public Response authorize(Request req) throws ProtocolServiceException {
		SaratogaAuthResponse response = new SaratogaAuthResponse();

		if (inFailureMode(req.getAmount())) {
			LOG.debug("mock: amount passed triggered the paymentech failure mode");
			createFailureResponse(req, response);
		} else {
			createSuccessResponse(req, response);
		}
		return response;
	}

	private static void createFailureResponse(Request req, SaratogaAuthResponse response) {
		response.setCreditCardNumber("                   ");
		response.setExpirationDate("    ");
		response.setAmount(req.getAmount());

		response.setResponseCode(new SaratogaResponseCode("005"));
		response.setRecordType("T");
		response.setResponseDate(new java.util.Date());
		response.setAuthorizationCode("AMFAM1");
		response.setAvsResponseCode("");
		response.setCsvResponse(" ");
		response.setRecurringPaymentAdviceCode("  ");
		response.setCavvResponseCode(" ");
		response.setResponseText(" ");
		response.setResponseTimeNum(Integer.parseInt(dateTime.substring(7, 13)));
		response.setResponseDateNum(Integer.parseInt(dateTime.substring(0, 6)));
		
	}

	private static void createSuccessResponse(Request req, SaratogaAuthResponse response) {
		response.setCreditCardNumber("                   ");
		response.setExpirationDate("    ");
		response.setAmount(req.getAmount());
		response.setCorrelationId(req.getCorrelationId());
		response.setMethodOfPayment(req.getCreditCardNumber().getNetwork().getCodeValueId());

		response.setResponseCode(new SaratogaResponseCode("000"));
		response.setRecordType("T");
		response.setResponseDate(new java.util.Date());
		response.setAuthorizationCode("AMFAM1");
		response.setAvsResponseCode(" ");
		response.setCsvResponse(" ");
		response.setRecurringPaymentAdviceCode("  ");
		response.setCavvResponseCode(" ");
		if((CreditCardNumber.Network.DI).equals(req.getCreditCardNumber().getNetwork())){
			response.setResponseText("USING MOCK SERVICE{17=075049736225723, 1=Y, 3=00}");
		}else if((CreditCardNumber.Network.VI).equals(req.getCreditCardNumber().getNetwork())){
			response.setResponseText("USING MOCK SERVICE{1=V, 2=0385057670493397, 3=6LR2, 23=A}");
		}else if((CreditCardNumber.Network.MC).equals(req.getCreditCardNumber().getNetwork())){
			response.setResponseText("USING MOCK SERVICE{17=0226MPLOCJIGR, 1=Y}");
		}else if ((CreditCardNumber.Network.AX).equals(req.getCreditCardNumber().getNetwork())){
			response.setResponseText("USING MOCK SERVICE{17=002956901489873, 1=Y}");
		}else{
			response.setResponseText("USING MOCK SERVICE{CARDTYPEUNKNOWN, 1}");
		}
		response.setResponseTimeNum(Integer.parseInt(dateTime.substring(7, 13)));
		response.setResponseDateNum(Integer.parseInt(dateTime.substring(0, 6)));
	}

	private static boolean inFailureMode(BigDecimal amount) {

		switch (Integer.valueOf(amount.intValue())) {
		// removed all the other failure amounts in order to
		// work for automation testing regression suite.
		// case 303: return true;
		// case 522: return true;
		// case 902: return true;
		// case 903: return true;
		// case 904: return true;
		// case 905: return true;

		case 1:
			return true;
		case 3:
			return true;
		case 5:
			return true;
		case 7:
			return true;
		case 9:
			return true;
		case 157:
			return true;
		case 257:
			return true;
		case 357:
			return true;
		case 557:
			return true;
		}
		return false;
	}

	
	public void shutdown() {
		// no op
	}

	public boolean makePrimaryActive() {
		return true;
	}

	public boolean makeSecondaryActive() {
		return true;
	}

	public boolean reconnectPrimary() {
		return true;
	}

	public boolean reconnectSecondary() {
		return true;
	}

	public String getActiveServer() {
		return "MockServer";
	}

}
