/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.sql.fishery;

// J2SE dependencies
import java.sql.*;
import javax.sql.RowSet;
import javax.sql.RowSetEvent;
import javax.sql.RowSetListener;

import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Arrays;
import java.io.Reader;
import java.io.InputStream;
import java.math.BigDecimal;
import javax.swing.event.EventListenerList;

// Geotools dependencies
import org.geotools.resources.Utilities;


/**
 * The environmental values for each station location.  This object is created as
 * a result of {@link EnvironmentTable#getRowSet} invocation. This is a connected
 * {@link RowSet} made of the juxtaposition of many {@link EnvironmentTableStep}
 * objects.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentRowSet implements RowSet, ResultSetMetaData {
    /**
     * Liste des objets à informer des changements apportés à cet objet <code>RowSet</code>.
     */
    private EventListenerList listenerList;

    /**
     * Un événements indiquant que cet objet <code>RowSet</code> a changé.
     * Ne sera construit que la première fois où il sera nécessaire.
     */
    private RowSetEvent event;

    /**
     * Les objets {@link ResultSet} sous-jacents. La première colonne de chacune
     * de ces requêtes doit être l'identifiant ID de la capture. Toutes les autres
     * colonnes sont des paramètres environnementaux. Les numéros ID de tous ces
     * {@link ResultSet}s <strong>doivent</strong> être en ordre croissant.
     */
    private final ResultSet[] results;

    /**
     * Objet {@link ResultSet} à utiliser pour un numéro de colonne donnée.
     * Pour une colonne numérotée <var>n</var>, le {@link ResultSet} a utiliser
     * est <code>resultMap[n-2]</code>.
     */
    private final ResultSet[] resultMap;

    /**
     * Index de la colonne dans {@link #resultMap} à utiliser pour un numéro de
     * colonne donnée. Ce tableau s'utilise de pair avec {@link #resultMap}.
     */
    private final int[] columnMap;

    /**
     * Titre des colonnes.
     */
    private final String[] columnLabels;

    /**
     * L'identifiant de la capture pour la ligne courante.
     */
    private int ID = Integer.MIN_VALUE;

    /**
     * Le dernier {@link ResultSet} utilisé.
     */
    private ResultSet last;

    /**
     * Construit un objet <code>CouplingResultSet</code>.
     *
     * @param  results Les objets <code>results</code>.
     * @param  labels Les titres des colonnes.
     * @throws SQLException si une erreur est survenue lors de l'accès à la base de données.
     */
    EnvironmentRowSet(final ResultSet[] results, final String[] labels) throws SQLException {
        columnLabels = labels;
        this.results = results;
        int count = 0;
        final int[] columnCount = new int[results.length];
        for (int i=0; i<results.length; i++) {
            final int c = results[i].getMetaData().getColumnCount()-1;
            if (c<0) {
                throw new IllegalArgumentException();
            }
            columnCount[i] = c;
            count += c;
        }
        if (count != labels.length-1) {
            throw new IllegalArgumentException();
        }
        resultMap = new ResultSet[count];
        columnMap = new int      [count];
        for (int i=results.length; --i>=0;) {
            int c = columnCount[i];
            Arrays.fill(resultMap, count-c, count, results[i]);
            while (--c>=0) {
                columnMap[--count] = c+2;
            }
        }
        if (count != 0) {
            throw new AssertionError();
        }
    }

    /**
     * Moves the cursor down one row from its current position.
     */
    public boolean next() throws SQLException {
        int alreadyMoved = -1;
        for (int i=0; i<results.length; i++) {
            if (i == alreadyMoved) {
                continue;
            }
            final ResultSet result = results[i];
            int candidate;
            do {
                if (!result.next()) {
                    return false;
                }
                candidate = result.getInt(1);
            }
            while (candidate < ID);
            if (candidate > ID) {
                ID = candidate;
                alreadyMoved = i;
                i = -1; // Redo all previous ResultSet.
            }
        }
        //
        // Préviens tous les objets intéressés que
        // cet objet a avancé d'un enregistrement.
        //
        if (listenerList != null) {
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;) {
                if (listeners[i] == RowSetListener.class) {
                    if (event == null) {
                        event = new RowSetEvent(this);
                    }
                    ((RowSetListener)listeners[i+1]).cursorMoved(event);
                }
            }
        }
        return true;
    }

    /**
     * Moves the cursor to the previous row in this <code>ResultSet</code> object.
     */
    public boolean previous() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Releases this <code>ResultSet</code> object's database and
     * JDBC resources immediately instead of waiting for
     * this to happen when it is automatically closed.
     */
    public void close() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i] != null) {
                results[i].close();
                results[i] = null;
            }
        }
    }

    /**
     * Reports whether the last column read had a value of SQL <code>NULL</code>.
     */
    public boolean wasNull() throws SQLException {
        if (last == null) {
            throw new SQLException();
        }
        if (last == this) {
            return false;
        }
        return last.wasNull();
    }

    /**
     * Convertit un numéro de colonne global en numéro de colonne dans le {@link ResultSet}
     * à utiliser. Le <code>ResultSet</code> à utilisé sera mémorisé dans la variable
     * {@link #last}.
     */
    private int toResultSet(int columnIndex) throws SQLException {
        if (--columnIndex == 0) {
            last = this;
            return 0;
        }
        if (--columnIndex>=0 && columnIndex<resultMap.length) {
            last = resultMap[columnIndex];
            return columnMap[columnIndex];
        }
        throw new SQLException("Numéro de colonne invalide.");
    }
    
    //======================================================================
    // Methods for accessing results by column index
    //======================================================================

    public Object getObject(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return new Integer(ID);
        }
        return last.getObject(columnIndex);
    }

    public Object getObject(int columnIndex, Map map) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return new Integer(ID);
        }
        return last.getObject(columnIndex, map);
    }

    public String getString(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return String.valueOf(ID);
        }
        return last.getString(columnIndex);
    }

    public boolean getBoolean(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return ID != 0;
        }
        return last.getBoolean(columnIndex);
    }

    public byte getByte(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return (byte) ID;
        }
        return last.getByte(columnIndex);
    }

    public short getShort(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return (short) ID;
        }
        return last.getShort(columnIndex);
    }

    public int getInt(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return ID;
        }
        return last.getInt(columnIndex);
    }

    public long getLong(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return ID;
        }
        return last.getLong(columnIndex);
    }

    public float getFloat(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return ID;
        }
        return last.getFloat(columnIndex);
    }

    public double getDouble(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return ID;
        }
        return last.getDouble(columnIndex);
    }

    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return new BigDecimal(ID);
        }
        return last.getBigDecimal(columnIndex);
    }

    /** @deprecated Use {@link #getBigDecimal(int)} instead. */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            return new BigDecimal(ID);
        }
        return last.getBigDecimal(columnIndex, scale);
    }

    public byte[] getBytes(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getBytes(columnIndex);
    }

    public Date getDate(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getDate(columnIndex);
    }

    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getDate(columnIndex, cal);
    }

    public Time getTime(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getTime(columnIndex);
    }

    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getTime(columnIndex, cal);
    }

    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getTimestamp(columnIndex);
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getTimestamp(columnIndex, cal);
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getAsciiStream(columnIndex);
    }

    /** @deprecated use {@link #getCharacterStream(int)} instead. */
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getUnicodeStream(columnIndex);
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getBinaryStream(columnIndex);
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getCharacterStream(columnIndex);
    }

    public URL getURL(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getURL(columnIndex);
    }

    public Ref getRef(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getRef(columnIndex);
    }

    public Blob getBlob(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getBlob(columnIndex);
    }

    public Clob getClob(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getClob(columnIndex);
    }

    public Array getArray(int columnIndex) throws SQLException {
        columnIndex = toResultSet(columnIndex);
        if (columnIndex == 0) {
            throw unsupportedOperation();
        }
        return last.getArray(columnIndex);
    }


    //======================================================================
    // Methods for accessing results by column name
    //======================================================================

    /**
     * Maps the given <code>ResultSet</code> column name to its
     * <code>ResultSet</code> column index.
     */
    public int findColumn(String columnName) throws SQLException {
        throw unsupportedOperation();
    }

    public Object getObject(String columnName) throws SQLException {
        return getObject(findColumn(columnName));
    }

    public Object getObject(String columnName, Map map) throws SQLException {
        return getObject(findColumn(columnName), map);
    }

    public String getString(String columnName) throws SQLException {
        return getString(findColumn(columnName));
    }

    public boolean getBoolean(String columnName) throws SQLException {
        return getBoolean(findColumn(columnName));
    }

    public byte getByte(String columnName) throws SQLException {
        return getByte(findColumn(columnName));
    }

    public short getShort(String columnName) throws SQLException {
        return getShort(findColumn(columnName));
    }

    public int getInt(String columnName) throws SQLException {
        return getInt(findColumn(columnName));
    }

    public long getLong(String columnName) throws SQLException {
        return getLong(findColumn(columnName));
    }

    public float getFloat(String columnName) throws SQLException {
        return getFloat(findColumn(columnName));
    }

    public double getDouble(String columnName) throws SQLException {
        return getDouble(findColumn(columnName));
    }

    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        return getBigDecimal(findColumn(columnName));
    }

    /** @deprecated Use {@link #getBigDecimal(String)} instead. */
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        return getBigDecimal(findColumn(columnName), scale);
    }

    public byte[] getBytes(String columnName) throws SQLException {
        return getBytes(findColumn(columnName));
    }

    public Date getDate(String columnName) throws SQLException {
        return getDate(findColumn(columnName));
    }

    public Date getDate(String columnName, Calendar cal) throws SQLException {
        return getDate(findColumn(columnName), cal);
    }

    public Time getTime(String columnName) throws SQLException {
        return getTime(findColumn(columnName));
    }

    public Time getTime(String columnName, Calendar cal) throws SQLException {
        return getTime(findColumn(columnName), cal);
    }

    public Timestamp getTimestamp(String columnName) throws SQLException {
        return getTimestamp(findColumn(columnName));
    }

    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
        return getTimestamp(findColumn(columnName), cal);
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
        return getAsciiStream(findColumn(columnName));
    }

    /** @deprecated use {@link #getCharacterStream(String)} instead. */
    public InputStream getUnicodeStream(String columnName) throws SQLException {
        return getUnicodeStream(findColumn(columnName));
    }

    public InputStream getBinaryStream(String columnName) throws SQLException {
        return getBinaryStream(findColumn(columnName));
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
        return getCharacterStream(findColumn(columnName));
    }

    public URL getURL(String columnName) throws SQLException {
        return getURL(findColumn(columnName));
    }

    public Ref getRef(String columnName) throws SQLException {
        return getRef(findColumn(columnName));
    }

    public Blob getBlob(String columnName) throws SQLException {
        return getBlob(findColumn(columnName));
    }

    public Clob getClob(String columnName) throws SQLException {
        return getClob(findColumn(columnName));
    }

    public Array getArray(String columnName) throws SQLException {
        return getArray(findColumn(columnName));
    }


    //=====================================================================
    // Advanced features:
    //=====================================================================

    /**
     * Retrieves the first warning reported by calls on this <code>ResultSet</code> object.
     * Subsequent warnings on this <code>ResultSet</code> object will be chained to the
     * <code>SQLWarning</code> object that this method returns.
     */
    public SQLWarning getWarnings() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Clears all warnings reported on this <code>ResultSet</code> object.
     */
    public void clearWarnings() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].clearWarnings();
        }
    }

    /**
     * Retrieves the name of the SQL cursor used by this <code>ResultSet</code> object.
     */
    public String getCursorName() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the number, types and properties of
     * this <code>ResultSet</code> object's columns.
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        return this;
    }

    //---------------------------------------------------------------------
    // Traversal/Positioning
    //---------------------------------------------------------------------

    /**
     * Retrieves whether the cursor is before the first row in 
     * this <code>ResultSet</code> object.
     */
    public boolean isBeforeFirst() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i].isBeforeFirst()) {
                return true;
            }
        }
        return false;
    }
      
    /**
     * Retrieves whether the cursor is after the last row in 
     * this <code>ResultSet</code> object.
     */
    public boolean isAfterLast() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i].isAfterLast()) {
                return true;
            }
        }
        return false;
    }
 
    /**
     * Retrieves whether the cursor is on the first row of
     * this <code>ResultSet</code> object.
     */
    public boolean isFirst() throws SQLException {
        throw unsupportedOperation();
    }
 
    /**
     * Retrieves whether the cursor is on the last row of 
     * this <code>ResultSet</code> object.
     */
    public boolean isLast() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Moves the cursor to the front of
     * this <code>ResultSet</code> object, just before the
     * first row. This method has no effect if the result set contains no rows.
     */
    public void beforeFirst() throws SQLException {
        ID = Integer.MIN_VALUE;
        for (int i=0; i<results.length; i++) {
            results[i].beforeFirst();
        }
    }

    /**
     * Moves the cursor to the end of
     * this <code>ResultSet</code> object, just after the
     * last row. This method has no effect if the result set contains no rows.
     */
    public void afterLast() throws SQLException {
        ID = Integer.MIN_VALUE;
        for (int i=0; i<results.length; i++) {
            results[i].afterLast();
        }
    }

    /**
     * Moves the cursor to the first row in
     * this <code>ResultSet</code> object.
     */
    public boolean first() throws SQLException {
        beforeFirst();
        return next();
    }

    /**
     * Moves the cursor to the last row in
     * this <code>ResultSet</code> object.
     */
    public boolean last() throws SQLException {
        afterLast();
        return previous();
    }

    /**
     * Retrieves the current row number.  The first row is number 1, the
     * second number 2, and so on.  
     */
    public int getRow() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Moves the cursor to the given row number in
     * this <code>ResultSet</code> object.
     */
    public boolean absolute(int row) throws SQLException {
        return relative(row-getRow());
    }

    /**
     * Moves the cursor a relative number of rows, either positive or negative.
     * Attempting to move beyond the first/last row in the
     * result set positions the cursor before/after the
     * the first/last row. Calling <code>relative(0)</code> is valid, but does
     * not change the cursor position.
     */
    public boolean relative(int rows) throws SQLException {
        if (rows > 0) {
            do if (!next()) return false;
            while (--rows != 0);
        } else if (rows < 0) {
            do if (!previous()) return false;
            while (++rows != 0);
        }
        return true;
    }

    /**
     * Gives a hint as to the direction in which the rows in this
     * <code>ResultSet</code> object will be processed. 
     */
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != FETCH_FORWARD) {
            throw unsupportedOperation();
        }
    }

    /**
     * Retrieves the fetch direction for this 
     * <code>ResultSet</code> object.
     */
    public int getFetchDirection() throws SQLException {
        return FETCH_FORWARD;
    }

    /**
     * Gives the JDBC driver a hint as to the number of rows that should 
     * be fetched from the database when more rows are needed for this 
     * <code>ResultSet</code> object.
     */
    public void setFetchSize(int rows) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the fetch size for this 
     * <code>ResultSet</code> object.
     */
    public int getFetchSize() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the type of this <code>ResultSet</code> object.  
     */
    public int getType() throws SQLException {
        return TYPE_FORWARD_ONLY;
    }

    /**
     * Retrieves the concurrency mode of this <code>ResultSet</code> object.
     */
    public int getConcurrency() throws SQLException {
        return CONCUR_READ_ONLY;
    }

    //=====================================================================
    // Updates
    //=====================================================================

    /**
     * Retrieves whether the current row has been updated.  The value returned 
     * depends on whether or not the result set can detect updates.
     */
    public boolean rowUpdated() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i].rowUpdated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves whether the current row has had an insertion.
     * The value returned depends on whether or not this
     * <code>ResultSet</code> object can detect visible inserts.
     */
    public boolean rowInserted() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i].rowInserted()) {
                return true;
            }
        }
        return false;
    }
   
    /**
     * Retrieves whether a row has been deleted.  A deleted row may leave
     * a visible "hole" in a result set.  This method can be used to
     * detect holes in a result set.  The value returned depends on whether 
     * or not this <code>ResultSet</code> object can detect deletions.
     */
    public boolean rowDeleted() throws SQLException {
        for (int i=0; i<results.length; i++) {
            if (results[i].rowDeleted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives a nullable column a null value.
     */
    public void updateNull(int columnIndex) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateBytes(int columnIndex, byte x[]) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw unsupportedOperation();
    }

    public void updateNull(String columnName) throws SQLException {
        updateNull(findColumn(columnName));
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException {
        updateBoolean(findColumn(columnName), x);
    }

    public void updateByte(String columnName, byte x) throws SQLException {
        updateByte(findColumn(columnName), x);
    }

    public void updateShort(String columnName, short x) throws SQLException {
        updateShort(findColumn(columnName), x);
    }

    public void updateInt(String columnName, int x) throws SQLException {
        updateInt(findColumn(columnName), x);
    }

    public void updateLong(String columnName, long x) throws SQLException {
        updateLong(findColumn(columnName), x);
    }

    public void updateFloat(String columnName, float x) throws SQLException {
        updateFloat(findColumn(columnName), x);
    }

    public void updateDouble(String columnName, double x) throws SQLException {
        updateDouble(findColumn(columnName), x);
    }

    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
        updateBigDecimal(findColumn(columnName), x);
    }

    public void updateString(String columnName, String x) throws SQLException {
        updateString(findColumn(columnName), x);
    }

    public void updateBytes(String columnName, byte x[]) throws SQLException {
        updateBytes(findColumn(columnName), x);
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        updateDate(findColumn(columnName), x);
    }

    public void updateTime(String columnName, Time x) throws SQLException {
        updateTime(findColumn(columnName), x);
    }

    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
        updateTimestamp(findColumn(columnName), x);
    }

    public void updateObject(String columnName, Object x) throws SQLException {
        updateObject(findColumn(columnName), x);
    }

    public void updateObject(String columnName, Object x, int scale) throws SQLException {
        updateObject(findColumn(columnName), x, scale);
    }

    public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
        updateAsciiStream(findColumn(columnName), x, length);
    }

    public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
        updateBinaryStream(findColumn(columnName), x, length);
    }

    public void updateCharacterStream(String columnName, Reader x, int length) throws SQLException {
        updateCharacterStream(findColumn(columnName), x, length);
    }
    
    public void updateRef(String columnName, Ref x) throws SQLException {
        updateRef(findColumn(columnName), x);
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
        updateBlob(findColumn(columnName), x);
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
        updateClob(findColumn(columnName), x);
    }

    public void updateArray(String columnName, Array x) throws SQLException {
        updateArray(findColumn(columnName), x);
    }

    /**
     * Inserts the contents of the insert row into this 
     * <code>ResultSet</code> object and into the database.  
     * The cursor must be on the insert row when this method is called.
     */
    public void insertRow() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Updates the underlying database with the new contents of the
     * current row of this <code>ResultSet</code> object.
     * This method cannot be called when the cursor is on the insert row.
     */
    public void updateRow() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].updateRow();
        }
    }

    /**
     * Deletes the current row from this <code>ResultSet</code> object 
     * and from the underlying database.  This method cannot be called when
     * the cursor is on the insert row.
     */
    public void deleteRow() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].deleteRow();
        }
    }

    /**
     * Refreshes the current row with its most recent value in 
     * the database.  This method cannot be called when
     * the cursor is on the insert row.
     */
    public void refreshRow() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].refreshRow();
        }
    }

    /**
     * Cancels the updates made to the current row in this
     * <code>ResultSet</code> object.
     */
    public void cancelRowUpdates() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].cancelRowUpdates();
        }
    }

    /**
     * Moves the cursor to the insert row.  The current cursor position is 
     * remembered while the cursor is positioned on the insert row.
     */
    public void moveToInsertRow() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Moves the cursor to the remembered cursor position, usually the
     * current row.  This method has no effect if the cursor is not on 
     * the insert row. 
     */
    public void moveToCurrentRow() throws SQLException {
        for (int i=0; i<results.length; i++) {
            results[i].moveToCurrentRow();
        }
    }

    /**
     * Retrieves the <code>Statement</code> object that produced this 
     * <code>ResultSet</code> object.
     * If the result set was generated some other way, such as by a
     * <code>DatabaseMetaData</code> method, this method returns 
     * <code>null</code>.
     */
    public Statement getStatement() throws SQLException {
        return null;
    }

    //=====================================================================
    // Setters
    //=====================================================================
    
    public void setNull(int columnIndex, int sqlType) throws SQLException {
        updateNull(columnIndex); // WARNING: sqlType is ignored.
    }

    public void setNull(int columnIndex, int sqlType, String typeName) throws SQLException {
        updateNull(columnIndex); // WARNING: sqlType and typeName are ignored.
    }

    public void setRef(int columnIndex, Ref x) throws SQLException {
        updateRef(columnIndex, x);
    }

    public void setObject(int columnIndex, Object x) throws SQLException {
        updateObject(columnIndex, x);
    }

    public void setObject(int columnIndex, Object x, int targetSqlType) throws SQLException {
        updateObject(columnIndex, x); // WARNING: sqlType is ignored.
    }

    public void setObject(int columnIndex, Object x, int targetSqlType, int scale) throws SQLException {
        updateObject(columnIndex, x, scale); // WARNING: sqlType is ignored.
    }
    
    public void setArray(int columnIndex, Array x) throws SQLException {
        updateArray(columnIndex, x);
    }

    public void setBoolean(int columnIndex, boolean x) throws SQLException {
        updateBoolean(columnIndex, x);
    }

    public void setByte(int columnIndex, byte x) throws SQLException {
        updateByte(columnIndex, x);
    }

    public void setBytes(int columnIndex, byte[] x) throws SQLException {
        updateBytes(columnIndex, x);
    }
    
    public void setShort(int columnIndex, short x) throws SQLException {
        updateShort(columnIndex, x);
    }
    
    public void setInt(int columnIndex, int x) throws SQLException {
        updateInt(columnIndex, x);
    }
    
    public void setLong(int columnIndex, long x) throws SQLException {
        updateLong(columnIndex, x);
    }

    public void setFloat(int columnIndex, float x) throws SQLException {
        updateFloat(columnIndex, x);
    }
    
    public void setDouble(int columnIndex, double x) throws SQLException {
        updateDouble(columnIndex, x);
    }
    
    public void setBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        updateBigDecimal(columnIndex, x);
    }

    public void setString(int columnIndex, String x) throws SQLException {
        updateString(columnIndex, x);
    }
    
    public void setDate(int columnIndex, Date x) throws SQLException {
        updateDate(columnIndex, x);
    }
    
    public void setDate(int columnIndex, Date x, Calendar calendar) throws SQLException {
        updateDate(columnIndex, x); // WARNING: Calendar is ignored.
    }
    
    public void setTime(int columnIndex, Time x) throws SQLException {
        updateTime(columnIndex, x);
    }
    
    public void setTime(int columnIndex, Time x, Calendar cal) throws SQLException {
        updateTime(columnIndex, x); // WARNING: Calendar is ignored.
    }
    
    public void setTimestamp(int columnIndex, Timestamp x) throws SQLException {
        updateTimestamp(columnIndex, x);
    }
    
    public void setTimestamp(int columnIndex, Timestamp x, Calendar cal) throws SQLException {
        updateTimestamp(columnIndex, x); // WARNING: Calendar is ignored.
    }
    
    public void setAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        updateAsciiStream(columnIndex, x, length);
    }

    public void setBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        updateBinaryStream(columnIndex, x, length);
    }

    public void setCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        updateCharacterStream(columnIndex, x, length);
    }
    
    public void setBlob(int columnIndex, Blob x) throws SQLException {
        updateBlob(columnIndex, x);
    }
    
    public void setClob(int columnIndex, Clob x) throws SQLException {
        updateClob(columnIndex, x);
    }


    //=====================================================================
    // Listeners
    //=====================================================================
    
    /**
     * Registers the given listener so that it will be notified of events
     * that occur on this <code>RowSet</code> object.
     */
    public void addRowSetListener(final RowSetListener listener) {
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        listenerList.add(RowSetListener.class, listener);
    }
    
    /**
     * Removes the specified listener from the list of components that will be
     * notified when an event occurs on this <code>RowSet</code> object.
     */
    public void removeRowSetListener(RowSetListener listener) {
        if (listenerList != null) {
            listenerList.remove(RowSetListener.class, listener);
        }
    }


    //=====================================================================
    // RowSet implementation
    //=====================================================================

    /**
     * Clears the parameters set for this <code>RowSet</code> object's command.
     */
    public void clearParameters() throws SQLException {
    }

    /**
     * Retrieves whether this <code>RowSet</code> object is read-only.
     */
    public boolean isReadOnly() {
        return true;
    }
    
    /**
     * Sets whether this <code>RowSet</code> object is read-only.
     */
    public void setReadOnly(boolean value) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Retrieves the logical name that identifies the data source for this
     * <code>RowSet</code> object.
     */
    public String getDataSourceName() {
        return null;
    }

    /**
     * Sets the data source name property for this <code>RowSet</code> object to the
     * given <code>String</code>.
     */
    public void setDataSourceName(String name) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the url property this <code>RowSet</code> object will use to
     * create a connection if it uses the <code>DriverManager</code>
     * instead of a <code>DataSource</code> object to establish the connection.
     */
    public String getUrl() throws SQLException {
        return null;
    }
    
    /**
     * Sets the URL this <code>RowSet</code> object will use when it uses the
     * <code>DriverManager</code> to create a connection.
     */
    public void setUrl(String url) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the username used to create a database connection for this
     * <code>RowSet</code> object.
     */
    public String getUsername() {
        return null;
    }
    
    /**
     * Sets the username property for this <code>RowSet</code> object to the
     * given <code>String</code>.
     */
    public void setUsername(String name) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the password used to create a database connection.
     */
    public String getPassword() {
        return null;
    }
    
    /**
     * Sets the database password for this <code>RowSet</code> object to
     * the given <code>String</code>.
     */
    public void setPassword(String password) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Retrieves this <code>RowSet</code> object's command property.
     */
    public String getCommand() {
        return null;
    }

    /**
     * Sets this <code>RowSet</code> object's command property to the given SQL query.
     */
    public void setCommand(String cmd) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves whether escape processing is enabled for this <code>RowSet</code> object.
     */
    public boolean getEscapeProcessing() throws SQLException {
        return true;
    }

    /**
     * Sets escape processing for this <code>RowSet</code> object on or off.
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the <code>Map</code> object associated with this
     * <code>RowSet</code> object, which specifies the custom mapping
     * of SQL user-defined types, if any.
     */
    public Map getTypeMap() throws SQLException {
        final Map map = new HashMap();
        for (int i=0; i<results.length; i++) {
            map.putAll(results[i].getStatement().getConnection().getTypeMap());
        }
        return map;
    }
    
    /**
     * Installs the given <code>java.util.Map</code> object as the default
     * type map for this <code>RowSet</code> object.
     */
    public void setTypeMap(java.util.Map map) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Sets the type of this <code>RowSet</code> object to the given type.
     */
    public void setType(int type) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Retrieves the transaction isolation level set for this <code>RowSet</code> object.
     */
    public int getTransactionIsolation() {
        int code = 0;
        for (int i=0; i<results.length; i++) {
            try {
                code |= results[i].getStatement().getConnection().getTransactionIsolation();
            } catch (SQLException exception) {
                // NOTE: Method in RowSet interface do not declare SQLException.
            }
        }
        return code;
    }
    
    /**
     * Sets the transaction isolation level for this <code>RowSet</code> obejct.
     */
    public void setTransactionIsolation(int level) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Sets the concurrency of this <code>RowSet</code> object to the given
     * concurrency level.
     */
    public void setConcurrency(int concurrency) throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Retrieves the maximum number of bytes that may be returned
     * for certain column values. Zero means that there is no limit.
     */
    public int getMaxFieldSize() throws SQLException {
        int max = 0;
        for (int i=0; i<results.length; i++) {
            final int candidate = results[i].getStatement().getMaxFieldSize();
            if (candidate!=0 && candidate<max) {
                max = candidate;
            }
        }
        return max;
    }
    
    /**
     * Sets the maximum number of bytes that can be returned for a column
     * value to the given number of bytes.
     */
    public void setMaxFieldSize(int max) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Retrieves the maximum number of rows that this <code>RowSet</code>
     * object can contain. Zero means unlimited.
     */
    public int getMaxRows() throws SQLException {
        int max = 0;
        for (int i=0; i<results.length; i++) {
            final int candidate = results[i].getStatement().getMaxRows();
            if (candidate!=0 && candidate<max) {
                max = candidate;
            }
        }
        return max;
    }
    
    /**
     * Sets the maximum number of rows that this <code>RowSet</code>
     * object can contain to the specified number.
     */
    public void setMaxRows(int max) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Retrieves the maximum number of seconds the driver will wait for
     * a statement to execute. Zero means unlimited.
     */
    public int getQueryTimeout() throws SQLException {
        int max = 0;
        for (int i=0; i<results.length; i++) {
            final int candidate = results[i].getStatement().getQueryTimeout();
            if (candidate!=0 && candidate<max) {
                max = candidate;
            }
        }
        return max;
    }
    
    /**
     * Sets the maximum time the driver will wait for
     * a statement to execute to the given number of seconds.
     */
    public void setQueryTimeout(int seconds) throws SQLException {
        throw unsupportedOperation();
    }
    
    /**
     * Fills this <code>RowSet</code> object with data.
     */
    public void execute() throws SQLException {
        throw unsupportedOperation();
    }

    /**
     * Construit une exception disant que l'opération
     * n'est pas supportée.
     */
    private static SQLException unsupportedOperation() {
        return new SQLException("Opération non-supportée.");
    }



    /////////////////////////////////////////////////////////////////
    ////////                                                 ////////
    ////////                M E T A - D A T A                ////////
    ////////                                                 ////////
    /////////////////////////////////////////////////////////////////

    /**
     * Returns the number of columns in this <code>ResultSet</code> object.
     */
    public int getColumnCount() throws SQLException {
        return columnMap.length + 1;
    }
    
    /**
     * Get the designated column's table's schema.
     */
    public String getSchemaName(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return null;
        }
        return last.getMetaData().getSchemaName(column);
    }
    
    /**
     * Gets the designated column's table name.
     */
    public String getTableName(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return null;
        }
        return last.getMetaData().getTableName(column);
    }
    
    /**
     * Gets the designated column's table's catalog name.
     */
    public String getCatalogName(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return null;
        }
        return last.getMetaData().getCatalogName(column);
    }
    
    /**
     * Retrieves the designated column's SQL type.
     */
    public int getColumnType(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return Types.INTEGER;
        }
        return last.getMetaData().getColumnType(column);
    }
    
    /**
     * Retrieves the designated column's database-specific type name.
     */
    public String getColumnTypeName(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return "int";
        }
        return last.getMetaData().getColumnTypeName(column);
    }
    
    /**
     * Returns the fully-qualified name of the Java class whose instances
     * are manufactured if the method <code>ResultSet.getObject</code>
     * is called to retrieve a value from the column.
     */
    public String getColumnClassName(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return Integer.class.getName();
        }
        return last.getMetaData().getColumnClassName(column);
    }
    
    /**
     * Indicates the nullability of values in the designated column.
     */
    public int isNullable(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return columnNoNulls;
        }
        return last.getMetaData().isNullable(column);
    }
    
    /**
     * Indicates whether the designated column can be used in a where clause.
     */
    public boolean isSearchable(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isSearchable(column);
    }
    
    /**
     * Indicates whether a write on the designated column will definitely succeed.
     */
    public boolean isDefinitelyWritable(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isDefinitelyWritable(column);
    }
    
    /**
     * Indicates whether it is possible for a write on the designated column to succeed.
     */
    public boolean isWritable(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isWritable(column);
    }

    /**
     * Indicates whether the designated column is definitely not writable.
     */
    public boolean isReadOnly(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return true;
        }
        return last.getMetaData().isReadOnly(column);
    }

    /**
     * Indicates whether the designated column is automatically numbered, thus read-only.
     */
    public boolean isAutoIncrement(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isAutoIncrement(column);
    }
    
    /**
     * Indicates whether a column's case matters.
     */
    public boolean isCaseSensitive(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isCaseSensitive(column);
    }
    
    /**
     * Indicates whether the designated column is a cash value.
     */
    public boolean isCurrency(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return false;
        }
        return last.getMetaData().isCurrency(column);
    }
    
    /**
     * Indicates whether values in the designated column are signed numbers.
     */
    public boolean isSigned(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return true;
        }
        return last.getMetaData().isSigned(column);
    }
    
    /**
     * Get the designated column's number of decimal digits.
     */
    public int getPrecision(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return 10;
        }
        return last.getMetaData().getPrecision(column);
    }
    
    /**
     * Gets the designated column's number of digits to right of the decimal point.
     */
    public int getScale(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return 0;
        }
        return last.getMetaData().getScale(column);
    }
    
    /**
     * Indicates the designated column's normal maximum width in characters.
     */
    public int getColumnDisplaySize(int column) throws SQLException {
        column = toResultSet(column);
        if (column == 0) {
            return 10;
        }
        return last.getMetaData().getColumnDisplaySize(column);
    }
    
    /**
     * Gets the designated column's suggested title for use in printouts and
     * displays.
     */
    public String getColumnLabel(int column) throws SQLException {
        if (--column>=0 && column<columnLabels.length) {
            return columnLabels[column];
        }
        throw new SQLException("Numéro de colonne invalide.");
    }
    
    /**
     * Get the designated column's name. The default implementation use
     * the label name and replace Unicode character by some ASCII character.
     */
    public String getColumnName(int column) throws SQLException {
        final StringBuffer label = new StringBuffer(getColumnLabel(column));
        for (int i=label.length(); --i>=0;) {
            char c = Utilities.toNormalScript(label.charAt(i));
            switch (c) {
                case '-':      c='a'; break;
                case '+':      c='p'; break;
                case '\u225E': c='p'; break;  // Pixel value (mesured by...)
                case '\u2207': c='g'; break;  // Gradient (nabla)
            }
            label.setCharAt(i, c);
        }
        return label.toString();
    }
}
