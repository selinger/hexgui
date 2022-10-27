// AnalyzeDefinition.java

package hexgui.htp;

import hexgui.util.ErrorMessage;
import hexgui.util.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Definition of an analyze command. See GoGui documentation, chapter "Analyze Commands". This class
 * is immutable.
 */
public class AnalyzeDefinition {
  public AnalyzeDefinition(String line) {
    String array[] = line.split("/");
    String typeStr = array[0];
    if (typeStr.equals("bwboard")) m_type = AnalyzeType.BWBOARD;
    else if (typeStr.equals("cboard")) m_type = AnalyzeType.CBOARD;
    else if (typeStr.equals("dboard")) m_type = AnalyzeType.DBOARD;
    else if (typeStr.equals("eplist")) m_type = AnalyzeType.EPLIST;
    else if (typeStr.equals("gfx")) m_type = AnalyzeType.GFX;
    else if (typeStr.equals("group")) m_type = AnalyzeType.GROUP;
    else if (typeStr.equals("hstring")) m_type = AnalyzeType.HSTRING;
    else if (typeStr.equals("hpstring")) m_type = AnalyzeType.HPSTRING;
    else if (typeStr.equals("inferior")) m_type = AnalyzeType.INFERIOR;
    else if (typeStr.equals("move")) m_type = AnalyzeType.MOVE;
    else if (typeStr.equals("param")) m_type = AnalyzeType.PARAM;
    else if (typeStr.equals("plist")) m_type = AnalyzeType.PLIST;
    else if (typeStr.equals("pspairs")) m_type = AnalyzeType.PSPAIRS;
    else if (typeStr.equals("pstring")) m_type = AnalyzeType.PSTRING;
    else if (typeStr.equals("string")) m_type = AnalyzeType.STRING;
    else if (typeStr.equals("sboard")) m_type = AnalyzeType.SBOARD;
    else if (typeStr.equals("var")) m_type = AnalyzeType.VAR;
    else if (typeStr.equals("varb")) m_type = AnalyzeType.VARB;
    else if (typeStr.equals("varc")) m_type = AnalyzeType.VARC;
    else if (typeStr.equals("varp")) m_type = AnalyzeType.VARP;
    else if (typeStr.equals("varpo")) m_type = AnalyzeType.VARPO;
    else if (typeStr.equals("varw")) m_type = AnalyzeType.VARW;
    else if (typeStr.equals("vc")) m_type = AnalyzeType.VC;
    else m_type = AnalyzeType.NONE;
    m_label = array[1];
    m_command = array[2];
  }

  public AnalyzeDefinition(AnalyzeType type, String label, String command) {
    m_type = type;
    m_label = label;
    m_command = command;
  }

  public String getCommand() {
    return m_command;
  }

  public String getLabel() {
    return m_label;
  }

  public AnalyzeType getType() {
    return m_type;
  }

  /**
   * Should the response be shown as text. Returns true for types that should be shown (not
   * necessarily only) as text to the user. That is string and variation commands.
   */
  public boolean isTextType() {
    return m_type == AnalyzeType.STRING
        || m_type == AnalyzeType.HSTRING
        || m_type == AnalyzeType.HPSTRING
        || m_type == AnalyzeType.PSTRING
        || m_type == AnalyzeType.VAR
        || m_type == AnalyzeType.VARC
        || m_type == AnalyzeType.VARW
        || m_type == AnalyzeType.VARB
        || m_type == AnalyzeType.VARP
        || m_type == AnalyzeType.VARPO;
  }

  public boolean needsColorArg() {
    return (m_command.indexOf("%c") >= 0);
  }

  public boolean needsFileArg() {
    return (m_command.indexOf("%f") >= 0);
  }

  public boolean needsFileOpenArg() {
    return (m_command.indexOf("%r") >= 0);
  }

  public boolean needsFileSaveArg() {
    return (m_command.indexOf("%w") >= 0);
  }

  public boolean needsOnlyPointArg() {
    return (needsPointArg()
        && !needsColorArg()
        && !needsFileArg()
        && !needsFileOpenArg()
        && !needsFileSaveArg()
        && !needsPointListArg()
        && !needsStringArg()
        && !needsOptStringArg());
  }

  public boolean needsOnlyPointAndColorArg() {
    return (needsPointArg()
        && needsColorArg()
        && !needsFileArg()
        && !needsFileOpenArg()
        && !needsFileSaveArg()
        && !needsPointListArg()
        && !needsStringArg()
        && !needsOptStringArg());
  }

  public boolean needsPointArg() {
    return (m_command.indexOf("%p") >= 0);
  }

  public boolean needsPointListArg() {
    return (m_command.indexOf("%P") >= 0 || m_type == AnalyzeType.EPLIST);
  }

  public boolean needsStringArg() {
    return (m_command.indexOf("%s") >= 0);
  }

  public boolean needsOptStringArg() {
    return (m_command.indexOf("%o") >= 0);
  }

  public static ArrayList<AnalyzeDefinition> read(String programAnalyzeCommands)
      throws ErrorMessage {
    if (programAnalyzeCommands == null) throw new ErrorMessage("no analyze commands!");

    Reader stringReader = new StringReader(programAnalyzeCommands);
    BufferedReader reader = new BufferedReader(stringReader);
    return readConfig(reader, "program response to hexgui-analyze_commands", null);
  }

  private final AnalyzeType m_type;

  private final String m_label;

  private final String m_command;

  private static ArrayList<AnalyzeDefinition> readConfig(
      BufferedReader reader, String name, ArrayList<String> supportedCommands) throws ErrorMessage {
    ArrayList<AnalyzeDefinition> result = new ArrayList<AnalyzeDefinition>();
    ArrayList<String> labels = new ArrayList<String>();
    try {
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        ++lineNumber;
        line = line.trim();
        if (line.length() > 0 && line.charAt(0) != '#') {
          String array[] = line.split("/");
          if (array.length < 3 || array.length > 5)
            throw new ErrorMessage("Error in " + name + " line " + lineNumber);
          if (supportedCommands != null) {
            String[] cmdArray = StringUtils.splitArguments(array[2].trim());
            if (cmdArray.length == 0 || !supportedCommands.contains(cmdArray[0])) continue;
          }
          String label = array[1];
          if (labels.contains(label)) continue;
          labels.add(label);
          result.add(new AnalyzeDefinition(line));
        }
      }
      return result;
    } catch (IOException e) {
      throw new ErrorMessage("Error reading " + name);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        throw new ErrorMessage("Error reading " + name);
      }
    }
  }
}
