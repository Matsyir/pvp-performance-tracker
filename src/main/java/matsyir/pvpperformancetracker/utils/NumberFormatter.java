package matsyir.pvpperformancetracker.utils;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class NumberFormatter
{
	public static final NumberFormat nf = NumberFormat.getInstance();
	public static final NumberFormat nf1 = NumberFormat.getInstance();
	public static final NumberFormat nf2 = NumberFormat.getInstance();

	static
	{
		nf.setMaximumFractionDigits(0);
		nf.setRoundingMode(RoundingMode.HALF_UP);

		nf1.setMaximumFractionDigits(1);
		nf1.setRoundingMode(RoundingMode.HALF_UP);

		nf2.setMaximumFractionDigits(2);
		nf2.setRoundingMode(RoundingMode.HALF_UP);


	}

}
