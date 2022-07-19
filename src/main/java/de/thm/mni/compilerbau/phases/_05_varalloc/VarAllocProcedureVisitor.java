package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitable;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;

public class VarAllocProcedureVisitor extends DoNothingVisitor {

    private final SymbolTable symbolTable;
    private int maxCall;

    public VarAllocProcedureVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void visit(Program program) {
        for (Visitable procedure : program.declarations) {
            if (procedure instanceof ProcedureDeclaration) {
                procedure.accept(this);
            }
        }
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);

        maxCall = -1;
        for (Statement statement : procedureDeclaration.body) {
            statement.accept(this);
        }
        procedureEntry.stackLayout.outgoingAreaSize = maxCall;
    }

    public void visit(CompoundStatement compoundStatement) {
        for (Statement statement : compoundStatement.statements) {
            statement.accept(this);
        }
    }

    public void visit(IfStatement ifStatement) {
        ifStatement.thenPart.accept(this);
        ifStatement.elsePart.accept(this);
    }

    public void visit(WhileStatement whileStatement) {
        whileStatement.body.accept(this);
    }

    public void visit(CallStatement callStatement) {
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
        maxCall = Math.max(maxCall, procedureEntry.stackLayout.argumentAreaSize);
    }


}
