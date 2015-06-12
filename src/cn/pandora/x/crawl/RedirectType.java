package cn.pandora.x.crawl;

public enum RedirectType {
	/**
	 * 状态零默认为非跳转
	 */
	defaultType("0"),httpStatusRedirect("1"), metaRefreshRedirect("2"),locationHref("3"),windowLocation("4"),frameSrc("5");
	private final String value;

	public String getValue() {
		return value;
	}
	// 构造器默认也只能是private, 从而保证构造函数只能在内部使用
	RedirectType(String value) {
		this.value = value;
	}
}
