package cn.pandora.x.crawl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetaRefreshRedirect implements IAnsRedirectURL {
	protected final Log log = LogFactory.getLog(MetaRefreshRedirect.class);
	
	public String getRedirectURL(String lhtml) {
//		<meta http-equiv="refresh" content="0;url=http://bbs.eb.cn">
		String url = "";
		int metaTagBeg = lhtml.toLowerCase().indexOf("<meta");
		int metaTagEnd = -1;
		if (metaTagBeg > -1) {
			metaTagEnd = lhtml.toLowerCase().indexOf(">", metaTagBeg);
			String metaContent = lhtml.substring(metaTagBeg, metaTagEnd);
			if (metaContent.contains("http-equiv") && metaContent.contains("refresh")) {
				log.debug(" metaContent :" + metaContent);
				int urlPos = metaContent.indexOf("url=");
				if (urlPos > -1) {
					String urlAttribute = metaContent.substring(urlPos);
					log.debug("urlAttribute : " + urlAttribute);
					urlAttribute = urlAttribute.substring(0, urlAttribute.indexOf("\""));
					log.debug("urlAttribute : " + urlAttribute);
					String[] str = urlAttribute.split("=");
					url = (str.length == 2 ? str[1] : "");
				}

			} else {
				lhtml = lhtml.substring(metaTagEnd);
				url = getRedirectURL(lhtml);
			}
		}
		log.debug("RefreshURL:" + url);
		return url;
	}

}
