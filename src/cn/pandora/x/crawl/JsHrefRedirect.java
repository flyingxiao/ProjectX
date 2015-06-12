package cn.pandora.x.crawl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JsHrefRedirect {
	protected final Log log = LogFactory.getLog(JsHrefRedirect.class);

	public String getRedirectURL(String lhtml) {
		// <script>window.location.href='http://www.ddhbb.com';</script>
		String url = "";
		int hrefBeg = lhtml.indexOf("window.location.href");

		int hrefEnd = lhtml.indexOf(";", hrefBeg); // 以单引号区分
		String hrefStr = "";
		if (hrefEnd > -1) {
			hrefStr = lhtml.substring(hrefBeg + "window.location.href".length(), hrefEnd);
		} else {
			hrefStr = lhtml.substring(hrefBeg + "window.location.href".length());
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
