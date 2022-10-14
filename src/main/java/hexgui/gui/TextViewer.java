// TextViewer.java

package hexgui.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Dialog for displaying text. Allows syntax highlighting for analyze commands of type
 * <i>hstring</i>.
 */
public final class TextViewer extends JDialog {
  /** Callback for events generated by TextViewer. */
  public interface Listener {
    /**
     * Callback if some text is selected. If text is unselected again this function will be called
     * with the complete text content of the window.
     */
    void textSelected(String text);
  }

  public TextViewer(Frame owner, String title, String text, boolean highlight, Listener listener) {
    super(owner, title);
    initialize(text, highlight, listener);
  }

  public TextViewer(Dialog owner, String title, String text, boolean highlight, Listener listener) {
    super(owner, title);
    initialize(text, highlight, listener);
  }

  public void setText(String title, String text, boolean highlight) {
    setTitle(title);
    insertText(text);
    if (highlight) doSyntaxHighlight();
  }

  private JTextPane m_textPane;

  private Listener m_listener;

  private void insertText(String text) {
    Document doc = m_textPane.getDocument();
    while (text.charAt(text.length() - 1) == '\n') text = text.substring(0, text.length() - 1);
    try {
      doc.remove(0, doc.getLength());
      doc.insertString(0, text, null);
    } catch (BadLocationException e) {
      assert false;
    }
  }

  private void doSyntaxHighlight() {
    GuiUtil.addStyle(m_textPane, "title", null, null, true);
    GuiUtil.addStyle(m_textPane, "point", new Color(0.25f, 0.5f, 0.7f));
    GuiUtil.addStyle(m_textPane, "number", new Color(0f, 0.54f, 0f));
    GuiUtil.addStyle(m_textPane, "const", new Color(0.8f, 0f, 0f));
    GuiUtil.addStyle(m_textPane, "color", new Color(0.54f, 0f, 0.54f));
    m_textPane.setEditable(true);
    highlight("number", "\\b-?\\d+\\.?\\d*([Ee][+-]\\d+)?\\b");
    highlight("const", "\\b[A-Z_][A-Z_]+[A-Z]\\b");
    highlight("color", "\\b([Bb][Ll][Aa][Cc][Kk]|[Ww][Hh][Ii][Tt][Ee])\\b");
    highlight("point", "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1\\d|[1-9]))\\b");
    highlight("title", "^\\S+:(\\s|$)");
    m_textPane.setEditable(false);
  }

  private void highlight(String styleName, String regex) {
    Document doc = m_textPane.getDocument();
    Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    try {
      CharSequence text = doc.getText(0, doc.getLength());
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();
        GuiUtil.setStyle(m_textPane, start, end - start, styleName);
      }
    } catch (BadLocationException e) {
      assert false;
    }
  }

  private void initialize(String text, boolean highlight, Listener listener) {
    m_listener = listener;
    JPanel panel = new JPanel(new BorderLayout());
    Container contentPane = getContentPane();
    contentPane.add(panel, BorderLayout.CENTER);
    m_textPane = new JTextPane();
    GuiUtil.setMonospacedFont(m_textPane);
    insertText(text);
    JScrollPane scrollPane = new JScrollPane(m_textPane);
    panel.add(scrollPane, BorderLayout.CENTER);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    KeyListener keyListener =
        new KeyAdapter() {
          public void keyReleased(KeyEvent e) {
            int c = e.getKeyCode();
            if (c == KeyEvent.VK_ESCAPE) dispose();
          }
        };
    m_textPane.addKeyListener(keyListener);
    CaretListener caretListener =
        new CaretListener() {
          public void caretUpdate(CaretEvent event) {
            if (m_listener == null) return;
            int start = m_textPane.getSelectionStart();
            int end = m_textPane.getSelectionEnd();
            Document doc = m_textPane.getDocument();
            try {
              if (start == end) {
                String text = doc.getText(0, doc.getLength());
                m_listener.textSelected(text);
                return;
              }
              String text = doc.getText(start, end - start);
              m_listener.textSelected(text);
            } catch (BadLocationException e) {
              assert false;
            }
          }
        };
    m_textPane.addCaretListener(caretListener);
    if (highlight) doSyntaxHighlight();
    m_textPane.setCaretPosition(0);
    m_textPane.setEditable(false);
    packTextViewer();
  }

  private void packTextViewer() {
    pack();
    // Workaround for problems with oversized windows on some platforms
    // Also increase width by 10%, because automatic computation of
    // JTextPane/JTextArea width according to content text does not
    // work (maybe related to Sun Bug 4829215)
    Dimension size = getSize();
    size.width *= 1.1;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int maxHeight = (int) (0.9 * screenSize.height);
    int maxWidth = (int) (0.9 * screenSize.width);
    if (size.height > maxHeight || size.width > maxWidth) {
      size.width = Math.min(size.width, maxWidth);
      size.height = Math.min(size.height, maxHeight);
    }
    setMinimumSize(new Dimension(128, 96));
    size.height = Math.max(size.height, 96);
    size.width = Math.max(size.width, 128);
    setPreferredSize(size);
    setSize(size);
  }
}
