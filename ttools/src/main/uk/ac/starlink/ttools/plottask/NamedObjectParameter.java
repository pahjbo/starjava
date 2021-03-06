package uk.ac.starlink.ttools.plottask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter subclass for selecting named options.
 * This resembles {@link uk.ac.starlink.task.ChoiceParameter} in that several
 * named choices are available.
 * However, it is also possible to select options which are not in the
 * known option list.  For this to work, a pair of methods
 * {@link #toString(java.lang.Object)} and {@link #fromString(java.lang.String)}
 * must be implemented as inverses of each other so that a string can be
 * turned into an object.
 * The supplied options do not need to have names which follow this scheme.
 *
 * <strong>Note:</strong> this class duplicates some of the functionality
 * in other Parameter subclasses.  It's here for historical reasons.
 * New code in general ought not to make use of this class.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2008
 */
public abstract class NamedObjectParameter<T> extends Parameter<T> {

    private final List<NamedOption<T>> optList_;
    private boolean usageSet_;

    /**
     * Constructs a new parameter with no named options.
     *
     * @param  name  parameter name
     */
    public NamedObjectParameter( String name, Class<T> clazz ) {
        super( name, clazz, true );
        optList_ = new ArrayList<NamedOption<T>>();
    }

    /**
     * Adds an option with an associated name.
     * This name does not need to be <code>toString(option)</code>.
     *
     * @param  name  option alias
     * @param  option  option value object
     */
    public void addOption( String name, T option ) {
        optList_.add( new NamedOption<T>( name, option ) );
    }

    public T stringToObject( Environment env, String sval )
            throws TaskException {
        for ( NamedOption<T> opt : optList_ ) {
            if ( opt.name_.equalsIgnoreCase( sval ) ) {
                return opt.option_;
            }
        }
        try {
            return fromString( sval );
        }
        catch ( RuntimeException e ) {
            throw new ParameterValueException( this, "Bad format " + sval, e );
        }
    }

    /**
     * Sets the default value of this parameter as an option value object. 
     * <code>option</code> must be either one of the values added using
     * {@link #addOption} or {@link #toString(java.lang.Object)} must be
     * able to translate it.  Or it could be null.
     *
     * @param  option  new default value as an object
     */
    public void setDefaultOption( T option ) {
        if ( option == null ) {
            super.setStringDefault( null );
            return;
        }
        for ( NamedOption<T> opt : optList_ ) {
            if ( opt.option_.equals( option ) ) {
                super.setStringDefault( opt.name_ );
                return;
            }
        }
        super.setStringDefault( toString( option ) );
    }

    /**
     * Translates a possible option value of this parameter into a string
     * which represents it as a string value.
     *
     * @param  option   object value
     * @return  corresponding string
     */
    public String toString( T option ) {
        return option.toString();
    }

    /**
     * Translates a string value for this parameter into the object value
     * which it represents.  Must return a suitable object value for this
     * parameter, or throw an unchecked exception.
     *
     * <p>The implementation must be such that
     * <code>fromString(toString(o)).equals(o)</code>.
     *
     * @param   name   option name
     * @return   corresponding option value
     */
    public abstract T fromString( String name );

    /**
     * Returns a formatted XML string giving an unordered list of the options
     * for this parameter.  Suitable for insertion into a parameter description.
     * Not enclosed in a &lt;p&gt; element.
     *
     * @return  option list XML string
     */
    public String getOptionList() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<ul>\n" );
        for ( NamedOption<T> opt : optList_ ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<![CDATA[" )
                .append( opt.name_ )
                .append( "]]>" )
                .append( "</code>" )
                .append( "</li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    /**
     * Returns the names of all the named options known for this parameter.
     *
     * @return  name list
     */
    public String[] getNames() {
        String[] names = new String[ optList_.size() ];
        for ( int i = 0; i < names.length; i++ ) {
            names[ i ] = ((NamedOption) optList_.get( i )).name_;
        }
        return names;
    }

    /**
     * Returns the option objects for all the named options known for this
     * parameter.
     *
     * @return   object list
     */
    public T[] getOptions() {
        List<T> list = new ArrayList<T>();
        for ( int i = 0; i < optList_.size(); i++ ) {
            list.add( optList_.get( i ).option_ );
        }
        return toArray( list );
    }

    public void setUsage( String usage ) {
        usageSet_ = true;
        super.setUsage( usage );
    }

    public String getUsage() {
        if ( usageSet_ ) {
            return super.getUsage();
        }
        else {

            /* Truncates usage message to a few items and an ellipsis if it
             * would otherwise be too long.  Styles typically have a
             * lot of options. */
            int nopt = optList_.size();
            StringBuffer sbuf = new StringBuffer();
            if ( nopt > 4 ) {
                for ( int i = 0; i < 2; i++ ) {
                    sbuf.append( optList_.get( i ).name_ );
                    sbuf.append( '|' );
                }
                sbuf.append( "..." );
            }
            else {
                for ( int i = 0; i < nopt; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( '|' );
                    }
                    sbuf.append( optList_.get( i ).name_ );
                }
            }
            return sbuf.toString();
        }
    }

    /**
     * Utility class which aggregates a name and an object.
     */
    private static class NamedOption<T> {
        final String name_;
        final T option_;
        NamedOption( String name, T option ) {
            name_ = name;
            option_ = option;
        }
    }
}
