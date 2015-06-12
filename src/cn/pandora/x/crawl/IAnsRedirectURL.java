package cn.pandora.x.crawl;

public interface IAnsRedirectURL {
	/**
	 * 
	 * 分析跳转后的url地址
	 * @param lhtml 小写的html内容
	 * @return
	 */
	String getRedirectURL(String lhtml);
}
