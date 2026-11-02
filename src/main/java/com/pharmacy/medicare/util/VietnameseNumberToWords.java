package com.pharmacy.medicare.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích chuyển đổi số tiền sang chữ tiếng Việt chuẩn hóa theo quy tắc kế toán & hóa đơn thương mại.
 * Ví dụ: 20000 -> "Hai mươi nghìn đồng chẵn."
 */
public final class VietnameseNumberToWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] SCALES = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    private VietnameseNumberToWords() {
    }

    /**
     * Chuyển đổi số tiền kiểu BigDecimal sang chữ tiếng Việt.
     * @param amount Số tiền cần chuyển đổi
     * @return Chuỗi chữ tiếng Việt (kết thúc bằng "đồng chẵn.")
     */
    public static String convert(BigDecimal amount) {
        if (amount == null) {
            return "Không đồng chẵn.";
        }
        return convert(amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    /**
     * Chuyển đổi số nguyên long sang chữ tiếng Việt.
     * @param amount Số tiền nguyên
     * @return Chuỗi chữ tiếng Việt
     */
    public static String convert(long amount) {
        if (amount == 0) {
            return "Không đồng chẵn.";
        }
        if (amount < 0) {
            return "Âm " + convertWords(-amount).toLowerCase() + " đồng chẵn.";
        }

        String words = convertWords(amount);
        if (words.isEmpty()) {
            return "Không đồng chẵn.";
        }

        // Viết hoa chữ cái đầu tiên và nối thêm " đồng chẵn."
        String capitalized = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        return capitalized + " đồng chẵn.";
    }

    private static String convertWords(long n) {
        if (n == 0) return "không";

        List<Integer> groups = new ArrayList<>();
        long temp = n;
        while (temp > 0) {
            groups.add((int) (temp % 1000));
            temp /= 1000;
        }

        StringBuilder sb = new StringBuilder();
        int totalGroups = groups.size();

        for (int i = totalGroups - 1; i >= 0; i--) {
            int groupValue = groups.get(i);
            if (groupValue == 0) {
                // Nhóm = 0, nếu là nhóm tỷ mà các nhóm trước có giá trị thì có thể cần đọc
                continue;
            }

            boolean isHighest = (i == totalGroups - 1);
            String groupText = readThreeDigits(groupValue, isHighest);

            if (!groupText.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(groupText);
                if (i < SCALES.length && !SCALES[i].isEmpty()) {
                    sb.append(" ").append(SCALES[i]);
                }
            }
        }

        return sb.toString().trim();
    }

    private static String readThreeDigits(int number, boolean isHighestGroup) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int ones = number % 10;

        StringBuilder sb = new StringBuilder();

        // Đọc hàng trăm
        if (hundreds > 0) {
            sb.append(DIGITS[hundreds]).append(" trăm");
        } else if (!isHighestGroup) {
            // Khi không phải nhóm cao nhất và có số hàng chục hoặc đơn vị: đọc "không trăm"
            if (tens > 0 || ones > 0) {
                sb.append("không trăm");
            }
        }

        // Đọc hàng chục
        if (tens > 1) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(DIGITS[tens]).append(" mươi");
        } else if (tens == 1) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("mười");
        } else if (tens == 0 && ones > 0 && sb.length() > 0) {
            sb.append(" linh");
        }

        // Đọc hàng đơn vị
        if (ones > 0) {
            if (sb.length() > 0) sb.append(" ");
            if (ones == 1) {
                if (tens > 1) {
                    sb.append("mốt");
                } else {
                    sb.append("một");
                }
            } else if (ones == 5) {
                if (tens > 0) {
                    sb.append("lăm");
                } else {
                    sb.append("năm");
                }
            } else if (ones == 4 && tens > 1) {
                sb.append("tư");
            } else {
                sb.append(DIGITS[ones]);
            }
        }

        return sb.toString().trim();
    }
}
