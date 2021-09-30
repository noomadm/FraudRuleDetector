package th.co.ktb.fraud.monitas.rule.utils;

import java.sql.Date;
import java.util.Calendar;

public class DataUtils {

	private static Date today;
	
	public static void setToday(Date value) {
		
		today = value;
		
	}
	
	
	public static Date getToday() {
		
		return today;
		
		
		/*Calendar calendar = Calendar.getInstance();
		
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		Date date = new Date(calendar.getTimeInMillis());
		
		return date;*/
		
	}
	
}
