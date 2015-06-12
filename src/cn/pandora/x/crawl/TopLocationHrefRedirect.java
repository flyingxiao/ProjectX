package cn.pandora.x.crawl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TopLocationHrefRedirect {
	protected final Log log = LogFactory.getLog(TopLocationHrefRedirect.class);
	public String getRedirectURL(String lhtml) {
//		if( self.location == "http://www.refractive-surgery.net/" ) {top.location.href = "/newrs2009";} 
		String flag  = "top.location.href";
		String url = "";
		
		int hrefBeg = lhtml.indexOf(flag);

		int hrefEnd = lhtml.indexOf(";", hrefBeg); // 以单引号区分
		String hrefStr = "";
		if (hrefEnd > -1) {
			hrefStr = lhtml.substring(hrefBeg + flag.length(), hrefEnd);
		} else {
			hrefStr = lhtml.substring(hrefBeg + flag.length());
		}
		log.info("hrefStr:" + hrefStr);
		if (hrefStr.indexOf("'") > -1) {
			url = hrefStr.substring(hrefStr.indexOf("'") + 1, hrefStr.lastIndexOf("'"));
		} else if (hrefStr.indexOf("\"") > -1) {
			url = hrefStr.substring(hrefStr.indexOf("\"") + 1, hrefStr.lastIndexOf("\""));
		}
		log.debug("jsRedirectURL:" + url);
		return url;
	}
}
