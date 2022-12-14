package de.thm.mni.compilerbau.absyn;

import de.thm.mni.compilerbau.absyn.visitor.Visitor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class represents a statement that combines a list of statements into a single one.
 * <p>
 * {@link CompoundStatement}s are used whenever it is necessary to semantically combine multiple statements into a single one.
 * This is for example the case with {@link WhileStatement}s, which can only hold a single statement as their body.
 */
public class CompoundStatement extends Statement {
    public final List<Statement> statements;

    public final List<VariableDeclaration> variables;

    /**
     * Creates a new node representing a compound statement.
     *
     * @param position   The position of the statement in the source code.
     * @param statements The list of statements that this statement combines.
     */
    public CompoundStatement(Position position, List<Statement> statements, List<VariableDeclaration> variables) {
        super(position);
        this.statements = statements;
        this.variables = variables;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return formatAst("CompoundStatement", formatAst("Variables", variables.toArray()),formatAst("Body", statements.toArray()));
    }
}
