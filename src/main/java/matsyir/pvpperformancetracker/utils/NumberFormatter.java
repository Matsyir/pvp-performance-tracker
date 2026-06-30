package matsyir.pvpperformancetracker.utils;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class NumberFormatter
{
	public static final NumberFormat nf = NumberFormat.getInstance();
	public static final NumberFormat nf1 = NumberFormat.getInstance();
	public static final NumberFormat nf2 = NumberFormat.getInstance();

	public static final NumberFormat nfP = NumberFormat.getPercentInstance();
	public static final NumberFormat nfP1 = NumberFormat.getPercentInstance();

	static
	{
		nf.setMaximumFractionDigits(0);
		nf.setRoundingMode(RoundingMode.HALF_UP);

		nf1.setMaximumFractionDigits(1);
		nf1.setRoundingMode(RoundingMode.HALF_UP);

		nf2.setMaximumFractionDigits(2);
		nf2.setRoundingMode(RoundingMode.HALF_UP);

		
		nfP.setMaximumFractionDigits(0);
		nfP.setRoundingMode(RoundingMode.HALF_UP);

		nfP1.setMaximumFractionDigits(1);
		nfP1.setRoundingMode(RoundingMode.HALF_UP);
	}

}
