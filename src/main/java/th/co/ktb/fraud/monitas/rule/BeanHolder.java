package th.co.ktb.fraud.monitas.rule;

import th.co.ktb.fraud.monitas.repositories.CaseRepository;
import th.co.ktb.fraud.monitas.repositories.CustomerProfileRepository;
import th.co.ktb.fraud.monitas.repositories.DTJRepository;
import th.co.ktb.fraud.monitas.repositories.EmployeeRepository;
import th.co.ktb.fraud.monitas.repositories.NotificationAcknowledgeRepository;
import th.co.ktb.fraud.monitas.repositories.NotificationRepository;
import th.co.ktb.fraud.monitas.repositories.RuleRepository;
import th.co.ktb.fraud.monitas.repositories.RunningNoRepository;
import th.co.ktb.fraud.monitas.repositories.UserGroupRepository;

public class BeanHolder {

	private static BeanHolder instance;
	private RunningNoRepository runningRepo;
	private CaseRepository caseRepo;
	private UserGroupRepository userGroupRepo;
	private NotificationRepository notiRepo;
	private NotificationAcknowledgeRepository notiActRepo;
	private DTJRepository dtjRepo;
	private RuleRepository ruleRepo;
	private EmployeeRepository empRepo;
	private CustomerProfileRepository cpRepo;
	
	private BeanHolder(){
		
	}
	
	public static BeanHolder getInstance() {
		
		if(instance == null) {
			instance = new BeanHolder();
		}
		
		return instance;
		
	}

	public RunningNoRepository getRunningRepo() {
		return runningRepo;
	}

	public void setRunningRepo(RunningNoRepository runningRepo) {
		this.runningRepo = runningRepo;
	}

	public CaseRepository getCaseRepo() {
		return caseRepo;
	}

	public void setCaseRepo(CaseRepository caseRepo) {
		this.caseRepo = caseRepo;
	}

	public NotificationRepository getNotiRepo() {
		return notiRepo;
	}

	public void setNotiRepo(NotificationRepository notiRepo) {
		this.notiRepo = notiRepo;
	}

	public NotificationAcknowledgeRepository getNotiActRepo() {
		return notiActRepo;
	}

	public void setNotiActRepo(NotificationAcknowledgeRepository notiActRepo) {
		this.notiActRepo = notiActRepo;
	}

	public UserGroupRepository getUserGroupRepo() {
		return userGroupRepo;
	}

	public void setUserGroupRepo(UserGroupRepository userGroupRepo) {
		this.userGroupRepo = userGroupRepo;
	}

	public DTJRepository getDtjRepo() {
		return dtjRepo;
	}

	public void setDtjRepo(DTJRepository dtjRepo) {
		this.dtjRepo = dtjRepo;
	}

	public RuleRepository getRuleRepo() {
		return ruleRepo;
	}

	public void setRuleRepo(RuleRepository ruleRepo) {
		this.ruleRepo = ruleRepo;
	}

	public EmployeeRepository getEmpRepo() {
		return empRepo;
	}

	public void setEmpRepo(EmployeeRepository empRepo) {
		this.empRepo = empRepo;
	}

	public CustomerProfileRepository getCpRepo() {
		return cpRepo;
	}

	public void setCpRepo(CustomerProfileRepository cpRepo) {
		this.cpRepo = cpRepo;
	}
	
}
