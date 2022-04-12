package org.qbicc.machine.llvm;

/**
 *
 */
public enum SymbolMangling {
    ELF('e'),
    GOFF('l'),
    MIPS('m'),
    MACH_O('o'),
    COFF('x'),
    WIN_X86_COFF('w'),
    XCOFF('a')
    ;

    private final char ch;

    SymbolMangling(final char ch) {
        this.ch = ch;
    }

    public char getDataLayoutCharacter() {
        return ch;
    }
}
