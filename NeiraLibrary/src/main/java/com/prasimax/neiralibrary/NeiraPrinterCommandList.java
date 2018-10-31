package com.prasimax.neiralibrary;

/**
 * Merupakan kelas yang digunakan oleh NeiraPrinterFunction.
 * Kelas ini berisi bytes data yang sudah terdefinisi.
 *
 * @author deani
 * @author Prasimax Software Team - Technology Center
 * @see  NeiraPrinterFunction
 */
public class NeiraPrinterCommandList {
    final byte ESC = 0x1B, FS = 0x1C, GS = 0x1D, LF=0x0A;

    final byte[] SelfTest = {0x1F, 0x11, 0x04};
    final byte[] DefaultLineSpacing = {ESC, 0x32},
            EMPHASIZE = {ESC, 'E'}, DPRINT ={ESC, 'G'};
}
