package com.pharmacy.util;

import java.math.BigDecimal;

public class MoneyToWordsUtil {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    public static String convertToVietnameseWords(BigDecimal amount) {
        if (amount == null) {
            return "Không đồng chẵn.";
        }
        long number = amount.longValue();
        if (number == 0) {
            return "Không đồng chẵn.";
        }

        boolean isNegative = number < 0;
        if (isNegative) {
            number = -number;
        }

        String sNumber = String.valueOf(number);
        // Pad with zeros to make length divisible by 3
        while (sNumber.length() % 3 != 0) {
            sNumber = "0" + sNumber;
        }

        int groupCount = sNumber.length() / 3;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < groupCount; i++) {
            int groupIndex = groupCount - 1 - i;
            String group = sNumber.substring(i * 3, i * 3 + 3);
            int h = group.charAt(0) - '0';
            int t = group.charAt(1) - '0';
            int u = group.charAt(2) - '0';

            if (h == 0 && t == 0 && u == 0) {
                continue;
            }

            StringBuilder groupText = new StringBuilder();

            // Hundreds
            if (i > 0 || h > 0) {
                groupText.append(DIGITS[h]).append(" trăm ");
            }

            // Tens
            if (t == 0 && u != 0) {
                if (i > 0 || h > 0) {
                    groupText.append("lẻ ");
                }
            } else if (t == 1) {
                groupText.append("mười ");
            } else if (t > 1) {
                groupText.append(DIGITS[t]).append(" mươi ");
            }

            // Units
            if (u == 1) {
                if (t > 1) {
                    groupText.append("mốt ");
                } else {
                    groupText.append(DIGITS[u]).append(" ");
                }
            } else if (u == 5) {
                if (t > 0) {
                    groupText.append("lăm ");
                } else {
                    groupText.append(DIGITS[u]).append(" ");
                }
            } else if (u > 0) {
                groupText.append(DIGITS[u]).append(" ");
            }

            // Add unit (nghìn, triệu, tỷ...)
            if (groupIndex < UNITS.length && !UNITS[groupIndex].isEmpty()) {
                groupText.append(UNITS[groupIndex]).append(" ");
            }

            result.append(groupText);
        }

        String finalStr = result.toString().trim().replaceAll("\\s+", " ");
        if (isNegative) {
            finalStr = "Âm " + finalStr;
        }

        if (finalStr.isEmpty()) {
            return "Không đồng chẵn.";
        }

        // Capitalize first letter
        finalStr = Character.toUpperCase(finalStr.charAt(0)) + finalStr.substring(1) + " đồng chẵn.";
        return finalStr;
    }
}
