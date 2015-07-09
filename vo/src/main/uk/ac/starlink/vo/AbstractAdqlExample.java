package uk.ac.starlink.vo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a type of example ADQL query.
 * The query text can be generated as a function of given service metadata.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2011
 */
public abstract class AbstractAdqlExample implements AdqlExample {

    private final String name_;
    private final String description_;
    private final URL url_;
    private static final int COL_COUNT = 3;
    private static final int ROW_COUNT = 1000;
    private static final Pattern[] RADEC_UCD_REGEXES = new Pattern[] {
        Pattern.compile( "^pos.eq.ra[_;.]?(.*)", Pattern.CASE_INSENSITIVE ),
        Pattern.compile( "^pos.eq.dec[_;.]?(.*)", Pattern.CASE_INSENSITIVE ),
    };
    private static final TableMeta DUMMY_TABLE = createDummyTable();

    /**
     * Constructor.
     *
     * @param   name  example name
     * @param   description   example short description
     */
    protected AbstractAdqlExample( String name, String description ) {
        name_ = name;
        description_ = description;
        url_ = null;
    }

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public URL getInfoUrl() {
        return url_;
    }

    /**
     * Indicates if a language string represents ADQL version 1.
     * If not, at time of writing, it's a fair guess that it's ADQL version 2.
     *
     * @param  lang  language string
     * @return   true if lang looks like ADQL 1
     */
    private static boolean isAdql1( String lang ) {
        return lang != null && lang.toUpperCase().startsWith( "ADQL-1." );
    }

    /**
     * Returns a breaker instance suitable for a given line break policy.
     *
     * @param  lineBreaks  whether line breaks are required
     * @return   breaker instance
     */
    public static Breaker createBreaker( boolean lineBreaks ) {
        return lineBreaks
             ? new Breaker() {
                   public String level( int ilev ) {
                       StringBuffer sbuf = new StringBuffer();
                       sbuf.append( '\n' );
                       for ( int i = 0; i < ilev; i++ ) {
                           sbuf.append( "   " );
                       }
                       return sbuf.toString();
                   }
               }
             : new Breaker() {
                   public String level( int ilev ) {
                       return "";
                   }
               };
    }

    /**
     * Returns a table ref for a given table and a given language variant.
     *
     * @param  table  table metadata object
     * @param   lang  language string
     */
    private static TableRef createTableRef( final TableMeta table,
                                            String lang ) {
        if ( ! isAdql1( lang ) ) {
            return new TableRef() {
                public String getColumnName( String cname ) {
                    return cname;
                }
                public String getIntroName() {
                    return table.getName();
                }
            };
        }
        else {
            return createAliasedTableRef( table, getAlias( table ) );
        }
    }

    /**
     * Returns a table ref with a given alias.
     *
     * @param  table  table
     * @param  alias  table alias
     * @return  table ref
     */
    private static TableRef createAliasedTableRef( final TableMeta table,
                                                   final String alias ) {
        return new TableRef() {
            public String getColumnName( String cname ) {
                return alias + "." + cname;
            }
            public String getIntroName() {
                return table.getName() + " AS " + alias;
            }
        };
    }

    /**
     * Returns multiple table refs for a given list of tables.
     *
     * @param  tables  tables to reference
     * @return   table refs
     */
    private static TableRef[] createTableRefs( final TableMeta[] tables ) {
        int nt = tables.length;
        String[] aliases = new String[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            aliases[ i ] = getAlias( tables[ i ] );
        }
        if ( new HashSet( Arrays.asList( aliases ) ).size() < nt ) {
            for ( int i = 0; i < nt; i++ ) {
                aliases[ i ] = new StringBuffer().append( (char) ('a' + i) )
                                                 .toString();
            }
        }
        TableRef[] trefs = new TableRef[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            trefs[ i ] = createAliasedTableRef( tables[ i ], aliases[ i ] );
        }
        return trefs;
    }

    /**
     * Interface for configurable line breaking.
     */
    public static abstract class Breaker {

        /**
         * Returns a string which can be used to separate parts of an output
         * example string.
         *
         * @param   ilev  notional indentation level
         * @return   whitespace string
         */
        public abstract String level( int ilev );
    }

    /**
     * Interface for providing table references in ADQL text.
     */
    private static abstract class TableRef {

        /**
         * Returns the text by which a given column in this object's table
         * should be referred to in ADQL text.  The input name must be
         * quoted as appropriate, but additional table qualification
         * may be added by this method.
         *
         * @param  cname  basic column name
         * @return   name for use in ADQL
         */
        public abstract String getColumnName( String cname );

        /**
         * Returns the text with which this object's table should be
         * introduced in ADQL text.
         *
         * @return   table introductory text
         */
        public abstract String getIntroName();
    }

