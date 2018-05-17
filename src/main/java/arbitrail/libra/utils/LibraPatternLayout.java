package arbitrail.libra.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class LibraPatternLayout extends HTMLLayout {
	
	// RegEx pattern looks for <tr> <td> nnn...nnn </td> (all whitespace ignored)
	 
	private static final String rxTimestamp = "\\s*<\\s*tr\\s*>\\s*<\\s*td\\s*>\\s*(\\d*)\\s*<\\s*/td\\s*>";
	 
	/* The timestamp format. The format can be overriden by including the following 
	  * property in the Log4J configuration file:
	  *
	  * log4j.appender.<category>.layout.TimestampFormat
	  *
	  * using the same format string as would be specified with SimpleDateFormat.
	  *
	  */
	 
	private String timestampFormat = "yyyy-MM-dd-HH:mm:ss.SZ"; // Default format. Example: 2008-11-21-18:35:21.472-0800
	 
	private SimpleDateFormat sdf = new SimpleDateFormat(timestampFormat);
	
	private static int REFRESH = 30; 

	@Override
	public String getHeader() {
	    StringBuffer sbuf = new StringBuffer();
	    sbuf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">"  + Layout.LINE_SEP);
	    sbuf.append("<html>" + Layout.LINE_SEP);
	    sbuf.append("<head>" + Layout.LINE_SEP);
	    sbuf.append("<meta http-equiv=\"refresh\" content=\"" + REFRESH + "\">"  + Layout.LINE_SEP);
	    sbuf.append("<title>" + getTitle() + "</title>" + Layout.LINE_SEP);
	    sbuf.append("<style type=\"text/css\">"  + Layout.LINE_SEP);
	    sbuf.append("body, table {font-family: arial,sans-serif; font-size: x-small;}" + Layout.LINE_SEP);
	    sbuf.append("th {background: #336699; color: #FFFFFF; text-align: left;}" + Layout.LINE_SEP);
	    sbuf.append("</style>" + Layout.LINE_SEP);
	    sbuf.append("</head>" + Layout.LINE_SEP);
	    sbuf.append("<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">" + Layout.LINE_SEP);
	    sbuf.append("<button onclick=\"window.scrollTo(0, document.body.scrollHeight)\">Bottom</button>" + Layout.LINE_SEP);
	    sbuf.append("<hr size=\"1\" noshade>" + Layout.LINE_SEP);
	    sbuf.append("Log session start time " + new java.util.Date() + "<br>" + Layout.LINE_SEP);
	    sbuf.append("<br>" + Layout.LINE_SEP);
	    sbuf.append("<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">" + Layout.LINE_SEP);
	    sbuf.append("<tr>" + Layout.LINE_SEP);
	    sbuf.append("<th>Time</th>" + Layout.LINE_SEP);
	    sbuf.append("<th>Thread</th>" + Layout.LINE_SEP);
	    sbuf.append("<th>Level</th>" + Layout.LINE_SEP);
	    sbuf.append("<th>Category</th>" + Layout.LINE_SEP);
	    if(getLocationInfo()) {
	      sbuf.append("<th>File:Line</th>" + Layout.LINE_SEP);
	    }
	    sbuf.append("<th>Message</th>" + Layout.LINE_SEP);
	    sbuf.append("</tr>" + Layout.LINE_SEP);
	    return sbuf.toString();
	}
	
	public String format(LoggingEvent event) {
		String record = super.format(event); // Get the log record in the default HTMLLayout format.

		Pattern pattern = Pattern.compile(rxTimestamp); // RegEx to find the default timestamp
		Matcher matcher = pattern.matcher(record);

		if (!matcher.find()) // If default timestamp cannot be found,
		{
			return record; // Just return the unmodified log record.
		}

		StringBuffer buffer = new StringBuffer(record);

		buffer.replace(matcher.start(1), // Replace the default timestamp with one formatted as desired.
				matcher.end(1), sdf.format(new Date(event.timeStamp)));

		return buffer.toString(); // Return the log record with the desired timestamp format.
	}

	/**
	 * Setter for timestamp format. Called if
	 * log4j.appender.<category>.layout.TimestampFormat property is specfied
	 */

	public void setTimestampFormat(String format) {
		this.timestampFormat = format;
		this.sdf = new SimpleDateFormat(format); // Use the format specified by the TimestampFormat property
	}

	/** Getter for timestamp format being used. */

	public String getTimestampFormat() {
		return this.timestampFormat;
	}

}
