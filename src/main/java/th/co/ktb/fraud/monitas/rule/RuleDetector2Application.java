package th.co.ktb.fraud.monitas.rule;

import java.sql.Date;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import th.co.ktb.fraud.monitas.repositories.CaseRepository;
import th.co.ktb.fraud.monitas.repositories.CustomerProfileRepository;
import th.co.ktb.fraud.monitas.repositories.DTJRepository;
import th.co.ktb.fraud.monitas.repositories.EmployeeRepository;
import th.co.ktb.fraud.monitas.repositories.NotificationAcknowledgeRepository;
import th.co.ktb.fraud.monitas.repositories.NotificationRepository;
import th.co.ktb.fraud.monitas.repositories.RuleRepository;
import th.co.ktb.fraud.monitas.repositories.RunningNoRepository;
import th.co.ktb.fraud.monitas.repositories.TaskDistributorRepository;
import th.co.ktb.fraud.monitas.repositories.UserGroupRepository;
import th.co.ktb.fraud.monitas.rule.service.RuleDetectorService;
import th.co.ktb.fraud.monitas.rule.utils.DataUtils;
import th.co.ktb.fraud.monitas.thread.TaskDistributorThread;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "th.co.ktb.fraud.monitas.repositories")
@EntityScan(basePackages = "th.co.ktb.fraud.monitas.models")
public class RuleDetector2Application implements CommandLineRunner{

	@Autowired CaseRepository caseRepo;
	@Autowired DTJRepository dtjRepo;
	@Autowired NotificationRepository notiRepo;
	@Autowired NotificationAcknowledgeRepository notiActRepo;
	@Autowired RuleRepository ruleRepo;
	@Autowired RunningNoRepository runningRepo;
	@Autowired UserGroupRepository userGroupRepo;
	@Autowired EmployeeRepository empRepo;
	@Autowired CustomerProfileRepository cpRepo;
	@Autowired TaskDistributorRepository tdisRepo;
	
	private static Logger logger = LoggerFactory.getLogger(RuleDetector2Application.class);
	
	public static void main(String[] args) {
		SpringApplication.run(RuleDetector2Application.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
		
		initRepository();
		
		logger.debug("before run rule detector");
		
		/*for(int i=21;i<=21;i++) {
			
			
			Calendar calendar = Calendar.getInstance();
			
			calendar.set(Calendar.DAY_OF_MONTH, i);
			calendar.set(Calendar.MONTH, 11);
			calendar.set(Calendar.YEAR, 2021);
			
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			
			Date date = new Date(calendar.getTimeInMillis());
			DataUtils.setToday(date);
			
		
			RuleDetectorService detectorService = new RuleDetectorService();
			
			logger.debug("run rule detector");
			
			detectorService.run();
		
		}*/
		
		RuleDetectorService detectorService = new RuleDetectorService();
		TaskDistributorThread taskDist = new TaskDistributorThread(ruleRepo,tdisRepo,caseRepo);
		Thread thread = new Thread(taskDist, "Task Distribute");
		thread.start();
		
		while(true) {
			
			detectorService.run();
			
			Thread.sleep(1000);
			
		}
		
	}

	private void initRepository() {
		BeanHolder.getInstance().setCaseRepo(caseRepo);
		BeanHolder.getInstance().setDtjRepo(dtjRepo);
		BeanHolder.getInstance().setNotiActRepo(notiActRepo);
		BeanHolder.getInstance().setNotiRepo(notiRepo);
		BeanHolder.getInstance().setRuleRepo(ruleRepo);
		BeanHolder.getInstance().setRunningRepo(runningRepo);
		BeanHolder.getInstance().setUserGroupRepo(userGroupRepo);
		BeanHolder.getInstance().setEmpRepo(empRepo);
		BeanHolder.getInstance().setCpRepo(cpRepo);
	}

}
