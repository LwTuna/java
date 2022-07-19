package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.utils.SplError;

class Register {
    final int number;

    Register(int number) {
        this.number = number;

    }

    /**
     * Checks if the register is available for free use, so a value can be stored in it.
     * Only a few of the registers in the ECO32 system, are available for free use. Other registers hold special values
     * like the stack or frame pointer registers or are reserved for the systems use only.
     *
     * @return true is available for free use.
     */
    boolean isFreeUse() {
        return number >= 8 && number <= 23;
    }

    /**
     * Returns the register with the number of this
     *
     * @param subtrahend The number to subtract from this register's number.
     * @return the new register
     */
    Register minus(int subtrahend) {
        return new Register(number - subtrahend);
    }

    /**
     * @return The register preceding this register.
     */
    Register previous() {
        return new Register(number - 1);
    }

    /**
     * @return The register following this register.
     */
    Register next() {
        if(!new Register(number+1).isFreeUse()){
            throw SplError.RegisterOverflow();
        }
        return new Register(number + 1);
    }

    @Override
    public String toString() {
        return "$" + number;
    }
}
