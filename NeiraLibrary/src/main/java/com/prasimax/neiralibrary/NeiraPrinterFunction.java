package com.prasimax.neiralibrary;

/**
 * Merupakan kelas untuk mendapatkan fungsi-fungsi yang dapat digunakan didalam
 * Printer Neira. Fungsi-fungsi akan mengembalikan data byte yang kemudian dapat
 * dikirimkan ke printer.
 *
 * @author deani
 * @author Prasimax - Software Team
 */
public class NeiraPrinterFunction {

    private static NeiraPrinterCommandList CMD = new NeiraPrinterCommandList();

    /**
     * Fungsi untuk Printer Init, melakukan pembersihan buffer printer dan mengembalikan
     * setting printer ke kondisi pabrikan.
     *
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] PrinterInit() {
        //SPRINT 1 6.2
        return new byte[]{CMD.ESC, '@'};
    }

    /**
     * Fungsi untuk melakukan "Print Self Test" ke printer.
     *
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] PrintSelfTest() {
        //SPRINT 1 6.5
        return CMD.SelfTest;
    }

    /**
     * Fungsi untuk melakukan feed, tanpa fungsi ini dapat
     * menggunakan "0x0A" atau "\n" pada java String.
     *
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte LineFeed() {
        //SPRINT 1 6.3
        return CMD.LF;
    }

    /**
     * Fungsi untuk melakukan print line feed sebanyak n kali.
     *
     * @param multiply besar perkalian/banyak line feed yang akan diprint. RANGE=0-255.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] PrintAndFeedPaper(int multiply) {
        //SPRINT 1 6.4
        if (multiply < 0) multiply = 0;
        else if (multiply > 255) multiply = 255;

        return new byte[]{CMD.ESC, 'J', (byte) multiply};
    }

    /**
     * Fungsi untuk print teks di buffer kemudian memberikan
     * Linespace sebanyak 'multiply'.
     *
     * @param multiply batasan dari 0 hingga 255 kali
     * @return
     */
    public static byte[] PrintFeedAndLines(byte multiply) {
        return new byte[]{CMD.ESC, 0x64, multiply};
    }


    /**
     * Fungsi untuk memilih mode print ke kertas
     *
     * @param FontType     hanya dua tipe set true untuk tipe 2 dan false untuk tipe 1.
     * @param Emphasized   true untuk ON, false untuk OFF.
     * @param DoubleHeight true untuk ON, false untuk OFF.
     * @param DoubleWidth  true untuk ON, false untuk OFF.
     * @param Underline    true untuk ON, false untuk OFF.
     * @return data byte yang dapat dikirimkan ke Printer.
     */

    public static byte[] SelectPrintMode(boolean FontType, boolean Emphasized,
                                         boolean DoubleHeight, boolean DoubleWidth,
                                         boolean Underline) {

        byte a = (byte) 0b00000000;
        if (FontType) a = (byte) (a | 0b00000001);
        if (Emphasized) a = (byte) (a | 0b00001000);
        if (DoubleHeight) a = (byte) (a | 0b00010000);
        if (DoubleWidth) a = (byte) (a | 0b00100000);
        if (Underline) a = (byte) (a | 0b10000000);

        return new byte[]{CMD.ESC, '!', a};
    }

