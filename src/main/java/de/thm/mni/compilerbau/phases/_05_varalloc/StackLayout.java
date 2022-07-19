package de.thm.mni.compilerbau.phases._05_varalloc;

/**
 * This class describes the stack frame layout of a procedure.
 * It contains the sizes of the various subareas and provides methods to retrieve information about the stack frame required to generate code for the procedure.
 */
public class StackLayout {
    // The following values have to be set in phase 5
    public Integer argumentAreaSize = null;
    public Integer localVarAreaSize = null;
    public Integer outgoingAreaSize = null;

    /**
     * A leaf procedure is a procedure that does not call any other procedure in its body.
     *
     * @return whether the procedure this stack layout describes is a leaf procedure.
     */
    public boolean isLeafProcedure() {
        return outgoingAreaSize <= 0;
    }

    /**
     * @return The total size of the stack frame described by this object.
     */
    public int frameSize() {
        return isLeafProcedure() ? localVarAreaSize + 4 : localVarAreaSize + outgoingAreaSize + 8;
    }

    /**
     * @return The offset (starting from the new stack pointer) where the old frame pointer is stored in this stack frame.
     */
    public int oldFramePointerOffset() {
        return isLeafProcedure() ? 0 : outgoingAreaSize + 4;
    }

    /**
     * @return The offset (starting from the new frame pointer) where the old return adress is stored in this stack frame.
     */
    public int oldReturnAddressOffset() {
        return -(localVarAreaSize + 8);
    }
}
