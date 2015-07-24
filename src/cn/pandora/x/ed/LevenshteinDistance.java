package cn.pandora.x.ed;

public class LevenshteinDistance {
	public static void main(String[] args) {
		String str1 = "icbc.com.cn";
		String str2 = "1cbc.com.cn";
		System.out.println("字符串1 {0}:" + str1);

		System.out.println("字符串2 {0}:" + str2);

		System.out.println("相似度 {0} %:"
				+ new LevenshteinDistance().LevenshteinDistancePercent(str1,
						str2) * 100);

	}

	private int LowerOfThree(int first, int second, int third) {
		int min = Math.min(first, second);

		return Math.min(min, third);
	}

	private int Levenshtein_Distance(String str1, String str2) {
		int[][] Matrix;
		int n = str1.length();
		int m = str2.length();

		int temp = 0;
		char ch1;
		char ch2;
		int i = 0;
		int j = 0;
		if (n == 0) {
			return m;
		}
		if (m == 0) {

			return n;
		}
		Matrix = new int[n + 1][m + 1];

		for (i = 0; i <= n; i++) {
			// 初始化第一列
			Matrix[i][0] = i;
		}

		for (j = 0; j <= m; j++) {
			// 初始化第一行
			Matrix[0][j] = j;
		}

		for (i = 1; i <= n; i++) {
			ch1 = str1.charAt(i - 1);
			for (j = 1; j <= m; j++) {
				ch2 = str2.charAt(j - 1);
				if (ch1 == ch2) {
					temp = 0;
				} else {
					temp = 1;
				}
				Matrix[i][j] = LowerOfThree(Matrix[i - 1][j] + 1,
						Matrix[i][j - 1] + 1, Matrix[i - 1][j - 1] + temp);
			}
		}
		for (i = 0; i <= n; i++) {
			for (j = 0; j <= m; j++) {
				System.out.println(" {0} :" + Matrix[i][j]);
			}
			System.out.println("");
		}

		return Matrix[n][m];
	}

	public double LevenshteinDistancePercent(String str1, String str2) {
		// int maxLenth = str1.Length > str2.Length ? str1.Length : str2.Length;
		int val = Levenshtein_Distance(str1, str2);
		return 1 - (double) val / Math.max(str1.length(), str2.length());
	}
}