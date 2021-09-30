package th.co.ktb.fraud.monitas.thread;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import th.co.ktb.fraud.monitas.models.Case;
import th.co.ktb.fraud.monitas.models.Notification;
import th.co.ktb.fraud.monitas.models.NotificationAcknowledge;
import th.co.ktb.fraud.monitas.models.Rule;
import th.co.ktb.fraud.monitas.repositories.CaseRepository;
import th.co.ktb.fraud.monitas.repositories.NotificationRepository;
import th.co.ktb.fraud.monitas.repositories.RuleRepository;
import th.co.ktb.fraud.monitas.repositories.TaskDistributorRepository;
import th.co.ktb.fraud.monitas.rule.BeanHolder;
import th.co.ktb.fraud.monitas.rule.RuleDetector2Application;

@Component
public class TaskDistributorThread implements Runnable{
	
	@Autowired private TaskDistributorRepository tdisRepo;
	@Autowired private RuleRepository ruleRepo;
	private CaseRepository caseRepo;
	Gson gson = new Gson();
	
	private static Logger logger = LoggerFactory.getLogger(TaskDistributorThread.class);
	
	public TaskDistributorThread(RuleRepository ruleRepo,TaskDistributorRepository tdisRepo,CaseRepository caseRepo) {
		
		this.tdisRepo = tdisRepo;
		this.ruleRepo = ruleRepo;
		this.caseRepo = caseRepo;
		
	}

	@Override
	public void run() {
		
		while(true) {
		
			try {
				
				distributeTask();
				
				Thread.sleep(1000);
				
			}catch(InterruptedException ex) {
				//handle stop running
			}
		
		}
		
	}
	
	@Transactional
	public void distributeTask() {
		
		Sort sort = Sort.by("id");
		sort = sort.ascending();
		
		Pageable pagable = PageRequest.of(0, 1,sort);
		
		Page<Case> result = tdisRepo.getCaseToDistribute(pagable);
		
		if(result.isEmpty()) {
			//no case to distribute
			
			logger.debug("Case is empty to assign");
			
			return;
		}
		
		for(Case c : result) {
			
			logger.debug("case id {}",c.getId());
			
			int ruleId = c.getRule_id();
			Rule rule = ruleRepo.findById(ruleId).get();
			List<Double> groupIds = rule.getGroupIds();
			
			logger.debug("group of rule : {}",gson.toJson(groupIds));
			
			List<Integer> userIds = tdisRepo.getAvailableUserByGroups(groupIds);
			int userCount = userIds.size();
			
			if(userCount == 0) {
				//no user available
				
				logger.debug("no available user");
				
				return;
			}
			
			int index = (int)(Math.random() % userCount);
			int assignUser = userIds.get(index);
			
			logger.debug("assign user id {}",assignUser);
			
			List<Integer> assigned = new ArrayList<Integer>();
			assigned.add(assignUser);
			
			//create case notification for this user
			createCaseNotification(c,rule,assigned);
			
			c.setUser_id(assignUser);
			caseRepo.save(c);
		
		}
		
	}
	
	protected void createCaseNotification(Case fraudCase,Rule rule,List<Integer> userIds) {
		
		String message = String.format("You have new alert : %s", fraudCase.getAlert_id());
		
		Notification notification = createNotification(fraudCase, message,rule);
		
		createNotificationAcknowledge(notification, userIds,rule);
		
	}
	
	private Notification createNotification(Case fraudCase, String message,Rule rule) {
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
	
	private void createNotificationAcknowledge(Notification notification, List<Integer> userIds,Rule rule) {
		
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

}
