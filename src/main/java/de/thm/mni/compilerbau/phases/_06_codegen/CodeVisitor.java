package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.PrimitiveType;

public class CodeVisitor extends DoNothingVisitor {
    private final CodePrinter printer;
    private final SymbolTable symbolTable;
    private Register register;
    private int labelCounter;


    public CodeVisitor(CodePrinter printer, SymbolTable symbolTable) {
        this.printer = printer;
        this.symbolTable = symbolTable;
        this.register = new Register(7);
        this.labelCounter = 0;
    }

    public CodeVisitor(CodePrinter printer, SymbolTable symbolTable, int labelCounter) {
        this.printer = printer;
        this.symbolTable = symbolTable;
        this.register = new Register(7);
        this.labelCounter = labelCounter;
    }

    public void visit(Program program) {
        for (int i = 0; i < program.declarations.size(); i++) {
            program.declarations.get(i).accept(this);
        }
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        printer.emitExport(procedureDeclaration.name.toString());
        printer.emitLabel(procedureDeclaration.name.toString());

        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name);
        printer.emitInstruction("sub", new Register(29), new Register(29), procedureEntry.stackLayout.frameSize(),"allocate frame");
        printer.emitInstruction("stw", new Register(25), new Register(29), procedureEntry.stackLayout.oldFramePointerOffset(), "save old frame pointer");
        printer.emitInstruction("add", new Register(25), new Register(29), procedureEntry.stackLayout.frameSize(), "setup new frame pointer");

        if (!procedureEntry.stackLayout.isLeafProcedure()) {
            printer.emitInstruction("stw", new Register(31), new Register(25), procedureEntry.stackLayout.oldReturnAddressOffset(), "save return register");
        }

        CodeVisitor codeVisitor = new CodeVisitor(printer, procedureEntry.localTable, this.labelCounter);
        procedureDeclaration.body.stream().forEach(x -> x.accept(codeVisitor));
        labelCounter = codeVisitor.labelCounter;

        if (!procedureEntry.stackLayout.isLeafProcedure()) {
            printer.emitInstruction("ldw", new Register(31), new Register(25), procedureEntry.stackLayout.oldReturnAddressOffset(),"restore return register");
        }

        printer.emitInstruction("ldw", new Register(25), new Register(29), procedureEntry.stackLayout.oldFramePointerOffset(),"restore old frame pointer");
        printer.emitInstruction("add", new Register(29), new Register(29), procedureEntry.stackLayout.frameSize(),"release frame");

