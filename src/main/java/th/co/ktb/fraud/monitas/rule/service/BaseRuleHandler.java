package th.co.ktb.fraud.monitas.rule.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import th.co.ktb.fraud.monitas.models.Case;
import th.co.ktb.fraud.monitas.models.DTJ;
import th.co.ktb.fraud.monitas.models.Notification;
import th.co.ktb.fraud.monitas.models.NotificationAcknowledge;
import th.co.ktb.fraud.monitas.models.Rule;
import th.co.ktb.fraud.monitas.repositories.NotificationRepository;
import th.co.ktb.fraud.monitas.rule.BeanHolder;


public class BaseRuleHandler {
	
	//get transaction ที่เข้าข่าย
	//ตรวจสอบ
	//ดูสถานของ case ว่ามี watch list อยู่รึป่าว
	//ถ้ามี สร้าง notification
	//ถ้าไม่มีสร้าง case
	//stamp รายการที่เจอว่าอ่านข้อมูลไปแล้ว
	
	protected Rule rule;
	private static Logger logger = LoggerFactory.getLogger(BaseRuleHandler.class);
	private Gson gson = new Gson();
	protected List<DTJ> fraudRecords = null;
	protected String transCode = null;
	protected String branchCode = null;
	protected DTJ dtjTran = null;
	
	
	protected void run() {
		
		logger.info("Start rule detector");
		
		detect();
		
		logger.info("Detect completed");
		
	
		proveConcernList();
		
		logger.info("Detect concern transaction completed");
		
		
		createNotification();
		
		logger.info("Create notification completed");
		//stampReadCase();
		
	}
	

	protected void detect() {
		
		
		
	}
	
	protected void proveConcernList() {
		
	}
	
	protected boolean isCaseInWatchList(Object fraud) {
		return false;
	}
	
	protected int getCaseOwnerId(int caseId) {
		return 0;
	}
	
	protected void createCaseNotification(Case fraudCase) {
		
		String message = String.format("You have new alert : %s", fraudCase.getAlert_id());
		
		Notification notification = createNotification(fraudCase, message);
		
		List<Double> groupIds = rule.getGroupIds();
		List<Integer> paramGroupId = new ArrayList<Integer>();
		for(Double value : groupIds) {
			logger.debug("group id value : {}",value.intValue());
			paramGroupId.add(value.intValue());
		}
		logger.debug("group id : {}",gson.toJson(paramGroupId));
		
		List<Integer> userIds = BeanHolder.getInstance().getUserGroupRepo().getUsersInGroups(paramGroupId);
		
		logger.debug("user id : {}",gson.toJson(userIds));
		
		createNotificationAcknowledge(notification, userIds);
		
	}
		
	protected void createWatchListNotification(Case fraudCase) {
		
		String message = String.format("Your watchlist %s has new fraud transaction", fraudCase.getAlert_id());
		
		Notification notification = createNotification(fraudCase, message);
		
		List<Integer> userIds = new ArrayList<Integer>();
		userIds.add(fraudCase.getUser_id());
		
		createNotificationAcknowledge(notification, userIds);
		
		
	}


	private Notification createNotification(Case fraudCase, String message) {
		Notification notification = new Notification();
		notification.setActive(true);
		notification.setCreated_by(rule.getRule_name());
		notification.setCreated_datetime(new Timestamp(System.currentTimeMillis()));
		notification.setLink("investigation.html?id="+fraudCase.getId());
		notification.setMessage(message);
		notification.setMessage_type("INVESTIGATION");
		notification.setUser_id(fraudCase.getUser_id());
		
		NotificationRepository notiRepo = BeanHolder.getInstance().getNotiRepo();
		notiRepo.save(notification);
		return notification;
	}


	private void createNotificationAcknowledge(Notification notification, List<Integer> userIds) {
		
		for(Integer userId : userIds) {
			
			logger.debug("notification acknowledge for user : {}",userId);
			
			NotificationAcknowledge notiAct = new NotificationAcknowledge();
			notiAct.setNotification_id(notification.getId());
			notiAct.setType("ALERT");
			notiAct.setUser_id(userId);
			notiAct.setAcknowledge(false);
			notiAct.setCreated_by(rule.getRule_name());
			notiAct.setCreated_datetime(new Timestamp(System.currentTimeMillis()));
		
			BeanHolder.getInstance().getNotiActRepo().save(notiAct);
			
			logger.debug("saved notification acknowledge");
			
		}
		
	}
	
	
	
	protected void stampReadCase(List<DTJ> dtjs,Case alert) {
		
		Gson gson = new Gson();
		
		for(DTJ dtj : dtjs) {
			
			logger.debug("stamp read case for {} {}",dtj.getTrans_date(),dtj.getSequence_no());
			List<Integer> readByRules = dtj.getDtj_addition().getRead_by_rule();
			
			if(!readByRules.contains(rule.getId())) {
				
				//List<Integer> readRules = dtj.getReadByRules();
				readByRules.add(rule.getId());
				
				logger.debug("read rule : ",gson.toJson(readByRules));
				
				//dtj.setRead_by_rules(gson.toJson(readRules));
				dtj.getDtj_addition().setRead_by_rule(readByRules);
				dtj.getDtj_addition().getCases().put(alert.getId(), alert);
								
				logger.debug("save dtj");
				
			}
			
			if(dtj.getDetected_date() == null) {
				
				dtj.setDetected_date(new Timestamp(System.currentTimeMillis()));
				
			}
			
			if(dtj.getCase_no() == null) {
				
				dtj.setCase_no(alert.getAlert_id());
				
			}
			
			dtj = BeanHolder.getInstance().getDtjRepo().save(dtj);
			
			logger.debug("saved transaction");
			
			
		}
		
	}
	
	protected void saveCase() {
		
		
		
	}
	
	protected boolean hasNotReadTransaction(List<DTJ> transactions) {
		
		int ruleId = rule.getId();
		boolean hasNotRead = false;
		
		for(DTJ tran : transactions) {
			
			hasNotRead |= !tran.getDtj_addition().getRead_by_rule().contains(ruleId);
			
		}
		
		return hasNotRead;
		
	}


	protected void createNotification() {
		// TODO Auto-generated method stub
		
	}
	
}
