package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.absyn.ProcedureDeclaration;
import de.thm.mni.compilerbau.absyn.Program;
import de.thm.mni.compilerbau.absyn.VariableDeclaration;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitable;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;

public class VarallocVisitor extends DoNothingVisitor {

    private final SymbolTable symbolTable;
    private final VarAllocProcedureVisitor procedureVisitor;

    public VarallocVisitor(SymbolTable globalTable) {
        this.symbolTable = globalTable;
        this.procedureVisitor = new VarAllocProcedureVisitor(globalTable);
    }

    public void visit(Program program) {
        for (Visitable procedure : program.declarations) {
            if (procedure instanceof ProcedureDeclaration) {
                procedure.accept(this);
            }
        }

        program.accept(procedureVisitor);
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);
        SymbolTable localTable = procedureEntry.localTable;

        var varOffset = 0;
        for (VariableDeclaration variableDeclaration : procedureDeclaration.variables) {
            var variableEntry = (VariableEntry) localTable.lookup(variableDeclaration.name);
            varOffset -= variableEntry.type.byteSize;
            variableEntry.offset = varOffset;
        }

        var argOffset = 0;
        for (int i = 0; i < procedureEntry.parameterTypes.size(); i++) {
            var parameterType = procedureEntry.parameterTypes.get(i);
            parameterType.offset = argOffset;
            VariableEntry parameterEntry = (VariableEntry) localTable.lookup(procedureDeclaration.parameters.get(i).name);
            parameterEntry.offset = argOffset;
            argOffset += parameterType.isReference ? VarAllocator.REFERENCE_BYTESIZE : parameterType.type.byteSize;
        }
        procedureEntry.stackLayout.argumentAreaSize = argOffset;
        procedureEntry.stackLayout.localVarAreaSize = -varOffset;
    }
}
