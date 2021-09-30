package th.co.ktb.fraud.monitas.rule.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import th.co.ktb.fraud.monitas.models.RunningNo;
import th.co.ktb.fraud.monitas.repositories.RunningNoRepository;


public class RunningNumberService {

	private static RunningNumberService instance;
	private RunningNoRepository repository;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
	
	private Logger logger = LoggerFactory.getLogger(RunningNumberService.class);
	
	private RunningNumberService(RunningNoRepository repository) {
		
		this.repository = repository;
		
	}
	
	public static RunningNumberService getInstance(RunningNoRepository repository) {
		
		if(instance == null) {
			instance = new RunningNumberService(repository);
		}
		
		return instance;
		
	}
	
	public String generateAlertNo() {
		
		String prefix = sdf.format(new Date());
		
		Optional<RunningNo> current = repository.findById(prefix);
		
		logger.debug("is value is present "+current.isPresent());
		//logger.debug("is value is empty "+current.isEmpty());
		
		RunningNo running = !current.isPresent()?new RunningNo(prefix, 0):current.get();
		running.setCurrent(running.getCurrent()+1);
		running.setUpdated_by("rule");
		running.setUpdated_datetime(new Timestamp(System.currentTimeMillis()));
		
		String runningNo = String.format(prefix+"%06d", running.getCurrent());
		repository.save(running);
		
		return runningNo;
		
	}
}
