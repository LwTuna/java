package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.utils.SplError;

import java.util.ArrayList;
import java.util.List;

public class NodeVisitor extends DoNothingVisitor {

    SymbolTable table;
    boolean showTables;

    public NodeVisitor(SymbolTable symbolTable, boolean showTables) {
        this.table = symbolTable;
        this.showTables = showTables;
    }


    @Override
    public void visit(Program program) {
        program.declarations.forEach(decl -> decl.accept(this));
    }

    @Override
    public void visit(ProcedureDeclaration procedureDeclaration) {
        SymbolTable localTable = new SymbolTable(table);
        NodeVisitor localVisitor = new NodeVisitor(localTable, showTables);
        List<ParameterType> parameterTypes = new ArrayList<>();

        procedureDeclaration.parameters.stream().forEach(param -> {
            param.accept(localVisitor);
            parameterTypes.add(new ParameterType(param.typeExpression.dataType, param.isReference));
        });
        procedureDeclaration.variables.stream().forEach(var -> var.accept(localVisitor));

        table.enter(procedureDeclaration.name, new ProcedureEntry(localTable, parameterTypes), SplError.RedeclarationAsProcedure(procedureDeclaration.position, procedureDeclaration.name));

        if (showTables)
            TableBuilder.printSymbolTableAtEndOfProcedure(procedureDeclaration.name, (ProcedureEntry) table.find(procedureDeclaration.name).get());
    }

    @Override
    public void visit(TypeDeclaration typeDeclaration) {
        typeDeclaration.typeExpression.accept(this);

        table.enter(typeDeclaration.name, new TypeEntry(typeDeclaration.typeExpression.dataType), SplError.RedeclarationAsType(typeDeclaration.position, typeDeclaration.name));
    }


    @Override
    public void visit(VariableDeclaration variableDeclaration) {
        variableDeclaration.typeExpression.accept(this);

        table.enter(variableDeclaration.name, new VariableEntry(variableDeclaration.typeExpression.dataType, false), SplError.RedeclarationAsVariable(variableDeclaration.position, variableDeclaration.name));
    }

    @Override
    public void visit(ArrayTypeExpression arrayTypeExpression) {
        arrayTypeExpression.baseType.accept(this);
        arrayTypeExpression.dataType = new ArrayType(arrayTypeExpression.baseType.dataType, arrayTypeExpression.arraySize);
    }

    @Override
    public void visit(NamedTypeExpression namedTypeExpression) {
        Entry e = table.find(namedTypeExpression.name).orElseThrow(() -> SplError.UndefinedType(namedTypeExpression.position, namedTypeExpression.name));
        if (!(e instanceof TypeEntry)) {
            throw SplError.NotAType(namedTypeExpression.position, namedTypeExpression.name);
        } else {
            TypeEntry te = (TypeEntry) e;
            namedTypeExpression.dataType = te.type;
        }
    }

    @Override
    public void visit(ParameterDeclaration parameterDeclaration) {
        NodeVisitor globalVisitor = new NodeVisitor(table.getUpperLevel().orElseThrow(), showTables);
        parameterDeclaration.typeExpression.accept(globalVisitor);


        if (!parameterDeclaration.isReference && parameterDeclaration.typeExpression.dataType instanceof ArrayType) {
            throw SplError.MustBeAReferenceParameter(parameterDeclaration.position, parameterDeclaration.name);
        }

        table.enter(parameterDeclaration.name, new VariableEntry(parameterDeclaration.typeExpression.dataType, parameterDeclaration.isReference), SplError.RedeclarationAsParameter(parameterDeclaration.position, parameterDeclaration.name));
    }

}
