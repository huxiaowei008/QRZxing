package com.hxw.qr;

import com.google.zxing.BarcodeFormat;

import java.util.EnumSet;
import java.util.Set;

/**
 * 码的类型
 *
 * @author hxw
 * @date 2018/2/13
 */

public final class DecodeFormatManager {
    /**
     * 一维码：商品
     */
    public static final Set<BarcodeFormat> PRODUCT_FORMATS = EnumSet.of(
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED);
    /**
     * 一维码：工业
     */
    public static final Set<BarcodeFormat> INDUSTRIAL_FORMATS = EnumSet.of(
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.ITF,
            BarcodeFormat.CODABAR);
    /**
     * 二维码
     */
    public static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet.of(BarcodeFormat.QR_CODE);
    /**
     * Data Matrix
     */
    public static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet.of(BarcodeFormat.DATA_MATRIX);
    /**
     * Aztec
     */
    public static final Set<BarcodeFormat> AZTEC_FORMATS = EnumSet.of(BarcodeFormat.AZTEC);
    /**
     * PDF417 (测试)
     */
    public static final Set<BarcodeFormat> PDF417_FORMATS = EnumSet.of(BarcodeFormat.PDF_417);

    private DecodeFormatManager() {
    }
}
