package th.co.ktb.fraud.monitas.rule.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import th.co.ktb.fraud.monitas.models.Case;
import th.co.ktb.fraud.monitas.models.CaseAdditionData;
import th.co.ktb.fraud.monitas.models.DTJ;
import th.co.ktb.fraud.monitas.models.DTJIds;
import th.co.ktb.fraud.monitas.models.Rule;
import th.co.ktb.fraud.monitas.models.RuleTransactionTypeWithinDays;
import th.co.ktb.fraud.monitas.models.TellerAccountResult;
import th.co.ktb.fraud.monitas.repositories.CaseRepository;
import th.co.ktb.fraud.monitas.repositories.RuleRepository;
import th.co.ktb.fraud.monitas.repositories.RunningNoRepository;
import th.co.ktb.fraud.monitas.rule.BeanHolder;
import th.co.ktb.fraud.monitas.rule.utils.DataUtils;

public class Rule3Handler extends BaseRuleHandler{

	private RuleTransactionTypeWithinDays params;
	private RuleRepository ruleRepo;
	private List<TellerAccountResult> concernedList;
	private List<TellerAccountResult> caseList;
	private Map<TellerAccountResult,List<DTJ>> transactionMap;
	
	private Gson gson;
	
	private static Logger logger = LoggerFactory.getLogger(Rule3Handler.class);
	
	public Rule3Handler(Rule rule,RuleTransactionTypeWithinDays params) {
		
		this.rule = rule;
		this.params = params;
		this.ruleRepo = BeanHolder.getInstance().getRuleRepo();		
		
		gson = new Gson();
		
	}


	@Override
	protected void detect() {
		
		logger.debug("################ Rule {} start detect",rule.getRule_name());
		logger.debug("parameter : {}",gson.toJson(params));
		
		Date date = DataUtils.getToday();
		
		logger.debug("date param : {}",date);
		
		
		logger.info("query with transaction type : {}",gson.toJson(params.getTransactionTypes()));
		logger.info("query with transaction amount : {}",gson.toJson(params.getTransactionAmount()));
		
		concernedList = ruleRepo.getTransactionSumOver(params.getTransactionTypes(), params.getTransactionAmount(), date);
		
		logger.info("got {} concerned case ",concernedList.size());
		
	}
	
	@Override
	protected void proveConcernList() {
	
		logger.debug("start prove concerned list");
		
		caseList = new ArrayList<TellerAccountResult>();
		transactionMap = new Hashtable<TellerAccountResult, List<DTJ>>();
		
		for(TellerAccountResult fraud : concernedList) {
			
			
			logger.info("prove transaction for account : {}",fraud.getAccount_number());
			
			
			Date date = DataUtils.getToday();
			List<DTJ> transactions = BeanHolder.getInstance().getDtjRepo().getAccountTransactions(params.getTransactionTypes(),fraud.getAccount_number(), date);
			
			
			boolean hasNotRead = hasNotReadTransaction(transactions);
			
			logger.info("Is transaction already proved and no new transaction : {}",hasNotRead);
			
			
			if(!hasNotRead) {
				logger.debug("all transaction has bean read");
				continue;
			}
			
			
			logger.info("got {} transactions",transactions.size());
			
			boolean isFraud = isFraud(fraud.getAccount_number(),date,params.getTransactionTypes());
			
			logger.info("is fraud {}",isFraud);
			
			if(isFraud) {
				
				//caseList.add(fraud);
				//transactionMap.put(fraud, transactions);
				
				this.fraudRecords = transactions;
				
				DTJ tran = transactions.get(transactions.size()-1);
				this.branchCode = tran.getBranch_code();
				this.transCode = tran.getTrans_trace_number();
				this.dtjTran = tran;	
				
				DTJIds dtjId = new DTJIds();
				dtjId.setAccount_number(dtjTran.getAccount_number());
				dtjId.setDate_time(dtjTran.getDate_time());
				dtjId.setTrans_date(dtjTran.getTrans_date());
				dtjId.setTrans_sequence_no(dtjTran.getTrans_sequence_no());
				
				fraud.setDtjId(dtjId);
				
				caseList.add(fraud);
				transactionMap.put(fraud, transactions);
				
			}
			
		}
		
	}

	
	
	private boolean isFraud(String accountNumber,Date date,List<String> transactionTypes) {
	
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date.getTime());
		calendar.add(Calendar.MONTH, -6);
		Date startDate = new Date(calendar.getTimeInMillis());
		
