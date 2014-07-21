import org.openscience.cdk.config.Elements;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AliphaticSymbolAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AromaticAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AromaticSymbolAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.AtomicNumberAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.RingMembershipAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalConnectionAtom;
import org.openscience.cdk.isomorphism.matchers.smarts.TotalHCountAtom;
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
 * @author John May
 */
public class SmartsAtomExprParser {

    static IQueryAtom parse(String str) throws IOException {
        return parse(CharBuffer.wrap(str.toCharArray()));
    }

    static IQueryAtom parse(CharBuffer buffer) throws IOException {

        // stack of atom predicates and operators
        Deque<IQueryAtom> atoms = new ArrayDeque<IQueryAtom>();
        Deque<Character> operators = new ArrayDeque<Character>();
        operators.push(Character.MAX_VALUE);

        while (buffer.hasRemaining()) {
            char c = buffer.get();
            if (isOperator(c))
                shunt(atoms, operators, c);
            else
                atoms.push(readPrimitive(buffer));
        }

        // apply remaining operators
        while (!operators.isEmpty())
            apply(operators.pop(), atoms);

        trace("\n");

        return atoms.pop();
    }

    static void shunt(Deque<IQueryAtom> atoms, Deque<Character> operators, char op) {
        while (precedence(operators.peek()) <= precedence(op))
            apply(operators.pop(), atoms);
        operators.push(op);
    }

    static boolean isOperator(char c) {
        return c == '!' || c == '&' || c == ',' || c == ';' || c == '(' || c == ')';
    }

    static int precedence(char c) {
        return c;
    }

    static void apply(char op, Deque<IQueryAtom> atoms) {
        if (op == '&' || op == ';')
            atoms.push(and(atoms.pop(), atoms.pop()));
        else if (op == ',')
            atoms.push(or(atoms.pop(), atoms.pop()));
        else if (op == '!')
            atoms.push(not(atoms.pop()));
        else
            return;
        trace("" + op);
    }


    static IQueryAtom readPrimitive(CharBuffer buffer) throws IOException {
        switch (buffer.get(buffer.position() - 1)) {
            case 'A':
                return newAliphaticQryAtm();
            case 'C':
                return newAliphaticQryAtm(6);
            case 'N':
                return newAliphaticQryAtm(7);
            case 'O':
                return newAliphaticQryAtm(8);
            case 'P':
                return newAliphaticQryAtm(15);
            case 'S':
                return newAliphaticQryAtm(16);

            case 'a':
                return newAromaticQryAtm();
            case 'c':
                return newAromaticQryAtm(6);
            case 'n':
                return newAromaticQryAtm(7);
            case 'o':
                return newAromaticQryAtm(8);
            case 'p':
                return newAromaticQryAtm(15);
            case 's':
                return newAromaticQryAtm(16);

            case '#':
                return newNumberQryAtm(parseNum(buffer));
            case 'X':
                return newConnectivityQryAtm(parseNum(buffer));
            case 'H':
                return newHydrogenCountQryAtm(parseNum(buffer));
            case 'R':
                return newRingMembershipQryAtom(parseNum(buffer));
            case 'v':
                return newValenceQryAtom(parseNum(buffer));
        }
        throw new IOException("Unknown predicate");
    }

    static int parseNum(CharBuffer buffer) throws IOException {
        int num = 0;
        char c = '\0';
        while (nextIsDigit(buffer))
            num = (num * 10) + toDigit(c = buffer.get());
        return num == 0 && c != '0' ? -1 : num;
    }

    static boolean nextIsDigit(CharBuffer buffer) {
        return buffer.hasRemaining() && isDigit(buffer.get(buffer.position()));
    }

    static int toDigit(char c) {
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

    static void trace(String str) {
        System.out.print(str + " ");
    }
}
            