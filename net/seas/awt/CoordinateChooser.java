/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.awt;

// Time
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

// Geometry and coordinates
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import net.seas.opengis.pt.Angle;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.util.XDimension2D;
import net.seas.util.XRectangle2D;

// User interface (Swing)
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import net.seas.util.SwingUtilities;

// Events
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Parsing and formating
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import net.seas.text.AngleFormat;

// Miscellaneous
import java.util.Arrays;
import net.seas.resources.Resources;


/**
 * A pane of controls designed to allow a user to select spatio-temporal coordinates.
 * Current implementation use geographic coordinates (longitudes/latitudes) and dates
 * according some locale calendar. Future version may allow the use of some user-specified
 * coordinate system.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/widget/CoordinateChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CoordinateChooser extends JPanel
{
    /**
     * R�solution par d�faut propos�e � l'utilisateur,
     * en degr�s de longitude et de latitude.
     */
    private static final double DEFAULT_RESOLUTION = 6;

    /**
     * Largeur � donner par d�faut aux champs de dates.
     */
    private static final int TIME_WIDTH = 10;

    /**
     * Largeur � donner par d�faut aux champs de coordonn�es.
     */
    private static final int COORD_WIDTH = 8;

    /**
     * Largeur � donner par d�faut aux champs de r�solutions.
     */
    private static final int RES_WIDTH = 3;

    /**
     * Formatteur � utiliser pour lire et interpr�ter
     * les dates dans les champs de dates.
     */
    private final DateFormat dateFormat=DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    /**
     * Formatteur � utiliser pour lire et interpr�ter
     * les angles les champs de coordonn�es.
     */
    private final AngleFormat angleFormat=new AngleFormat();

    /**
     * Format � utiliser pour lire et
     * �crire la r�solution d�sir�e.
     */
    private final NumberFormat numberFormat=NumberFormat.getNumberInstance();

    /**
     * Champ dans lequel l'utilisateur entrera la
     * date de d�but de la plage de temps qui l'int�resse.
     */
    private final JTextField startTimeEd=new JTextField(TIME_WIDTH);

    /**
     * Champ dans lequel l'utilisateur entrera la
     * date de fin de la plage de temps qui l'int�resse.
     */
    private final JTextField endTimeEd=new JTextField(TIME_WIDTH);

    /**
     * Liste de choix dans laquelle l'utilisateur
     * choisira le fuseau horaire de ses dates.
     */
    private final JComboBox timezoneEd;

    /**
     * Champ dans lequel l'utilisateur entrera la longitude
     * minimale de la r�gion qui l'int�resse.
     */
    private final JTextField xminEd=new JTextField(COORD_WIDTH);

    /**
     * Champ dans lequel l'utilisateur entrera la longitude
     * minimale de la r�gion qui l'int�resse.
     */
    private final JTextField xmaxEd=new JTextField(COORD_WIDTH);

    /**
     * Champ dans lequel l'utilisateur entrera la longitude
     * minimale de la r�gion qui l'int�resse.
     */
    private final JTextField yminEd=new JTextField(COORD_WIDTH);

    /**
     * Champ dans lequel l'utilisateur entrera la longitude
     * minimale de la r�gion qui l'int�resse.
     */
    private final JTextField ymaxEd=new JTextField(COORD_WIDTH);

    /**
     * Champ dans lequel l'utilisateur entrera la largeur
     * d�sir�e des pixels, en minutes d'angle.
     */
    private final JTextField pixelWidthEd;

    /**
     * Champ dans lequel l'utilisateur entrera la hauteur
     * d�sir�e des pixels, en minutes d'angle.
     */
    private final JTextField pixelHeightEd;

    /**
     * Bouton radio pour s�lectioner la meilleure r�solution possible.
     */
    private final AbstractButton radioBestRes=new JRadioButton(Resources.format(Cl�.USE_BEST_RESOLUTION));

    /**
     * Bouton radio pour s�lectioner la r�solution sp�cifi�e.
     */
    private final AbstractButton radioPrefRes=new JRadioButton(Resources.format(Cl�.SET_PREFERRED_RESOLUTION), true);

    /**
     * Composante qui affiche l'arborescence des s�ries, ou <code>null</code> s'il n'y en a pas.
     * Ce champ peut �tre mis � <code>null</code> plus tard apr�s la construction par un appel �
     * la m�thode {@link #disposeSeriesPanel}.
     */
    private JComponent accessory;

    /**
     * Coordonn�es g�ographiques s�lectionn�es par l'utilisateur.
     */
    private final Rectangle2D geographicArea=new XRectangle2D();

    /**
     * Date de d�but d'�chantillonage s�lectionn�e par l'utilisateur.
     */
    private long startTime;

    /**
     * Date de fin d'�chantillonage s�lectionn�e par l'utilisateur.
     */
    private long endTime;

    /**
     * R�solution pr�f�r�e des images.
     */
    private Dimension2D preferredResolution;

    /**
     * Construct a default coordinate chooser.
     */
    public CoordinateChooser()
    {
        super(new GridBagLayout());

        final JSpinner xResSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_RESOLUTION, 0, 120, 1));
        final JSpinner yResSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_RESOLUTION, 0, 120, 1));

        final JTextField xResField = ((JSpinner.DefaultEditor)xResSpinner.getEditor()).getTextField();
        final JTextField yResField = ((JSpinner.DefaultEditor)yResSpinner.getEditor()).getTextField();

        xResField.setColumns(RES_WIDTH);
        yResField.setColumns(RES_WIDTH);

        pixelWidthEd  = xResField;
        pixelHeightEd = yResField;

        final String[] timezones=TimeZone.getAvailableIDs();
        Arrays.sort(timezones);
        timezoneEd=new JComboBox(timezones);
        timezoneEd.setSelectedItem(dateFormat.getTimeZone().getID());

        final String defaultResolution=numberFormat.format(DEFAULT_RESOLUTION);
        pixelWidthEd .setText(defaultResolution);
        pixelHeightEd.setText(defaultResolution);

        final JLabel labelSize1=new JLabel(Resources.label(Cl�.SIZE_IN_MINUTES));
        final JLabel labelSize2=new JLabel("\u00D7"  /*Symbole multiplication*/);
        final ButtonGroup group=new ButtonGroup();
        group.add(radioBestRes);
        group.add(radioPrefRes);
        radioPrefRes.addChangeListener(new ChangeListener()
        {
            public void stateChanged(final ChangeEvent event)
            {
                final boolean state=radioPrefRes.isSelected();
                labelSize1   .setEnabled(state);
                labelSize2   .setEnabled(state);
                pixelWidthEd .setEnabled(state);
                pixelHeightEd.setEnabled(state);
            }
        });

        final JPanel p1=getPanel(Cl�.GEOGRAPHIC_COORDINATES);
        final JPanel p2=getPanel(Cl�.TIME_RANGE            );
        final JPanel p3=getPanel(Cl�.PREFERRED_RESOLUTION  );
        final GridBagConstraints c=new GridBagConstraints();

        c.weightx=1;
        c.gridx=1; c.gridy=0; p1.add(ymaxEd, c);
        c.gridx=0; c.gridy=1; p1.add(xminEd, c);
        c.gridx=2; c.gridy=1; p1.add(xmaxEd, c);
        c.gridx=1; c.gridy=2; p1.add(yminEd, c);

        JLabel label;
        c.gridx=0; c.anchor=c.WEST; c.insets.right=3; c.weightx=0;
        c.gridy=0; p2.add(label=new JLabel(Resources.label(Cl�.START_TIME)), c); label.setLabelFor(  startTimeEd);
        c.gridy=1; p2.add(label=new JLabel(Resources.label(Cl�.END_TIME  )), c); label.setLabelFor(    endTimeEd);
        c.gridy=2; p2.add(label=new JLabel(Resources.label(Cl�.TIME_ZONE )), c); label.setLabelFor(   timezoneEd); c.gridwidth=4;
        c.gridy=0; p3.add(radioBestRes,  c);
        c.gridy=1; p3.add(radioPrefRes,  c);
        c.gridy=2; c.gridwidth=1; c.anchor=c.EAST; c.insets.right=c.insets.left=1; c.weightx=1;
        c.gridx=0; p3.add(labelSize1,    c); labelSize1.setLabelFor(xResSpinner);  c.weightx=0;
        c.gridx=1; p3.add(xResSpinner,   c);
        c.gridx=2; p3.add(labelSize2,    c); labelSize2.setLabelFor(yResSpinner);
        c.gridx=3; p3.add(yResSpinner,   c);

        c.gridx=1; c.fill=c.HORIZONTAL; c.insets.right=c.insets.left=0; c.weightx=1;
        c.gridy=0; p2.add(startTimeEd, c);
        c.gridy=1; p2.add(  endTimeEd, c);
        c.gridy=2; p2.add( timezoneEd, c);

        c.insets.right=c.insets.left=c.insets.top=c.insets.bottom=3;
        c.gridx=0; c.anchor=c.CENTER; c.fill=c.BOTH; c.weighty=1;
        c.gridy=0; add(p1, c);
        c.gridy=1; add(p2, c);
        c.gridy=2; add(p3, c);

        setGeographicArea(new Rectangle2D.Float(-180, -90, 360, 180));
        setTimeRange(new Date(0), new Date());
        setPreferredResolution(null);
    }

    /**
     * Retourne un panneau avec une bordure titr�e.
     */
    private static JPanel getPanel(final int title)
    {
        final JPanel panel=new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(Resources.format(title)),
                        BorderFactory.createEmptyBorder(6,6,6,6)));
        return panel;
    }

    /**
     * Gets the geographic area, in latitude and longitude degrees.
     */
    public Rectangle2D getGeographicArea()
    {return (Rectangle2D) geographicArea.clone();}

    /**
     * Sets the geographic area, in latitude and longitude degrees.
     */
    public void setGeographicArea(final Rectangle2D area)
    {
        xminEd.setText(angleFormat.format(new Longitude(area.getMinX())));
        xmaxEd.setText(angleFormat.format(new Longitude(area.getMaxX())));
        yminEd.setText(angleFormat.format(new  Latitude(area.getMinY())));
        ymaxEd.setText(angleFormat.format(new  Latitude(area.getMaxY())));
        this.geographicArea.setRect(area);
    }

    /**
     * Returns the preferred resolution. A <code>null</code>
     * value means that the best available resolution should
     * be used.
     */
    public Dimension2D getPreferredResolution()
    {return (preferredResolution!=null) ? (Dimension2D) preferredResolution.clone() : null;}

    /**
     * Sets the preferred resolution. A <code>null</code>
     * value means that the best available resolution should
     * be used.
     */
    public void setPreferredResolution(final Dimension2D resolution)
    {
        if (resolution!=null)
        {
            pixelWidthEd .setText(numberFormat.format(resolution.getWidth ()*60));
            pixelHeightEd.setText(numberFormat.format(resolution.getHeight()*60));
            radioPrefRes.setSelected(true);
        }
        else radioBestRes.setSelected(true);
    }

    /**
     * Returns the time zone used for displaying dates.
     */
    public TimeZone getTimeZone()
    {return dateFormat.getTimeZone();}

    /**
     * Sets the time zone. This method change the control's display.
     * It doesn't change the date values, i.e. it have no effect
     * on previous or future call to {@link #setTimeRange}.
     */
    public void setTimeZone(final TimeZone timezone)
    {
        dateFormat.setTimeZone(timezone);
        timezoneEd.setSelectedItem(timezone.getID());
        startTimeEd.setText(dateFormat.format(new Date(startTime)));
          endTimeEd.setText(dateFormat.format(new Date(  endTime)));
    }

    /**
     * Returns the start time, or <code>null</code> if there is none.
     */
    public Date getStartTime()
    {return new Date(startTime);}

    /**
     * Returns the end time, or <code>null</code> if there is none.
     */
    public Date getEndTime()
    {return new Date(endTime);}

    /**
     * Sets the time range.
     *
     * @param startTime The start time.
     * @param   endTime The end time.
     *
     * @see #getStartTime
     * @see #getEndTime
     */
    public void setTimeRange(final Date startTime, final Date endTime)
    {
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneEd.getSelectedItem().toString()));
        startTimeEd.setText(dateFormat.format(startTime));
          endTimeEd.setText(dateFormat.format(  endTime));
        this.startTime = startTime.getTime();
        this.  endTime =   endTime.getTime();
    }

    /**
     * Returns the accessory component.
     *
     * @return The accessory component, or <code>null</code> if there is none.
     */
    public JComponent getAccessory()
    {return accessory;}

    /**
     * Sets the accessory component. An accessory is often used to show available data.
     * However, it can be used for anything that the programmer wishes, such as extra
     * custom coordinate chooser controls.
     * <br><br>
     * Note: If there was a previous accessory, you should unregister
     * any listeners that the accessory might have registered with the
     * coordinate chooser.
     *
     * @param The accessory component, or <code>null</code> to remove any previous accessory.
     */
    public void setAccessory(final JComponent accessory)
    {
        synchronized (getTreeLock())
        {
            if (this.accessory!=null)
            {
                remove(this.accessory);
            }
            this.accessory = accessory;
            if (accessory!=null)
            {
                final GridBagConstraints c=new GridBagConstraints();
                c.insets.right=c.insets.left=c.insets.top=c.insets.bottom=3;
                c.gridx=1; c.weightx=1; c.gridwidth=1;
                c.gridy=0; c.weighty=1; c.gridheight=3;
                c.anchor=c.CENTER; c.fill=c.BOTH;
                add(accessory, c);
            }
            validate();
        }
    }

    /**
     * Proc�de � la lecture d'un angle �crit dans
     * un champ, puis retourne sa valeur en degr�s.
     */
    private double parseAngle(final JTextField field, final Class forbid) throws ParseException
    {
        final Angle angle=angleFormat.parse(field.getText());
        if (forbid.isAssignableFrom(angle.getClass()))
        {
            throw new ParseException(Resources.format(Cl�.BAD_COORDINATE�1, angle), 0);
        }
        return angle.degrees();
    }

    /**
     * Prend en compte les valeurs des champs �dit�s par l'utilisateur.
     *
     * @throws ParseException si une coordonn�e, une date ou une s�rie n'a
     *         pas pu �tre interpr�t�e. Dans ce cas, le focus sera plac� sur
     *         le champ fautif.
     */
    private void commitEdit() throws ParseException
    {
        JTextField focus=null;
        try
        {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneEd.getSelectedItem().toString()));
            final double        xmin = parseAngle(focus=xminEd,  Latitude.class);
            final double        xmax = parseAngle(focus=xmaxEd,  Latitude.class);
            final double        ymin = parseAngle(focus=yminEd, Longitude.class);
            final double        ymax = parseAngle(focus=ymaxEd, Longitude.class);
            final Date         start = dateFormat.parse((focus=  startTimeEd).getText());
            final Date           end = dateFormat.parse((focus=    endTimeEd).getText());
            final Number        xres = (pixelWidthEd .isEnabled()) ? numberFormat.parse((focus= pixelWidthEd).getText()) : null;
            final Number        yres = (pixelHeightEd.isEnabled()) ? numberFormat.parse((focus=pixelHeightEd).getText()) : null;
            this.          startTime = Math.min(start.getTime(), end.getTime());
            this.            endTime = Math.max(start.getTime(), end.getTime());
            this.preferredResolution = (xres!=null && yres!=null) ? new XDimension2D.Double(xres.doubleValue()/60, yres.doubleValue()/60) : null;
            geographicArea.setRect(Math.min(xmin, xmax), Math.min(ymin, ymax), Math.abs(xmax-xmin), Math.abs(ymax-ymin));
        }
        catch (ParseException exception)
        {
            focus.requestFocus();
            throw exception;
        }
    }

    /**
     * Prend en compte les valeurs des champs �dit�s par l'utilisateur.
     * Si les entr�s ne sont pas valide, affiche un message d'erreur en
     * utilisant la fen�tre parente <code>owner</code> sp�cifi�e.
     *
     * @param  owner Fen�tre dans laquelle faire appara�tre d'eventuels messages d'erreur.
     * @return <code>true</code> si la prise en compte des param�tres � r�ussie.
     */
    private boolean commitEdit(final Component owner)
    {
        try
        {
            commitEdit();
        }
        catch (ParseException exception)
        {
//          JOptionPane.showInternalMessageDialog(owner, exception.getLocalizedMessage(), Resources.format(Cl�.BAD_ENTRY), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Fait appara�tre ce panneau dans une bo�te de dialogue avec
     * les boutons "ok" et "annuler". Retourne <code>true</code>
     * si l'utilisateur a appuy� sur "ok", et <code>false</code>
     * sinon. Cette m�thode peut �tre appel�e de n'importe quel
     * thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param  owner Fen�tre dans laquelle faire appara�tre la bo�te de dialogue.
     * @return <code>true</code> si l'utilisateur a cliqu� sur "Ok".
     */
    public boolean showDialog(final Component owner)
    {return showDialog(owner, Resources.format(Cl�.COORDINATES_SELECTION));}

    /**
     * Fait appara�tre ce panneau dans une bo�te de dialogue avec
     * les boutons "ok" et "annuler" ainsi que le titre sp�cifi�.
     * Retourne <code>true</code> si l'utilisateur a appuy� sur
     * "ok", et <code>false</code> sinon. Cette m�thode peut �tre
     * appel�e de n'importe quel thread (pas n�cessairement celui
     * de <i>Swing</i>).
     *
     * @param  owner Fen�tre dans laquelle faire appara�tre la bo�te de dialogue.
     * @param  title Titre de la bo�te de dialogue.
     * @return <code>true</code> si l'utilisateur a cliqu� sur "Ok".
     */
    public boolean showDialog(final Component owner, final String title)
    {
        while (SwingUtilities.showOptionDialog(owner, this, title))
            if (commitEdit(owner))
                return true;
        return false;
    }

    /**
     * Fait appara�tre la bo�te de dialogue et termine le programme.
     * Cette m�thode ne sert qu'� tester l'apparence de la bo�te.
     */
    public static void main(final String[] args)
    {
        new CoordinateChooser().showDialog(null);
        System.exit(0);
    }
}
