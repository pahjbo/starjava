package uk.ac.starlink.treeview.votable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DOMUtils;

class TabledataTable extends Table {

    private Element tabledataEl;
    private Element nextRowElement;
    private int ncols;
    private int nrows = -1;

    TabledataTable( Element tableEl, Element tabledataEl ) {
        super( tableEl );
        this.tabledataEl = tabledataEl;
        this.nextRowElement = firstTRSibling( tabledataEl.getFirstChild() );
        this.ncols = getNumColumns();
    }

    public boolean hasNextRow() {
        return nextRowElement != null;
    }

    public Object[] nextRow() {
        if ( nextRowElement == null ) {
            throw new NoSuchElementException();
        }
        List cellNodes = getTDElements( nextRowElement );
        nextRowElement = firstTRSibling( nextRowElement.getNextSibling() );

        Object[] rowContents = new Object[ ncols ];
        int ncell = cellNodes.size();
        for ( int i = 0; i < ncols; i++ ) {
            if ( i < ncell ) {
                String txt = DOMUtils
                            .getTextContent( (Element) cellNodes.get( i ) );
                rowContents[ i ] = getField( i )
                                  .getDatatype().decodeString( txt );
            }
            else {
                rowContents[ i ] = null;
            }
        }
        return rowContents;
    }

    public int getNumRows() {
        if ( nrows < 0 ) {
            nrows = 0;
            for ( Node row = tabledataEl.getFirstChild(); row != null;
                  row = row.getNextSibling() ) {
                if ( row instanceof Element &&
                     ((Element) row).getTagName().equals( "TR" ) ) {
                    nrows++;
                }
            }
        }
        return nrows;
    }

    private static List getTDElements( Node rowNode ) {
        List tds = new ArrayList();
        for ( Node tdNode = rowNode.getFirstChild(); tdNode != null;
              tdNode = tdNode.getNextSibling() ) {
            if ( tdNode instanceof Element &&
                 ((Element) tdNode).getTagName().equals( "TD" ) ) {
                tds.add( tdNode );
            }
        }
        return tds;
    }

    private static Element firstTRSibling( Node node ) {
        if ( node == null ) {
            return null;
        }
        else if ( node instanceof Element && 
                  ((Element) node).getTagName().equals( "TR" ) ) {
            return (Element) node;
        }
        else {
            return firstTRSibling( node.getNextSibling() );
        }
    }
}
