/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.io;

// Input/output
import java.io.Writer;
import java.io.FilterWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

// Miscellaneous
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import net.seas.util.XArray;
import net.seas.util.XCharacter;
import net.seagis.resources.Utilities;
import javax.swing.text.StyleConstants;


/**
 * A character stream that can be used to format tables. Columns are separated
 * by tabulations (<code>'\t'</code>) and rows are separated by line terminators
 * (<code>'\r'</code>, <code>'\n'</code> or <code>"\r\n"</code>). Every table's
 * cells are stored in memory until {@link #flush()} is invoked. When invoked,
 * {@link #flush()} copy cell's contents to the underlying stream while replacing
 * tabulations by some amount of spaces. The exact number of spaces is computed
 * from cell's widths. <code>TableWriter</code> produces correct output when
 * displayed with a monospace font.
 * <br><br>
 * For example, the following code...
 *
 * <blockquote><pre>
 *     TableWriter out=new TableWriter(new OutputStreamWriter(System.out), 3);
 *     out.write("Prénom\tNom\n");
 *     out.nextLine('-');
 *     out.write("Idéphonse\tLaporte\nSarah\tCoursi\nYvan\tDubois");
 *     out.flush();
 * </pre></blockquote>
 *
 * ...produces the following output:
 *
 * <blockquote><pre>
 *      Prénom      Nom
 *      ---------   -------
 *      Idéphonse   Laporte
 *      Sarah       Coursi
 *      Yvan        Dubois
 * </pre></blockquote>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class TableWriter extends FilterWriter
{
    /**
     * A possible value for cell alignment. This
     * specifies that the text is aligned to the left
     * indent and extra whitespace should be placed on
     * the right.
     */
    public static final int ALIGN_LEFT = StyleConstants.ALIGN_LEFT;

    /**
     * A possible value for cell alignment. This
     * specifies that the text is aligned to the right
     * indent and extra whitespace should be placed on
     * the left.
     */
    public static final int ALIGN_RIGHT = StyleConstants.ALIGN_RIGHT;

    /**
     * A possible value for cell alignment. This
     * specifies that the text is aligned to the center
     * and extra whitespace should be placed equally on
     * the left and right.
     */
    public static final int ALIGN_CENTER = StyleConstants.ALIGN_CENTER;

    /**
     * Drawing-box characters. The last two characters
     * are horizontal and vertical line respectively.
     */
    private static final char[][] BOX = new char[][]
    {
        {// [0000]: single horizontal, single vertical
            '\u250C','\u252C','\u2510',
            '\u251C','\u253C','\u2524',
            '\u2514','\u2534','\u2518',
            '\u2500','\u2502'
        },
        {// [0001]: single horizontal, double vertical
            '\u2553','\u2565','\u2556',
            '\u255F','\u256B','\u2562',
            '\u2559','\u2568','\u255C',
            '\u2500','\u2551'
        },
        {// [0010]: double horizontal, single vertical
            '\u2552','\u2564','\u2555',
            '\u255E','\u256A','\u2561',
            '\u2558','\u2567','\u255B',
            '\u2550','\u2502'
        },
        {// [0011]: double horizontal, double vertical
            '\u2554','\u2566','\u2557',
            '\u2560','\u256C','\u2563',
            '\u255A','\u2569','\u255D',
            '\u2550','\u2551'
        }
    };

    /**
     * Default character for space.
     */
    private static final char SPACE = ' ';

    /**
     * Temporary string buffer. This buffer
     * contains only one cell's content.
     */
    private final StringBuffer buffer = new StringBuffer();

    /**
     * Cellules du tableau, de gauche à droite et de haut en bas. Par convention,
     * un objet {@link Cell} avec un champ {@link Cell#text} <code>null</code>
     * représente un changement de ligne (on peut aussi utiliser une valeur
     * <code>null</code> si {@link Cell#fill} est un espace).
     */
    private final List<Cell> cells = new ArrayList<Cell>();

    /**
     * Alignment for current and next cells.
     */
    private int alignment = ALIGN_LEFT;
    
    /**
     * Numéro de colonne en cours d'écriture. Ce champs est incrémenté à chaque
     * fois qu'on appelle {@link #nextColumn()} pour changer de colonne.
     */
    private int column;

    /**
     * Nombre de ligne en cours d'écriture. Ce champs est incrémenté à chaque
     * fois qu'on appelle {@link #nextLine()} pour changer de ligne.
     */
    private int row;

    /**
     * Largeur maximale de chaques colonnes. La longueur de ce tableau
     * est égale au nombre de colonnes de la table à écrire.
     */
    private int width[] = new int[0];

    /**
     * The column separator.
     */
    private final String separator;

    /**
     * The left table border.
     */
    private final String leftBorder;

    /**
     * The right table border.
     */
    private final String rightBorder;

    /**
     * Indique si les cellules pourront avoir plusieurs lignes.
     * Par défaut, elles ne le pourront pas. Les codes '\n' seront
     * alors interprétés comme signifiant qu'il faut changer de rangée.
     */
    private boolean multiLinesCells;

    /**
     * <code>true</code> if this <code>TableWriter</code>
     * has been constructed with the no-arg constructor.
     */
    private final boolean stringOnly;

    /**
     * Indique s'il faut ignorer le prochain caractère '\n'. Ce champ
     * sert à éviter de remplacer "\r\n" par deux retours chariots.
     */
    private boolean skipCR;

    /**
     * Create a new table writer with a default column separator.
     * Note: this writer may produces bad output on Windows console,
     * unless the underlying stream use the correct codepage (e.g.
     * <code>OutputStreamWriter(System.out,&nbsp;"Cp437")</code>).
     * To display the appropriate codepage for a Windows NT console,
     * type <code>chcp</code> on the command line.
     *
     * @param out Writer object to provide the underlying stream,
     *        or <code>null</code> if there is no underlying stream.
     *        If <code>out</code> is null, then the {@link #toString}
     *        method is the only way to get the table's content.
     */
    public TableWriter(final Writer out)
    {
        super(out!=null ? out : new StringWriter());
        stringOnly  = (out==null);
        leftBorder  =  "\u2551 ";
        rightBorder = " \u2551" ;
        separator   = " \u2502 ";
    }

    /**
     * Create a new table writer with the specified
     * amount of spaces as column separator.
     *
     * @param out Writer object to provide the underlying stream,
     *        or <code>null</code> if there is no underlying stream.
     *        If <code>out</code> is null, then the {@link #toString}
     *        method is the only way to get the table's content.
     * @param spaces Amount of white spaces to use as column separator.
     */
    public TableWriter(final Writer out, final int spaces)
    {this(out, Utilities.spaces(spaces));}

    /**
     * Create a new table writer with the specified column separator.
     *
     * @param out Writer object to provide the underlying stream,
     *        or <code>null</code> if there is no underlying stream.
     *        If <code>out</code> is null, then the {@link #toString}
     *        method is the only way to get the table's content.
     * @param separator String to write between columns. Drawing box characters
     *        are treated specially. For example <code>" \\u2502 "</code> can be
     *        used for a single-line box.
     */
    public TableWriter(final Writer out, final String separator)
    {
        super(out!=null ? out : new StringWriter());
        stringOnly = (out==null);
        final int length = separator.length();
        int lower=0;      while (lower<length && Character.isSpaceChar(separator.charAt(lower  ))) lower++;
        int upper=length; while (upper>0      && Character.isSpaceChar(separator.charAt(upper-1))) upper--;
        this.leftBorder  = separator.substring(lower);
        this.rightBorder = separator.substring(0, upper);
        this.separator   = separator;
    }

    /**
     * Ecrit dans le flot spécifié une bordure pour un des coins ou côtés du tableau.
     *
     * @param out Flot dans lequel écrire la bordure.
     * @param horizontalBorder -1 pour le côté gauche, +1 pour le côté droit et 0 pour le centre.
     * @param verticalBorder   -1 pour en haut,        +1 pour en bas        et 0 pour le centre.
     * @param horizontalChar   Caractère représentant une ligne horizontale.
     * @throws IOException si l'écriture a échouée.
     */
    private void writeBorder(final Writer out, final int horizontalBorder, final int verticalBorder, final char horizontalChar) throws IOException
    {
        /*
         * Obtiens les ensembles de caractères qui
         * conviennent pour la ligne horizontale.
         */
        int boxCount = 0;
        final char[][] box = new char[BOX.length][];
        for (int i=0; i<BOX.length; i++)
            if (BOX[i][9]==horizontalChar)
                box[boxCount++] = BOX[i];
        /*
         * Obtient une chaîne contenant les lignes verticales à
         * dessiner à gauche, à droite ou au centre de la table.
         */
        final String border;
        switch (horizontalBorder)
        {
            case -1: border = leftBorder;  break;
            case +1: border = rightBorder; break;
            case  0: border = separator;   break;
            default: throw new IllegalArgumentException(String.valueOf(horizontalBorder));
        }
        if (verticalBorder<-1 || verticalBorder>+1)
            throw new IllegalArgumentException(String.valueOf(verticalBorder));
        /*
         * Remplace les espaces par la ligne horizontale,
         * et les lignes verticales par une intersection.
         */
        final int index = (horizontalBorder+1) + (verticalBorder+1)*3;
        final int borderLength = border.length();
        for (int i=0; i<borderLength; i++)
        {
            char c=border.charAt(i);
            if (Character.isSpaceChar(c))
            {
                c = horizontalChar;
            }
            else for (int j=0; j<boxCount; j++)
            {
                if (box[j][10]==c)
                {
                    c = box[j][index];
                    break;
                }
            }
            out.write(c);
        }
    }

    /**
     * Set the desired behavior for EOL and tabulations characters.
     * <ul>
     *   <li>If <code>true</code>, EOL (<code>'\r'</code>, <code>'\n'</code> or
     *       <code>"\r\n"</code>) and tabulations (<code>'\t'</code>) characters
     *       are copied straight into the current cell, which mean that next write
     *       operations will continue inside the same cell.</li>
     *   <li>If <code>false</code>, then tabulations move to next column and EOL move
     *       to the first cell of next row (i.e. tabulation and EOL are equivalent to
     *       {@link #nextColumn()} and {@link #nextLine()} calls respectively).</li>
     * </ul>
     * The default value is <code>false</code>.
     *
     * @param multiLines <code>true</code> true if EOL are used for line feeds inside
     *        current cells, or <code>false</code> if EOL move to the next row.
     */
    public void setMultiLinesCells(final boolean multiLines)
    {
        synchronized (lock)
        {
            multiLinesCells = multiLines;
        }
    }

    /**
     * Tells if EOL characters are used for line feeds inside current cells.
     */
    public boolean isMultiLinesCells()
    {
        synchronized (lock)
        {
            return multiLinesCells;
        }
    }

    /**
     * Set the alignment for current and next cells. Change to the
     * alignment doesn't affect the alignment of previous cells and
     * previous rows. The default alignment is {@link #ALIGN_LEFT}.
     *
     * @param alignment Cell alignment. Must be {@link #ALIGN_LEFT}
     *        {@link #ALIGN_RIGHT} or {@link #ALIGN_CENTER}.
     */
    public void setAlignment(final int alignment)
    {
        switch (alignment)
        {
            case ALIGN_LEFT:
            case ALIGN_RIGHT:
            case ALIGN_CENTER:
            {
                synchronized (lock)
                {
                    this.alignment = alignment;
                    break;
                }
            }
            default: throw new IllegalArgumentException(String.valueOf(alignment));
        }
    }

    /**
     * Returns the alignment for current and next cells.
     *
     * @return Cell alignment: {@link #ALIGN_LEFT} (the default),
     *         {@link #ALIGN_RIGHT} or {@link #ALIGN_CENTER}.
     */
    public int getAlignment()
    {
        synchronized (lock)
        {
            return alignment;
        }
    }

    /**
     * Returns the number of rows in this table.
     * This count is reset to 0 by {@link #flush}.
     */
    private int getRowCount()
    {
        int count=row;
        if (column!=0) count++;
        return count;
    }

    /**
     * Returns the number of columns in this table.
     */
    private int getColumnCount()
    {return width.length;}

    /**
     * Write a single character. If {@link #isMultiLinesCells()}
     * is false (which is the default), then:
     * <ul>
     *   <li>Tabulations (<code>'\t'</code>) are replaced by {@link #nextColumn()} invocations.</li>
     *   <li>Line separators (<code>'\r'</code>, <code>'\n'</code> or <code>"\r\n"</code>) are replaced
     *       by {@link #nextLine()} invocations.</li>
     * </ul>
     *
     * @param c Character to write.
     */
    public void write(final int c)
    {
        synchronized (lock)
        {
            if (!multiLinesCells)
            {
                switch (c)
                {
                    case '\t':
                    {
                        nextColumn();
                        skipCR=false;
                        return;
                    }
                    case '\r':
                    {
                        nextLine();
                        skipCR=true;
                        return;
                    }
                    case '\n':
                    {
                        if (!skipCR) nextLine();
                        skipCR=false;
                        return;
                    }
                }
            }
            if (c<Character.MIN_VALUE || c>Character.MAX_VALUE)
                throw new IllegalArgumentException(String.valueOf(c));
            buffer.append((char)c);
            skipCR = false;
        }
    }

    /**
     * Write a string. Tabulations and line separators
     * are interpreted as by {@link #write(int)}.
     *
     * @param string String to write.
     */
    public void write(final String string)
    {write(string, 0, string.length());}

    /**
     * Write a portion of a string. Tabulations and line
     * separators are interpreted as by {@link #write(int)}.
     *
     * @param string String to write.
     * @param offset Offset from which to start writing characters.
     * @param length Number of characters to write.
     */
    public void write(final String string, int offset, int length)
    {
        if (offset<0 || length<0 || (offset+length)>string.length())
        {
            throw new IndexOutOfBoundsException();
        }
        if (length==0)
        {
            return;
        }
        synchronized (lock)
        {
            if (skipCR && string.charAt(offset)=='\n')
            {
                offset++;
                length--;
            }
            if (!multiLinesCells)
            {
                int upper=offset;
                for (; length!=0; length--)
                {
                    switch (string.charAt(upper++))
                    {
                        case '\t':
                        {
                            buffer.append(string.substring(offset, upper-1));
                            nextColumn();
                            offset=upper;
                            break;
                        }
                        case '\r':
                        {
                            buffer.append(string.substring(offset, upper-1));
                            nextLine();
                            if (length!=0 && string.charAt(upper)=='\n')
                            {
                                upper++;
                                length--;
                            }
                            offset=upper;
                            break;
                        }
                        case '\n':
                        {
                            buffer.append(string.substring(offset, upper-1));
                            nextLine();
                            offset=upper;
                            break;
                        }
                    }
                }
                length = upper-offset;
            }
            skipCR = (string.charAt(offset+length-1)=='\r');
            buffer.append(string.substring(offset, offset+length));
        }
    }

    /**
     * Write an array of characters. Tabulations and line
     * separators are interpreted as by {@link #write(int)}.
     *
     * @param cbuf Array of characters to be written.
     */
    public void write(final char cbuf[])
    {write(cbuf, 0, cbuf.length);}

    /**
     * Write a portion of an array of characters. Tabulations and
     * line separators are interpreted as by {@link #write(int)}.
     *
     * @param cbuf   Array of characters.
     * @param offset Offset from which to start writing characters.
     * @param length Number of characters to write.
     */
    public void write(final char cbuf[], int offset, int length)
    {
        if (offset<0 || length<0 || (offset+length)>cbuf.length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (length==0)
        {
            return;
        }
        synchronized (lock)
        {
            if (skipCR && cbuf[offset]=='\n')
            {
                offset++;
                length--;
            }
            if (!multiLinesCells)
            {
                int upper=offset;
                for (; length!=0; length--)
                {
                    switch (cbuf[upper++])
                    {
                        case '\t':
                        {
                            buffer.append(cbuf, offset, upper-offset-1);
                            nextColumn();
                            offset=upper;
                            break;
                        }
                        case '\r':
                        {
                            buffer.append(cbuf, offset, upper-offset-1);
                            nextLine();
                            if (length!=0 && cbuf[upper]=='\n')
                            {
                                upper++;
                                length--;
                            }
                            offset=upper;
                            break;
                        }
                        case '\n':
                        {
                            buffer.append(cbuf, offset, upper-offset-1);
                            nextLine();
                            offset=upper;
                            break;
                        }
                    }
                }
                length = upper-offset;
            }
            skipCR = (cbuf[offset+length-1]=='\r');
            buffer.append(cbuf, offset, length);
        }
    }

    /**
     * Write an horizontal separator.
     */
    public void writeHorizontalSeparator()
    {
        synchronized (lock)
        {
            if (column!=0 || buffer.length()!=0)
            {
                nextLine();
            }
            nextLine('\u2500');
        }
    }

    /**
     * Moves one column to the right. Next write
     * operations will occur in a new cell on the
     * same row.
     */
    public void nextColumn()
    {nextColumn(SPACE);}

    /**
     * Moves one column to the right. Next write operations will occur in a
     * new cell on the same row.  This method fill every remaining space in
     * the current cell with the specified character. For example calling
     * <code>nextColumn('*')</code> from the first character of a cell is
     * a convenient way to put a pad value in this cell.
     *
     * @param  fill Character filling the cell (default to whitespace).
     */
    public void nextColumn(final char fill)
    {
        synchronized (lock)
        {
            final String cellText = buffer.toString();
            cells.add(new Cell(cellText, alignment, fill));
            if (column >= width.length)
            {
                width = XArray.resize(width, column+1);
            }
            final int length = XCharacter.getSize(cellText).width;
            if (length>width[column])
            {
                width[column]=length;
            }
            column++;
            buffer.setLength(0);
        }
    }

    /**
     * Moves to the first column on the next row.
     * Next write operations will occur on a new row.
     */
    public void nextLine()
    {nextLine(SPACE);}

    /**
     * Moves to the first column on the next row. Next write operations will
     * occur on a new row. This method fill every remaining cell in the current
     * row with the specified character. Calling <code>nextLine('-')</code>
     * from the first column of a row is a convenient way to fill this row
     * with a line separator.
     *
     * @param  fill Character filling the rest of the line
     *         (default to whitespace). This caracter may
     *         be use as a row separator.
     */
    public void nextLine(final char fill)
    {
        synchronized (lock)
        {
            if (buffer.length()!=0)
            {
                nextColumn(fill);
            }
            assert buffer.length()==0;
            cells.add(!Character.isSpaceChar(fill) ? new Cell(null, alignment, fill) : null);
            column=0;
            row++;
        }
    }

    /**
     * Flush the table content to the underlying stream.
     * This method should not be called before the table
     * is completed (otherwise, columns may have the
     * wrong width).
     *
     * @throws IOException if an output operation failed.
     */
    public void flush() throws IOException
    {
        synchronized (lock)
        {
            if (buffer.length()!=0)
            {
                nextLine();
                assert buffer.length()==0;
            }
            flushTo(out);
            row=column=0;
            cells.clear();
            out.flush();
        }
    }

    /**
     * Flush the table content and close the underlying stream.
     *
     * @throws IOException if an output operation failed.
     */
    public void close() throws IOException
    {
        synchronized (lock)
        {
            flush();
            out.close();
        }
    }

    /**
     * Ecrit vers le flot spécifié toutes les cellules qui avaient été disposées
     * dans le tableau. Ces cellules seront automatiquement alignées en colonnes.
     * Cette méthode peut être appelée plusieurs fois pour écrire le même tableau
     * par exemple vers plusieurs flots.
     *
     * @param  out Flot vers où écrire les données.
     * @throws IOException si une erreur est survenue lors de l'écriture dans <code>out</code>.
     */
    private void flushTo(final Writer out) throws IOException
    {
        final String columnSeparator = this.separator;
        final String   lineSeparator = System.getProperty("line.separator", "\n");
        final Cell[]     currentLine = new Cell[width.length];
        final int          cellCount = cells.size();
        for (int cellIndex=0; cellIndex<cellCount; cellIndex++)
        {
            /*
             * Copie dans  <code>currentLine</code>  toutes les données qui seront à écrire
             * sur la ligne courante de la table. Ces données excluent le <code>null</code>
             * terminal.  La liste <code>currentLine</code> ne contiendra donc initialement
             * aucun élément nul, mais ses éléments seront progressivement modifiés (et mis
             * à <code>null</code>) pendant l'écriture de la ligne dans la boucle qui suit.
             */
            Cell lineFill=null;
            int currentCount=0;
            do
            {
                final Cell cell=cells.get(cellIndex);
                if (cell==null) break;
                if (cell.text==null)
                {
                    lineFill = new Cell("", cell.alignment, cell.fill);
                    break;
                }
                currentLine[currentCount++] = cell;
            }
            while (++cellIndex<cellCount);
            Arrays.fill(currentLine, currentCount, currentLine.length, lineFill);
            /*
             * La boucle suivante sera exécutée tant qu'il reste des lignes à écrire
             * (c'est-à-dire tant qu'au moins un élément de <code>currentLine</code>
             * est non-nul). Si une cellule contient un texte avec des caractères EOL,
             * alors cette cellule devra s'écrire sur plusieurs lignes dans la cellule
             * courante.
             */
            while (!isEmpty(currentLine))
            {
                for (int j=0; j<currentLine.length; j++)
                {
                    final boolean isFirstColumn = (j   == 0);
                    final boolean isLastColumn  = (j+1 == currentLine.length);
                    final Cell cell = currentLine[j];
                    final int cellWidth = width[j];
                    if (cell==null)
                    {
                        if (isFirstColumn)
                            out.write(leftBorder);
                        repeat(out, SPACE, cellWidth);
                        out.write(isLastColumn ? rightBorder : columnSeparator);
                        continue;
                    }
                    String cellText = cell.toString();
                    int endCR=cellText.indexOf('\r');
                    int endLF=cellText.indexOf('\n');
                    int end=(endCR<0) ? endLF : (endLF<0) ? endCR : Math.min(endCR,endLF);
                    if (end>=0)
                    {
                        /*
                         * Si un retour chariot a été trouvé, n'écrit que la première
                         * ligne de la cellule. L'élément <code>currentLine[j]</code>
                         * sera modifié pour ne contenir que les lignes restantes qui
                         * seront écrites lors d'un prochain passage dans la boucle.
                         */
                        int top = end+1;
                        if (endCR>=0 && endCR+1==endLF) top++;
                        int scan = top;
                        final int textLength = cellText.length();
                        while (scan<textLength && Character.isWhitespace(cellText.charAt(scan))) scan++;
                        currentLine[j] = (scan<textLength) ? cell.substring(top) : null;
                        cellText = cellText.substring(0, end);
                    }
                    else currentLine[j] = null;
                    final int textLength = cellText.length();
                    /*
                     * Si la cellule à écrire est en fait une bordure,
                     * on fera un traitement spécial pour utiliser les
                     * caractères de jointures {@link #BOX}.
                     */
                    if (currentCount==0)
                    {
                        assert textLength==0;
                        final int verticalBorder;
                        if      (cellIndex==0)           verticalBorder = -1;
                        else if (cellIndex>=cellCount-1) verticalBorder = +1;
                        else                             verticalBorder =  0;
                        if (isFirstColumn)
                            writeBorder(out, -1, verticalBorder, cell.fill);
                        repeat(out, cell.fill, cellWidth);
                        writeBorder(out, isLastColumn ? +1 : 0, verticalBorder, cell.fill);
                        continue;
                    }
                    /*
                     * Si la cellule n'est pas une bordure, il s'agit
                     * d'une cellule "normale".  Procède maintenant à
                     * l'écriture d'une ligne de la cellule.
                     */
                    if (isFirstColumn)
                        out.write(leftBorder);
                    final Writer tabExpander = (cellText.indexOf('\t')>=0) ? new TabExpanderWriter(out) : out;
                    switch (cell.alignment)
                    {
                        default:
                        {
                            // Should not happen.
                            throw new AssertionError(cell.alignment);
                        }
                        case ALIGN_LEFT:
                        {
                            tabExpander.write(cellText);
                            repeat(tabExpander, cell.fill, cellWidth-textLength);
                            break;
                        }
                        case ALIGN_RIGHT:
                        {
                            repeat(tabExpander, cell.fill, cellWidth-textLength);
                            tabExpander.write(cellText);
                            break;
                        }
                        case ALIGN_CENTER:
                        {
                            final int rightMargin = (cellWidth-textLength)/2;
                            repeat(tabExpander, cell.fill, rightMargin);
                            tabExpander.write(cellText);
                            repeat(tabExpander, cell.fill, (cellWidth-rightMargin)-textLength);
                            break;
                        }
                    }
                    out.write(isLastColumn ? rightBorder : columnSeparator);
                }
                out.write(lineSeparator);
            }
        }
    }

    /**
     * Indique si le tableau <code>array</code>
     * ne contient que des éléments nuls.
     */
    private static boolean isEmpty(final Object[] array)
    {
        for (int i=array.length; --i>=0;)
            if (array[i]!=null) return false;
        return true;
    }

    /**
     * Répete un caractères plusieurs fois.
     *
     * @param out Flot vers où écrire les caractères.
     * @param car Caractère à écrire (habituellement ' ').
     * @param largeur nombre de caractères à écrire.
     */
    private static void repeat(final Writer out, final char car, int largeur) throws IOException
    {
        while (largeur>0)
        {
            out.write(car);
            largeur--;
        }
    }

    /**
     * Returns the table content as a string.
     */
    public String toString()
    {
        synchronized (lock)
        {
            int capacity = 2; // Room for EOL.
            for (int i=0; i<width.length; i++)
            {
                capacity += width[i];
            }
            capacity *= getRowCount();
            final StringWriter writer;
            if (stringOnly)
            {
                writer = (StringWriter) out;
                final StringBuffer buffer=writer.getBuffer();
                buffer.setLength(0);
                buffer.ensureCapacity(capacity);
            }
            else writer = new StringWriter(capacity);
            try
            {
                flushTo(writer);
            }
            catch (IOException exception)
            {
                // Should not happen
                final AssertionError error = new AssertionError(exception.getLocalizedMessage());
                error.initCause(exception);
                throw error;
            }
            return writer.toString();
        }
    }

    /**
     * Classe enveloppant une chaîne de caractères et son alignement.
     * Elle est réservée à un usage strictement interne.
     *
     * @version 1.1
     * @author Martin Desruisseaux
     */
    private static final class Cell
    {
        /**
         * Texte de la cellule.
         */
        public final String text;

        /**
         * Constante représentant l'alignement
         * du texte {@link #text}.
         */
        public final int alignment;

        /**
         * Caractère à utiliser pour remplir les
         * espaces manquant dans cette cellule.
         */
        public final char fill;

        /**
         * Construit un objet enveloppant la chaîne
         * spécifiée avec l'alignement spécifié.
         */
        public Cell(final String text, final int alignment, final char fill)
        {
            this.text            = text;
            this.alignment       = alignment;
            this.fill            = fill;
        }

        /**
         * Returns a new cell which contains substring of this cell.
         */
        public Cell substring(final int lower)
        {return new Cell(text.substring(lower), alignment, fill);}

        /**
         * Retourne le contenu de la cellule.
         */
        public String toString()
        {return text;}
    }
}