    /**
     * Encapsulates metadata for a table and a selection of colum names from it.
     */
    public static class TableWithCols {
        private final TableMeta table_;
        private final String[] cols_;

        /**
         * Constructor.
         *
         * @param  table   table metadata
         * @param  cols   column names from table
         */
        TableWithCols( TableMeta table, String[] cols ) {
            table_ = table;
            cols_ = cols;
        }

        /**
         * Returns the table object.
         *
         * @return  table
         */
        public TableMeta getTable() {
            return table_;
        }

        /**
         * Returns the columns array.
         * Column names are ADQL-ready as per {@link ColumnMeta#getName}.
         *
         * @return  array of column names of interest,
         *          each ready for use in ADQL
         */
        public String[] getColumns() {
            return cols_;
        }
    }

    /**
     * Returns a suitable ADQL alias for a given table.
     *
     * @param  table  table being referenced
     * @return  alias
     */
    private static String getAlias( TableMeta table ) {
        String tname = table.getName();
        String subname = tname == null
                       ? null
                       : tname.replaceFirst( "^[^\\.]*\\.", "" );
        char letter = '\0';
        if ( subname != null && subname.length() > 0 ) {
            letter = subname.charAt( 0 );
        }
        if ( ( letter >= 'a' && letter <= 'z' ) ||
             ( letter >= 'A' && letter <= 'Z' ) ) {
            return new String( new char[] { letter } );
        }
        else {
            return "t";
        }
    }

    /**
     * Utility function to turn a single table and a table array into a
     * single array.  The input single table may or may not appear in the
     * input table array; it will not appear twice in the output array.
     *
     * @param  table  single input table, or null
     * @param  tables  input table array
     * @return  output table array
     */
    public static TableMeta[] toTables( TableMeta table, TableMeta[] tables ) {
        List<TableMeta> tlist = new ArrayList<TableMeta>();
        if ( table != null ) {
            tlist.add( table );
        }
        if ( tables != null ) {
            for ( int i = 0; i < tables.length; i++ ) {
                if ( tables[ i ] != table ) {
                    tlist.add( tables[ i ] );
                }
            }
        }
        return (TableMeta[]) tlist.toArray( new TableMeta[ 0 ] );
    }

    /**
     * Identifies tables in a given array which contain RA/Dec positional
     * columns.
     *
     * @param  tables  candidate table list
     * @param  max   the maximum number of output tables required
     * @return  array of tables with RA/Dec columns
     */
    public static TableWithCols[] getRaDecTables( TableMeta[] tables,
                                                  int max ) {
        List<TableWithCols> tlist = new ArrayList<TableWithCols>();
        for ( int i = 0; i < tables.length && tlist.size() < max; i++ ) {
            TableMeta table = tables[ i ];
            ColumnMeta[] cols = table.getColumns();
            if ( cols != null ) {
                String[] radec = getRaDecDegreesNames( cols );
                if ( radec != null ) {
                    tlist.add( new TableWithCols( table, radec ) );
                }
            }
        }
        return tlist.toArray( new TableWithCols[ 0 ] );
    }

    /**
     * Returns the names for suitable RA/Dec columns in degrees from a table.
     * If no such column pair can be found, null is returned.
     *
     * @param   table  table to investiate
     * @return  2-element array with column names for RA, Dec respectively,
     *          or null if nothing suitable
     */
    private static String[] getRaDecDegreesNames( ColumnMeta[] cols ) {
        String[] coords = new String[ 2 ];
        int[] scores = new int[ 2 ];
        for ( int ic = 0; ic < cols.length; ic++ ) {
            ColumnMeta col = cols[ ic ];
            String ucd = col.getUcd();
            String unit = col.getUnit();
            String name = col.getName();
            if ( name != null && name.trim().length() > 0 &&
                 ucd != null && ucd.trim().length() > 0 &&
                 ( unit == null || unit.trim().length() == 0
                                || unit.toLowerCase().startsWith( "deg" ) ) ) {
                for ( int id = 0; id < 2; id++ ) {
                    Matcher matcher = RADEC_UCD_REGEXES[ id ].matcher( ucd );
                    if ( matcher.matches() ) {
                        int score = 1;
                        String trailer = matcher.group( 1 );
                        if ( trailer == null || trailer.trim().length() == 0 ) {
                            score = 2;
                        }
                        else if ( trailer.toLowerCase().equals( "main" ) ) {
                            score = 4;
                        }
                        else if ( trailer.toLowerCase().startsWith( "main" ) ) {
                            score = 3;
                        }
                        if ( col.isIndexed() ) {
                            score += 2;
                        }
                        if ( score > scores[ id ] ) {
                            scores[ id ] = score;
                            coords[ id ] = name;
                        }
                    }
                }
            }
        }
        return scores[ 0 ] > 0 && scores[ 1 ] > 0 ? coords : null;
    }

