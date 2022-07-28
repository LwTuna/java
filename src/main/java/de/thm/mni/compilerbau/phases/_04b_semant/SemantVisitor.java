package de.thm.mni.compilerbau.phases._04b_semant;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.absyn.visitor.Visitable;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.utils.SplError;

public class SemantVisitor extends DoNothingVisitor {


    private SymbolTable symbolTable;

    public SemantVisitor(SymbolTable globalTable) {
        this.symbolTable = globalTable;
    }

    @Override
    public void visit(Program program) {
        Entry procedureEntry = symbolTable.lookup(new Identifier("main"));
        if (procedureEntry == null) {
            throw SplError.MainIsMissing();
        }

        if (!(procedureEntry instanceof ProcedureEntry)) {
            throw SplError.MainIsNotAProcedure();
        }

        if (((ProcedureEntry) procedureEntry).parameterTypes.size() > 0) {
            throw SplError.MainMustNotHaveParameters();
        }

        for (Visitable procedure : program.declarations) {
            procedure.accept(this);
        }
    }

    @Override
    public void visit(ProcedureDeclaration procedureDeclaration) {
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);
        SemantVisitor procedureVisitor = new SemantVisitor(procedureEntry.localTable);
        for (Statement statement : procedureDeclaration.body) {
            statement.accept(procedureVisitor);
        }
    }

    //Callstatement, ifstatement,whilestatement,assignstatement,compundstatement,binaryexpresssion,variableexpression
    //NamedVariable,ArrayAccess, IntLiteral

    @Override
    public void visit(CallStatement callStatement) {
        if (symbolTable.lookup(callStatement.procedureName) == null) {
            throw SplError.UndefinedProcedure(callStatement.position, callStatement.procedureName);
        }
        if (!(symbolTable.lookup(callStatement.procedureName) instanceof ProcedureEntry)) {
            throw SplError.CallOfNonProcedure(callStatement.position, callStatement.procedureName);
        } else {
            ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
            if (callStatement.arguments.size() > procedureEntry.parameterTypes.size()) {
                throw SplError.TooManyArguments(callStatement.position, callStatement.procedureName);
            } else if (callStatement.arguments.size() < procedureEntry.parameterTypes.size()) {
                throw SplError.TooFewArguments(callStatement.position, callStatement.procedureName);
            }

            for (int i = 0; i < callStatement.arguments.size(); i++) {
                callStatement.arguments.get(i).accept(this);
                if (!callStatement.arguments.get(i).dataType.equals(procedureEntry.parameterTypes.get(i).type)) {
                    throw SplError.ArgumentTypeMismatch(callStatement.position, callStatement.procedureName, i, callStatement.arguments.get(i).dataType, procedureEntry.parameterTypes.get(i).type);
                }
                if (procedureEntry.parameterTypes.get(i).isReference) {
                    if (!(callStatement.arguments.get(i) instanceof VariableExpression)) {
                        throw SplError.ArgumentMustBeAVariable(callStatement.arguments.get(i).position, callStatement.procedureName, i);
                    }
                }
            }
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.condition.accept(this);
        if (!ifStatement.condition.dataType.equals(PrimitiveType.boolType)) {
            throw SplError.IfConditionMustBeBoolean(ifStatement.position, ifStatement.condition.dataType);
        }
        ifStatement.thenPart.accept(this);
        if (ifStatement.elsePart != null) {
            ifStatement.elsePart.accept(this);
        }
    }
    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.condition.accept(this);
        if (!whileStatement.condition.dataType.equals(PrimitiveType.boolType)) {
            throw SplError.WhileConditionMustBeBoolean(whileStatement.position, whileStatement.condition.dataType);
        }
        whileStatement.body.accept(this);
    }

    @Override
    public void visit(ArrayAccess arrayAccess){
        arrayAccess.index.accept(this);
        arrayAccess.array.accept(this);
        if (!arrayAccess.index.dataType.equals(PrimitiveType.intType)) {
            throw SplError.IndexingWithNonInteger(arrayAccess.position);
        }
        if(arrayAccess.array.dataType instanceof ArrayType){
            arrayAccess.dataType = ((ArrayType) arrayAccess.array.dataType).baseType;
        }else{
            throw SplError.IndexingNonArray(arrayAccess.position);
        }
    }

    public void visit(NamedVariable namedVariable){

        Entry e = symbolTable.lookup(namedVariable.name,SplError.UndefinedVariable(namedVariable.position,namedVariable.name));
        if(!(e instanceof VariableEntry) ){
            throw SplError.NotAVariable(namedVariable.position,namedVariable.name);
        }
        namedVariable.dataType = ((VariableEntry) e).type;
    }
    public void visit(AssignStatement assignStatement){
        assignStatement.target.accept(this);
        assignStatement.value.accept(this);
        if(!assignStatement.target.dataType.equals(assignStatement.value.dataType)){
            throw SplError.AssignmentRequiresIntegers(assignStatement.position,assignStatement.target.dataType  );
        }
        if(assignStatement.target instanceof ArrayAccess){
            if(!((ArrayAccess) assignStatement.target).dataType.equals(assignStatement.value.dataType)){
                throw SplError.AssignmentHasDifferentTypes(assignStatement.position,assignStatement.target.dataType,assignStatement.value.dataType);
            }
        }
    }

    public void visit(VariableExpression variableExpression){
        variableExpression.variable.accept(this);
        variableExpression.dataType = variableExpression.variable.dataType;
    }

    public void visit(CompoundStatement compoundStatement){

        for(Statement statement:compoundStatement.statements){
            statement.accept(this);
        }
    }

    public void visit(BinaryExpression binaryExpression){
        binaryExpression.leftOperand.accept(this);
        binaryExpression.rightOperand.accept(this);
        if(!binaryExpression.leftOperand.dataType.equals(binaryExpression.rightOperand.dataType))
            SplError.OperatorDifferentTypes(binaryExpression.position,binaryExpression.leftOperand.dataType,binaryExpression.rightOperand.dataType);
        if(binaryExpression.operator.isComparison()){
            if(!binaryExpression.leftOperand.dataType.equals(PrimitiveType.intType)){
                throw SplError.ComparisonNonInteger(binaryExpression.position,binaryExpression.leftOperand.dataType);
            }
            if(!binaryExpression.rightOperand.dataType.equals(PrimitiveType.intType)){
                throw SplError.ComparisonNonInteger(binaryExpression.position,binaryExpression.rightOperand.dataType);
            }
            binaryExpression.dataType = PrimitiveType.boolType;
        }else{
            if(!binaryExpression.leftOperand.dataType.equals(PrimitiveType.intType)){
                throw SplError.ArithmeticOperatorNonInteger(binaryExpression.position,binaryExpression.leftOperand.dataType);
            }
            if(!binaryExpression.rightOperand.dataType.equals(PrimitiveType.intType)){
                throw SplError.ArithmeticOperatorNonInteger(binaryExpression.position,binaryExpression.rightOperand.dataType);
            }
            binaryExpression.dataType = PrimitiveType.intType;
        }
    }
    public void visit(IntLiteral intLiteral){
        intLiteral.dataType = PrimitiveType.intType;
    }



}
