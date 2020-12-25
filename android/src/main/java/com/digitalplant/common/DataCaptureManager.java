package com.digitalplant.common;

import com.scandit.datacapture.barcode.data.Symbology;
import com.scandit.datacapture.core.capture.DataCaptureContext;

public class DataCaptureManager {
    public static DataCaptureContext dataCaptureContext;

    public static Symbology[] BarcodeFormats = {
        Symbology.AZTEC,
        Symbology.CODABAR,
        Symbology.CODE39,
        Symbology.CODE93,
        Symbology.CODE128,
        Symbology.DATA_MATRIX,
        Symbology.EAN8,
        Symbology.EAN13_UPCA,
        Symbology.IATA_TWO_OF_FIVE,
        Symbology.PDF417,
        Symbology.QR,
        Symbology.UPCE,
        Symbology.UPCE,
    };

    public static void init(String licenseKey) {
        dataCaptureContext = DataCaptureContext.forLicenseKey(licenseKey);
    }
}