        printer.emitInstruction("jr", new Register(31), "return");
    }
    public void visit(IfStatement statement){
        /*String elseLabel = generateNewLabel();
        String endLabel = !(statement.elsePart instanceof EmptyStatement) ?  generateNewLabel() : elseLabel;

        BinaryExpression condition = (BinaryExpression) statement.condition;
        printBinaryCondition(condition,elseLabel);
        statement.thenPart.accept(this);

        if(!(statement.elsePart instanceof EmptyStatement)) {
            printer.emitInstruction("j", endLabel);
            printer.emitLabel(elseLabel);
            statement.elsePart.accept(this);
        }
        printer.emitLabel(endLabel);*/
        String skipthenlabel = generateNewLabel();
        printBinaryCondition((BinaryExpression) statement.condition, skipthenlabel);
        statement.thenPart.accept(this);
        if(!(statement.elsePart instanceof EmptyStatement)) {
            String skipElseLabel = generateNewLabel();
            printer.emitInstruction("j", skipElseLabel);
            printer.emitLabel(skipthenlabel);
            statement.elsePart.accept(this);
            printer.emitLabel(skipElseLabel);
        }else {
            printer.emitLabel(skipthenlabel);
        }

    }


    public void printBinaryCondition(BinaryExpression expression, String label) {
        expression.leftOperand.accept(this);
        expression.rightOperand.accept(this);
        String mmnemonic = switch(expression.operator.flipComparison()){
            case EQU-> "beq";
            case NEQ-> "bne";
            case LST-> "blt";
            case LSE-> "ble";
            case GRT-> "bgt";
            case GRE-> "bge";
            default -> throw new IllegalStateException("Unexpected value: " + expression.operator.flipComparison());
        };
        printer.emitInstruction(mmnemonic, register.previous(), register, label);
        register = register.minus(2);
    }

    //CallStatement

    public void visit(CallStatement callStatement){
        ProcedureEntry procedureEntry = (ProcedureEntry) symbolTable.lookup(callStatement.procedureName);
        for(int i = 0; i < callStatement.arguments.size(); i++){
            if(procedureEntry.parameterTypes.get(i).isReference){
                ((VariableExpression)callStatement.arguments.get(i)).variable.accept(this);
            }else {
                callStatement.arguments.get(i).accept(this);
            }
            //TODO Change Value
            printer.emitInstruction("stw", register, new Register(29), i* PrimitiveType.intType.byteSize, "store argument #"+i);
            register = register.minus(1);
        }
        printer.emitInstruction("jal", callStatement.procedureName.toString());
    }

    public void visit(BinaryExpression binaryExpression){
        binaryExpression.leftOperand.accept(this);
        binaryExpression.rightOperand.accept(this);
        if(binaryExpression.operator.isArithmetic()) {
            String mmnemonic = switch(binaryExpression.operator){
                case ADD -> "add";
                case SUB -> "sub";
                case MUL -> "mul";
                case DIV -> "div";
                default -> throw new IllegalStateException("Unexpected value: " + binaryExpression.operator);
            };
            printer.emitInstruction(mmnemonic, register.previous(), register.previous(), register);
        }

        register = register.minus(1);
    }

    public void visit(ArrayAccess arrayAccess) {
        int arraySize = ((ArrayType)arrayAccess.array.dataType).arraySize;
        arrayAccess.array.accept(this);
        arrayAccess.index.accept(this);
        printer.emitInstruction("add", register.next(), new Register(0), arraySize);
        printer.emitInstruction("bgeu",register,register.next(),"_indexError");
        printer.emitInstruction("mul",register,register,((ArrayType)arrayAccess.array.dataType).baseType.byteSize);
        register = register.minus(1);
        printer.emitInstruction("add",register,register,register.next());
    }

    public void visit(WhileStatement statement) {
        String conditionLabel = generateNewLabel();
        String endLabel = generateNewLabel();
        printer.emitLabel(conditionLabel);
        BinaryExpression condition = (BinaryExpression) statement.condition;
        printBinaryCondition(condition,endLabel);
        statement.body.accept(this);
        printer.emitInstruction("j", conditionLabel);
        printer.emitLabel(endLabel);
    }


    public void visit(CompoundStatement statement) {
        for (int i = 0; i < statement.statements.size(); i++) {
            statement.statements.get(i).accept(this);
        }
    }
    public void visit(VariableExpression expression) {
        expression.variable.accept(this);
        printer.emitInstruction("ldw", register, register, 0);
    }
    public void visit(AssignStatement statement) {
        statement.target.accept(this);
        statement.value.accept(this);
        printer.emitInstruction("stw", register, register.previous(), 0);
        register = register.minus(2);
    }
    public void visit(NamedVariable variable) {
        register = register.next();
        VariableEntry variableEntry = (VariableEntry) symbolTable.lookup(variable.name);
        printer.emitInstruction("add", register, new Register(25), variableEntry.offset);
        if (variableEntry.isReference) {
            printer.emitInstruction("ldw", register, register, 0);
        }
    }
    public void visit(IntLiteral literal) {
        register = register.next();
        printer.emitInstruction("add", register, new Register(0), literal.value);
    }

    public String generateNewLabel() {
        return "L" + labelCounter++;
    }
}