    /**
     * Fungsi untuk membunyikan Buzzer atau Beep pada printer. Tipe Non Epson
     *
     * @param times merupakan jumlah beep. Set ke 0 untuk mematikan Buzzer ketika "Out of Paper".
     * @param delay merupakan jarak waktu antara beep dalam 0,5 Sec atau detik.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetBeepTimes(int times, int delay) {
        //SPRINT 1 6.6
        byte n = (byte) times;
        byte t = (byte) delay;

        return new byte[]{CMD.ESC, 'B', n, t};
    }

    /**
     * Fungsi untuk membunyikan Buzzer atau Beep pada printer. Tipe Epson
     *
     * @param times merupakan jumlah beep. Set ke 0 untuk mematikan Buzzer ketika "Out of Paper".
     * @param delay merupakan jarak waktu antara beep dalam 0,5 Sec atau detik.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetBeepTimesEpsonCommand(int times, int delay) {
        //SPRINT 1 6.6
        byte n = (byte) times;
        byte t = (byte) delay;

        return new byte[]{CMD.ESC, 0x28, 0x41, 0x04, 0, 0x30, 0, n, t};
    }

    /**
     * Fungsi untuk menset mode underline pada teks yang diprint:
     * <p>
     * Valid n = 0, 48 : turn off
     * Valid n = 1, 49 : turn on 1 line underline
     * Valid n = 2, 50 : turn on 2 line underline
     *
     * @param n nilai yang dimasukan, jika diluar batas maka dioverride ke nilai yang mendekati.
     * @return
     */
    public static byte[] SetUnderlineMode(int n) {
        if (n < 4) {
            if (n < 0) n = 0;
            if (n > 3) n = 3;
        } else {
            if (n < 48) n = 48;
            if (n > 50) n = 50;
        }

        return new byte[]{CMD.ESC, 0x2D, (byte) n};
    }

    /**
     * Fungsi untuk menset Margin kiri pada printer.
     *
     * @param size merupakan besar margin yang diinginkan.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetLeftMargin(int size) {
        //SPRINT 1 6.7
        final int maxSize = 31;

        size = (size > maxSize) ? maxSize : ((size < 0) ? 0 : size);

        byte nL = (byte) size;
        byte nH = (byte) 0;

        return new byte[]{CMD.GS, 'L', nL, nH};
    }

    /**
     * Fungsi untuk menentukan ukuran lebar cetak printer ke kertas.
     *
     * @param size merupakan lebar yang diinginkan.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetPrintWidth(int size) {
        //SPRINT 1 6.8
        final int maxSize = 32;

        size = (size > maxSize) ? maxSize : ((size < 1) ? 1 : size);

        byte nL = (byte) size;
        byte nH = (byte) 0;

        return new byte[]{CMD.GS, 'W', nL, nH};
    }

    /**
     * Fungsi untuk mengembalikan LineSpacing ke default-nya.
     * Secara default sebesar 40 lines.
     *
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetLineSpacingToDefault() {
        //SPRINT 1 6.9
        return CMD.DefaultLineSpacing;
    }

    /**
     * Fungsi untuk menentukan jarak LineSpacing antar baris.
     *
     * @param size jarak besaran antar baris yang diinginkan.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetLineSpacing(int size) {
        //SPRINT 1 6.10
        final int maxSize = 255, minSize = 0;

        size = (size > maxSize) ? maxSize :
                ((size < minSize) ? minSize : size);

        byte n = (byte) size;

        return new byte[]{CMD.ESC, 0x33, n};
    }

    /**
     * Fungsi untuk menset printer untuk mencetak "Bold" pada
     * cetakan selanjutnya.
     *
     * @param size nilai LSB 0 untuk nonaktifkan bold dan LSB 1 untuk aktifkan bold.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetBold(byte size) {
        //SPRINT 1 6.11
        return new byte[]{
                CMD.EMPHASIZE[0], CMD.EMPHASIZE[1], size};
    }

    /**
     * Fungsi untuk menset printer untuk mencetak "Bold" pada
     * cetakan selanjutnya.
     *
     * @param set true untuk aktifkan; false untuk mematikan.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetBold(boolean set) {
        //SPRINT 1 6.11
        return new byte[]{
                CMD.EMPHASIZE[0], CMD.EMPHASIZE[1], set ? (byte) 1 : (byte) 0};
    }

    /**
     * Fungsi untuk menset printer untuk mencetak dua kali pada tempat
     * yang sama (Double Strike) atau Alternatif Bold pada cetakan selanjutnya.
     *
     * @param size nilai LSB 0 untuk nonaktifkan dan LSB 1 untuk aktifkan.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetDoubleStrike(byte size) {

        return new byte[]{CMD.DPRINT[0], CMD.DPRINT[1], size};
    }

    /**
     * Fungsi untuk menset besar font/karakter cetakan pada printer.
     *
     * @param size merupakan besar font yang diinginkan, 1 hingga 8. Override ke nilai yang mendekati jika diluar batas.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetFontSize(int size) {
        //SPRINT 1 6.?
        final int maxSize = 8, minSize = 1;

        size = (size > maxSize) ? maxSize :
                ((size < minSize) ? minSize : size);

        byte n = 0b00010001;
        switch (size) {
            case 1:
                n = (byte) 0b00010001;
                break;
            case 2:
                n = (byte) 0b00100010;
                break;
            case 3:
                n = (byte) 0b00110011;
                break;
            case 4:
                n = (byte) 0b01000100;
                break;
            case 5:
                n = (byte) 0b01010101;
                break;
            case 6:
                n = (byte) 0b01100110;
                break;
            case 7:
                n = (byte) 0b01110111;
                break;
            case 8:
                n = (byte) 0b01000100;
                break;
        }

        return new byte[]{
                CMD.GS, '!', n};
    }

    /**
     * Fungsi untuk menset tipe font yang diinginkan.
     * Override nilai type jika tidak sesuai ketentuan pada
     * nilai yang mendekatinya.
     *
     * @param type 0 atau 48 untuk Font1, 1 atau 49 untuk Font2.
     * @return data byte yang dapat dikirimkan ke Printer.
     */
    public static byte[] SetFontType(int type) {
        //SPRINT 1 6.?
        if (type < 2) {
            if (type < 0) type = 0;
            if (type > 1) type = 1;
        } else {
            if (type < 48) type = 48;
            if (type > 49) type = 49;
        }
        byte n = (byte) type;

        return new byte[]{
                CMD.ESC, 'M', n};
    }