    /**
     * Returns a dummy example which never provides any text.
     *
     * @return  dummy example
     */
    public static AdqlExample createDummyExample() {
        return new AbstractAdqlExample( "Dummy", "Never enabled" ) {
            public String getText( boolean lineBreaks, String lang,
                                   TapCapability tcap, TableMeta[] tables,
                                   TableMeta table ) {
                return null;
            }
        };
    }

    /**
     * Returns a selection of examples.
     *
     * @return   example list
     */
    public static AdqlExample[] createSomeExamples() {
        return new AdqlExample[] {

            new AbstractAdqlExample( "Full table",
                                     "All columns from a single table" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( table == null &&
                         tables != null && tables.length > 0 ) {
                        table = tables[ 0 ];
                    }
                    if ( table == null ) {
                        table = DUMMY_TABLE;
                    }
                    return new StringBuffer()
                        .append( "SELECT TOP " )
                        .append( ROW_COUNT )
                        .append( " * FROM " )
                        .append( table.getName() )
                        .toString();
                }
            },

            new AbstractAdqlExample( "Columns from table",
                                     "Selection of columns from "
                                   + "a single table" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    TableMeta ptable = getPopulatedTable( table, tables );
                    Breaker breaker = createBreaker( lineBreaks );
                    TableRef tref = createTableRef( ptable, lang );
                    ColumnMeta[] cols = ptable.getColumns();
                    final String colSelection;
                    if ( cols != null && cols.length > 0 ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < COL_COUNT && i < cols.length;
                              i++ ) {
                            if ( i > 0 ) {
                                sbuf.append( ", " );
                            }
                            sbuf.append( tref
                                        .getColumnName( cols[ i ].getName() ) );
                        }
                        colSelection = sbuf.toString();
                    }
                    else {
                        colSelection = "*";
                    }
                    return new StringBuffer()
                        .append( "SELECT" )
                        .append( breaker.level( 1 ) )
                        .append( "TOP " )
                        .append( ROW_COUNT )
                        .append( breaker.level( 1 ) )
                        .append( colSelection )
                        .append( breaker.level( 1 ) )
                        .append( "FROM" )
                        .append( ' ' )
                        .append( tref.getIntroName() )
                        .toString();
                }
                private TableMeta getPopulatedTable( TableMeta table,
                                                     TableMeta[] tables ) {
                    if ( isPopulated( table ) ) {
                        return table;
                    }
                    if ( tables != null ) {
                        for ( TableMeta t : tables ) {
                            if ( isPopulated( t ) ) {
                                return t;
                            }
                        }
                    }
                    return DUMMY_TABLE;
                }
                private boolean isPopulated( TableMeta table ) {
                    if ( table != null ) {
                        ColumnMeta[] cols = table.getColumns();
                        if ( cols != null ) {
                            return cols.length >= COL_COUNT;
                        }
                    }
                    return false;
                }
            },

            new AbstractAdqlExample( "Count rows",
                                     "Count the rows in a table" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( table == null &&
                         tables != null && tables.length > 0 ) {
                        table = tables[ 0 ];
                    }
                    if ( table == null ) {
                        table = DUMMY_TABLE;
                    }
                    return new StringBuffer()
                        .append( "SELECT COUNT(*) FROM " )
                        .append( table.getName() )
                        .toString();
                }
            },

            new AbstractAdqlExample( "Box selection",
                                     "Select rows based on rectangular "
                                   + "RA/Dec position constraints" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    TableWithCols[] rdTabs =
                        getRaDecTables( toTables( table, tables ), 1 );
                    if ( rdTabs.length == 0 ) {
                        return null;
                    }
                    TableMeta rdTab = rdTabs[ 0 ].getTable();
                    String[] radec = rdTabs[ 0 ].getColumns();
                    String raCol = radec[ 0 ];
                    String decCol = radec[ 1 ];
                    Breaker breaker = createBreaker( lineBreaks );
                    TableRef tref = createTableRef( rdTab, lang );
                    return new StringBuffer()
                        .append( "SELECT" )
                        .append( breaker.level( 1 ) )
                        .append( "TOP " )
                        .append( ROW_COUNT )
                        .append( breaker.level( 1 ) )
                        .append( "*" )
                        .append( breaker.level( 1 ) )
                        .append( "FROM " )
                        .append( tref.getIntroName() )
                        .append( breaker.level( 1 ) )
                        .append( "WHERE" )
                        .append( breaker.level( 2 ) )
                        .append( tref.getColumnName( raCol ) )
                        .append( " BETWEEN 189.1 AND 189.3" )  // HDF
                        .append( breaker.level( 2 ) )
                        .append( "AND" )
                        .append( breaker.level( 2 ) )
                        .append( tref.getColumnName( decCol ) )
                        .append( " BETWEEN 62.18 AND 62.25" )  // HDF
                        .toString();
                }
            },

            new AbstractAdqlExample( "Cone selection",
                                     "Select rows within a given radius of "
                                   + "a sky position" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( isAdql1( lang ) ) {
                        return null;
                    }
                    TableWithCols[] rdTabs =
                        getRaDecTables( toTables( table, tables ), 1 );
                    if ( rdTabs.length == 0 ) {
                        return null;
                    }
                    TableMeta rdTab = rdTabs[ 0 ].getTable();
                    String[] radec = rdTabs[ 0 ].getColumns();
                    Breaker breaker = createBreaker( lineBreaks );
                    TableRef tref = createTableRef( rdTab, lang );
                    return new StringBuffer()
                        .append( "SELECT" )
                        .append( breaker.level( 1 ) )
                        .append( "TOP " )
                        .append( ROW_COUNT )
                        .append( breaker.level( 1 ) )
                        .append( "*" )
                        .append( breaker.level( 1 ) )
                        .append( "FROM " )
                        .append( tref.getIntroName() )
                        .append( breaker.level( 1 ) )
                        .append( "WHERE " )
                        .append( breaker.level( 2 ) )
                        .append( "1=CONTAINS(POINT('ICRS', " )
                        .append( tref.getColumnName( radec[ 0 ] ) )
                        .append( ", " )
                        .append( tref.getColumnName( radec[ 1 ] ) )
                        .append( ")," )
                        .append( breaker.level( 2 ) )
                        .append( "           " )
                        .append( "CIRCLE('ICRS', 189.2, 62.21, 0.05 )" )
                        .append( ")" )
                        .toString();
                }
            },

            new AbstractAdqlExample( "Sky pair match",
                                     "Join two tables on sky position" ) {
                public String getText( boolean lineBreaks, String lang,
                                       TapCapability tcap, TableMeta[] tables,
                                       TableMeta table ) {
                    if ( isAdql1( lang ) ) {
                        return null;
                    }
                    TableWithCols[] rdTabs =
                        getRaDecTables( toTables( table, tables ), 2 );
                    if ( rdTabs.length < 2 ) {
                        return null;
                    }
                    TableRef[] trefs =
                        createTableRefs( new TableMeta[] {
                            rdTabs[ 0 ].getTable(),
                            rdTabs[ 1 ].getTable() } );
                    TableRef tref1 = trefs[ 0 ];
                    TableRef tref2 = trefs[ 1 ];
                    String[] radec1 = rdTabs[ 0 ].getColumns();
                    String[] radec2 = rdTabs[ 1 ].getColumns();
                    Breaker breaker = createBreaker( lineBreaks );
                    return new StringBuffer()
                        .append( "SELECT" )
                        .append( breaker.level( 1 ) )
                        .append( "TOP " )
                        .append( ROW_COUNT )
                        .append( breaker.level( 1 ) )
                        .append( "*" )
                        .append( breaker.level( 1 ) )
                        .append( "FROM " )
                        .append( tref1.getIntroName() )
                        .append( breaker.level( 1 ) )
                        .append( "JOIN " )
                        .append( tref2.getIntroName() )
                        .append( breaker.level( 1 ) )
                        // CONTAINS is not mandatory, though INTERSECTS is.
                        // However, Markus has problems with INTERSECTS and
                        // POINTs, so avoid it here.
                        .append( "ON 1=CONTAINS(POINT('ICRS', " )
                        .append( tref1.getColumnName( radec1[ 0 ] ) )
                        .append( ", " )
                        .append( tref1.getColumnName( radec1[ 1 ] ) )
                        .append( ")," )
                        .append( breaker.level( 1 ) )
                        .append( "              CIRCLE('ICRS', " )
                        .append( tref2.getColumnName( radec2[ 0 ] ) )
                        .append( ", " )
                        .append( tref2.getColumnName( radec2[ 1 ] ) )
                        .append( ", 5./3600." )
                        .append( "))" )
                        .toString();
                }
            },
        };
    }

    /**
     * Creates an uninteresting table which ought to be OK to use for examples.
     *
     * @return  metadata table
     */
    private static TableMeta createDummyTable() {

        /* TAP_SCHEMA should always be present. */
        TableMeta table = new TableMeta() {{ name_ = "TAP_SCHEMA.tables"; }};
        table.setColumns( new ColumnMeta[] {
            new ColumnMeta() {{ name_ = "table_name"; }},
            new ColumnMeta() {{ name_ = "schema_name"; }},
        } );
        return table;
    }
}
