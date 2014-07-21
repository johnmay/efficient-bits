import org.openscience.cdk.config.Elements;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticSymbolAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AromaticAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AromaticSymbolAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AtomicNumberAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.ExplicitConnectionAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.FormalChargeAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.HybridizationNumberAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.ImplicitHCountAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.MassAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.RingMembershipAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.SmallestRingAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalConnectionAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalHCountAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalRingConnectionAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalValencyAtom;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.Character.isDigit;
import static org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom.and;
import static org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom.not;
import static org.openscience.cdk.isomorphism.matchers.smarts.LogicalOperatorAtom.or;

/**
 * <p>Example of parsing SMARTS expressions and reelecting operator precedence using the
 * Shunting-yard algorithm and reverse polish notation.</p>
 *
 * <p>The output produces an IQueryAtom predicate and traces the output to STDOUT.</p>
 *
 * @author John May
 */
public class SmartsAtomExprParser {

    public static final int NEGATION         = '!';
    public static final int CONJUNCTION_HIGH = '&';
    public static final int DISJUNCTION      = ',';
    public static final int CONJUNCTION_LOW  = ';';

    static IQueryAtom parse(String str) throws IOException {
        return parse(CharBuffer.wrap(str.toCharArray()));
    }

    static IQueryAtom parse(CharBuffer buffer) throws IOException {

        // stack of atom primitives and operators
        Deque<IQueryAtom> primitives = new ArrayDeque<IQueryAtom>();
        Deque<Character> operators = new ArrayDeque<Character>();
        operators.push(Character.MAX_VALUE);

        while (buffer.hasRemaining()) {
            char c = buffer.get();
            if (isOperator(c))
                shunt(primitives, operators, c);
            else
                primitives.push(readPrimitive(buffer));
        }

        // apply remaining operators
        while (!operators.isEmpty())
            apply(operators.pop(), primitives);

        trace("\n");

        return primitives.pop();
    }

    /**
     * Operators are "shunted" into a siding (stack) before being applied.
     *
     * @param atoms     stack of atom
     * @param operators stack of operators
     * @param op        operator code
     */
    static void shunt(Deque<IQueryAtom> atoms, Deque<Character> operators, char op) {
        while (precedence(operators.peek()) < precedence(op))
            apply(operators.pop(), atoms);
        operators.push(op);
    }

    /**
     * Applies an operator to the stack of atom primitives.
     *
     * @param op    operator
     * @param atoms stack of atoms
     */
    static void apply(char op, Deque<IQueryAtom> atoms) {
        trace("" + op);
        if (op == CONJUNCTION_HIGH || op == CONJUNCTION_LOW) {
            atoms.push(and(atoms.pop(), atoms.pop()));
        }
        else if (op == DISJUNCTION) {
            atoms.push(or(atoms.pop(), atoms.pop()));
        }
        else if (op == NEGATION) {
            atoms.push(not(atoms.pop()));
        }
    }

    /**
     * Read an atom primitive from the buffer.
     *
     * @param buffer traversable buffer of character input
     * @return atom primitive
     * @throws IOException unknown primitive
     */
    static IQueryAtom readPrimitive(CharBuffer buffer) throws IOException {
        switch (buffer.get(buffer.position() - 1)) {
            // all elements are not handled            
            case 'A': return newAliphaticQryAtm();
            case 'a': return newAromaticQryAtm();
            
            case 'C': return newAliphaticQryAtm(6);
            case 'N': return newAliphaticQryAtm(7);
            case 'O': return newAliphaticQryAtm(8);
            case 'P': return newAliphaticQryAtm(15);
            case 'S': return newAliphaticQryAtm(16);

            
            case 'c': return newAromaticQryAtm(6);
            case 'n': return newAromaticQryAtm(7);
            case 'o': return newAromaticQryAtm(8);
            case 'p': return newAromaticQryAtm(15);
            case 's': return newAromaticQryAtm(16);

            case '#': return newNumberQryAtm(readNonNegInt(buffer));
            case 'X': return newConnectivityQryAtm(readNonNegInt(buffer));
            case 'x': return newRingConnectivityQryAtm(readNonNegInt(buffer));
            case 'D': return newDegreeAtom(readNonNegInt(buffer));
            case 'H': return newHydrogenCountQryAtm(readNonNegInt(buffer));
            case 'h': return newImplHydrogenCountQryAtm(readNonNegInt(buffer));
            case 'R': return newRingMembershipQryAtom(readNonNegInt(buffer));
            case 'r': return newRingSizeQryAtom(readNonNegInt(buffer));
            case 'v': return newValenceQryAtom(readNonNegInt(buffer));
            case '^': return newHybridisationQryAtm(readNonNegInt(buffer));
            
            // atomic mass
            case '0': 
            case '1': 
            case '2': 
            case '3': 
            case '4': 
            case '5': 
            case '6': 
            case '7': 
            case '8': 
            case '9':
                buffer.position(buffer.position() - 1); // backup
                return newAtomicMassQryAtom(readNonNegInt(buffer));
            
            // charge
            case '+': return newChargeQryAtm(readNonNegInt(buffer));
            case '-': return newChargeQryAtm(-readNonNegInt(buffer));
            
        }
        throw new IOException("Unknown atom primitive");
    }

