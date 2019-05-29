package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.DummyJELRowReader;
import uk.ac.starlink.ttools.jel.RandomJELRowReader;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;

/**
 * Filter for making algebraic (JEL) assertions about table data contents.
 *
 * @author   Mark Taylor
 * @since    2 May 2006
 */
public class AssertFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public AssertFilter() {
        super( "assert", "<expr>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Check that a boolean expression is true for each row.",
            "If the expression <code>&lt;expr&gt;</code> does not",
            "evaluate true for any row of the table, execution terminates",
            "with an error.",
            "As long as no error occurs, the output table is identical",
            "to the input one.",
            "</p>",
            "<p>The exception generated by an assertion violation is of class",
            "<code>" + AssertException.class.getName() + "</code>",
            "although that is not usually obvious if you are running from",
            "the shell in the usual way.",
            "</p>",
            explainSyntax( new String[] { "expr" } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        final String expr;
        if ( argIt.hasNext() ) {
            expr = argIt.next();
            argIt.remove();
        }
        else {
            throw new ArgException( "Missing expression" );
        }
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                try {
                    return new JELAssertionTable( base, expr );
                }
                catch ( CompilationException e ) {
                    throw JELUtils.toIOException( e, expr );
                }
            }
        };
    }

    /**
     * StarTable implementation which performs assertions.
     * Table behaviour is exactly the same as that of the base table,
     * except that an {@link AssertException} will be thrown if the assertion
     * is violated.
     */
    private static class JELAssertionTable extends WrapperStarTable {
        private final String expr_;
        private final StarTable baseTable_;
        private final RandomJELRowReader randomReader_;
        private final CompiledExpression compEx_;

        /**
         * Constructor.
         *
         * @param  baseTable  base input table
         * @param  expr  JEL expression to assert
         */
        public JELAssertionTable( StarTable baseTable, String expr )
                throws CompilationException {
            super( baseTable );
            baseTable_ = baseTable;
            expr_ = expr;
            randomReader_ = new RandomJELRowReader( baseTable );
            Library lib =
                JELUtils.getLibrary( new DummyJELRowReader( baseTable ) );
            compEx_ = JELUtils.compile( lib, baseTable, expr );
            JELUtils.checkExpressionType( lib, baseTable, expr, boolean.class );
        }

        public Object getCell( long irow, int icol ) throws IOException {
            Object cell = super.getCell( irow, icol );
            assertAtRow( irow );
            return cell;
        }

        public Object[] getRow( long irow ) throws IOException {
            Object[] row = super.getRow( irow );
            assertAtRow( irow );
            return row;
        }

        private void assertAtRow( long irow ) throws IOException {
            Object result;
            try {
                result = randomReader_.evaluateAtRow( compEx_, irow );
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( Error e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            check( result, irow );
        }

        public RowSequence getRowSequence() throws IOException {
            final SequentialJELRowReader seqReader =
                new SequentialJELRowReader( baseTable_ );
            Library lib = JELUtils.getLibrary( seqReader );
            final CompiledExpression compEx;
            try {
                compEx = JELUtils.compile( lib, baseTable_, expr_ );
            }
            catch ( CompilationException e ) {
                throw JELUtils.toIOException( e, expr_ );
            }
            return new WrapperRowSequence( seqReader ) {
                long lrow_;
                public boolean next() throws IOException {
                    boolean next = super.next();
                    if ( next ) {
                        Object result;
                        try {
                            result = seqReader.evaluate( compEx );
                        }
                        catch ( IOException e ) {
                            throw e;
                        }
                        catch ( RuntimeException e ) {
                            throw e;
                        }
                        catch ( Error e ) {
                            throw e;
                        }
                        catch ( Throwable e ) {
                            throw (IOException)
                                  new IOException( e.getMessage() )
                                 .initCause( e );
                        }
                        check( result, lrow_++ );
                    }
                    return next;
                }
            };
        }

        private void check( Object result, long irow ) throws AssertException {
            if ( ! ( result instanceof Boolean ) ||
                 ! ((Boolean) result).booleanValue() ) {
                throw new AssertException( "Assertion \"" + expr_ + "\" "
                                         + "violated at row " + ( irow + 1 ) );
            }
        }
    }
}
