import org.junit.Test;

import java.io.IOException;

public class SmartsAtomExprParserTest {

    @Test public void example1() throws IOException {
        SmartsAtomExprParser.parse("O&X1");
    }

    @Test public void example2() throws IOException {
        SmartsAtomExprParser.parse("!C&!N");
    }

    @Test public void example3() throws IOException {
        SmartsAtomExprParser.parse("C,c;X3&v4");
    }

    @Test public void example4() throws IOException {
        SmartsAtomExprParser.parse("N&!H0&X3");
    }

    @Test public void example5() throws IOException {
        SmartsAtomExprParser.parse("!#6&X4");
    }

    @Test public void example6() throws IOException {
        SmartsAtomExprParser.parse("O,S,#7,#15");
    }
}