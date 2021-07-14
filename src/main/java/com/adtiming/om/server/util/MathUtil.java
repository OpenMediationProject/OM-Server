package com.adtiming.om.server.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.OptionalDouble;

public class MathUtil {

    // population variance 总体方差
    public static double popVariance(double[] data) {
        double variance = 0;
        for (double val : data) {
            variance = variance + (Math.pow((val - avg(data)), 2));
        }
        variance = variance / data.length;
        return variance;
    }

    // population variance 总体方差
    public static BigDecimal popVariance(BigDecimal[] data) {
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal val : data) {
            variance = variance.add(new BigDecimal(String.valueOf(Math.pow(val.subtract(avg(data)).doubleValue(), 2))));
        }
        return variance.divide(BigDecimal.valueOf(data.length), 6, BigDecimal.ROUND_HALF_UP);
    }

    // population standard deviation 总体标准差
    public static double popStdDev(double[] data) {
        double std_dev;
        std_dev = Math.sqrt(popVariance(data));
        return std_dev;
    }

    public static BigDecimal popStdDev(BigDecimal[] data) {
        return new BigDecimal(String.valueOf(Math.sqrt(popVariance(data).doubleValue())));
    }

    //sample variance 样本方差
    public static double sampleVariance(double[] data) {
        double variance = 0;
        for (double val : data) {
            variance = variance + (Math.pow((val - avg(data)), 2));
        }
        variance = variance / (data.length - 1);
        return variance;
    }

    public static BigDecimal sampleVariance(BigDecimal[] data) {
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal val : data) {
            variance = variance.add(new BigDecimal(String.valueOf(Math.pow(val.subtract(avg(data)).doubleValue(), 2))));
        }
        return variance.divide(BigDecimal.valueOf(data.length - 1), 6, BigDecimal.ROUND_HALF_UP);
    }

    // sample standard deviation 样本标准差
    public static double sampleStdDev(double[] data) {
        double std_dev;
        std_dev = Math.sqrt(sampleVariance(data));
        return std_dev;
    }

    public static BigDecimal sampleStdDev(BigDecimal[] data) {
        return new BigDecimal(String.valueOf(Math.sqrt(sampleVariance(data).doubleValue())));
    }

    public static double avg(double[] data) {
        OptionalDouble avg = Arrays.stream(data).average();
        if (avg.isPresent()) {
            return avg.getAsDouble();
        }
        return 0;
    }

    public static BigDecimal avg(BigDecimal[] data) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal val : data) {
            sum = sum.add(val);
        }
        return sum.divide(BigDecimal.valueOf(data.length), 6, BigDecimal.ROUND_HALF_UP);
    }

    // coefficient of variation 计算离散系数 离散系数F1=标准差/平均值
    public static double cov(double [] data){
        double stdDevVal = popStdDev(data);
        double avgVal = avg(data);
        if (avgVal == 0) {
            return 0;
        }
        return stdDevVal/avgVal;
    }

    public static BigDecimal cov(BigDecimal[] data){
        BigDecimal stdDevVal = popStdDev(data);
        BigDecimal avgVal = avg(data);
        if (avgVal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return stdDevVal.divide(avgVal, 6, BigDecimal.ROUND_HALF_UP);
    }

    private static final BigDecimal D1 = new BigDecimal(1);

    // exponentialSmoothing 1次指数平滑 yt+1'=ayt+(1-a)yt' 又可写作yt+1'=yt'+a(yt- yt')
    // 式中，yt+1'--t+1期的预测值，即本期（t期）的平滑值St ； 　
    // yt--t期的实际值； 　
    // yt'--t期的预测值，即上期的平滑值St-1
    // a -- 平滑系数,[0,1]
    // y0使用第一个元素
    public static BigDecimal expSmoothing(BigDecimal smoothParam, BigDecimal[] data) {
        BigDecimal lastPredictParam = data[0];
        for (BigDecimal val : data) {
            lastPredictParam = smoothParam.multiply(val).add(D1.subtract(smoothParam).multiply(lastPredictParam));
        }
        return lastPredictParam;
    }

    // y0使用前3个元素平均值
    public static BigDecimal expSmoothing2(BigDecimal smoothParam, BigDecimal[] data) {
        int length = Math.min(data.length, 3);
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < length; i++) {
            sum = sum.add(data[i]);
        }
        BigDecimal lastPredictParam = sum.divide(BigDecimal.valueOf(length), 6, BigDecimal.ROUND_HALF_UP);
        for (BigDecimal val : data) {
            lastPredictParam = smoothParam.multiply(val).add(D1.subtract(smoothParam).multiply(lastPredictParam));
        }
        return lastPredictParam;
    }

    public static double expSmoothing(double smoothParam, double[] data) {
        double lastPredictParam = data[0];
        for (double val : data) {
            lastPredictParam = smoothParam * val + (1 - smoothParam) * lastPredictParam;
        }
        return lastPredictParam;
    }

    public static double expSmoothing2(double smoothParam, double[] data) {
        int length = Math.min(data.length, 3);
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += data[i];
        }
        double lastPredictParam = sum / length;
        for (double val : data) {
            lastPredictParam = smoothParam * val + (1 - smoothParam) * lastPredictParam;
        }
        return lastPredictParam;
    }
}