		boolean isBeforeMaturity = isBeforeMatrutiyDate(accountNumber);
		boolean hasWithDraw = hasWithDrawWithin(accountNumber, startDate,date, transactionTypes);
		
		return isBeforeMaturity || !hasWithDraw;
		
	}

	private boolean hasWithDrawWithin(String accountNumber,Date date,Date transDate,List<String> transactionTypes) {
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(date.getTime());
		calendar.add(Calendar.DATE, -1);
		calendar.set(Calendar.HOUR,23);
		calendar.set(Calendar.MINUTE,59);
		calendar.set(Calendar.SECOND,59);
		//Date endDate = calendar.getTime();
		Timestamp endTime = new Timestamp(calendar.getTimeInMillis());
		
		long withDrawCount = BeanHolder.getInstance().getDtjRepo().countTransactionWithinDate(accountNumber, date,endTime, transactionTypes);
		
		return withDrawCount>0;
		
	}
	

	@Override
	@Transactional
	protected void createNotification() {
	
		logger.debug("create notification");
		
		for(TellerAccountResult fraud : caseList) {
			
			String tellerId = fraud.getUser_id();
			String accountNumber = fraud.getAccount_number();
			
			logger.debug("Notification for case teller {} account_no {}",tellerId,accountNumber);
			
			List<Case> watchingList = ruleRepo.getWatchingList(tellerId, accountNumber,rule.getId());
			boolean isWatchList = watchingList.size() > 0;
			
			logger.debug("Is case in watch list {}",isWatchList);
			
			logger.debug("stamp read case");
			List<DTJ> transactions = transactionMap.get(fraud);
			
				
			if(isWatchList) {
				
				//create
				for(Case fraudCase : watchingList) {
					
					logger.debug("create watch list notification");
					
					createWatchListNotification(fraudCase);
					
					logger.debug("transaction count {}",transactions.size());
					stampReadCase(transactions,fraudCase);
					
				}
				
			}else {
			
				logger.debug("create case notification");
				
				Case fraudCase = createCase(fraud);
				fraudCase = BeanHolder.getInstance().getCaseRepo().save(fraudCase);
				//createCaseNotification(fraudCase);
				
				logger.debug("transaction count {}",transactions.size());
				stampReadCase(transactions,fraudCase);
				
			}
			
			
		}
		
	}
	
	private boolean isBeforeMatrutiyDate(String accountNumber) {
		
		Timestamp maturityDate = BeanHolder.getInstance().getDtjRepo().getAccountMatrutiyDate(accountNumber);
		
		logger.debug("maturity data : {}",maturityDate);
		
		if(maturityDate == null) {
			return false;
		}
		
		//return false;
		
		Timestamp today = new Timestamp(System.currentTimeMillis());
		return today.getTime() < maturityDate.getTime();
		
	}
	
	private Case createCase(TellerAccountResult fraud) {
		
		Case fraudCase = new Case();
		RunningNoRepository runningRepo = BeanHolder.getInstance().getRunningRepo();
		CaseRepository caseRepo = BeanHolder.getInstance().getCaseRepo();
		String runningNo = RunningNumberService.getInstance(runningRepo).generateAlertNo();
		
		fraudCase.setAlert_id(runningNo);
		fraudCase.setAccount_no(fraud.getAccount_number());
		fraudCase.setAlert_datetime(new Timestamp(System.currentTimeMillis()));
		fraudCase.setAmount(fraud.getLast_amount());//require
		fraudCase.setArea(rule.getArea());
		fraudCase.setCreated_by(rule.getRule_name());
		fraudCase.setCreated_datetime(new Timestamp(System.currentTimeMillis()));
		fraudCase.setStatus("PENDING");
		fraudCase.setTrxn_code(fraud.getLast_trxn());
		fraudCase.setTeller_id(fraud.getUser_id());
		fraudCase.setRule_id(rule.getId());
		fraudCase.setCif_branch(branchCode);
		fraudCase.setTrxn_code(transCode);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		CaseAdditionData caseData = new CaseAdditionData();
		caseData.setDtjId(fraud.getDtjId());
		caseData.setTranDate(sdf.format(fraud.getDtjId().getTrans_date()));
		
		fraudCase.setJson_data(caseData);
		
		return fraudCase;
		
	}
	
	@Override
	protected boolean isCaseInWatchList(Object fraud) {
		
		return super.isCaseInWatchList(fraud);
		
	}
	
}