    /**
     * Determine if a character is an operator.
     *
     * @param c a character
     * @return the character is an operator
     */
    static boolean isOperator(char c) {
        return c == NEGATION || c == CONJUNCTION_HIGH || c == DISJUNCTION || c == CONJUNCTION_LOW;
    }

    /**
     * Access a value that defines relative operator precedence.
     *
     * @param op operator
     * @return the relative precedence
     */
    static int precedence(char op) {
        return op; // ASCII code point is precedence
    }

    /**
     * Read a non-negative integer from the buffer. If buffer contains no such value, -1 is
     * returned.
     *
     * @param buffer traversable buffer of character input
     * @return value
     * @throws IOException
     */
    static int readNonNegInt(CharBuffer buffer) throws IOException {
        int num = 0;
        char c = '\0';
        while (nextIsDigit(buffer))
            num = (num * 10) + toInt(c = buffer.get());
        return num == 0 && c != '0' ? -1 : num;
    }

    /** Util - check if the next character in the buffer is digit. */
    static boolean nextIsDigit(CharBuffer buffer) {
        return buffer.hasRemaining() && isDigit(buffer.get(buffer.position()));
    }

    /** Util - convert a digit character to an integer. */
    static int toInt(char c) {
        return c - '0';
    }

    static IQueryAtom newAliphaticQryAtm() {
        trace("A");
        return new AliphaticAtom(null);
    }

    static IQueryAtom newAliphaticQryAtm(int num) {
        trace(Elements.ofNumber(num).symbol());
        return new AliphaticSymbolAtom(Elements.ofNumber(num).symbol(), null);
    }

    static IQueryAtom newAromaticQryAtm() {
        trace("a");
        return new AromaticAtom(null);
    }

    static IQueryAtom newAromaticQryAtm(int num) {
        trace(Elements.ofNumber(num).symbol().toLowerCase());
        return new AromaticSymbolAtom(Elements.ofNumber(num).symbol(), null);
    }

    static IQueryAtom newHydrogenCountQryAtm(int num) {
        trace("H" + num);
        return new TotalHCountAtom(num, null);
    }

    static IQueryAtom newImplHydrogenCountQryAtm(int num) {
        trace("h" + num);
        return new ImplicitHCountAtom(num, null);
    }

    static IQueryAtom newValenceQryAtom(int num) {
        trace("v" + num);
        return new TotalValencyAtom(num, null);
    }

    static IQueryAtom newRingMembershipQryAtom(int num) {
        trace("R" + num);
        return new RingMembershipAtom(num, null);
    }

    static IQueryAtom newConnectivityQryAtm(int num) {
        trace("X" + num);
        return new TotalConnectionAtom(num, null);
    }

    static IQueryAtom newNumberQryAtm(int num) {
        trace("#" + num);
        return new AtomicNumberAtom(num, null);
    }

    private static IQueryAtom newRingConnectivityQryAtm(int num) {
        trace("R" + num);
        return new TotalRingConnectionAtom(num, null);
    }

    private static IQueryAtom newDegreeAtom(int num) {
        trace("D" + num);
        return new ExplicitConnectionAtom(num, null);
    }

    private static IQueryAtom newRingSizeQryAtom(int num) {
        trace("r" + num);
        return new SmallestRingAtom(num, null);
    }

    private static IQueryAtom newAtomicMassQryAtom(int num) {
        trace("" + num);
        return new MassAtom(num, null);
    }

    private static IQueryAtom newChargeQryAtm(int num) {
        if (num > 0) trace("+" + num);
        else trace("" + num);
        return new FormalChargeAtom(num, null);
    }
    
    private static IQueryAtom newHybridisationQryAtm(int num) {
        trace("^" + num);
        return new HybridizationNumberAtom(num, null);
    }


    static void trace(String str) {
        System.out.print(str + " ");
    }
}
            