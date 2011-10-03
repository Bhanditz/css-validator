/*
 * CssValidator.java
 *
 * Created on April 19, 2006, 2:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.w3c.css.css;

import org.w3c.css.parser.CssSelectors;
import org.w3c.css.util.ApplContext;
import org.w3c.css.util.CssVersion;
import org.w3c.css.util.HTTPURL;
import org.w3c.css.util.Util;
import org.w3c.tools.resources.ProtocolException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author jean
 */
public class CssValidator {

    ApplContext ac;
    PrintWriter out;
    ArrayList<String> uris = new ArrayList<String>();
    HashMap<String, String> params;

    // @@ HACK
    static boolean showCSS = false;

    private Exception exception;

    /**
     * Creates a new instance of CssValidator with the following default values:
     * <ul>
     * <li>profile = css21</li>
     * <li>medium = all</li>
     * <li>output = text</li>
     * <li>lang = en</li>
     * <li>warning = 2</li>
     * <li>showCSS = false</li>
     * </ul>
     */
    public CssValidator() {
        params = new HashMap<String, String>();
        params.put("profile", CssVersion.getDefault().toString());
        params.put("medium", "all");
        params.put("output", "text");
        params.put("lang", "en");
        params.put("warning", "2");
        params.put("vextwarning", "false");
    }

    public static void main(String args[])
            throws IOException, MalformedURLException {

        CssSelectors selector = null;

        CssValidator style = new CssValidator();

        // first, we get the parameters and create an application context
        try {
            style.getParams(args);
            style.ac = new ApplContext((String) style.params.get("lang"));
            System.out.println(style.params);
        } catch (Exception e) {
            System.out.println("Usage: java org.w3c.css.css.CssValidator " +
                    " [OPTIONS] | [URL]*");
            System.out.println("OPTIONS");
            System.out.println("\t-p, --printCSS");
            System.out.println("\t\tPrints the validated CSS (only with text " +
                    "output, the CSS is printed with other outputs)");
            System.out.println("\t-profile PROFILE, --profile=PROFILE");
            System.out.println("\t\tChecks the Stylesheet against PROFILE");
            System.out.println("\t\tPossible values for PROFILE are css1, " +
                    "css2, css21 (default), css3, svg, svgbasic, svgtiny, " +
                    "atsc-tv, mobile, tv");
            System.out.println("\t-medium MEDIUM, --medium=MEDIUM");
            System.out.println("\t\tChecks the Stylesheet using the medium MEDIUM");
            System.out.println("\t\tPossible values for MEDIUM are all " +
                    "(default), aural, braille, embossed, handheld, print, " +
                    "projection, screen, tty, tv, presentation");
            System.out.println("\t-output OUTPUT, --output=OUTPUT");
            System.out.println("\t\tPrints the result in the selected format");
            System.out.println("\t\tPossible values for OUTPUT are text (default), xhtml, " +
                    "html (same result as xhtml), soap12");
            System.out.println("\t-lang LANG, --lang=LANG");
            System.out.println("\t\tPrints the result in the specified language");
            System.out.println("\t\tPossible values for LANG are " +
                    "de, en (default), es, fr, ja, ko, nl, zh-cn, pl, it");
            System.out.println("\t-warning WARN, --warning=WARN");
            System.out.println("\t\tWarnings verbosity level");
            System.out.println("\t\tPossible values for WARN are -1 (no " +
                    "warning), 0, 1, 2 (default, all the warnings");
            System.out.println("\t-vextwarning true, --vextwarning=true");
            System.out.println("\t\tTreat Vendor Extensions as warnings");
            System.out.println("\t\tPossible values for vextwarning are true or false " +
                    "(default, is false");
            System.out.println();
            System.out.println("URL");
            System.out.println("\tURL can either represent a distant " +
                    "web resource (http://) or a local file (file:/)");
            System.exit(1);
        }

        // CSS version to use
        String profile = (String) style.params.get("profile");
        style.ac.setCssVersionAndProfile(profile);

        // medium to use
        style.ac.setMedium((String) style.params.get("medium"));

        String vextwarn = (String) style.params.get("vextwarning");
        style.ac.setTreatVendorExtensionsAsWarnings("true".equalsIgnoreCase(vextwarn));

        String encoding = style.ac.getMsg().getString("output-encoding-name");
        if (encoding != null) {
            style.out = new PrintWriter(new OutputStreamWriter(System.out, encoding));
        } else {
            style.out = new PrintWriter(new OutputStreamWriter(System.out));
        }

        for (int i = 0; i < style.uris.size(); i++) {
            String uri = (String) style.uris.get(i);

            if (uri != null) {
                // HTML document
                try {
                    uri = HTTPURL.getURL(uri).toString(); // needed to be sure
                    // that it is a valid
                    // url
                    DocumentParser URLparser = new DocumentParser(style.ac,
                            uri);

                    style.handleRequest(style.ac, uri, URLparser.getStyleSheet(),
                            (String) style.params.get("output"),
                            Integer.parseInt((String) style.params.get("warning")),
                            true);
                } catch (ProtocolException pex) {
                    if (Util.onDebug) {
                        pex.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleRequest(ApplContext ac,
                               String title, StyleSheet styleSheet,
                               String output, int warningLevel,
                               boolean errorReport) throws Exception {

        if (styleSheet == null) {
            throw new IOException(ac.getMsg().getServletString("process") + " "
                    + title);
        }

        styleSheet.findConflicts(ac);

        StyleReport style = StyleReportFactory.getStyleReport(ac, title,
                styleSheet,
                output,
                warningLevel);

        if (!errorReport) {
            style.desactivateError();
        }

        style.print(out);

    }

    /**
     * Analyses the command-line parameters
     *
     * @param args the parameters list
     */
    private void getParams(String[] args) throws Exception {

        // first, we get an enumeration from the array, because it's easier to
        // manage
        Iterator<String> iterator = Arrays.asList(args).iterator();
        while (iterator.hasNext()) {
            String paramName = "";
            String paramValue = "";

            // the current command-line argument
            String param = iterator.next();

            if (param.equals("--printCSS") || param.equals("-p")) {
                // special case
                showCSS = true;
            }
            // all the parameters have the form --param=PARAM...
            else if (param.startsWith("--")) {
                int separator = param.indexOf("=");
                paramName = param.substring(2, separator);
                paramValue = param.substring(separator + 1);
            }
            // ... or -param PARAM
            else if (param.startsWith("-")) {
                paramName = param.substring(1);
                if (iterator.hasNext()) {
                    paramValue = iterator.next();
                } else {
                    paramValue = "";
                }
            }
            // this is not a parameter, so it's probably a URL
            else {
                uris.add(param);
            }

            if (paramName.length() != 0 && params.containsKey(paramName)) {
                if (paramValue.length() == 0) {
                    // empty value
                    throw new Exception("You must specify a value for the "
                            + "parameter " + paramName);
                } else {
                    // update the params table
                    params.put(paramName, paramValue);
                }
            }
        }
    }
}
