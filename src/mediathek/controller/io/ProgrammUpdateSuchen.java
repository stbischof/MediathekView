/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.controller.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import mediathek.daten.DDaten;
import mediathek.daten.Daten;
import mediathek.gui.dialog.DialogHinweisUpdate;
import mediathek.tool.DatumZeit;
import mediathek.tool.Konstanten;
import mediathek.tool.Log;

public class ProgrammUpdateSuchen {

    private final String PROGRAM_VERSION = "Program_Version";
    private final String PROGRAM_RELEASE_INFO = "Program_Release_Info";
    private final String DOWNLOAD_PROGRAM = "Download_Programm";
    private final String INFO = "Info";
    private final String INFO_NO = "number";
    private LinkedList<String[]> listInfos = new LinkedList<String[]>(); // String[] info = {Nummer, Info};

    public boolean checkVersion(DDaten ddaten, boolean anzeigen, boolean hinweis, boolean hinweiseAlleAnzeigen) {
        // prüft auf neue Version, aneigen: wenn true, dann AUCH wenn es keine neue Version gibt ein Fenster
        String version;
        String release;
        String downloadUrlProgramm;
        String[] ret;
        boolean neueVersion = false;
        try {
            ret = suchen();
            // Hinweise anzeigen
            if (hinweis) {
                hinweiseAnzeigen(hinweiseAlleAnzeigen);
            }
            // Update-Info anzeigen
            version = ret[0];
            release = ret[1];
            downloadUrlProgramm = ret[2];
            if (!version.equals("")) {
                Daten.system[Konstanten.SYSTEM_UPDATE_DATUM_NR] = DatumZeit.getHeute_yyyyMMdd();
                if (checkObNeueVersion(version, Konstanten.VERSION)) {
                    neueVersion = true;
                    // DialogHinweisUpdate(java.awt.Frame parent, boolean modal, String ttext, String dialogTitel, DDaten ddaten) {
                    new DialogHinweisUpdate(null, true, "Eine neue Version liegt vor",
                            "   ==================================================\n"
                            + "   Neue Version:\n" + "   " + version + "\n\n"
                            + "   ==================================================\n"
                            + "   Änderungen:\n" + "   " + release + "\n\n"
                            + "   ==================================================\n"
                            + "   URL:\n"
                            + "   " + downloadUrlProgramm + "\n\n").setVisible(true);
                } else {
                    DialogHinweisUpdate dialog = new DialogHinweisUpdate(null, true, "Update suchen", "Alles aktuell!");
                    if (anzeigen) {
                        dialog.setVisible(true);
                    }
                }
            } else {
                new DialogHinweisUpdate(null, true, "Fehler bei der Versionsprüfung!", "Es ist ein Fehler aufgetreten!" + "\n\n" + "").setVisible(true);
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(159002583, "ProgrammUpdateSuchen.checkVersion", ex);
        }
        return neueVersion;
    }

    private void hinweiseAnzeigen(boolean alleAnzeigen) {
        if (listInfos.size() > 0) {
            try {
                StringBuilder text = new StringBuilder();
                int angezeigt = 0;
                if (DDaten.system[Konstanten.SYSTEM_HINWEIS_NR_ANGEZEIGT_NR].equals("")) {
                    DDaten.system[Konstanten.SYSTEM_HINWEIS_NR_ANGEZEIGT_NR] = Integer.toString(-1);
                } else {
                    angezeigt = Integer.parseInt(DDaten.system[Konstanten.SYSTEM_HINWEIS_NR_ANGEZEIGT_NR]);
                }
                for (int i = 0; i < listInfos.size(); ++i) {
                    String[] h = listInfos.get(i);
                    if (alleAnzeigen || angezeigt < Integer.parseInt(h[0])) {
                        text.append(h[1]);
                        text.append("\n");
                        text.append("\n");
                    }
                }
                if (text.length() > 0) {
                    new DialogHinweisUpdate(null, true, "Infos", text.toString()).setVisible(true);
                    DDaten.system[Konstanten.SYSTEM_HINWEIS_NR_ANGEZEIGT_NR] = Integer.toString(listInfos.size());
                }
            } catch (Exception ex) {
                Log.fehlerMeldung(693298731, "ProgrammUpdateSuchen.checkVersion", ex);
            }
        }
    }

    private boolean checkObNeueVersion(String infoVersion, String ichVersion) {
        // liefert true, wenn es eine neue Version gibt
        try {
            // erste stelle
            int info = Integer.parseInt(infoVersion.substring(0, 1) + infoVersion.substring(2, 3) + infoVersion.substring(4, 5));
            int ich = Integer.parseInt(ichVersion.substring(0, 1) + ichVersion.substring(2, 3) + ichVersion.substring(4, 5));
            if (info > ich) {
                return true;
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(683021193, "ProgrammUpdateSuchen.checkObNeueVersion", ex);
        }
        return false;
    }

    private String[] suchen() throws MalformedURLException, IOException, XMLStreamException {
        String[] ret = new String[]{""/* version */, ""/* release */, ""/* updateUrl */};
        //String parsername = "";
        int event;
        XMLInputFactory inFactory = XMLInputFactory.newInstance();
        inFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        XMLStreamReader parser;
        InputStreamReader inReader;
        int timeout = 10000;
        URLConnection conn;
        conn = new URL(Konstanten.ADRESSE_PROGRAMM_VERSION).openConnection();
        conn.setRequestProperty("User-Agent", Daten.getUserAgent());
        conn.setReadTimeout(timeout);
        conn.setConnectTimeout(timeout);
        inReader = new InputStreamReader(conn.getInputStream(), Konstanten.KODIERUNG_UTF);
        parser = inFactory.createXMLStreamReader(inReader);
        while (parser.hasNext()) {
            event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                //parsername = parser.getLocalName();
                if (parser.getLocalName().equals(PROGRAM_VERSION)) {
                    ret[0] = parser.getElementText();
                } else if (parser.getLocalName().equals(PROGRAM_RELEASE_INFO)) {
                    ret[1] = parser.getElementText();
                } else if (parser.getLocalName().equals(DOWNLOAD_PROGRAM)) {
                    ret[2] = parser.getElementText();
                } else if (parser.getLocalName().equals(INFO)) {
                    int count = parser.getAttributeCount();
                    String nummer = "";
                    for (int i = 0; i < count; ++i) {
                        if (parser.getAttributeName(i).toString().equals(INFO_NO)) {
                            nummer = parser.getAttributeValue(i);
                        }
                    }
                    String info = parser.getElementText();
                    if (!nummer.equals("") && !info.equals("")) {
                        listInfos.add(new String[]{nummer, info});
                    }
                }
            }
        }
        return ret;
    }
}
