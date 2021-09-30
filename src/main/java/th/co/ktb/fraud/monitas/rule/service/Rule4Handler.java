package th.co.ktb.fraud.monitas.rule.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import th.co.ktb.fraud.monitas.models.Case;
import th.co.ktb.fraud.monitas.models.DTJ;
import th.co.ktb.fraud.monitas.models.Rule;
import th.co.ktb.fraud.monitas.models.RuleTransactionTypeWithinDays;
import th.co.ktb.fraud.monitas.models.TellerAccountResult;
import th.co.ktb.fraud.monitas.repositories.CaseRepository;
import th.co.ktb.fraud.monitas.repositories.RuleRepository;
import th.co.ktb.fraud.monitas.repositories.RunningNoRepository;
import th.co.ktb.fraud.monitas.rule.BeanHolder;
import th.co.ktb.fraud.monitas.rule.utils.DataUtils;

public class Rule4Handler extends BaseRuleHandler{

	private RuleTransactionTypeWithinDays params;
	private RuleRepository ruleRepo;
	private List<TellerAccountResult> concernedList;
	private List<TellerAccountResult> caseList;
	private Map<TellerAccountResult,List<DTJ>> transactionMap;
	private Gson gson;
	
	private static Logger logger = LoggerFactory.getLogger(Rule4Handler.class);
	
	public Rule4Handler(Rule rule,RuleTransactionTypeWithinDays params) {
		
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
		
		concernedList = ruleRepo.getTransactionSumOver(params.getDepositTransactionTypes(), params.getTransactionAmount(), date);
		
		logger.debug("got {} concerned case ",concernedList.size());
		
	}
	
	@Override
	protected void proveConcernList() {
	
		logger.debug("start prove concerned list");
		
		caseList = new ArrayList<TellerAccountResult>();
		transactionMap = new Hashtable<TellerAccountResult, List<DTJ>>();
		
		for(TellerAccountResult fraud : concernedList) {
			
			logger.debug("proved transaction for concerned case");
			
			Date date = DataUtils.getToday();
			
			List<String> tranType = new ArrayList<String>();
			tranType.addAll(params.getDepositTransactionTypes());
			tranType.addAll(params.getWithdrawTransactionTyps());
			
			List<DTJ> transactions = BeanHolder.getInstance().getDtjRepo().getTellerTransactions(tranType, fraud.getUser_id(),fraud.getAccount_number(), date);
			
			boolean hasNotRead = hasNotReadTransaction(transactions);
			
			if(!hasNotRead) {
				logger.debug("all transaction has bean read");
				continue;
			}
			
			logger.debug("got {} transactions",transactions.size());
			
			boolean isFraud = isFraud(transactions);
			
			logger.debug("is fraud {}",isFraud);
			
			if(isFraud) {
				
				caseList.add(fraud);
				transactionMap.put(fraud, transactions);
				
				this.fraudRecords = transactions;
				
				DTJ tran = transactions.get(transactions.size()-1);
				this.branchCode = tran.getBranch_code();
				this.transCode = tran.getTrans_trace_number();
				
			}
			
		}
		
	}

	private boolean isFraud(List<DTJ> transactions) {

		double deposit = 0;
		double withdraw = 0;
		
		for(DTJ dtj : transactions) {
			
			if(params.getDepositTransactionTypes().contains(dtj.getExternal_trans_code())) {
				deposit += dtj.getTrans_amount();
			}
			
			if(params.getWithdrawTransactionTyps().contains(dtj.getExternal_trans_code())) {
				withdraw += dtj.getTrans_amount();
			}
			
			boolean isFraud = deposit >= params.getTransactionAmount() && deposit >= params.getTransactionAmount() && (withdraw/deposit) >= 0.9;
			
			if(isFraud) {
				return true;
			}
			
		}
		
		return false;
		
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
		
		return fraudCase;
		
	}
	
	@Override
	protected boolean isCaseInWatchList(Object fraud) {
		
		return super.isCaseInWatchList(fraud);
		
	}
	
}
