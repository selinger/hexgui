// ----------------------------------------------------------------------------
// $Id$
// ----------------------------------------------------------------------------

package hexgui.sgf;

import hexgui.game.GameInfo;
import hexgui.game.Node;
import hexgui.hex.HexColor;
import hexgui.hex.HexPoint;
import hexgui.hex.Move;
import hexgui.version.Version;
import java.awt.Dimension;
import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

// ----------------------------------------------------------------------------

/** SGF Writer. See https://www.red-bean.com/sgf/ for the SGF definition. */
public final class SgfWriter {

  /** Write a game tree. */
  public SgfWriter(OutputStream out, Node root, GameInfo game) {
    m_out = new PrintStream(out);
    m_buffer = new StringBuffer(128);
    m_gameinfo = game;

    writeTree(root, true);
    print("\n");
    flushBuffer();
    m_out.flush();
    m_out.close();
  }

  private void writeTree(Node root, boolean isroot) {
    print("(");
    writeNode(root, isroot);
    print(")");
  }

  private void writeNode(Node node, boolean isroot) {
    print(";");

    if (isroot) {
      String value;

      node.setSgfProperty("FF", "4");
      node.setSgfProperty("AP", "HexGui:" + Version.id);
      node.setSgfProperty("GM", "11");

      Dimension dim = m_gameinfo.getBoardSize();
      value = Integer.toString(dim.width);
      if (dim.width != dim.height) value += ":" + Integer.toString(dim.height);
      node.setSgfProperty("SZ", value);
    }

    if (node.getMove() != null) {
      printMove(node.getMove());
    }

    Map<String, String> map = node.getProperties();
    Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> e = it.next();
      if (!(e.getKey().equals("C") && e.getValue().equals(""))) {
        String val = e.getValue();
        if (e.getKey().equals("C")) {
          // For now we only escape comments, although there
          // may be other text values that should be escaped
          // too. Avoids escaping the ":" in AP field.
          val = escapeString(val);
        }
        print(e.getKey() + "[" + val + "]");
      }
    }

    if (node.hasSetup()) {
      Vector<HexPoint> list;
      list = node.getSetup(HexColor.BLACK);
      if (!list.isEmpty()) {
        print("AB");
        printPointList(list);
      }
      list = node.getSetup(HexColor.WHITE);
      if (!list.isEmpty()) {
        print("AW");
        printPointList(list);
      }
      list = node.getSetup(HexColor.EMPTY);
      if (!list.isEmpty()) {
        print("AE");
        printPointList(list);
      }
    }

    int num = node.numChildren();
    if (num == 0) return;

    if (num == 1) {
      writeNode(node.getChild(), false);
      return;
    }

    for (int i = 0; i < num; i++) {
      writeTree(node.getChild(i), false);
    }
  }

  private String escapeString(String s) {
    StringBuilder out = new StringBuilder(256);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\':
        case '[':
        case ']':
        case ':':
          out.append('\\');
          out.append(c);
          break;
        default:
          out.append(c);
      }
    }
    return out.toString();
  }

  private void printMove(Move move) {
    String color = "B";
    if (move.getColor() == HexColor.WHITE) color = "W";
    print(color + "[" + move.getPoint().toString() + "]");
  }

  private void printPointList(Vector<HexPoint> list) {
    for (int i = 0; i < list.size(); ++i) {
      print("[" + list.get(i).toString() + "]");
    }
  }

  private void print(String str) {
    if (m_buffer.length() + str.length() > 72) {
      m_out.append(m_buffer.toString());
      m_out.append("\n");
      m_buffer.setLength(0);
    }
    m_buffer.append(str);
  }

  private void flushBuffer() {
    m_out.append(m_buffer.toString());
    m_buffer.setLength(0);
  }

  private PrintStream m_out;
  private StringBuffer m_buffer;
  private GameInfo m_gameinfo;
}

// ----------------------------------------------------------------------------