    public static byte[] ChangePrintSpeed(int speed) {
        final int maxS1 = 9, minS1 = 0, maxS2 = 57, minS2 = 48;
        if (speed > maxS1) {
            if (speed > maxS2) {
                speed = maxS2;
            } else if (speed < minS2) {
                speed = minS2;
            }

        } else {
            if (speed > maxS1) {
                speed = maxS1;
            } else if (speed < minS1) {
                speed = minS1;
            }
        }

        byte n = (byte) speed;

        return new byte[]{
                0x1D, 0x28, 0x4B, 0x02, 0x00, 0x32, n
        };
    }

    public static byte[] SetQRCodeModel(int type) {
        type = 49;

        byte n = (byte) type;
        return new byte[]{
                0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, n, 0
        };
    }

    public static byte[] SetQRCodeSize(int size) {
        size = 8;
        return new byte[]{
                0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) size
        };
    }

    /**
     * Valid n = 48 : Select ECC Level L.
     * Valid n = 49 : Select ECC Level M.
     * Valid n = 50 : Select ECC Level Q.
     * Valid n = 51 : Select ECC Level H
     *
     * @param level
     * @return
     */
    public static byte[] SetQRCodeECCLevel(int level) {
        if (level < 48) level = 48;
        if (level > 51) level = 51;
        return new byte[]{
                0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, (byte) level
        };
    }


    public static byte[] PrintQRCode() {
        return new byte[]{
                0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30
        };

    }

    public static byte[] StoreQRCodeData(String data) {
        if (data.isEmpty()) return new byte[]{0, 0};
        if (data.length() < 4) return new byte[]{0, 1};
        if (data.length() > 157) return new byte[]{0, 2};

        int size = data.length();
        byte pl = (byte) size;
        byte ph = (byte) 0;
        byte[] cmd = {
                0x1D, 0x28, 0x6B, pl, ph, 0x31, 0x50, 0x30
        };

        byte[] result = new byte[cmd.length + data.length()];
        System.arraycopy(cmd, 0, result, 0, cmd.length);
        System.arraycopy(data.getBytes(), 0, result, cmd.length, data.length());

        return result;
    }

    /**
     * Fungsi untuk menset alignment pada teks yang diprint:
     * Valid n = 0, 48 : Left Alignment
     * Valid n = 1, 49 : Center Alignment
     * Valid n = 2, 50 : Right Alignment
     *
     * @param type diluar tipe di override ke tipe yang mendekatinya.
     * @return
     */
    public static byte[] SetAlignMode(byte type) {
        if (type < 3) {
            if (type < 0) type = 0;
        } else {
            if (type < 48) type = 48;
            if (type > 50) type = 50;
        }

        return new byte[]{CMD.ESC, 0x61, type};
    }

    /**
     * Funsi untuk melakukan cek kondisi tegangan battery dalam persentase.
     *
     * @return byte data yang dikirimkan ke printer
     */
    public static byte[] CheckBatteryVoltage() {
        return new byte[]{CMD.GS, 0x28, 0x45, 0x00,
                0x00, 0x00};
    }

    public static byte[] SetPrinterBluetoothName(String name) {
        if (name.length() > 16) return new byte[]{0, 0};
        else if (name.isEmpty()) return new byte[]{0, 1};
        else {
            byte size = (byte) name.length();
            size = (byte) (size + 0x03);
            byte[] nameArray = name.getBytes();
            byte[] commandTemp = {CMD.GS, 0x28, 0x45, 0x00, size, 0x0D, 65};
            byte[] rCMD = new byte[commandTemp.length + nameArray.length];
            System.arraycopy(commandTemp, 0, rCMD, 0, commandTemp.length);
            System.arraycopy(nameArray, 0, rCMD, commandTemp.length, nameArray.length);
            return rCMD;
        }
    }

    public static byte[] SetPrinterBluetoothPassKey(String key) {
        if (key.length() > 16) return new byte[]{0, 0};
        else if (key.isEmpty()) return new byte[]{0, 1};
        else {
            byte size = (byte) key.length();
            size = (byte) (size + 0x03);
            byte[] nameArray = key.getBytes();
            byte[] commandTemp = {CMD.GS, 0x28, 0x45, 0x00, size, 0x0D, 49};
            byte[] rCMD = new byte[commandTemp.length + nameArray.length];
            System.arraycopy(commandTemp, 0, rCMD, 0, commandTemp.length);
            System.arraycopy(nameArray, 0, rCMD, commandTemp.length, nameArray.length);
            return rCMD;
        }
    }

    public static byte[] SetReverseColorMode(boolean set) {
        if (set) return SetReverseColorMode((byte) 1);
        else return SetReverseColorMode((byte) 0);
    }

    public static byte[] SetReverseColorMode(byte x) {
        return new byte[]{CMD.GS, 0x42, x};
    }

    /**
     * Select Print Position of Human Readable Interpretation.
     * <p>
     * Valid type = 0, 48 : Not Printed.
     * Valid type = 1, 49 : Above the Barcode.
     * Valid type = 2, 50 : Below the Barcode.
     * Valid type = 3, 51 : Both above and below.
     *
     * @param type
     * @return
     */
    public static byte[] SetBarcodeHRIPosition(int type) {
        if (type < 4) {
            if (type < 0) type = 0;
        } else {
            if (type < 48) type = 48;
            if (type > 51) type = 51;
        }

        return new byte[]{
                CMD.GS, 0x48, (byte) type
        };
    }

    /**
     * Set Module Height of Barcode.
     *
     * Valid n = 0 : Disable Barcode Printing.
     * Valid n = 1-255 : Set Module Height.
     * Resetted By (ESC @) Command.
     *
     * @param height
     * @return
     */
    public static byte[] SetBarcodeHeight(int height){
        return new byte[] {
                CMD.GS, 0x4C, (byte) height
        };
    }

    /**
     * Set Module Width of Barcode.
     *
     * Override all input to 2 dots by documentation.
     * Resetted By (ESC @) Command.
     *
     * @param width only valid (2 dots)
     * @return
     */
    public static byte[] SetBarcodeWidth(int width){
        width = 2;
        return new byte[] {
                CMD.GS, 0x77, (byte) width
        };
    }


    /**
     * This function not checking your barcode data, any data not fit by our documentation will ignored by printer.
     *
     * Valid m = 0 : UPC-A. Valid k = 11,12.
     * Valid m = 1 : UPC-E. Valid k = 6,7,8,11,12.
     * Valid m = 2 : EAN13. Valid k = 12, 13.
     * Valid m = 3 : EAN8. Valid k = 7,8.
     * Valid m = 4 : CODE39. Valid k = Up to 14.
     * Valid m = 5 : ITF. Valid k = Up to 26.
     * Valid m = 6 : CODABAR. Valid k = Up to 21.
     *
     * k mean to data.length in this function
     *
     * @param type or m value
     * @param data barcode data to be inputed
     * @return data to be inserted to printer
     */
    public static byte[] PrintBarcode(int type, byte[] data){
        byte[] rCMD = {CMD.GS, 0x6B, (byte)type};
        byte[] result = new byte[data.length+rCMD.length+1];
        System.arraycopy(rCMD,0,result,0,rCMD.length);
        System.arraycopy(data,0,result,rCMD.length,data.length);
        result[data.length+rCMD.length] = 0;
        return result;
    }

    /**
     * This function not checking your barcode data, any data not fit by our documentation will ignored by printer.
     *
     * Valid m = 65 : UPC-A. Valid n = 11,12.
     * Valid m = 66 : UPC-E. Valid n = 6,7,8,11,12.
     * Valid m = 67 : EAN13. Valid n = 12, 13.
     * Valid m = 68 : EAN8. Valid n = 7,8.
     * Valid m = 69 : CODE39. Valid n = Up to 14.
     * Valid m = 70 : ITF. Valid n = Up to 26.
     * Valid m = 71 : CODABAR. Valid n = Up to 21.
     * Valid m = 72 : CODE93. Valid n = Up to 17.
     * Valid m = 73 : CODE128. Valid n = Up to 15.
     *
     * n mean to data.length in this function
     *
     * @param type or m value
     * @param data barcode data to be inputed
     * @return data to be inserted to printer
     */
    public static byte[] PrintBarcodeAlt(int type, byte[] data){
        byte[] rCMD = {CMD.GS, 0x6B, (byte)type, (byte) data.length};
        byte[] result = new byte[data.length+rCMD.length];
        System.arraycopy(rCMD,0,result,0,rCMD.length);
        System.arraycopy(data,0,result,rCMD.length,data.length);
        return result;
    }

    /**
     * Valid m = 0. Ignored.
     * Valid xH = 0 : Horizontal bytes amount.
     * Valid xL = 1-48 : Horizontal bytes amount.
     * Valid yH = 0-79 : Vertical bytes amount.
     * Valid yL = 1-255 : Vertical bytes amount.
     * Valid k = (xL + xH*256) * (yL + yH*256).
     *
     * Valid data.length = x*y;
     *
     * @param x
     * @param y
     * @param data
     * @return
     */
    public static byte[] PrintRasterBitImageData(int x, int y, byte[] data){
        byte xH = (byte) (x/256);
        byte xL = (byte) (x%256);
        byte yH = (byte) (y/256);
        byte yL = (byte) (y%256);

        byte[] rCMD = {CMD.GS, 0x76, 0x30, 0x00, xL, xH, yL, yH};
        byte[] result = new byte[data.length+rCMD.length];
        System.arraycopy(rCMD,0,result,0,rCMD.length);
        System.arraycopy(data,0,result,rCMD.length,data.length);
        return result;
    }

}
