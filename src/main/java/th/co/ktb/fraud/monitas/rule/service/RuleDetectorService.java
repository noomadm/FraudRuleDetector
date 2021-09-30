package th.co.ktb.fraud.monitas.rule.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import th.co.ktb.fraud.monitas.models.Rule;
import th.co.ktb.fraud.monitas.models.RuleTransactionTypeWithinDays;
import th.co.ktb.fraud.monitas.rule.BeanHolder;


public class RuleDetectorService {

	private List<Rule> activeRuleList;
	private Gson gson = new Gson();

	private static Logger logger = LoggerFactory.getLogger(RuleDetectorService.class);

	public void run() {

		activeRuleList = BeanHolder.getInstance().getRuleRepo().getActiveRuleList();

		for (Rule rule : activeRuleList) {

			BaseRuleHandler ruleHandler = null;

			logger.debug("rule {} rule param : {}", rule.getRule_name(), rule.getParameter());
			RuleTransactionTypeWithinDays params = gson.fromJson(rule.getParameter(),
					RuleTransactionTypeWithinDays.class);
			logger.debug("converted data : {}", params);

			ruleHandler = getRule(rule, ruleHandler, params);

			logger.debug("got rule handler {}", ruleHandler);

			if (ruleHandler == null) {
				continue;
			}

			logger.debug("Run detector");
			ruleHandler.run();

		}

	}

	private BaseRuleHandler getRule(Rule rule, BaseRuleHandler ruleHandler, RuleTransactionTypeWithinDays params) {

		logger.debug("rule type : {}", rule.getRule_type());

		switch (rule.getRule_type()) {

			case RuleConstants.TRANSACTION_TYPE_RULE1:
	
				ruleHandler = new TTWIDRuleHandler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE2:
	
				ruleHandler = new TTSOWIDRuleHandler(rule, params);
				logger.debug("Rule2");
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE3:
	
				ruleHandler = new Rule3Handler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE4:
	
				ruleHandler = new Rule4Handler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE5:
	
				ruleHandler = new Rule5Handler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE6:
	
				ruleHandler = new Rule6Handler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE7:
	
				ruleHandler = new Rule7Handler(rule, params);
				break;
	
			case RuleConstants.TRANSACTION_TYPE_RULE8:
	
				ruleHandler = new Rule8Handler(rule, params);
				break;
			default:
				return null;

		}
		return ruleHandler;
	}

}
