package cn.pandora.x.crawl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

public class RedirectAnsImpl {
	// private PropertiesUnit pu = new PropertiesUnit();
	protected final Log log = LogFactory.getLog(RedirectAnsImpl.class);
	
	public Map<String, Object> ansRedirect(String html, String inPutURL,
			int statusCode, String moveToURL) {
		/** 属于跳转 */
		boolean isRedirect = false;
		/** 属于站内跳转 */
		boolean isSiteInnerRedirect = false;
		/** 跳转类型 */
		String redirectType = RedirectType.defaultType.getValue();
		Map<String, Object> map = new HashMap<String, Object>();
		
		String moveTo = "";
		if (statusCode == HttpStatus.SC_OK) {
			Map<String, Object> res = ans200StatusRedirect(html, inPutURL);
			isRedirect = (Boolean)res.get("isRedirect");
			isSiteInnerRedirect = (Boolean)res.get("isSiteInnerRedirect");
			redirectType = res.get("redirectType").toString();
			moveTo = res.get("moveTo").toString();
		} else if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
				|| statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
			Map<String, Boolean> res = ans301StatusRedirect(inPutURL, moveToURL);
			isRedirect = res.get("isRedirect");
			isSiteInnerRedirect = res.get("isSiteInnerRedirect");
			redirectType = RedirectType.httpStatusRedirect.getValue();
		}
		
		map.put("isRedirect", isRedirect);
		map.put("isSiteInnerRedirect", isSiteInnerRedirect);
		map.put("redirectType", redirectType);
		map.put("moveTo", moveTo);

		log.debug("isRedirect:" + isRedirect + " isSiteInnerRedirect:"
				+ isSiteInnerRedirect);
		return map;
	}

	/**
	 * analyse the webSite Redirct which the status is 301
	 * 
	 * @param inPutURL
	 * @param moveToURL
	 * @return isRedirect && isSiteInnerRedirect
	 */
	private Map<String, Boolean> ans301StatusRedirect(String inPutURL,
			String moveToURL) {
		Map<String, Boolean> para = new HashMap<String, Boolean>();
		boolean isRedirect = false;
		boolean isSiteInnerRedirect = false;

		if (moveToURL == null || moveToURL.trim().equals("")) {

		} else {
			isRedirect = true;
			String parent = inPutURL.toLowerCase();
			String redirectURL = moveToURL.toLowerCase();
			if (redirectURL.startsWith(parent)) {
				isSiteInnerRedirect = true;
			}
		}
		para.put("isRedirect", isRedirect);
		para.put("isSiteInnerRedirect", isSiteInnerRedirect);
		return para;
	}

	/**
	 * analyse the webSite Redirct which the status is 200
	 * 
	 * @param html
	 * @param inPutURL
	 * @return isRedirect && isSiteInnerRedirect
	 */
	public Map<String, Object> ans200StatusRedirect(String html, String inPutURL) {
		Map<String, Object> para = new HashMap<String, Object>();
		boolean isRedirect = false;
		boolean isSiteInnerRedirect = false;
		String redirectType = RedirectType.defaultType.getValue();
		Long beg = System.currentTimeMillis();
		String lhtml = html.toLowerCase();
		lhtml = lhtml.replace("\n", "");//替换换行
		lhtml = lhtml.replace("\r", "");//替换换行
		
		lhtml = lhtml.replace("	", "");//替换tab制表�?
		lhtml = lhtml.replace(" ", "");//替换空格
		Long end = System.currentTimeMillis();
		log.debug("replace time:"+ (end -beg));
		String metaRefreshRegex = ".*\n*.*<meta\\s*http-equiv\\s*=\\s*\"refresh\".*>.*\n*.*";
		String locationHrefRegex = ".*<script.*>.*location.href\\s*=.*;.*</script>.*";
		String windowLocationRegex =".*<script.*>.*window.location\\s*=.*;.*</script>.*";
		String frameSrcRegex = ".*<frame.*\\s*src\\s*=.*";
		IAnsRedirectURL ansRedirectUrl = null;
		if (matcherRedirect(metaRefreshRegex, lhtml)) {
			isRedirect = true;
			redirectType = RedirectType.metaRefreshRedirect.getValue();
			ansRedirectUrl = new MetaRefreshRedirect();
		}else if (matcherRedirect(locationHrefRegex, lhtml)){
			isRedirect = true;
			redirectType = RedirectType.locationHref.getValue();
			String flag = "location.href";
			ansRedirectUrl = new LocationRedirect(flag);
		}else if(matcherRedirect(windowLocationRegex, lhtml)){
			isRedirect = true;
			redirectType = RedirectType.windowLocation.getValue();
			String flag = "window.location";
			ansRedirectUrl = new LocationRedirect(flag);
		}else if(matcherRedirect(frameSrcRegex, lhtml)){
			isRedirect = true;
			redirectType = RedirectType.frameSrc.getValue();
			String flag = "src";
			ansRedirectUrl = new LocationRedirect(flag);
		}
		String moveTo = "";
		if (isRedirect) {
			String url = ansRedirectUrl.getRedirectURL(lhtml);
			log.debug("url : " + url);
			if (url.contains(inPutURL)) {
				moveTo = url;
				isSiteInnerRedirect = true;
			} else if (!url.startsWith("http")) {
				moveTo = inPutURL + url;
				isSiteInnerRedirect = true;
			}
		}
		para.put("isRedirect", isRedirect);
		para.put("isSiteInnerRedirect", isSiteInnerRedirect);
		para.put("redirectType", redirectType);
		para.put("moveTo", moveTo);
		return para;
	}

	/**
	 * match the regex
	 * 
	 * @param regex
	 * @param lhtml
	 * @return true/fasle
	 */
	private boolean matcherRedirect(String regex, String lhtml) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(lhtml);
		return m.matches();
	}
}
