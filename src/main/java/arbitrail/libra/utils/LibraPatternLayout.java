package arbitrail.libra.utils;

import org.apache.log4j.HTMLLayout;

public class LibraPatternLayout extends HTMLLayout {
	
	private static int REFRESH = 30; 

	@Override
	public String getHeader() {
		return super.getHeader() + "<meta http-equiv=\"refresh\" content=\"" + REFRESH + "\">"; 
	}

}
