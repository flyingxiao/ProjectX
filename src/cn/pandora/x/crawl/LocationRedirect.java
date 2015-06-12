package cn.pandora.x.crawl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LocationRedirect implements IAnsRedirectURL {
	private String flag ;
	protected final Log log = LogFactory.getLog(LocationRedirect.class);
	public LocationRedirect(String cFlag){
		this.flag = cFlag;
	}
	public String getRedirectURL(String lhtml) {
//		if( self.location == "http://www.refractive-surgery.net/" ) {top.location.href = "/newrs2009";} 
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
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	
}
